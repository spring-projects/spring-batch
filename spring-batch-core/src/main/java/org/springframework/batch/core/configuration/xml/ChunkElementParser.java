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

import org.springframework.batch.core.listener.StepListenerMetaData;
import org.springframework.batch.core.step.item.ForceRollbackForWriteSkipException;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.List;

/**
 * Internal parser for the &lt;chunk/&gt; element inside a step.
 * 
 * @author Thomas Risberg
 * @since 2.0
 */
public class ChunkElementParser {

	private static final String REF_ATTR = "ref";

	private static final String MERGE_ATTR = "merge";

	private static final String COMMIT_INTERVAL_ATTR = "commit-interval";

	private static final String CHUNK_COMPLETION_POLICY_ATTR = "chunk-completion-policy";

	private static final String BEAN_ELE = "bean";

	private static final String REF_ELE = "ref";

	private static final String ITEM_READER_ADAPTER_CLASS = "org.springframework.batch.item.adapter.ItemReaderAdapter";

	private static final String ITEM_PROCESSOR_ADAPTER_CLASS = "org.springframework.batch.item.adapter.ItemProcessorAdapter";

	private static final String ITEM_WRITER_ADAPTER_CLASS = "org.springframework.batch.item.adapter.ItemWriterAdapter";

	private static final StepListenerParser stepListenerParser = new StepListenerParser(
			StepListenerMetaData.itemListenerMetaData());

