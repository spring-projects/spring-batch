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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.batch.core.jsr.configuration.support.BatchArtifact;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
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
	private static final String BATCH_PROPERTY_CONTEXT_BEAN_CLASS_NAME = "org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext";
	private static final String BATCH_PROPERTY_CONTEXT_BEAN_NAME = "batchPropertyContext";

	private String beanName;
	private String stepName;
	private ParserContext parserContext;
	private BatchArtifact.BatchArtifactType batchArtifactType;

	public PropertyParser(String beanName, ParserContext parserContext, BatchArtifact.BatchArtifactType batchArtifactType) {
		this.beanName = beanName;
		this.parserContext = parserContext;
		this.batchArtifactType = batchArtifactType;

		registerBatchPropertyContext();
	}

	public PropertyParser(String beanName, ParserContext parserContext, BatchArtifact.BatchArtifactType batchArtifactType, String stepName) {
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

		Properties properties = new Properties();

		if (propertiesElements.size() == 1) {
			parsePropertiesElement(propertiesElements, properties);
		} else if (propertiesElements.size() > 1) {
			parserContext.getReaderContext().error("The <properties> element may not appear more than once in a single <listener>.", element);
		}

		setJobProperties(properties);
	}

	public void parsePartitionProperties(Element element) {
		Properties properties = new Properties();

		List<Element> elements = new ArrayList<Element>();
		elements.add(element);
		parsePropertiesElement(elements, properties);

		setJobProperties(properties);
	}

	private void parsePropertiesElement(List<Element> propertiesElements, Properties properties) {
		List<Element> propertyElements = DomUtils.getChildElementsByTagName(propertiesElements.get(0), PROPERTY_ELEMENT);

		for (Element propertyElement : propertyElements) {
			properties.put(propertyElement.getAttribute(PROPERTY_NAME_ATTRIBUTE), propertyElement.getAttribute(PROPERTY_VALUE_ATTRIBUTE));
		}

		addProperties(properties);
	}

	private void addProperties(Properties properties) {
		BeanDefinition beanDefinition = parserContext.getRegistry().getBeanDefinition(BATCH_PROPERTY_CONTEXT_BEAN_NAME);

		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		BatchPropertyContext.BatchPropertyContextEntry batchPropertyContextEntry =
				batchPropertyContext.new BatchPropertyContextEntry(beanName, properties, batchArtifactType);

		if (StringUtils.hasText(stepName)) {
			batchPropertyContextEntry.setStepName(stepName);
		}

		ManagedList<BatchPropertyContext.BatchPropertyContextEntry> managedList = new ManagedList<BatchPropertyContext.BatchPropertyContextEntry>();
		managedList.setMergeEnabled(true);
		managedList.add(batchPropertyContextEntry);

		beanDefinition.getPropertyValues().addPropertyValue(batchPropertyContext.getPropertyName(batchArtifactType), managedList);
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

	private void setJobProperties(Properties properties) {
		if (batchArtifactType.equals(BatchArtifact.BatchArtifactType.JOB)) {
			BeanDefinition beanDefinition = parserContext.getRegistry().getBeanDefinition(BATCH_PROPERTY_CONTEXT_BEAN_NAME);

			BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
			BatchPropertyContext.BatchPropertyContextEntry batchPropertyContextEntry =
					batchPropertyContext.new BatchPropertyContextEntry(beanName, properties, batchArtifactType);

			beanDefinition.getPropertyValues().addPropertyValue(batchPropertyContext.getPropertyName(batchArtifactType), batchPropertyContextEntry);

			registerJobProperties(properties);
		}
	}

	private void registerJobProperties(Properties properties) {
		if (batchArtifactType.equals(BatchArtifact.BatchArtifactType.JOB)) {
			Map<String, String> jobProperties = new HashMap<String, String>();

			if (properties != null && ! properties.isEmpty()) {
				for (String param : properties.stringPropertyNames()) {
					jobProperties.put(param, properties.getProperty(param));
				}
			}

			BeanDefinition jobPropertiesBeanDefinition = parserContext.getRegistry().getBeanDefinition(JOB_PROPERTIES_BEAN_NAME);
			jobPropertiesBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(jobProperties);
		}
	}
}
