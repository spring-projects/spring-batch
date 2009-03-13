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

import org.springframework.batch.core.step.tasklet.TaskletStep;
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
	 * @param element
	 * @param parserContext
	 * @return a BeanDefinition if possible
	 */
	protected AbstractBeanDefinition parseTasklet(Element element, ParserContext parserContext, String jobRepositoryRef) {

		String taskletRef = element.getAttribute("tasklet");
		@SuppressWarnings("unchecked")
		List<Element> taskletElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "tasklet");
		boolean taskletElementExists = taskletElements.size() > 0;
		AbstractBeanDefinition bd = null;
		if (StringUtils.hasText(taskletRef)) {
			if (taskletElementExists) {
				parserContext.getReaderContext().error(
						"The <" + taskletElements.get(0).getNodeName()
								+ "> element can't be combined with the 'tasklet=\"" + taskletRef
								+ "\"' attribute specification for <" + element.getNodeName() + ">", element);
			}
			bd = parseTaskletRef(element, taskletRef, parserContext, jobRepositoryRef);
		}
		else if (taskletElementExists) {
			Element taskElement = taskletElements.get(0);
			bd = parseTaskletElement(element, taskElement, parserContext, jobRepositoryRef);
		}
		return bd;
	
	}

	/**
	 * @param stepElement
	 * @param taskletRef
	 * @param parserContext
	 */
	private AbstractBeanDefinition parseTaskletRef(Element stepElement, String taskletRef,
			ParserContext parserContext, String jobRepositoryRef) {

		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(TaskletStep.class);

		if (StringUtils.hasText(taskletRef)) {
			RuntimeBeanReference taskletBeanRef = new RuntimeBeanReference(taskletRef);
			bd.getPropertyValues().addPropertyValue("tasklet", taskletBeanRef);
		}

		setUpBeanDefinition(stepElement, bd, parserContext, jobRepositoryRef);

		return bd;

	}

	/**
	 * @param element
	 * @param parserContext
	 */
	private AbstractBeanDefinition parseTaskletElement(Element stepElement, Element element,
			ParserContext parserContext, String jobRepositoryRef) {

		AbstractBeanDefinition bd = taskletElementParser.parse(element, parserContext);
		setUpBeanDefinition(stepElement, bd, parserContext, jobRepositoryRef);
		return bd;

	}

	protected void setUpBeanDefinition(Element stepElement, AbstractBeanDefinition bd, ParserContext parserContext,
			String jobRepositoryRef) {
		checkStepAttributes(stepElement, bd);

		RuntimeBeanReference jobRepositoryBeanRef = new RuntimeBeanReference(jobRepositoryRef);
		bd.getPropertyValues().addPropertyValue("jobRepository", jobRepositoryBeanRef);

		String transactionManagerRef = stepElement.getAttribute("transaction-manager");
		RuntimeBeanReference transactionManagerBeanRef = new RuntimeBeanReference(transactionManagerRef);
		bd.getPropertyValues().addPropertyValue("transactionManager", transactionManagerBeanRef);

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
	private void handleListenersElement(Element element, BeanDefinition bd, ParserContext parserContext) {
		Element listenersElement = DomUtils.getChildElementByTagName(element, "listeners");
		if (listenersElement != null) {
			CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(listenersElement.getTagName(),
					parserContext.extractSource(element));
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
	}

}
