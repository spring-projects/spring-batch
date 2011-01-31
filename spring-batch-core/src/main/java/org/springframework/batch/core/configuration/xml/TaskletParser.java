/*
 * Copyright 2006-2010 the original author or authors.
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

import java.util.List;

import org.springframework.batch.core.step.tasklet.MethodInvokingTaskletAdapter;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parse a tasklet element for a step.
 * 
 * @author Dave Syer
 * 
 * @since 2.1
 * 
 */
public class TaskletParser {

	/**
	 * 
	 */
	private static final String TRANSACTION_MANAGER_ATTR = "transaction-manager";

	private static final String TASKLET_REF_ATTR = "ref";

	private static final String TASKLET_METHOD_ATTR = "method";

	private static final String BEAN_ELE = "bean";

	private static final String REF_ELE = "ref";

	private static final String TASK_EXECUTOR_ATTR = "task-executor";

	private static final String CHUNK_ELE = "chunk";

	private static final String TX_ATTRIBUTES_ELE = "transaction-attributes";

	private static final String MERGE_ATTR = "merge";

	private static final ChunkElementParser chunkElementParser = new ChunkElementParser();

	// TODO: BATCH-1689, make this StepListenerParser.taskletListenerMetaData()
	private static final StepListenerParser stepListenerParser = new StepListenerParser();

	public void parseTasklet(Element stepElement, Element taskletElement, AbstractBeanDefinition bd,
			ParserContext parserContext, boolean stepUnderspecified) {

		bd.setBeanClass(StepParserStepFactoryBean.class);
		bd.setAttribute("isNamespaceStep", true);

		String taskletRef = taskletElement.getAttribute(TASKLET_REF_ATTR);
		String taskletMethod = taskletElement.getAttribute(TASKLET_METHOD_ATTR);
		@SuppressWarnings("unchecked")
		List<Element> chunkElements = DomUtils.getChildElementsByTagName(taskletElement, CHUNK_ELE);
		@SuppressWarnings("unchecked")
		List<Element> beanElements = DomUtils.getChildElementsByTagName(taskletElement, BEAN_ELE);
		@SuppressWarnings("unchecked")
		List<Element> refElements = DomUtils.getChildElementsByTagName(taskletElement, REF_ELE);

		validateTaskletAttributesAndSubelements(taskletElement, parserContext, stepUnderspecified, taskletRef,
				chunkElements, beanElements, refElements);

		if (!chunkElements.isEmpty()) {
			chunkElementParser.parse(chunkElements.get(0), bd, parserContext, stepUnderspecified);
		}
		else {
			BeanMetadataElement bme = null;
			if (StringUtils.hasText(taskletRef)) {
				bme = new RuntimeBeanReference(taskletRef);
			}
			else if (beanElements.size() == 1) {
				Element beanElement = beanElements.get(0);
				BeanDefinitionHolder beanDefinitionHolder = parserContext.getDelegate().parseBeanDefinitionElement(
						beanElement, bd);
				parserContext.getDelegate().decorateBeanDefinitionIfRequired(beanElement, beanDefinitionHolder);
				bme = beanDefinitionHolder;
			}
			else if (refElements.size() == 1) {
				bme = (BeanMetadataElement) parserContext.getDelegate().parsePropertySubElement(refElements.get(0),
						null);
			}

			if (StringUtils.hasText(taskletMethod)) {
				bme = getTaskletAdapter(bme, taskletMethod);
			}

			if (bme != null) {
				bd.getPropertyValues().addPropertyValue("tasklet", bme);
			}
		}

		handleTaskletElement(taskletElement, bd, parserContext);
	}

