/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import static org.springframework.batch.core.configuration.xml.AbstractStepParser.handleExceptionElement;

import java.util.List;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Internal parser for the &lt;chunk/&gt; element inside a step.
 * 
 * @author Thomas Risberg
 * @since 2.0
 */
public class ChunkElementParser {

	private static final String ID_ATTR = "id";

	private static final String REF_ATTR = "ref";

	private static final String CLASS_ATTR = "class";

	private static final String MERGE_ATTR = "merge";

	private static final String COMMIT_INTERVAL_ATTR = "commit-interval";

	private static final String CHUNK_COMPLETION_POLICY_ATTR = "chunk-completion-policy";

	/**
	 * @param element
	 * @param parserContext
	 */
	protected void parse(Element element, AbstractBeanDefinition bd, ParserContext parserContext, boolean underspecified) {

		MutablePropertyValues propertyValues = bd.getPropertyValues();

		propertyValues.addPropertyValue("hasChunkElement", Boolean.TRUE);

		String readerBeanId = element.getAttribute("reader");
		if (StringUtils.hasText(readerBeanId)) {
			RuntimeBeanReference readerRef = new RuntimeBeanReference(readerBeanId);
			propertyValues.addPropertyValue("itemReader", readerRef);
		}

		String processorBeanId = element.getAttribute("processor");
		if (StringUtils.hasText(processorBeanId)) {
			RuntimeBeanReference processorRef = new RuntimeBeanReference(processorBeanId);
			propertyValues.addPropertyValue("itemProcessor", processorRef);
		}

		String writerBeanId = element.getAttribute("writer");
		if (StringUtils.hasText(writerBeanId)) {
			RuntimeBeanReference writerRef = new RuntimeBeanReference(writerBeanId);
			propertyValues.addPropertyValue("itemWriter", writerRef);
		}

		String taskExecutorBeanId = element.getAttribute("task-executor");
		if (StringUtils.hasText(taskExecutorBeanId)) {
			RuntimeBeanReference taskExecutorRef = new RuntimeBeanReference(taskExecutorBeanId);
			propertyValues.addPropertyValue("taskExecutor", taskExecutorRef);
		}

		String commitInterval = element.getAttribute(COMMIT_INTERVAL_ATTR);
		if (StringUtils.hasText(commitInterval)) {
			propertyValues.addPropertyValue("commitInterval", commitInterval);
		}

		String completionPolicyRef = element.getAttribute(CHUNK_COMPLETION_POLICY_ATTR);
		if (StringUtils.hasText(completionPolicyRef)) {
			RuntimeBeanReference completionPolicy = new RuntimeBeanReference(completionPolicyRef);
			propertyValues.addPropertyValue("chunkCompletionPolicy", completionPolicy);
		}

		if (!underspecified
				&& propertyValues.contains("commitInterval") == propertyValues.contains("chunkCompletionPolicy")) {
			if (propertyValues.contains("commitInterval")) {
				parserContext.getReaderContext().error(
						"The <" + element.getNodeName() + "/> element must contain either '" + COMMIT_INTERVAL_ATTR
								+ "' " + "or '" + CHUNK_COMPLETION_POLICY_ATTR + "', but not both.", element);
			}
			else {
				parserContext.getReaderContext().error(
						"The <" + element.getNodeName() + "/> element must contain either '" + COMMIT_INTERVAL_ATTR
								+ "' " + "or '" + CHUNK_COMPLETION_POLICY_ATTR + "'.", element);

			}
		}

		String skipLimit = element.getAttribute("skip-limit");
		if (StringUtils.hasText(skipLimit)) {
			propertyValues.addPropertyValue("skipLimit", skipLimit);
		}

		String retryLimit = element.getAttribute("retry-limit");
		if (StringUtils.hasText(retryLimit)) {
			propertyValues.addPropertyValue("retryLimit", retryLimit);
		}

		String throttleLimit = element.getAttribute("throttle-limit");
		if (StringUtils.hasText(throttleLimit)) {
			propertyValues.addPropertyValue("throttleLimit", throttleLimit);
		}
		
		String cacheCapacity = element.getAttribute("cache-capacity");
		if (StringUtils.hasText(cacheCapacity)) {
			propertyValues.addPropertyValue("cacheCapacity", cacheCapacity);
		}

		String isReaderTransactionalQueue = element.getAttribute("is-reader-transactional-queue");
		if (StringUtils.hasText(isReaderTransactionalQueue)) {
			propertyValues.addPropertyValue("isReaderTransactionalQueue", isReaderTransactionalQueue);
		}

		handleExceptionElement(element, parserContext, propertyValues, "skippable-exception-classes",
				"skippableExceptionClasses");

		handleExceptionElement(element, parserContext, propertyValues, "retryable-exception-classes",
				"retryableExceptionClasses");

		handleExceptionElement(element, parserContext, propertyValues, "fatal-exception-classes",
				"fatalExceptionClasses");

		handleRetryListenersElement(element, propertyValues, parserContext);

		handleStreamsElement(element, propertyValues, parserContext);

	}

