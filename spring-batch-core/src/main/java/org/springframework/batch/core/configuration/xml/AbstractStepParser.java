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

import java.util.ArrayList;
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
 * references a bean definition for a {@link org.springframework.batch.core.Step} and goes on to (optionally)
 * list a set of transitions from that step to others with &lt;next on="pattern"
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
	 * @param taskletRef
	 * @param parserContext
	 */
	protected AbstractBeanDefinition parseTaskletRef(Element stepElement, String taskletRef, ParserContext parserContext, String jobRepositoryRef) {

		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(TaskletStep.class);

        if (StringUtils.hasText(taskletRef)) {
            RuntimeBeanReference taskletBeanRef = new RuntimeBeanReference(taskletRef);
            bd.getPropertyValues().addPropertyValue("tasklet", taskletBeanRef);
        }

        checkStepAttributes(stepElement, bd);

        RuntimeBeanReference jobRepositoryBeanRef = new RuntimeBeanReference(jobRepositoryRef);
        bd.getPropertyValues().addPropertyValue("jobRepository", jobRepositoryBeanRef);

        String transactionManagerRef = stepElement.getAttribute("transaction-manager");
        RuntimeBeanReference transactionManagerBeanRef = new RuntimeBeanReference(transactionManagerRef);
        bd.getPropertyValues().addPropertyValue("transactionManager", transactionManagerBeanRef);
		
        handleListenersElement(stepElement, bd, parserContext, "stepExecutionListeners");
        
        bd.setRole(BeanDefinition.ROLE_SUPPORT);
        
        bd.setSource(parserContext.extractSource(stepElement));
        
        return bd;

    }

	/**
	 * @param element
	 * @param parserContext
	 */
	protected AbstractBeanDefinition parseTaskletElement(Element stepElement, Element element, ParserContext parserContext, String jobRepositoryRef) {

    	AbstractBeanDefinition bd = taskletElementParser.parseTaskletElement(element, parserContext);

		// now, set the properties on the new bean 
        checkStepAttributes(stepElement, bd);

        RuntimeBeanReference jobRepositoryBeanRef = new RuntimeBeanReference(jobRepositoryRef);
        bd.getPropertyValues().addPropertyValue("jobRepository", jobRepositoryBeanRef);

        String transactionManagerRef = stepElement.getAttribute("transaction-manager");
        RuntimeBeanReference tx = new RuntimeBeanReference(transactionManagerRef);
        bd.getPropertyValues().addPropertyValue("transactionManager", tx);
        
        handleListenersElement(stepElement, bd, parserContext, "listeners");
        
        bd.setRole(BeanDefinition.ROLE_SUPPORT);
        
        bd.setSource(parserContext.extractSource(stepElement));

        return bd;
        
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
	private void handleListenersElement(Element element, BeanDefinition bd, ParserContext parserContext, String property) {
		Element listenersElement = 
        	DomUtils.getChildElementByTagName(element, "listeners");
		if (listenersElement != null) {
			CompositeComponentDefinition compositeDef =
				new CompositeComponentDefinition(listenersElement.getTagName(), parserContext.extractSource(element));
			parserContext.pushContainingComponent(compositeDef);
			List<Object> listenerBeans = new ArrayList<Object>(); 
			List<Element> listenerElements = 
				DomUtils.getChildElementsByTagName(listenersElement, "listener");
			if (listenerElements != null) {
				for (Element listenerElement : listenerElements) {
					listenerBeans.add(stepListenerParser.parse(listenerElement, parserContext));
				}
			}
	        ManagedList arguments = new ManagedList();
	        arguments.addAll(listenerBeans);
        	bd.getPropertyValues().addPropertyValue(property, arguments);
        	parserContext.popAndRegisterContainingComponent();
		}
	}

}
