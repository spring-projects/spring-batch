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

import java.util.List;

import org.springframework.batch.core.configuration.xml.ExceptionElementParser;
import org.springframework.batch.core.jsr.configuration.support.BatchArtifactType;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parser for the &lt;chunk /&gt; element as specified in JSR-352.  The current state
 * parses a chunk element into it's related batch artifacts ({@link ChunkOrientedTasklet}, {@link ItemReader},
 * {@link ItemProcessor}, and {@link ItemWriter}).
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 *
 */
public class ChunkParser {
	private static final String TIME_LIMIT_ATTRIBUTE = "time-limit";
	private static final String ITEM_COUNT_ATTRIBUTE = "item-count";
	private static final String CHECKPOINT_ALGORITHM_ELEMENT = "checkpoint-algorithm";
	private static final String CLASS_ATTRIBUTE = "class";
	private static final String INCLUDE_ELEMENT = "include";
	private static final String NO_ROLLBACK_EXCEPTION_CLASSES_ELEMENT = "no-rollback-exception-classes";
	private static final String RETRYABLE_EXCEPTION_CLASSES_ELEMENT = "retryable-exception-classes";
	private static final String SKIPPABLE_EXCEPTION_CLASSES_ELEMENT = "skippable-exception-classes";
	private static final String WRITER_ELEMENT = "writer";
	private static final String PROCESSOR_ELEMENT = "processor";
	private static final String READER_ELEMENT = "reader";
	private static final String REF_ATTRIBUTE = "ref";
	private static final String RETRY_LIMIT_ATTRIBUTE = "retry-limit";
	private static final String SKIP_LIMIT_ATTRIBUTE = "skip-limit";
	private static final String CUSTOM_CHECKPOINT_POLICY = "custom";
	private static final String ITEM_CHECKPOINT_POLICY = "item";
	private static final String CHECKPOINT_POLICY_ATTRIBUTE = "checkpoint-policy";

