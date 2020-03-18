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

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.batch.core.jsr.configuration.support.BatchArtifactType;
import org.springframework.batch.core.jsr.partition.JsrPartitionHandler;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;partition&gt; element as defined by JSR-352.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public class PartitionParser {

	private static final String REF = "ref";
	private static final String MAPPER_ELEMENT = "mapper";
	private static final String PLAN_ELEMENT = "plan";
	private static final String PARTITIONS_ATTRIBUTE = "partitions";
	private static final String THREADS_ATTRIBUTE = "threads";
	private static final String PROPERTIES_ELEMENT = "properties";
	private static final String ANALYZER_ELEMENT = "analyzer";
	private static final String COLLECTOR_ELEMENT = "collector";
	private static final String REDUCER_ELEMENT = "reducer";
	private static final String PARTITION_CONTEXT_PROPERTY = "propertyContext";
	private static final String PARTITION_MAPPER_PROPERTY = "partitionMapper";
	private static final String PARTITION_ANALYZER_PROPERTY = "partitionAnalyzer";
	private static final String PARTITION_REDUCER_PROPERTY = "partitionReducer";
	private static final String PARTITION_QUEUE_PROPERTY = "partitionDataQueue";
	private static final String LISTENERS_PROPERTY = "listeners";
	private static final String THREADS_PROPERTY = "threads";
	private static final String PARTITIONS_PROPERTY = "partitions";
	private static final String PARTITION_LOCK_PROPERTY = "partitionLock";

	private final String name;
	private boolean allowStartIfComplete = false;

	/**
	 * @param stepName the name of the step that is being partitioned
	 * @param allowStartIfComplete  boolean to establish the allowStartIfComplete property for partition properties.
	 */
	public PartitionParser(String stepName, boolean allowStartIfComplete) {
		this.name = stepName;
		this.allowStartIfComplete = allowStartIfComplete;
	}

	public void parse(Element element, AbstractBeanDefinition bd, ParserContext parserContext, String stepName) {
		BeanDefinitionRegistry registry = parserContext.getRegistry();
		MutablePropertyValues factoryBeanProperties = bd.getPropertyValues();

		AbstractBeanDefinition partitionHandlerDefinition = BeanDefinitionBuilder.genericBeanDefinition(JsrPartitionHandler.class)
				.getBeanDefinition();

		MutablePropertyValues properties = partitionHandlerDefinition.getPropertyValues();
		properties.addPropertyValue(PARTITION_CONTEXT_PROPERTY, new RuntimeBeanReference("batchPropertyContext"));
		properties.addPropertyValue("jobRepository", new RuntimeBeanReference("jobRepository"));
		properties.addPropertyValue("allowStartIfComplete", allowStartIfComplete);

		parseMapperElement(element, parserContext, properties);
		parsePartitionPlan(element, parserContext, stepName, properties);
		parseAnalyzerElement(element, parserContext, properties);
		parseReducerElement(element, parserContext, factoryBeanProperties);
		parseCollectorElement(element, parserContext, factoryBeanProperties,
				properties);

		String partitionHandlerBeanName = name + ".partitionHandler";
		registry.registerBeanDefinition(partitionHandlerBeanName, partitionHandlerDefinition);
		factoryBeanProperties.add("partitionHandler", new RuntimeBeanReference(partitionHandlerBeanName));
	}

	private void parseCollectorElement(Element element,
			ParserContext parserContext,
			MutablePropertyValues factoryBeanProperties,
			MutablePropertyValues properties) {
		Element collectorElement = DomUtils.getChildElementByTagName(element, COLLECTOR_ELEMENT);

		if(collectorElement != null) {
			// Only needed if a collector is used
			registerCollectorAnalyzerQueue(parserContext);
			properties.add(PARTITION_QUEUE_PROPERTY, new RuntimeBeanReference(name + "PartitionQueue"));
			properties.add(PARTITION_LOCK_PROPERTY, new RuntimeBeanReference(name + "PartitionLock"));
			factoryBeanProperties.add("partitionQueue", new RuntimeBeanReference(name + "PartitionQueue"));
			factoryBeanProperties.add("partitionLock", new RuntimeBeanReference(name + "PartitionLock"));
			String collectorName = collectorElement.getAttribute(REF);
			factoryBeanProperties.add(LISTENERS_PROPERTY, new RuntimeBeanReference(collectorName));
			new PropertyParser(collectorName, parserContext, BatchArtifactType.STEP_ARTIFACT, name).parseProperties(collectorElement);
		}
	}

	private void parseReducerElement(Element element,
			ParserContext parserContext,
			MutablePropertyValues factoryBeanProperties) {
		Element reducerElement = DomUtils.getChildElementByTagName(element, REDUCER_ELEMENT);

		if(reducerElement != null) {
			String reducerName = reducerElement.getAttribute(REF);
			factoryBeanProperties.add(PARTITION_REDUCER_PROPERTY, new RuntimeBeanReference(reducerName));
			new PropertyParser(reducerName, parserContext, BatchArtifactType.STEP_ARTIFACT, name).parseProperties(reducerElement);
		}
	}

	private void parseAnalyzerElement(Element element,
			ParserContext parserContext, MutablePropertyValues properties) {
		Element analyzerElement = DomUtils.getChildElementByTagName(element, ANALYZER_ELEMENT);

		if(analyzerElement != null) {
			String analyzerName = analyzerElement.getAttribute(REF);
			properties.add(PARTITION_ANALYZER_PROPERTY, new RuntimeBeanReference(analyzerName));
			new PropertyParser(analyzerName, parserContext, BatchArtifactType.STEP_ARTIFACT, name).parseProperties(analyzerElement);
		}
	}

	private void parseMapperElement(Element element,
			ParserContext parserContext, MutablePropertyValues properties) {
		Element mapperElement = DomUtils.getChildElementByTagName(element, MAPPER_ELEMENT);

		if(mapperElement != null) {
			String mapperName = mapperElement.getAttribute(REF);
			properties.add(PARTITION_MAPPER_PROPERTY, new RuntimeBeanReference(mapperName));
			new PropertyParser(mapperName, parserContext, BatchArtifactType.STEP_ARTIFACT, name).parseProperties(mapperElement);
		}
	}

	private void registerCollectorAnalyzerQueue(ParserContext parserContext) {
		AbstractBeanDefinition partitionQueueDefinition = BeanDefinitionBuilder.genericBeanDefinition(ConcurrentLinkedQueue.class)
				.getBeanDefinition();
		AbstractBeanDefinition partitionLockDefinition = BeanDefinitionBuilder.genericBeanDefinition(ReentrantLock.class)
				.getBeanDefinition();

		parserContext.getRegistry().registerBeanDefinition(name + "PartitionQueue", partitionQueueDefinition);
		parserContext.getRegistry().registerBeanDefinition(name + "PartitionLock", partitionLockDefinition);
	}

	protected void parsePartitionPlan(Element element,
			ParserContext parserContext, String stepName,
			MutablePropertyValues properties) {
		Element planElement = DomUtils.getChildElementByTagName(element, PLAN_ELEMENT);

		if(planElement != null) {
			String partitions = planElement.getAttribute(PARTITIONS_ATTRIBUTE);
			String threads = planElement.getAttribute(THREADS_ATTRIBUTE);

			if(!StringUtils.hasText(threads)) {
				threads = partitions;
			}

			List<Element> partitionProperties = DomUtils.getChildElementsByTagName(planElement, PROPERTIES_ELEMENT);

			if(partitionProperties != null) {
				for (Element partition : partitionProperties) {
					String partitionStepName = stepName + ":partition" + partition.getAttribute("partition");
					new PropertyParser(partitionStepName, parserContext, BatchArtifactType.STEP, partitionStepName).parseProperty(partition);
				}
			}

			properties.add(THREADS_PROPERTY, threads);
			properties.add(PARTITIONS_PROPERTY, partitions);
		}
	}
}