	private void handleRetryListenersElement(Element element, MutablePropertyValues propertyValues,
			ParserContext parserContext) {
		Element listenersElement = DomUtils.getChildElementByTagName(element, "retry-listeners");
		if (listenersElement != null) {
			CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(listenersElement.getTagName(),
					parserContext.extractSource(element));
			parserContext.pushContainingComponent(compositeDef);
			ManagedList retryListenerBeans = new ManagedList();
			retryListenerBeans.setMergeEnabled(listenersElement.hasAttribute(MERGE_ATTR)
					&& Boolean.valueOf(listenersElement.getAttribute(MERGE_ATTR)));
			handleRetryListenerElements(parserContext, listenersElement, retryListenerBeans);
			propertyValues.addPropertyValue("retryListeners", retryListenerBeans);
			parserContext.popAndRegisterContainingComponent();
		}
	}

	@SuppressWarnings("unchecked")
	private void handleRetryListenerElements(ParserContext parserContext, Element element, ManagedList beans) {
		List<Element> listenerElements = DomUtils.getChildElementsByTagName(element, "listener");
		if (listenerElements != null) {
			for (Element listenerElement : listenerElements) {
				String id = listenerElement.getAttribute(ID_ATTR);
				String listenerRef = listenerElement.getAttribute(REF_ATTR);
				String className = listenerElement.getAttribute(CLASS_ATTR);
				checkListenerElementAttributes(parserContext, element, listenerElement, id, listenerRef, className);
				if (StringUtils.hasText(listenerRef)) {
					BeanReference bean = new RuntimeBeanReference(listenerRef);
					beans.add(bean);
				}
				else if (StringUtils.hasText(className)) {
					RootBeanDefinition beanDef = new RootBeanDefinition(className, null, null);
					if (!StringUtils.hasText(id)) {
						id = parserContext.getReaderContext().generateBeanName(beanDef);
					}
					beans.add(beanDef);
				}
				else {
					parserContext.getReaderContext().error(
							"Neither '" + REF_ATTR + "' or '" + CLASS_ATTR + "' specified for <"
									+ listenerElement.getTagName() + "> element", element);
				}
			}
		}
	}

	private void checkListenerElementAttributes(ParserContext parserContext, Element element, Element listenerElement,
			String id, String listenerRef, String className) {
		if (StringUtils.hasText(className) && StringUtils.hasText(listenerRef)) {
			NamedNodeMap attributeNodes = listenerElement.getAttributes();
			StringBuilder attributes = new StringBuilder();
			for (int i = 0; i < attributeNodes.getLength(); i++) {
				if (i > 0) {
					attributes.append(" ");
				}
				attributes.append(attributeNodes.item(i));
			}
			parserContext.getReaderContext().error(
					"Both '" + REF_ATTR + "' and '" + CLASS_ATTR + "' specified; use '" + CLASS_ATTR
							+ "' with an optional '" + ID_ATTR + "' or just '" + REF_ATTR + "' for <"
							+ listenerElement.getTagName() + "> element specified with attributes: " + attributes,
					element);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleStreamsElement(Element element, MutablePropertyValues propertyValues, ParserContext parserContext) {
		Element streamsElement = DomUtils.getChildElementByTagName(element, "streams");
		if (streamsElement != null) {
			ManagedList streamBeans = new ManagedList();
			streamBeans.setMergeEnabled(streamsElement.hasAttribute(MERGE_ATTR)
					&& Boolean.valueOf(streamsElement.getAttribute(MERGE_ATTR)));
			List<Element> streamElements = DomUtils.getChildElementsByTagName(streamsElement, "stream");
			if (streamElements != null) {
				for (Element streamElement : streamElements) {
					String streamRef = streamElement.getAttribute(REF_ATTR);
					if (StringUtils.hasText(streamRef)) {
						BeanReference bean = new RuntimeBeanReference(streamRef);
						streamBeans.add(bean);
					}
					else {
						parserContext.getReaderContext().error(
								REF_ATTR + " not specified for <" + streamElement.getTagName() + "> element", element);
					}
				}
			}
			propertyValues.addPropertyValue("streams", streamBeans);
		}
	}

}
