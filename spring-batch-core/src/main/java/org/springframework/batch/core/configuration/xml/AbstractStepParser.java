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

import java.util.List;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Internal parser for the &lt;step/&gt; elements inside a job. A step element
 * references a bean definition for a
 * {@link org.springframework.batch.core.Step} and goes on to (optionally) list
 * a set of transitions from that step to others with &lt;next on="pattern"
 * to="stepName"/&gt;. Used by the {@link JobParser}.
 * 
 * @see JobParser
 * 
 * @author Dave Syer
 * @author Thomas Risberg
 * @since 2.0
 */
public abstract class AbstractStepParser {

	protected static final String ID_ATTR = "id";

	private static final String PARENT_ATTR = "parent";

	private static final String TASKLET_REF_ATTR = "ref";

	private static final String TASKLET_ELE = "tasklet";

	private static final String CHUNK_ELE = "chunk";

	private static final String LISTENERS_ELE = "listeners";

	private static final String MERGE_ATTR = "merge";

	private static final String TX_ATTRIBUTES_ELE = "transaction-attributes";

	private static final String JOB_REPO_ATTR = "job-repository";

	private static final ChunkElementParser chunkElementParser = new ChunkElementParser();

	private static final StepListenerParser stepListenerParser = new StepListenerParser();

	/**
	 * @param stepElement The &lt;step/&gt; element
	 * @param parserContext
	 * @param jobFactoryRef the reference to the {@link JobParserJobFactoryBean}
	 * from the enclosing tag. Use 'null' if unknown.
	 */
	protected AbstractBeanDefinition parseStep(Element stepElement, ParserContext parserContext, String jobFactoryRef) {

		AbstractBeanDefinition bd = new GenericBeanDefinition();

		@SuppressWarnings("unchecked")
		List<Element> taskletElements = DomUtils.getChildElementsByTagName(stepElement, TASKLET_ELE);
		if (taskletElements.size() == 1) {
			boolean stepUnderspecified = CoreNamespaceUtils.isUnderspecified(stepElement);
			parseTasklet(stepElement, taskletElements.get(0), bd, parserContext, stepUnderspecified);
		}
		else if (taskletElements.size() > 1) {
			parserContext.getReaderContext().error(
					"The '<" + TASKLET_ELE + "/>' element may not appear more than once in a single <"
							+ stepElement.getNodeName() + "/>.", stepElement);
		}

		String parentRef = stepElement.getAttribute(PARENT_ATTR);
		if (StringUtils.hasText(parentRef)) {
			bd.setParentName(parentRef);
		}

		String isAbstract = stepElement.getAttribute("abstract");
		if (StringUtils.hasText(isAbstract)) {
			bd.setAbstract(Boolean.valueOf(isAbstract));
		}

		if (StringUtils.hasText(jobFactoryRef)) {
			bd.setAttribute("jobParserJobFactoryBeanRef", jobFactoryRef);
		}
		
		Element description = DomUtils.getChildElementByTagName(stepElement, "description");
		if (description!=null) {
			bd.setDescription(description.getTextContent());
		}

		return bd;

	}

	private void parseTasklet(Element stepElement, Element taskletElement, AbstractBeanDefinition bd,
			ParserContext parserContext, boolean stepUnderspecified) {

		bd.setBeanClass(StepParserStepFactoryBean.class);
		bd.setAttribute("isNamespaceStep", true);

		String taskletRef = taskletElement.getAttribute(TASKLET_REF_ATTR);
		@SuppressWarnings("unchecked")
		List<Element> chunkElements = DomUtils.getChildElementsByTagName(taskletElement, CHUNK_ELE);
		if (StringUtils.hasText(taskletRef)) {
			if (chunkElements.size() > 0) {
				parserContext.getReaderContext().error(
						"The <" + CHUNK_ELE + "/> element can't be combined with the '" + TASKLET_REF_ATTR + "=\""
								+ taskletRef + "\"' attribute specification for <" + taskletElement.getNodeName()
								+ "/>", taskletElement);
			}
			parseTaskletRef(taskletRef, bd.getPropertyValues());
		}
		else if (chunkElements.size() == 1) {
			chunkElementParser.parse(chunkElements.get(0), bd, parserContext, stepUnderspecified);
		}
		else if (!stepUnderspecified) {
			parserContext.getReaderContext().error(
					"Step [" + stepElement.getAttribute(ID_ATTR) + "] has neither a <" + CHUNK_ELE
							+ "/> element nor a '" + TASKLET_REF_ATTR + "' attribute referencing a Tasklet.",
					taskletElement);
		}

		handleTaskletElement(taskletElement, bd, parserContext);
	}

