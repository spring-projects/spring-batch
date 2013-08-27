/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.jsr.configuration.xml;

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * <p>
 * Parser for the &lt;properties /&gt; element defined by JSR-352. Parsed job level properties are
 * also registered as a Map similar to systemProperties. Job level properties can be obtained for
 * example through SPeL by referencing #{jobProperties['key']}.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class PropertyParser {
	static final String JOB_ARTIFACT_PROPERTY_PREFIX = "job-";
	private static final String PROPERTY_ELEMENT = "property";
	private static final String PROPERTIES_ELEMENT = "properties";
	private static final String PROPERTY_NAME_ATTRIBUTE = "name";
	private static final String PROPERTY_VALUE_ATTRIBUTE = "value";
	private static final String JOB_PROPERTIES_BEAN_NAME = "jobProperties";
	private static final String BATCH_CONTEXT_ENTRIES_PROPERTY_NAME = "batchContextEntries";
	private static final String BATCH_PROPERTY_CONTEXT_BEAN_CLASS_NAME = "org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext";
	private static final String BATCH_PROPERTY_CONTEXT_BEAN_NAME = "batchPropertyContext";
	private static final Deque<String> PATH = new LinkedList<String>();

	private String beanName;
	private ParserContext parserContext;

	public PropertyParser(String beanName, ParserContext parserContext) {
		this.beanName = beanName;
		this.parserContext = parserContext;

		registerBatchPropertyContext();
		registerJobProperties();
	}

	public PropertyParser(ParserContext parserContext) {
		this("", parserContext);
	}

	public static void pushPath(String pathElement) {
		PATH.push(pathElement);
	}

	public static void popPath() {
		PATH.pop();
	}

	public static boolean hasPath() {
		return !PATH.isEmpty();
	}

	public static void clearPath() {
		PATH.clear();
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

		Properties properties = new Properties();

		if (propertiesElements.size() == 1) {
			List<Element> propertyElements = DomUtils.getChildElementsByTagName(propertiesElements.get(0), PROPERTY_ELEMENT);

			for (Element propertyElement : propertyElements) {
				properties.put(propertyElement.getAttribute(PROPERTY_NAME_ATTRIBUTE), propertyElement.getAttribute(PROPERTY_VALUE_ATTRIBUTE));
			}

			addProperties(properties);
		} else if (propertiesElements.size() > 1) {
			parserContext.getReaderContext().error("The <properties> element may not appear more than once in a single <listener>.", element);
		}

		setJobProperties(properties);
	}

	private void addProperties(Properties properties) {
		BeanDefinition beanDefinition = parserContext.getRegistry().getBeanDefinition(BATCH_PROPERTY_CONTEXT_BEAN_NAME);

		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		BatchPropertyContext.BatchPropertyContextEntry batchPropertyContextEntry =
			batchPropertyContext.new BatchPropertyContextEntry(getContextEntryKey(), properties);

		ManagedList<BatchPropertyContext.BatchPropertyContextEntry> managedList = new ManagedList<BatchPropertyContext.BatchPropertyContextEntry>();
		managedList.setMergeEnabled(true);
		managedList.add(batchPropertyContextEntry);

		beanDefinition.getPropertyValues().addPropertyValue(BATCH_CONTEXT_ENTRIES_PROPERTY_NAME, managedList);
	}

	private void registerBatchPropertyContext() {
		if (!parserContext.getRegistry().containsBeanDefinition(BATCH_PROPERTY_CONTEXT_BEAN_NAME)) {
			BeanDefinitionBuilder batchPropertyContextBeanDefinitionBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(BATCH_PROPERTY_CONTEXT_BEAN_CLASS_NAME);

			AbstractBeanDefinition batchPropertyContextBeanDefinition = batchPropertyContextBeanDefinitionBuilder.getBeanDefinition();
			batchPropertyContextBeanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			parserContext.getRegistry().registerBeanDefinition(BATCH_PROPERTY_CONTEXT_BEAN_NAME, batchPropertyContextBeanDefinition);
		}
	}

	private void registerJobProperties() {
		if (!parserContext.getRegistry().containsBeanDefinition(JOB_PROPERTIES_BEAN_NAME)) {
			AbstractBeanDefinition jobPropertiesBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(HashMap.class).getBeanDefinition();
			jobPropertiesBeanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			parserContext.getRegistry().registerBeanDefinition(JOB_PROPERTIES_BEAN_NAME, jobPropertiesBeanDefinition);
		}
	}

	private void setJobProperties(Properties properties) {
		if (beanName.startsWith(JOB_ARTIFACT_PROPERTY_PREFIX)) {
			Map<String, String> jobProperties = new HashMap<String, String>();

			if (properties != null) {
				for (String param : properties.stringPropertyNames()) {
					jobProperties.put(param, properties.getProperty(param));
				}
			}

			BeanDefinition jobPropertiesBeanDefinition = parserContext.getRegistry().getBeanDefinition(JOB_PROPERTIES_BEAN_NAME);
			jobPropertiesBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(jobProperties);
		}
	}

	private String getPath() {
		StringBuilder pathBuilder = new StringBuilder();
		Iterator pathIterator = PATH.descendingIterator();

		if (pathIterator.hasNext()) {
			pathBuilder.append(pathIterator.next());

			while (pathIterator.hasNext()) {
				pathBuilder.append(".").append(pathIterator.next());
			}
		}

		return pathBuilder.toString();
	}

	private String getContextEntryKey() {
		return "".equals(beanName) ? getPath() : getPath() + "." + beanName;
	}
}
