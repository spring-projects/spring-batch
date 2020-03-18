/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.jsr.configuration.xml;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.jsr.configuration.support.JsrExpressionParser;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.util.ClassUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

/**
 * <p>
 * {@link DefaultBeanDefinitionDocumentReader} extension to hook into the pre processing of the provided
 * XML document, ensuring any references to property operators such as jobParameters and jobProperties are
 * resolved prior to loading the context. Since we know these initial values upfront, doing this transformation
 * allows us to ensure values are retrieved in their resolved form prior to loading the context and property
 * operators can be used on any element. This document reader will also look for references to artifacts by
 * the same name and create new bean definitions to provide the ability to create new instances.
 * </p>
 *
 * @author Chris Schaefer
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public class JsrBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {
	private static final String NULL = "null";
	private static final String ROOT_JOB_ELEMENT_NAME = "job";
	private static final String JOB_PROPERTY_ELEMENT_NAME = "property";
	private static final String JOB_PROPERTIES_ELEMENT_NAME = "properties";
	private static final String JOB_PROPERTY_ELEMENT_NAME_ATTRIBUTE = "name";
	private static final String JOB_PROPERTY_ELEMENT_VALUE_ATTRIBUTE = "value";
	private static final String JOB_PROPERTIES_KEY_NAME = "jobProperties";
	private static final String JOB_PARAMETERS_KEY_NAME = "jobParameters";
	private static final String JOB_PARAMETERS_BEAN_DEFINITION_NAME = "jsr_jobParameters";
	private static final Log LOG = LogFactory.getLog(JsrBeanDefinitionDocumentReader.class);
	private static final Pattern PROPERTY_KEY_SEPARATOR = Pattern.compile("'([^']*?)'");
	private static final Pattern OPERATOR_PATTERN = Pattern.compile("(#\\{(job(Properties|Parameters))[^}]+\\})");

	private BeanDefinitionRegistry beanDefinitionRegistry;
	private JsrExpressionParser expressionParser = new JsrExpressionParser();
	private Map<String, Properties> propertyMap = new HashMap<>();

	/**
	 * <p>
	 * Creates a new {@link JsrBeanDefinitionDocumentReader} instance.
	 * </p>
	 */
	public JsrBeanDefinitionDocumentReader() { }

	/**
	 * <p>
	 * Create a new {@link JsrBeanDefinitionDocumentReader} instance with the provided
	 * {@link BeanDefinitionRegistry}.
	 * </p>
	 *
	 * @param beanDefinitionRegistry the {@link BeanDefinitionRegistry} to use
	 */
	public JsrBeanDefinitionDocumentReader(BeanDefinitionRegistry beanDefinitionRegistry) {
		this.beanDefinitionRegistry = beanDefinitionRegistry;
	}

	@Override
	protected void preProcessXml(Element root) {
		if (ROOT_JOB_ELEMENT_NAME.equals(root.getLocalName())) {
			initProperties(root);
			transformDocument(root);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Transformed XML from preProcessXml: " + elementToString(root));
			}
		}
	}

	protected void initProperties(Element root) {
		propertyMap.put(JOB_PARAMETERS_KEY_NAME, initJobParameters());
		propertyMap.put(JOB_PROPERTIES_KEY_NAME, initJobProperties(root));

		resolvePropertyValues(propertyMap.get(JOB_PARAMETERS_KEY_NAME));
		resolvePropertyValues(propertyMap.get(JOB_PROPERTIES_KEY_NAME));
	}

	private Properties initJobParameters() {
		Properties jobParameters = new Properties();

		if (getBeanDefinitionRegistry().containsBeanDefinition(JOB_PARAMETERS_BEAN_DEFINITION_NAME)) {
			BeanDefinition beanDefinition = getBeanDefinitionRegistry().getBeanDefinition(JOB_PARAMETERS_BEAN_DEFINITION_NAME);

			Properties properties = (Properties) beanDefinition.getConstructorArgumentValues()
					.getGenericArgumentValue(Properties.class)
					.getValue();

			if (properties == null) {
				return new Properties();
			}

			Enumeration<?> propertyNames = properties.propertyNames();

			while(propertyNames.hasMoreElements()) {
				String curName = (String) propertyNames.nextElement();
				jobParameters.put(curName, properties.getProperty(curName));
			}
		}

		return jobParameters;
	}

	private Properties initJobProperties(Element root) {
		Properties properties = new Properties();
		Node propertiesNode = root.getElementsByTagName(JOB_PROPERTIES_ELEMENT_NAME).item(0);

		if(propertiesNode != null) {
			NodeList children = propertiesNode.getChildNodes();

			for(int i=0; i < children.getLength(); i++) {
				Node child = children.item(i);

				if(JOB_PROPERTY_ELEMENT_NAME.equals(child.getLocalName())) {
					NamedNodeMap attributes = child.getAttributes();
					Node name = attributes.getNamedItem(JOB_PROPERTY_ELEMENT_NAME_ATTRIBUTE);
					Node value = attributes.getNamedItem(JOB_PROPERTY_ELEMENT_VALUE_ATTRIBUTE);

					properties.setProperty(name.getNodeValue(), value.getNodeValue());
				}
			}
		}

		return properties;
	}

	private void resolvePropertyValues(Properties properties) {
		for (String propertyKey : properties.stringPropertyNames()) {
			String resolvedPropertyValue = resolvePropertyValue(properties.getProperty(propertyKey));

			if(!properties.getProperty(propertyKey).equals(resolvedPropertyValue)) {
				properties.setProperty(propertyKey, resolvedPropertyValue);
			}
		}
	}

	private String resolvePropertyValue(String propertyValue) {
		String resolvedValue = resolveValue(propertyValue);

		Matcher jobParameterMatcher = OPERATOR_PATTERN.matcher(resolvedValue);

		while (jobParameterMatcher.find()) {
			resolvedValue = resolvePropertyValue(resolvedValue);
		}

		return resolvedValue;
	}

	private String resolveValue(String value) {
		StringBuffer valueBuffer = new StringBuffer();
		Matcher jobParameterMatcher = OPERATOR_PATTERN.matcher(value);

		while (jobParameterMatcher.find()) {
			Matcher jobParameterKeyMatcher = PROPERTY_KEY_SEPARATOR.matcher(jobParameterMatcher.group(1));

			if (jobParameterKeyMatcher.find()) {
				String propertyType = jobParameterMatcher.group(2);
				String extractedProperty = jobParameterKeyMatcher.group(1);

				Properties properties = propertyMap.get(propertyType);

				if(properties == null) {
					throw new IllegalArgumentException("Unknown property type: " + propertyType);
				}

				String resolvedProperty = properties.getProperty(extractedProperty, NULL);

				if (NULL.equals(resolvedProperty)) {
					LOG.info(propertyType + " with key of: " + extractedProperty + " could not be resolved. Possible configuration error?");
				}

				jobParameterMatcher.appendReplacement(valueBuffer, resolvedProperty);
			}
		}

		jobParameterMatcher.appendTail(valueBuffer);
		String resolvedValue = valueBuffer.toString();

		if (NULL.equals(resolvedValue)) {
			return "";
		}

		return expressionParser.parseExpression(resolvedValue);
	}

	private BeanDefinitionRegistry getBeanDefinitionRegistry() {
		return beanDefinitionRegistry != null ? beanDefinitionRegistry : getReaderContext().getRegistry();
	}

	private void transformDocument(Element root) {
		DocumentTraversal traversal = (DocumentTraversal) root.getOwnerDocument();
		NodeIterator iterator = traversal.createNodeIterator(root, NodeFilter.SHOW_ELEMENT, null, true);

		BeanDefinitionRegistry registry = getBeanDefinitionRegistry();
		Map<String, Integer> referenceCountMap = new HashMap<>();

		for (Node n = iterator.nextNode(); n != null; n = iterator.nextNode()) {
			NamedNodeMap map = n.getAttributes();

			if (map.getLength() > 0) {
				for (int i = 0; i < map.getLength(); i++) {
					Node node = map.item(i);

					String nodeName = node.getNodeName();
					String nodeValue = node.getNodeValue();
					String resolvedValue = resolveValue(nodeValue);
					String newNodeValue = resolvedValue;

					if("ref".equals(nodeName)) {
						if(!referenceCountMap.containsKey(resolvedValue)) {
							referenceCountMap.put(resolvedValue, 0);
						}

						boolean isClass = isClass(resolvedValue);
						Integer referenceCount = referenceCountMap.get(resolvedValue);

						// possibly fully qualified class name in ref tag in the JSL or pointer to bean/artifact ref.
						if(isClass && !registry.containsBeanDefinition(resolvedValue)) {
							AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(resolvedValue)
									.getBeanDefinition();
							beanDefinition.setScope("step");
							registry.registerBeanDefinition(resolvedValue, beanDefinition);

							newNodeValue = resolvedValue;
						} else {
							if(registry.containsBeanDefinition(resolvedValue)) {
								referenceCount++;
								referenceCountMap.put(resolvedValue, referenceCount);

								newNodeValue = resolvedValue + referenceCount;

								BeanDefinition beanDefinition = registry.getBeanDefinition(resolvedValue);
								registry.registerBeanDefinition(newNodeValue, beanDefinition);
							}
						}
					}

					if(!nodeValue.equals(newNodeValue)) {
						node.setNodeValue(newNodeValue);
					}
				}
			} else {
				String nodeValue = n.getTextContent();
				String resolvedValue = resolveValue(nodeValue);

				if(!nodeValue.equals(resolvedValue)) {
					n.setTextContent(resolvedValue);
				}
			}
		}
	}

	private boolean isClass(String className) {
		try {
			Class.forName(className, false, ClassUtils.getDefaultClassLoader());
		} catch (ClassNotFoundException e) {
			return false;
		}

		return true;
	}

	protected Properties getJobParameters() {
		return propertyMap.get(JOB_PARAMETERS_KEY_NAME);
	}

	protected Properties getJobProperties() {
		return propertyMap.get(JOB_PROPERTIES_KEY_NAME);
	}

	private String elementToString(Element root) {
		DOMImplementationLS domImplLS = (DOMImplementationLS) root.getOwnerDocument().getImplementation();
		return domImplLS.createLSSerializer().writeToString(root);
	}
}
