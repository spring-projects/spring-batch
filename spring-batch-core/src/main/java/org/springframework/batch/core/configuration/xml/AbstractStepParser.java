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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

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
	
	TaskletElementParser taskletElementParser = new TaskletElementParser();

	/**
	 * @param stepElement
	 * @param taskletRef
	 * @param parserContext
	 */
	protected AbstractBeanDefinition parseTaskletRef(Element stepElement, String taskletRef, ParserContext parserContext, String jobRepositoryRef) {

    	RootBeanDefinition bd = new RootBeanDefinition("org.springframework.batch.core.step.tasklet.TaskletStep", null, null);

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
			handleStepListenerElements(parserContext, listenersElement,
					listenerBeans);
	        ManagedList arguments = new ManagedList();
	        arguments.addAll(listenerBeans);
        	bd.getPropertyValues().addPropertyValue(property, arguments);
        	parserContext.popAndRegisterContainingComponent();
		}
	}

	@SuppressWarnings("unchecked")
	private void handleStepListenerElements(ParserContext parserContext,
			Element element, List<Object> beans) {
		List<Element> listenerElements = 
			DomUtils.getChildElementsByTagName(element, "listener");
		if (listenerElements != null) {
			for (Element listenerElement : listenerElements) {
				BeanDefinitionBuilder listenerBuilder = 
					BeanDefinitionBuilder.genericBeanDefinition("org.springframework.batch.core.listener.StepListenerFactoryBean");
				String id = listenerElement.getAttribute("id");
				String listenerRef = listenerElement.getAttribute("ref");
				String className = listenerElement.getAttribute("class");
				checkListenerElementAttributes(parserContext, element,
						listenerElement, id, listenerRef, className);
				if (StringUtils.hasText(listenerRef)) {
			        listenerBuilder.addPropertyReference("delegate", listenerRef);
				}
				else if (StringUtils.hasText(className)) {
					RootBeanDefinition beanDef = new RootBeanDefinition(className, null, null);
					listenerBuilder.addPropertyValue("delegate", beanDef);
				}
				else {
					parserContext.getReaderContext().error("Neither 'ref' or 'class' specified for <" + listenerElement.getTagName() + "> element", element);
				}
				
				ManagedMap metaDataMap = new ManagedMap();
				String[] methodNameAttributes = new String[] {
						"before-step-method",
						"after-step-method",
						"before-chunk-method",
						"after-chunk-method",
						"before-read-method",
						"after-read-method",
						"on-read-error-method",
						"before-process-method",
						"after-process-method",
						"on-process-error-method",
						"before-write-method",
						"after-write-method",
						"on-write-error-method",
						"on-skip-in-read-method",
						"on-skip-in-process-method",
						"on-skip-in-write-method"
				};
				for (String metaDataPropertyName : methodNameAttributes) {
					String listenerMethod = listenerElement.getAttribute(metaDataPropertyName);
					if(StringUtils.hasText(listenerMethod)){
						metaDataMap.put(metaDataPropertyName, listenerMethod);
					}
				}
				listenerBuilder.addPropertyValue("metaDataMap", metaDataMap);
				
				AbstractBeanDefinition beanDef = listenerBuilder.getBeanDefinition();
				if (!StringUtils.hasText(id)) {
					id = parserContext.getReaderContext().generateBeanName(beanDef);
				}
				beans.add(beanDef);
			}
		}
	}

	private void checkListenerElementAttributes(ParserContext parserContext,
			Element element, Element listenerElement, String id,
			String listenerRef, String className) {
		if ((StringUtils.hasText(id) || StringUtils.hasText(className)) 
				&& StringUtils.hasText(listenerRef)) {
			NamedNodeMap attributeNodes = listenerElement.getAttributes();
			StringBuilder attributes = new StringBuilder();
			for (int i = 0; i < attributeNodes.getLength(); i++) {
				if (i > 0) {
					attributes.append(" ");
				}
				attributes.append(attributeNodes.item(i));
			}
			parserContext.getReaderContext().error("Both 'ref' and " +
					(StringUtils.hasText(id) ? "'id'" : "'class'") +
					" specified; use 'class' with an optional 'id' or just 'ref' for <" + 
					listenerElement.getTagName() + "> element specified with attributes: " + attributes, element);
		}
	}

}