	/**
	 * Create a {@link MethodInvokingTaskletAdapter} for the POJO specified.
	 */
	private BeanMetadataElement getTaskletAdapter(BeanMetadataElement bme, String taskletMethod) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingTaskletAdapter.class);
		builder.addPropertyValue("targetMethod", taskletMethod);
		builder.addPropertyValue("targetObject", bme);
		return builder.getBeanDefinition();
	}

	private void validateTaskletAttributesAndSubelements(Element taskletElement, ParserContext parserContext,
			boolean stepUnderspecified, String taskletRef, List<Element> chunkElements, List<Element> beanElements,
			List<Element> refElements) {
		int total = (StringUtils.hasText(taskletRef) ? 1 : 0) + chunkElements.size() + beanElements.size()
				+ refElements.size();

		StringBuilder found = new StringBuilder();
		if (total > 1) {
			if (StringUtils.hasText(taskletRef)) {
				found.append("'" + TASKLET_REF_ATTR + "' attribute, ");
			}
			if (chunkElements.size() == 1) {
				found.append("<" + CHUNK_ELE + "/> element, ");
			}
			else if (chunkElements.size() > 1) {
				found.append(chunkElements.size() + " <" + CHUNK_ELE + "/> elements, ");
			}
			if (beanElements.size() == 1) {
				found.append("<" + BEAN_ELE + "/> element, ");
			}
			else if (beanElements.size() > 1) {
				found.append(beanElements.size() + " <" + BEAN_ELE + "/> elements, ");
			}
			if (refElements.size() == 1) {
				found.append("<" + REF_ELE + "/> element, ");
			}
			else if (refElements.size() > 1) {
				found.append(refElements.size() + " <" + REF_ELE + "/> elements, ");
			}
			found.delete(found.length() - 2, found.length());
		}
		else {
			found.append("None");
		}

		String error = null;
		if (stepUnderspecified) {
			if (total > 1) {
				error = "may not have more than";
			}
		}
		else if (total != 1) {
			error = "must have exactly";
		}

		if (error != null) {
			parserContext.getReaderContext().error(
					"The <" + taskletElement.getTagName() + "/> element " + error + " one of: '" + TASKLET_REF_ATTR
							+ "' attribute, <" + CHUNK_ELE + "/> element, <" + BEAN_ELE + "/> attribute, or <"
							+ REF_ELE + "/> element.  Found: " + found + ".", taskletElement);
		}
	}

	private void handleTaskletElement(Element taskletElement, AbstractBeanDefinition bd, ParserContext parserContext) {
		MutablePropertyValues propertyValues = bd.getPropertyValues();
		handleTaskletAttributes(taskletElement, propertyValues);
		handleTransactionAttributesElement(taskletElement, propertyValues);
		stepListenerParser.handleListenersElement(taskletElement, bd, parserContext);
		handleExceptionElement(taskletElement, parserContext, propertyValues, "no-rollback-exception-classes",
				"noRollbackExceptionClasses");
		bd.setRole(BeanDefinition.ROLE_SUPPORT);
		bd.setSource(parserContext.extractSource(taskletElement));
	}

	private void handleTransactionAttributesElement(Element stepElement, MutablePropertyValues propertyValues) {
		@SuppressWarnings("unchecked")
		List<Element> txAttrElements = DomUtils.getChildElementsByTagName(stepElement, TX_ATTRIBUTES_ELE);
		if (txAttrElements.size() == 1) {
			Element txAttrElement = txAttrElements.get(0);
			String propagation = txAttrElement.getAttribute("propagation");
			if (StringUtils.hasText(propagation)) {
				propertyValues.addPropertyValue("propagation", propagation);
			}
			String isolation = txAttrElement.getAttribute("isolation");
			if (StringUtils.hasText(isolation)) {
				propertyValues.addPropertyValue("isolation", isolation);
			}
			String timeout = txAttrElement.getAttribute("timeout");
			if (StringUtils.hasText(timeout)) {
				propertyValues.addPropertyValue("transactionTimeout", timeout);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void handleExceptionElement(Element element, ParserContext parserContext,
			MutablePropertyValues propertyValues, String exceptionListName, String propertyName) {
		List<Element> children = DomUtils.getChildElementsByTagName(element, exceptionListName);
		if (children.size() == 1) {
			Element exceptionClassesElement = children.get(0);
			ManagedList list = new ManagedList();
			list.setMergeEnabled(exceptionClassesElement.hasAttribute(MERGE_ATTR)
					&& Boolean.valueOf(exceptionClassesElement.getAttribute(MERGE_ATTR)));
			addExceptionClasses("include", exceptionClassesElement, list, parserContext);
			propertyValues.addPropertyValue(propertyName, list);
		}
		else if (children.size() > 1) {
			parserContext.getReaderContext().error(
					"The <" + exceptionListName + "/> element may not appear more than once in a single <"
							+ element.getNodeName() + "/>.", element);
		}
	}

	@SuppressWarnings("unchecked")
	private void addExceptionClasses(String elementName, Element exceptionClassesElement, ManagedList list,
			ParserContext parserContext) {
		for (Element child : (List<Element>) DomUtils.getChildElementsByTagName(exceptionClassesElement, elementName)) {
			String className = child.getAttribute("class");
			list.add(new TypedStringValue(className, Class.class));
		}
	}

	private void handleTaskletAttributes(Element taskletElement, MutablePropertyValues propertyValues) {
		String transactionManagerRef = taskletElement.getAttribute(TRANSACTION_MANAGER_ATTR);
		if (StringUtils.hasText(transactionManagerRef)) {
			propertyValues.addPropertyValue("transactionManager", new RuntimeBeanReference(transactionManagerRef));
		}
		String startLimit = taskletElement.getAttribute("start-limit");
		if (StringUtils.hasText(startLimit)) {
			propertyValues.addPropertyValue("startLimit", startLimit);
		}
		String allowStartIfComplete = taskletElement.getAttribute("allow-start-if-complete");
		if (StringUtils.hasText(allowStartIfComplete)) {
			propertyValues.addPropertyValue("allowStartIfComplete", allowStartIfComplete);
		}
		String taskExecutorBeanId = taskletElement.getAttribute(TASK_EXECUTOR_ATTR);
		if (StringUtils.hasText(taskExecutorBeanId)) {
			RuntimeBeanReference taskExecutorRef = new RuntimeBeanReference(taskExecutorBeanId);
			propertyValues.addPropertyValue("taskExecutor", taskExecutorRef);
		}
		String throttleLimit = taskletElement.getAttribute("throttle-limit");
		if (StringUtils.hasText(throttleLimit)) {
			propertyValues.addPropertyValue("throttleLimit", throttleLimit);
		}
	}

}
