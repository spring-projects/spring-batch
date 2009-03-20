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

	private TaskletElementParser taskletElementParser = new TaskletElementParser();

	private StepListenerParser stepListenerParser = new StepListenerParser();

	/**
	 * @param stepElement
	 * @param parserContext
	 * @return a BeanDefinition if possible
	 */
	protected AbstractBeanDefinition parseTasklet(Element stepElement, ParserContext parserContext,
			String jobRepositoryRef) {

		String taskletRef = stepElement.getAttribute("tasklet");
		@SuppressWarnings("unchecked")
		List<Element> taskletElements = (List<Element>) DomUtils.getChildElementsByTagName(stepElement, "tasklet");
		boolean stepUnderspecified = CoreNamespaceUtils.isUnderspecified(stepElement);
		AbstractBeanDefinition bd = null;
		if (StringUtils.hasText(taskletRef)) {
			if (taskletElements.size() > 0) {
				parserContext.getReaderContext().error(
						"The <" + taskletElements.get(0).getNodeName()
								+ "> element can't be combined with the 'tasklet=\"" + taskletRef
								+ "\"' attribute specification for <" + stepElement.getNodeName() + ">", stepElement);
			}
			bd = parseTaskletRef(stepElement, taskletRef, parserContext, jobRepositoryRef);
		}
		else if (taskletElements.size() == 1) {
			Element taskElement = taskletElements.get(0);
			bd = taskletElementParser.parse(taskElement, parserContext, stepUnderspecified);
		}
		else if (taskletElements.size() > 1) {
			parserContext.getReaderContext().error(
					"The 'tasklet' element may not appear more than once in a single <step/>.", stepElement);
		}

		if (bd != null) {
			setUpBeanDefinition(stepElement, bd, parserContext, jobRepositoryRef);
		}
		else if (!stepUnderspecified) {
			parserContext.getReaderContext().error(
					"Step [" + stepElement.getAttribute("id")
							+ "] has neither a <tasklet/> element nor a 'tasklet' attribute.", stepElement);
		}

		return bd;

	}

	/**
	 * @param stepElement
	 * @param taskletRef
	 * @param parserContext
	 */
	private AbstractBeanDefinition parseTaskletRef(Element stepElement, String taskletRef, ParserContext parserContext,
			String jobRepositoryRef) {

		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(StepParserStepFactoryBean.class);

		if (StringUtils.hasText(taskletRef)) {
			RuntimeBeanReference taskletBeanRef = new RuntimeBeanReference(taskletRef);
			bd.getPropertyValues().addPropertyValue("tasklet", taskletBeanRef);
		}

		return bd;

	}

	@SuppressWarnings("unchecked")
	protected void setUpBeanDefinition(Element stepElement, AbstractBeanDefinition bd, ParserContext parserContext,
			String jobRepositoryRef) {
		checkStepAttributes(stepElement, bd);

		bd.setAbstract(Boolean.valueOf(stepElement.getAttribute("abstract")));

		RuntimeBeanReference jobRepositoryBeanRef = new RuntimeBeanReference(jobRepositoryRef);
		bd.getPropertyValues().addPropertyValue("jobRepository", jobRepositoryBeanRef);

		String transactionManagerRef = stepElement.getAttribute("transaction-manager");
		RuntimeBeanReference transactionManagerBeanRef = new RuntimeBeanReference(transactionManagerRef);
		bd.getPropertyValues().addPropertyValue("transactionManager", transactionManagerBeanRef);

		List<Element> txAttrElements = DomUtils.getChildElementsByTagName(stepElement, "transaction-attributes");
		if (txAttrElements.size() == 1) {
			Element txAttrElement = txAttrElements.get(0);
			String attributes = DomUtils.getTextValue(txAttrElement);
			if (StringUtils.hasLength(attributes)) {
				String[] attributesArray = StringUtils.tokenizeToStringArray(attributes, ",\n");
				if (attributesArray.length > 0) {
					ManagedList managedList = new ManagedList();
					managedList.setMergeEnabled(Boolean.valueOf(txAttrElement.getAttribute("merge")));
					managedList.addAll(Arrays.asList(attributesArray));
					bd.getPropertyValues().addPropertyValue("transactionAttributeList", managedList);
				}
			}
		}
		else if (txAttrElements.size() > 1) {
			parserContext.getReaderContext().error(
					"The 'transaction-attribute' element may not appear more than once in a single <step/>.",
					stepElement);
		}

		handleListenersElement(stepElement, bd, parserContext);

		bd.setRole(BeanDefinition.ROLE_SUPPORT);

		bd.setSource(parserContext.extractSource(stepElement));

	}

	private void checkStepAttributes(Element stepElement, AbstractBeanDefinition bd) {
		String startLimit = stepElement.getAttribute("start-limit");
		if (StringUtils.hasText(startLimit)) {
			bd.getPropertyValues().addPropertyValue("startLimit", startLimit);
		}
		String allowStartIfComplete = stepElement.getAttribute("allow-start-if-complete");
		if (StringUtils.hasText(allowStartIfComplete)) {
			bd.getPropertyValues().addPropertyValue("allowStartIfComplete", allowStartIfComplete);
		}
		String parentRef = stepElement.getAttribute("parent");
		if (StringUtils.hasText(parentRef)) {
			bd.setParentName(parentRef);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleListenersElement(Element stepElement, BeanDefinition bd, ParserContext parserContext) {
		List<Element> listenersElements = DomUtils.getChildElementsByTagName(stepElement, "listeners");
		if (listenersElements.size() == 1) {
			Element listenersElement = listenersElements.get(0);
			CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(listenersElement.getTagName(),
					parserContext.extractSource(stepElement));
			parserContext.pushContainingComponent(compositeDef);
			ManagedList listenerBeans = new ManagedList();
			listenerBeans.setMergeEnabled(Boolean.valueOf(listenersElement.getAttribute("merge")));
			List<Element> listenerElements = DomUtils.getChildElementsByTagName(listenersElement, "listener");
			if (listenerElements != null) {
				for (Element listenerElement : listenerElements) {
					listenerBeans.add(stepListenerParser.parse(listenerElement, parserContext));
				}
			}
			bd.getPropertyValues().addPropertyValue("listeners", listenerBeans);
			parserContext.popAndRegisterContainingComponent();
		}
		else if (listenersElements.size() > 1) {
			parserContext.getReaderContext().error("The 'listeners' element may not appear more than once in a single <step/>.",
					stepElement);
		}
	}

}
