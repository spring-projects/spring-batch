/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.batch.core.jsr.configuration.support.BatchArtifactType;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * <p>
 * Parser for the &lt;properties /&gt; element defined by JSR-352.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class PropertyParser {
	private static final String PROPERTY_ELEMENT = "property";
	private static final String PROPERTIES_ELEMENT = "properties";
	private static final String PROPERTY_NAME_ATTRIBUTE = "name";
	private static final String PROPERTY_VALUE_ATTRIBUTE = "value";
	private static final String JOB_PROPERTIES_BEAN_NAME = "jobProperties";
	private static final String BATCH_PROPERTY_CONTEXT_BEAN_NAME = "batchPropertyContext";
	private static final String JOB_PROPERTIES_PROPERTY_NAME = "jobProperties";
	private static final String STEP_PROPERTIES_PROPERTY_NAME = "stepProperties";
	private static final String ARTIFACT_PROPERTIES_PROPERTY_NAME = "artifactProperties";
	private static final String STEP_ARTIFACT_PROPERTIES_PROPERTY_NAME = "stepArtifactProperties";

	private String beanName;
	private String stepName;
	private ParserContext parserContext;
	private BatchArtifactType batchArtifactType;

	public PropertyParser(String beanName, ParserContext parserContext, BatchArtifactType batchArtifactType) {
		this.beanName = beanName;
		this.parserContext = parserContext;
		this.batchArtifactType = batchArtifactType;
	}

	public PropertyParser(String beanName, ParserContext parserContext, BatchArtifactType batchArtifactType, String stepName) {
		this(beanName, parserContext, batchArtifactType);
		this.stepName = stepName;
	}

	/**
	 * <p>
	 * Parses &lt;property&gt; tag values from the provided {@link Element} if it contains a &lt;properties /&gt; element.
	 * Only one &lt;properties /&gt; element may be present. &lt;property&gt; elements have a name and value attribute
	 * which represent the property entries key and value.
	 * </p>
	 *
	 * @param element the element to parse looking for &lt;properties /&gt;
	 */
	public void parseProperties(Element element) {
		List<Element> propertiesElements = DomUtils.getChildElementsByTagName(element, PROPERTIES_ELEMENT);

		if (propertiesElements.size() == 1) {
			parsePropertyElement(propertiesElements.get(0));
		} else if (propertiesElements.size() > 1) {
			parserContext.getReaderContext().error("The <properties> element may not appear more than once.", element);
		}
	}

	/**
	 * <p>
	 * Parses a &lt;property&gt; tag value from the provided {@link Element}. &lt;property&gt; elements have a name and
	 * value attribute which represent the property entries key and value.
	 * </p>
	 *
	 * @param element the element to parse looking for &lt;property/&gt;
	 */
	public void parseProperty(Element element) {
		parsePropertyElement(element);
	}

	private void parsePropertyElement(Element propertyElement) {
		Properties properties = new Properties();

		for (Element element : DomUtils.getChildElementsByTagName(propertyElement, PROPERTY_ELEMENT)) {
			properties.put(element.getAttribute(PROPERTY_NAME_ATTRIBUTE), element.getAttribute(PROPERTY_VALUE_ATTRIBUTE));
		}

		setProperties(properties);
		setJobPropertiesBean(properties);
	}

	private void setProperties(Properties properties) {
		Object propertyValue;
		BeanDefinition beanDefinition = parserContext.getRegistry().getBeanDefinition(BATCH_PROPERTY_CONTEXT_BEAN_NAME);

		if(batchArtifactType.equals(BatchArtifactType.JOB)) {
			propertyValue = getJobProperties(properties);
		} else if (batchArtifactType.equals(BatchArtifactType.STEP)) {
			propertyValue = getProperties(stepName, properties);
		} else if (batchArtifactType.equals(BatchArtifactType.ARTIFACT)) {
			propertyValue = getProperties(beanName, properties);
		} else if (batchArtifactType.equals(BatchArtifactType.STEP_ARTIFACT)) {
			propertyValue = getStepArtifactProperties(beanDefinition, properties);
		} else {
			throw new IllegalStateException("Unhandled BatchArtifactType of: " + batchArtifactType);
		}

		beanDefinition.getPropertyValues().addPropertyValue(getPropertyName(batchArtifactType), propertyValue);
	}

	private Map<String, Properties> getProperties(String keyName, Properties properties) {
		ManagedMap<String, Properties> stepProperties = new ManagedMap<>();
		stepProperties.setMergeEnabled(true);
		stepProperties.put(keyName, properties);

		return stepProperties;
	}

	private Properties getJobProperties(Properties properties) {
		return properties;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Map<String, Properties>> getStepArtifactProperties(BeanDefinition beanDefinition, Properties properties) {
		ManagedMap<String, Map<String, Properties>> stepArtifacts = new ManagedMap<>();
		stepArtifacts.setMergeEnabled(true);

		Map<String, Map<String, Properties>> existingArtifacts
				= (Map<String, Map<String, Properties>>) beanDefinition.getPropertyValues().get(getPropertyName(batchArtifactType));

		ManagedMap<String, Properties> artifactProperties = new ManagedMap<>();
		artifactProperties.setMergeEnabled(true);

		if(existingArtifacts != null && existingArtifacts.containsKey(stepName)) {
			Map<String, Properties> existingArtifactsMap = existingArtifacts.get(stepName);

			for(Map.Entry<String, Properties> existingArtifactEntry : existingArtifactsMap.entrySet()) {
				artifactProperties.put(existingArtifactEntry.getKey(), existingArtifactEntry.getValue());
			}
		}

		artifactProperties.put(beanName, properties);
		stepArtifacts.put(stepName, artifactProperties);

		return stepArtifacts;
	}

	private void setJobPropertiesBean(Properties properties) {
		if (batchArtifactType.equals(BatchArtifactType.JOB)) {
			Map<String, String> jobProperties = new HashMap<>();

			if (properties != null && !properties.isEmpty()) {
				for (String param : properties.stringPropertyNames()) {
					jobProperties.put(param, properties.getProperty(param));
				}
			}

			BeanDefinition jobPropertiesBeanDefinition = parserContext.getRegistry().getBeanDefinition(JOB_PROPERTIES_BEAN_NAME);
			jobPropertiesBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(jobProperties);
		}
	}

	private String getPropertyName(BatchArtifactType batchArtifactType) {
		if(batchArtifactType.equals(BatchArtifactType.JOB)) {
			return JOB_PROPERTIES_PROPERTY_NAME;
		} else if (batchArtifactType.equals(BatchArtifactType.STEP)) {
			return STEP_PROPERTIES_PROPERTY_NAME;
		} else if (batchArtifactType.equals(BatchArtifactType.ARTIFACT)) {
			return ARTIFACT_PROPERTIES_PROPERTY_NAME;
		} else if (batchArtifactType.equals(BatchArtifactType.STEP_ARTIFACT)) {
			return STEP_ARTIFACT_PROPERTIES_PROPERTY_NAME;
		} else {
			throw new IllegalStateException("Unhandled BatchArtifactType of: " + batchArtifactType);
		}
	}
}