	public void parse(Element element, AbstractBeanDefinition bd, ParserContext parserContext, String stepName) {
		MutablePropertyValues propertyValues = bd.getPropertyValues();
		bd.setBeanClass(StepFactoryBean.class);
		bd.setAttribute("isNamespaceStep", false);

		propertyValues.addPropertyValue("hasChunkElement", Boolean.TRUE);

		String checkpointPolicy = element.getAttribute(CHECKPOINT_POLICY_ATTRIBUTE);
		if(StringUtils.hasText(checkpointPolicy)) {
			if(checkpointPolicy.equals(ITEM_CHECKPOINT_POLICY)) {
				String itemCount = element.getAttribute(ITEM_COUNT_ATTRIBUTE);
				if (StringUtils.hasText(itemCount)) {
					propertyValues.addPropertyValue("commitInterval", itemCount);
				} else {
					propertyValues.addPropertyValue("commitInterval", "10");
				}

				parseSimpleAttribute(element, propertyValues, TIME_LIMIT_ATTRIBUTE, "timeout");
			} else if(checkpointPolicy.equals(CUSTOM_CHECKPOINT_POLICY)) {
				parseCustomCheckpointAlgorithm(element, parserContext, propertyValues, stepName);
			}
		} else {
			String itemCount = element.getAttribute(ITEM_COUNT_ATTRIBUTE);
			if (StringUtils.hasText(itemCount)) {
				propertyValues.addPropertyValue("commitInterval", itemCount);
			} else {
				propertyValues.addPropertyValue("commitInterval", "10");
			}

			parseSimpleAttribute(element, propertyValues, TIME_LIMIT_ATTRIBUTE, "timeout");
		}

		parseSimpleAttribute(element, propertyValues, SKIP_LIMIT_ATTRIBUTE, "skipLimit");
		parseSimpleAttribute(element, propertyValues, RETRY_LIMIT_ATTRIBUTE, "retryLimit");

		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node nd = children.item(i);

			parseChildElement(element, parserContext, propertyValues, nd, stepName);
		}
	}

	private void parseSimpleAttribute(Element element,
			MutablePropertyValues propertyValues, String attributeName, String propertyName) {
		String propertyValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(propertyValue)) {
			propertyValues.addPropertyValue(propertyName, propertyValue);
		}
	}

	private void parseChildElement(Element element, ParserContext parserContext,
			MutablePropertyValues propertyValues, Node nd, String stepName) {
		if (nd instanceof Element) {
			Element nestedElement = (Element) nd;
			String name = nestedElement.getLocalName();
			String artifactName = nestedElement.getAttribute(REF_ATTRIBUTE);

			if(name.equals(READER_ELEMENT)) {
				if (StringUtils.hasText(artifactName)) {
					propertyValues.addPropertyValue("stepItemReader", new RuntimeBeanReference(artifactName));
				}

				new PropertyParser(artifactName, parserContext, BatchArtifactType.STEP_ARTIFACT, stepName).parseProperties(nestedElement);
			} else if(name.equals(PROCESSOR_ELEMENT)) {
				if (StringUtils.hasText(artifactName)) {
					propertyValues.addPropertyValue("stepItemProcessor", new RuntimeBeanReference(artifactName));
				}

				new PropertyParser(artifactName, parserContext, BatchArtifactType.STEP_ARTIFACT, stepName).parseProperties(nestedElement);
			} else if(name.equals(WRITER_ELEMENT)) {
				if (StringUtils.hasText(artifactName)) {
					propertyValues.addPropertyValue("stepItemWriter", new RuntimeBeanReference(artifactName));
				}

				new PropertyParser(artifactName, parserContext, BatchArtifactType.STEP_ARTIFACT, stepName).parseProperties(nestedElement);
			} else if(name.equals(SKIPPABLE_EXCEPTION_CLASSES_ELEMENT)) {
				ManagedMap<TypedStringValue, Boolean> exceptionClasses = new ExceptionElementParser().parse(element, parserContext, SKIPPABLE_EXCEPTION_CLASSES_ELEMENT);
				if(exceptionClasses != null) {
					propertyValues.addPropertyValue("skippableExceptionClasses", exceptionClasses);
				}
			} else if(name.equals(RETRYABLE_EXCEPTION_CLASSES_ELEMENT)) {
				ManagedMap<TypedStringValue, Boolean> exceptionClasses = new ExceptionElementParser().parse(element, parserContext, RETRYABLE_EXCEPTION_CLASSES_ELEMENT);
				if(exceptionClasses != null) {
					propertyValues.addPropertyValue("retryableExceptionClasses", exceptionClasses);
				}
			} else if(name.equals(NO_ROLLBACK_EXCEPTION_CLASSES_ELEMENT)) {
				//TODO: Update to support excludes
				ManagedList<TypedStringValue> list = new ManagedList<>();

				for (Element child : DomUtils.getChildElementsByTagName(nestedElement, INCLUDE_ELEMENT)) {
					String className = child.getAttribute(CLASS_ATTRIBUTE);
					list.add(new TypedStringValue(className, Class.class));
				}

				propertyValues.addPropertyValue("noRollbackExceptionClasses", list);
			}
		}
	}

	private void parseCustomCheckpointAlgorithm(Element element, ParserContext parserContext, MutablePropertyValues propertyValues, String stepName) {
		List<Element> elements = DomUtils.getChildElementsByTagName(element, CHECKPOINT_ALGORITHM_ELEMENT);

		if(elements.size() == 1) {
			Element checkpointAlgorithmElement = elements.get(0);

			String name = checkpointAlgorithmElement.getAttribute(REF_ATTRIBUTE);
			if(StringUtils.hasText(name)) {
				propertyValues.addPropertyValue("stepChunkCompletionPolicy", new RuntimeBeanReference(name));
			}

			new PropertyParser(name, parserContext, BatchArtifactType.STEP_ARTIFACT, stepName).parseProperties(checkpointAlgorithmElement);
		} else if(elements.size() > 1){
			parserContext.getReaderContext().error(
					"The <checkpoint-algorithm/> element may not appear more than once in a single <"
							+ element.getNodeName() + "/>.", element);
		}
	}
}
