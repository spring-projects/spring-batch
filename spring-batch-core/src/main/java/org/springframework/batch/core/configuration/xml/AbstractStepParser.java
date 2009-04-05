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
	 * @param jobRepositoryRef The name of the bean defining the JobRepository.
	 *        Use 'null' if the job-repository is specified on the
	 *        &lt;tasklet/&gt; element; this method will look it up.
	 */
	protected AbstractBeanDefinition parseStep(Element stepElement, ParserContext parserContext, String jobRepositoryRef) {

		AbstractBeanDefinition bd = new GenericBeanDefinition();

		@SuppressWarnings("unchecked")
		List<Element> taskletElements = (List<Element>) DomUtils.getChildElementsByTagName(stepElement, TASKLET_ELE);
		if (taskletElements.size() == 1) {
			boolean stepUnderspecified = CoreNamespaceUtils.isUnderspecified(stepElement);
			parseTasklet(taskletElements.get(0), bd, parserContext, jobRepositoryRef, stepUnderspecified);
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

		return bd;

	}

	private void parseTasklet(Element taskletElement, AbstractBeanDefinition bd, ParserContext parserContext,
			String jobRepositoryRef, boolean stepUnderspecified) {

		bd.setBeanClass(StepParserStepFactoryBean.class);

		String taskletRef = taskletElement.getAttribute(TASKLET_REF_ATTR);
		@SuppressWarnings("unchecked")
		List<Element> chunkElements = (List<Element>) DomUtils.getChildElementsByTagName(taskletElement, CHUNK_ELE);
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
					"Step [" + taskletElement.getAttribute(ID_ATTR) + "] has neither a <" + CHUNK_ELE
							+ "/> element nor a '" + TASKLET_REF_ATTR + "' attribute.", taskletElement);
		}

		setUpBeanDefinitionForTaskletStep(taskletElement, bd, parserContext, jobRepositoryRef);

	}

	private void parseTaskletRef(String taskletRef, MutablePropertyValues propertyValues) {
		if (StringUtils.hasText(taskletRef)) {
			RuntimeBeanReference taskletBeanRef = new RuntimeBeanReference(taskletRef);
			propertyValues.addPropertyValue("tasklet", taskletBeanRef);
		}
	}

	private void setUpBeanDefinitionForTaskletStep(Element taskletElement, AbstractBeanDefinition bd,
			ParserContext parserContext, String jobRepositoryRef) {

		MutablePropertyValues propertyValues = bd.getPropertyValues();

		checkStepAttributes(taskletElement, propertyValues);

		propertyValues.addPropertyValue("jobRepository", resolveJobRepositoryRef(taskletElement, parserContext,
				jobRepositoryRef));

		String transactionManagerRef = taskletElement.getAttribute("transaction-manager");
		RuntimeBeanReference transactionManagerBeanRef = new RuntimeBeanReference(transactionManagerRef);
		propertyValues.addPropertyValue("transactionManager", transactionManagerBeanRef);

		handleTransactionAttributesElement(taskletElement, propertyValues, parserContext);

		handleListenersElement(taskletElement, propertyValues, parserContext);

		handleExceptionElement(taskletElement, parserContext, propertyValues, "no-rollback-exception-classes",
				"noRollbackExceptionClasses");

		bd.setRole(BeanDefinition.ROLE_SUPPORT);

		bd.setSource(parserContext.extractSource(taskletElement));

	}

	private RuntimeBeanReference resolveJobRepositoryRef(Element taskletElement, ParserContext parserContext,
			String jobRepositoryRef) {
		if (!StringUtils.hasText(jobRepositoryRef)) {
			jobRepositoryRef = taskletElement.getAttribute(JOB_REPO_ATTR);
			if (!StringUtils.hasText(jobRepositoryRef)) {
				parserContext.getReaderContext().error(
						"The '" + JOB_REPO_ATTR + "' attribute may exist on an <" + taskletElement.getNodeName()
								+ "/> element.", taskletElement);
			}
		}
		RuntimeBeanReference jobRepositoryBeanRef = new RuntimeBeanReference(jobRepositoryRef);
		return jobRepositoryBeanRef;
	}

	private void handleTransactionAttributesElement(Element stepElement, MutablePropertyValues propertyValues,
			ParserContext parserContext) {
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
	public static void handleExceptionElement(Element element, ParserContext parserContext,
			MutablePropertyValues propertyValues, String subElementName, String propertyName) {
		List<Element> children = DomUtils.getChildElementsByTagName(element, subElementName);
		if (children.size() == 1) {
			Element child = children.get(0);
			String exceptions = DomUtils.getTextValue(child);
			if (StringUtils.hasLength(exceptions)) {
				String[] exceptionArray = StringUtils.tokenizeToStringArray(exceptions, ",\n");
				if (exceptionArray.length > 0) {
					ManagedList managedList = new ManagedList();
					managedList.setMergeEnabled(Boolean.valueOf(child.getAttribute(MERGE_ATTR)));
					managedList.addAll(Arrays.asList(exceptionArray));
					propertyValues.addPropertyValue(propertyName, managedList);
				}
			}
		}
		else if (children.size() > 1) {
			parserContext.getReaderContext().error(
					"The <" + subElementName + "/> element may not appear more than once in a single <"
							+ element.getNodeName() + "/>.", element);
		}

	}

	private void checkStepAttributes(Element stepElement, MutablePropertyValues propertyValues) {
		String startLimit = stepElement.getAttribute("start-limit");
		if (StringUtils.hasText(startLimit)) {
			propertyValues.addPropertyValue("startLimit", startLimit);
		}
		String allowStartIfComplete = stepElement.getAttribute("allow-start-if-complete");
		if (StringUtils.hasText(allowStartIfComplete)) {
			propertyValues.addPropertyValue("allowStartIfComplete", allowStartIfComplete);
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
			listenerBeans.setMergeEnabled(Boolean.valueOf(listenersElement.getAttribute(MERGE_ATTR)));
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
