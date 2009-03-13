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

import java.util.Arrays;
import java.util.List;

import org.springframework.batch.core.step.item.FaultTolerantStepFactoryBean;
import org.springframework.batch.core.step.item.SimpleStepFactoryBean;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Internal parser for the &lt;tasklet/&gt; element either inside a job or as a
 * standalone tasklet definition.
 * 
 * @author Thomas Risberg
 * @since 2.0
 */
public class TaskletElementParser {

	/**
	 * @param element
	 * @param parserContext
	 */
	protected AbstractBeanDefinition parse(Element element, ParserContext parserContext) {

		boolean isFaultTolerant = false;

		String skipLimit = element.getAttribute("skip-limit");
		if (!isFaultTolerant) {
			isFaultTolerant = checkIntValueForFaultToleranceNeeded(skipLimit);
		}
		String retryLimit = element.getAttribute("retry-limit");
		if (!isFaultTolerant) {
			isFaultTolerant = checkIntValueForFaultToleranceNeeded(retryLimit);
		}
		String cacheCapacity = element.getAttribute("cache-capacity");
		if (!isFaultTolerant) {
			isFaultTolerant = checkIntValueForFaultToleranceNeeded(cacheCapacity);
		}
		String isReaderTransactionalQueue = element.getAttribute("is-reader-transactional-queue");
		if (!isFaultTolerant && StringUtils.hasText(isReaderTransactionalQueue)) {
			if ("true".equals(isReaderTransactionalQueue)) {
				isFaultTolerant = true;
			}
		}
		checkExceptionElementForFaultToleranceNeeded(element, "skippable-exception-classes");
		checkExceptionElementForFaultToleranceNeeded(element, "retryable-exception-classes");
		checkExceptionElementForFaultToleranceNeeded(element, "fatal-exception-classes");

		GenericBeanDefinition bd = new GenericBeanDefinition();
		if (isFaultTolerant) {
			bd.setBeanClass(FaultTolerantStepFactoryBean.class);
		}
		else {
			bd.setBeanClass(SimpleStepFactoryBean.class);
		}

		boolean isAbstract = Boolean.valueOf(element.getAttribute("abstract"));
		bd.setAbstract(isAbstract);

		String parentRef = element.getAttribute("parent");
		if (StringUtils.hasText(parentRef)) {
			bd.setParentName(parentRef);
		}

		MutablePropertyValues propertyValues = bd.getPropertyValues();

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

		String commitInterval = element.getAttribute("commit-interval");
		if (StringUtils.hasText(commitInterval)) {
			propertyValues.addPropertyValue("commitInterval", commitInterval);
		}

		String completionPolicyRef = element.getAttribute("chunk-completion-policy");
		if (StringUtils.hasText(completionPolicyRef)) {
			RuntimeBeanReference completionPolicy = new RuntimeBeanReference(completionPolicyRef);
			propertyValues.addPropertyValue("chunkCompletionPolicy", completionPolicy);
		}

		if (!isAbstract
				&& propertyValues.contains("commitInterval") == propertyValues.contains("chunkCompletionPolicy")) {
			parserContext.getReaderContext().error(
					"The 'tasklet' element must contain either 'commit-interval' "
							+ "or 'chunk-completion-policy', but not both.", element);
		}

		if (StringUtils.hasText(skipLimit)) {
			propertyValues.addPropertyValue("skipLimit", skipLimit);
		}

		if (StringUtils.hasText(retryLimit)) {
			propertyValues.addPropertyValue("retryLimit", retryLimit);
		}

		if (StringUtils.hasText(cacheCapacity)) {
			propertyValues.addPropertyValue("cacheCapacity", cacheCapacity);
		}

		if (StringUtils.hasText(isReaderTransactionalQueue)) {
			if (isFaultTolerant) {
				propertyValues.addPropertyValue("isReaderTransactionalQueue", isReaderTransactionalQueue);
			}
		}

		handleExceptionElement(element, parserContext, bd, "skippable-exception-classes", "skippableExceptionClasses",
				isFaultTolerant, isAbstract);

		handleExceptionElement(element, parserContext, bd, "retryable-exception-classes", "retryableExceptionClasses",
				isFaultTolerant, isAbstract);

		handleExceptionElement(element, parserContext, bd, "fatal-exception-classes", "fatalExceptionClasses",
				isFaultTolerant, isAbstract);

		handleRetryListenersElement(element, bd, parserContext);

		handleStreamsElement(element, bd, parserContext);

		return bd;

	}

	private boolean checkIntValueForFaultToleranceNeeded(String stringValue) {
		if (StringUtils.hasText(stringValue)) {
			int value = Integer.valueOf(stringValue);
			if (value > 0) {
				return true;
			}
		}
		return false;
	}