	private void parseTaskletRef(String taskletRef, MutablePropertyValues propertyValues) {
		if (StringUtils.hasText(taskletRef)) {
			RuntimeBeanReference taskletBeanRef = new RuntimeBeanReference(taskletRef);
			propertyValues.addPropertyValue("tasklet", taskletBeanRef);
		}
	}

	private void handleTaskletElement(Element taskletElement, AbstractBeanDefinition bd, ParserContext parserContext) {
		MutablePropertyValues propertyValues = bd.getPropertyValues();
		handleTaskletAttributes(taskletElement, propertyValues);
		handleTransactionAttributesElement(taskletElement, propertyValues);
		handleListenersElement(taskletElement, propertyValues, parserContext);
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
			try {
				Class<Object> cls = (Class<Object>) Class.forName(className);
				if (!Throwable.class.isAssignableFrom(cls)) {
					parserContext.getReaderContext().error(
							"Non-Throwable class \'" + className + "\' found in <"
									+ exceptionClassesElement.getNodeName() + "/> element.", exceptionClassesElement);
				}
				if (list.contains(cls)) {
					parserContext.getReaderContext().error(
							"Duplicate entry for class \'" + className + "\' found in <"
									+ exceptionClassesElement.getNodeName() + "/> element.", exceptionClassesElement);
				}
				list.add(cls);
			}
			catch (ClassNotFoundException e) {
				parserContext.getReaderContext().error(
						"Cannot find class \'" + className + "\', given as an attribute of the <" + elementName
								+ "/> element.", child);
			}
		}
	}

	private void handleTaskletAttributes(Element taskletElement, MutablePropertyValues propertyValues) {
		String jobRepositoryRef = taskletElement.getAttribute(JOB_REPO_ATTR);
		if (StringUtils.hasText(jobRepositoryRef)) {
			propertyValues.addPropertyValue("jobRepository", new RuntimeBeanReference(jobRepositoryRef));
		}
		String transactionManagerRef = taskletElement.getAttribute("transaction-manager");
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
		String taskExecutorBeanId = taskletElement.getAttribute("task-executor");
		if (StringUtils.hasText(taskExecutorBeanId)) {
			RuntimeBeanReference taskExecutorRef = new RuntimeBeanReference(taskExecutorBeanId);
			propertyValues.addPropertyValue("taskExecutor", taskExecutorRef);
		}
		String throttleLimit = taskletElement.getAttribute("throttle-limit");
		if (StringUtils.hasText(throttleLimit)) {
		       propertyValues.addPropertyValue("throttleLimit", throttleLimit);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleListenersElement(Element stepElement, MutablePropertyValues propertyValues,
			ParserContext parserContext) {
		List<Element> listenersElements = DomUtils.getChildElementsByTagName(stepElement, LISTENERS_ELE);
		if (listenersElements.size() == 1) {
			Element listenersElement = listenersElements.get(0);
			CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(listenersElement.getTagName(),
					parserContext.extractSource(stepElement));
			parserContext.pushContainingComponent(compositeDef);
			ManagedList listenerBeans = new ManagedList();
			listenerBeans.setMergeEnabled(listenersElement.hasAttribute(MERGE_ATTR)
					&& Boolean.valueOf(listenersElement.getAttribute(MERGE_ATTR)));
			List<Element> listenerElements = DomUtils.getChildElementsByTagName(listenersElement, "listener");
			if (listenerElements != null) {
				for (Element listenerElement : listenerElements) {
					listenerBeans.add(stepListenerParser.parse(listenerElement, parserContext));
				}
			}
			propertyValues.addPropertyValue("listeners", listenerBeans);
			parserContext.popAndRegisterContainingComponent();
		}
	}

}