	/**
	 * @param element
	 * @param parserContext
	 */
	protected void parse(Element element, AbstractBeanDefinition bd, ParserContext parserContext, boolean underspecified) {

		MutablePropertyValues propertyValues = bd.getPropertyValues();

		propertyValues.addPropertyValue("hasChunkElement", Boolean.TRUE);

		handleItemHandler(bd, "reader", "itemReader", ITEM_READER_ADAPTER_CLASS, true, element, parserContext,
				propertyValues, underspecified);
		handleItemHandler(bd, "processor", "itemProcessor", ITEM_PROCESSOR_ADAPTER_CLASS, false, element, parserContext,
				propertyValues, underspecified);
		handleItemHandler(bd, "writer", "itemWriter", ITEM_WRITER_ADAPTER_CLASS, true, element, parserContext,
				propertyValues, underspecified);

		String commitInterval = element.getAttribute(COMMIT_INTERVAL_ATTR);
		if (StringUtils.hasText(commitInterval)) {
			if (commitInterval.startsWith("#")) {
				// It's a late binding expression, so we need step scope...
				BeanDefinitionBuilder completionPolicy = BeanDefinitionBuilder
						.genericBeanDefinition(SimpleCompletionPolicy.class);
				completionPolicy.addConstructorArgValue(commitInterval);
				completionPolicy.setScope("step");
				propertyValues.addPropertyValue("chunkCompletionPolicy", completionPolicy.getBeanDefinition());
			}
			else {
				propertyValues.addPropertyValue("commitInterval", commitInterval);
			}
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
		ManagedMap skippableExceptions = handleExceptionElement(element, parserContext, "skippable-exception-classes");
		if (StringUtils.hasText(skipLimit)) {
			if (skippableExceptions == null) {
				skippableExceptions = new ManagedMap();
				skippableExceptions.setMergeEnabled(true);
			}
			propertyValues.addPropertyValue("skipLimit", skipLimit);
		}
		if (skippableExceptions != null) {
			// Even if there is no retryLimit, we can still accept exception
			// classes for an abstract parent bean definition
			propertyValues.addPropertyValue("skippableExceptionClasses", skippableExceptions);
		}

		handleItemHandler(bd, "skip-policy", "skipPolicy", null, false, element, parserContext, propertyValues,
				underspecified);

		String retryLimit = element.getAttribute("retry-limit");
		ManagedMap retryableExceptions = handleExceptionElement(element, parserContext, "retryable-exception-classes");
		if (StringUtils.hasText(retryLimit)) {
			if (retryableExceptions == null) {
				retryableExceptions = new ManagedMap();
				retryableExceptions.setMergeEnabled(true);
			}
			propertyValues.addPropertyValue("retryLimit", retryLimit);
		}
		if (retryableExceptions != null) {
			// Even if there is no retryLimit, we can still accept exception
			// classes for an abstract parent bean definition
			propertyValues.addPropertyValue("retryableExceptionClasses", retryableExceptions);
		}

		handleItemHandler(bd, "retry-policy", "retryPolicy", null, false, element, parserContext, propertyValues,
				underspecified);

		String cacheCapacity = element.getAttribute("cache-capacity");
		if (StringUtils.hasText(cacheCapacity)) {
			propertyValues.addPropertyValue("cacheCapacity", cacheCapacity);
		}

		String isReaderTransactionalQueue = element.getAttribute("reader-transactional-queue");
		if (StringUtils.hasText(isReaderTransactionalQueue)) {
			propertyValues.addPropertyValue("isReaderTransactionalQueue", isReaderTransactionalQueue);
		}

		String isProcessorTransactional = element.getAttribute("processor-transactional");
		if (StringUtils.hasText(isProcessorTransactional)) {
			propertyValues.addPropertyValue("processorTransactional", isProcessorTransactional);
		}

		handleRetryListenersElement(element, propertyValues, parserContext, bd);

		handleStreamsElement(element, propertyValues, parserContext);

		stepListenerParser.handleListenersElement(element, bd, parserContext);

	}

	/**
	 * Handle the ItemReader, ItemProcessor, and ItemWriter attributes/elements.
	 */
	private void handleItemHandler(AbstractBeanDefinition enclosing, String handlerName, String propertyName, String adapterClassName, boolean required,
			Element element, ParserContext parserContext, MutablePropertyValues propertyValues, boolean underspecified) {
		String refName = element.getAttribute(handlerName);
		@SuppressWarnings("unchecked")
		List<Element> children = DomUtils.getChildElementsByTagName(element, handlerName);
		if (children.size() == 1) {
			if (StringUtils.hasText(refName)) {
				parserContext.getReaderContext().error(
						"The <" + element.getNodeName() + "/> element may not have both a '" + handlerName
								+ "' attribute and a <" + handlerName + "/> element.", element);
			}
			handleItemHandlerElement(enclosing, propertyName, adapterClassName, propertyValues, children.get(0), parserContext);
		}
		else if (children.size() > 1) {
			parserContext.getReaderContext().error(
					"The <" + handlerName + "/> element may not appear more than once in a single <"
							+ element.getNodeName() + "/>.", element);
		}
		else if (StringUtils.hasText(refName)) {
			propertyValues.addPropertyValue(propertyName, new RuntimeBeanReference(refName));
		}
		else if (required && !underspecified) {
			parserContext.getReaderContext().error(
					"The <" + element.getNodeName() + "/> element has neither a '" + handlerName
							+ "' attribute nor a <" + handlerName + "/> element.", element);
		}
	}

	/**
	 * Handle the &lt;reader/&gt;, &lt;processor/&gt;, or &lt;writer/&gt; that
	 * is defined within the item handler.
	 */
	@SuppressWarnings("unchecked")
	private void handleItemHandlerElement(AbstractBeanDefinition enclosing, String propertyName, String adapterClassName,
			MutablePropertyValues propertyValues, Element element, ParserContext parserContext) {
		List<Element> beanElements = DomUtils.getChildElementsByTagName(element, BEAN_ELE);
		List<Element> refElements = DomUtils.getChildElementsByTagName(element, REF_ELE);
		if (beanElements.size() + refElements.size() != 1) {
			parserContext.getReaderContext().error(
					"The <" + element.getNodeName() + "/> must have exactly one of either a <" + BEAN_ELE
							+ "/> element or a <" + REF_ELE + "/> element.", element);
		}
		else if (beanElements.size() == 1) {
			Element beanElement = beanElements.get(0);
			BeanDefinitionHolder beanDefinitionHolder = parserContext.getDelegate().parseBeanDefinitionElement(
					beanElement, enclosing);
			parserContext.getDelegate().decorateBeanDefinitionIfRequired(beanElement, beanDefinitionHolder);

			propertyValues.addPropertyValue(propertyName, beanDefinitionHolder);
		}
		else if (refElements.size() == 1) {
			propertyValues.addPropertyValue(propertyName,
					parserContext.getDelegate().parsePropertySubElement(refElements.get(0), null));
		}

		handleAdapterMethodAttribute(propertyName, adapterClassName, propertyValues, element);
	}

	/**
	 * Handle the adapter-method attribute by using an
	 * AbstractMethodInvokingDelegator
	 */
	private void handleAdapterMethodAttribute(String propertyName, String adapterClassName,
			MutablePropertyValues stepPvs, Element element) {
		String adapterMethodName = element.getAttribute("adapter-method");
		if (StringUtils.hasText(adapterMethodName)) {
			//
			// Create an adapter
			//
			AbstractBeanDefinition adapterDef = new GenericBeanDefinition();
			adapterDef.setBeanClassName(adapterClassName);
			MutablePropertyValues adapterPvs = adapterDef.getPropertyValues();
			adapterPvs.addPropertyValue("targetMethod", adapterMethodName);
			// Inject the bean into the adapter
			adapterPvs.addPropertyValue("targetObject", stepPvs.getPropertyValue(propertyName).getValue());

			//
			// Inject the adapter into the step
			//
			stepPvs.addPropertyValue(propertyName, adapterDef);
		}
	}

	private void handleRetryListenersElement(Element element, MutablePropertyValues propertyValues,
			ParserContext parserContext, BeanDefinition enclosing) {
		Element listenersElement = DomUtils.getChildElementByTagName(element, "retry-listeners");
		if (listenersElement != null) {
			CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(listenersElement.getTagName(),
					parserContext.extractSource(element));
			parserContext.pushContainingComponent(compositeDef);
			ManagedList retryListenerBeans = new ManagedList();
			retryListenerBeans.setMergeEnabled(listenersElement.hasAttribute(MERGE_ATTR)
					&& Boolean.valueOf(listenersElement.getAttribute(MERGE_ATTR)));
			handleRetryListenerElements(parserContext, listenersElement, retryListenerBeans, enclosing);
			propertyValues.addPropertyValue("retryListeners", retryListenerBeans);
			parserContext.popAndRegisterContainingComponent();
		}
	}

	@SuppressWarnings("unchecked")
	private void handleRetryListenerElements(ParserContext parserContext, Element element, ManagedList beans,
			BeanDefinition enclosing) {
		List<Element> listenerElements = DomUtils.getChildElementsByTagName(element, "listener");
		if (listenerElements != null) {
			for (Element listenerElement : listenerElements) {
				beans.add(AbstractListenerParser.parseListenerElement(listenerElement, parserContext, enclosing));
			}
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
						streamBeans.add(new RuntimeBeanReference(streamRef));
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

	@SuppressWarnings("unchecked")
	private ManagedMap handleExceptionElement(Element element, ParserContext parserContext, String exceptionListName) {
		List<Element> children = DomUtils.getChildElementsByTagName(element, exceptionListName);
		if (children.size() == 1) {
			ManagedMap map = new ManagedMap();
			Element exceptionClassesElement = children.get(0);
			map.setMergeEnabled(exceptionClassesElement.hasAttribute(MERGE_ATTR)
					&& Boolean.valueOf(exceptionClassesElement.getAttribute(MERGE_ATTR)));
			addExceptionClasses("include", true, exceptionClassesElement, map, parserContext);
			addExceptionClasses("exclude", false, exceptionClassesElement, map, parserContext);
			map.put(ForceRollbackForWriteSkipException.class, true);
			return map;
		}
		else if (children.size() > 1) {
			parserContext.getReaderContext().error(
					"The <" + exceptionListName + "/> element may not appear more than once in a single <"
							+ element.getNodeName() + "/>.", element);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void addExceptionClasses(String elementName, boolean include, Element exceptionClassesElement,
			ManagedMap map, ParserContext parserContext) {
		for (Element child : (List<Element>) DomUtils.getChildElementsByTagName(exceptionClassesElement, elementName)) {
			String className = child.getAttribute("class");
			map.put(new TypedStringValue(className, Class.class), include);
		}
	}

}