	private boolean checkExceptionElementForFaultToleranceNeeded(Element element, String subElementName) {
		String exceptions = DomUtils.getChildElementValueByTagName(element, subElementName);
		if (StringUtils.hasLength(exceptions)) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private void handleExceptionElement(Element element, ParserContext parserContext, BeanDefinition bd,
			String subElementName, String propertyName, boolean isFaultTolerant, boolean isAbstract) {
		Element child = DomUtils.getChildElementByTagName(element, subElementName);
		if (child != null) {
			String exceptions = DomUtils.getTextValue(child);
			if (StringUtils.hasLength(exceptions)) {
				if (isFaultTolerant || isAbstract) {
					String[] exceptionArray = StringUtils.tokenizeToStringArray(exceptions, ",\n");
					if (exceptionArray.length > 0) {
						ManagedList managedList = new ManagedList();
						managedList.setMergeEnabled(Boolean.valueOf(child.getAttribute("merge")));
						managedList.addAll(Arrays.asList(exceptionArray));
						bd.getPropertyValues().addPropertyValue(propertyName, managedList);
					}
				}
				else {
					parserContext.getReaderContext().error(
							subElementName + " can only be specified for fault-tolerant "
									+ "configurations providing skip-limit, retry-limit or cache-capacity", element);
				}
			}
		}
	}

	private void handleRetryListenersElement(Element element, BeanDefinition bd, ParserContext parserContext) {
		Element listenersElement = DomUtils.getChildElementByTagName(element, "retry-listeners");
		if (listenersElement != null) {
			CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(listenersElement.getTagName(),
					parserContext.extractSource(element));
			parserContext.pushContainingComponent(compositeDef);
			ManagedList retryListenerBeans = new ManagedList();
			retryListenerBeans.setMergeEnabled(Boolean.valueOf(listenersElement.getAttribute("merge")));
			handleRetryListenerElements(parserContext, listenersElement, retryListenerBeans);
			bd.getPropertyValues().addPropertyValue("retryListeners", retryListenerBeans);
			parserContext.popAndRegisterContainingComponent();
		}
	}

	@SuppressWarnings("unchecked")
	private void handleRetryListenerElements(ParserContext parserContext, Element element, ManagedList beans) {
		List<Element> listenerElements = DomUtils.getChildElementsByTagName(element, "listener");
		if (listenerElements != null) {
			for (Element listenerElement : listenerElements) {
				String id = listenerElement.getAttribute("id");
				String listenerRef = listenerElement.getAttribute("ref");
				String className = listenerElement.getAttribute("class");
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
							"Neither 'ref' or 'class' specified for <" + listenerElement.getTagName() + "> element",
							element);
				}
			}
		}
	}

	private void checkListenerElementAttributes(ParserContext parserContext, Element element, Element listenerElement,
			String id, String listenerRef, String className) {
		if ((StringUtils.hasText(id) || StringUtils.hasText(className)) && StringUtils.hasText(listenerRef)) {
			NamedNodeMap attributeNodes = listenerElement.getAttributes();
			StringBuilder attributes = new StringBuilder();
			for (int i = 0; i < attributeNodes.getLength(); i++) {
				if (i > 0) {
					attributes.append(" ");
				}
				attributes.append(attributeNodes.item(i));
			}
			parserContext.getReaderContext().error(
					"Both 'ref' and " + (StringUtils.hasText(id) ? "'id'" : "'class'")
							+ " specified; use 'class' with an optional 'id' or just 'ref' for <"
							+ listenerElement.getTagName() + "> element specified with attributes: " + attributes,
					element);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleStreamsElement(Element element, BeanDefinition bd, ParserContext parserContext) {
		Element streamsElement = DomUtils.getChildElementByTagName(element, "streams");
		if (streamsElement != null) {
			ManagedList streamBeans = new ManagedList();
			streamBeans.setMergeEnabled(Boolean.valueOf(streamsElement.getAttribute("merge")));
			List<Element> streamElements = DomUtils.getChildElementsByTagName(streamsElement, "stream");
			if (streamElements != null) {
				for (Element streamElement : streamElements) {
					String streamRef = streamElement.getAttribute("ref");
					if (StringUtils.hasText(streamRef)) {
						BeanReference bean = new RuntimeBeanReference(streamRef);
						streamBeans.add(bean);
					}
					else {
						parserContext.getReaderContext().error(
								"ref not specified for <" + streamElement.getTagName() + "> element", element);
					}
				}
			}
			bd.getPropertyValues().addPropertyValue("streams", streamBeans);
		}
	}

}
