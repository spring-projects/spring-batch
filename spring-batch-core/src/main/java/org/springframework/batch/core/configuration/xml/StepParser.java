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
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
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
public class StepParser {

	// For generating unique state names for end transitions
	private static int endCounter = 0;

	/**
	 * Parse the step and turn it into a list of transitions.
	 * 
	 * @param element the &lt;step/gt; element to parse
	 * @param parserContext the parser context for the bean factory
	 * @return a collection of bean definitions for {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 * instances objects
	 */
	public Collection<RuntimeBeanReference> parse(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder stateBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition("org.springframework.batch.core.job.flow.support.state.StepState");
		String stepRef = element.getAttribute("name");
		String taskletRef = element.getAttribute("tasklet");

		if (!StringUtils.hasText(stepRef)) {
			parserContext.getReaderContext().error("The name attribute can't be empty for <" + element.getNodeName() + ">", element);
		}
		
		@SuppressWarnings("unchecked")
		List<Element> processTaskElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "tasklet");
		if (StringUtils.hasText(taskletRef)) {
			handleTaskletRef(element, taskletRef, parserContext);
			stateBuilder.addConstructorArgReference(stepRef);
		}
		else if (processTaskElements.size() > 0) {
			Element taskElement = processTaskElements.get(0);
			handleTaskletElement(element, taskElement, parserContext);
			stateBuilder.addConstructorArgReference(stepRef);
		}
		else if (StringUtils.hasText(stepRef)) {
				stateBuilder.addConstructorArgReference(stepRef);
		}
		else {
			parserContext.getReaderContext().error("Incomplete configuration detected while creating step with name " + stepRef, element);
		}
		return getNextElements(parserContext, stateBuilder.getBeanDefinition(), element);

	}

	/**
	 * @param parserContext
	 * @param stateDef
	 * @param element
	 * @return a collection of {@link org.springframework.batch.core.job.flow.support.StateTransition} references
	 */
	public static Collection<RuntimeBeanReference> getNextElements(ParserContext parserContext,
			BeanDefinition stateDef, Element element) {

		Collection<RuntimeBeanReference> list = new ArrayList<RuntimeBeanReference>();

		String shortNextAttribute = element.getAttribute("next");
		boolean hasNextAttribute = StringUtils.hasText(shortNextAttribute);
		if (hasNextAttribute) {
			list.add(getStateTransitionReference(parserContext, stateDef, null, shortNextAttribute));
		}

		@SuppressWarnings("unchecked")
		List<Element> nextElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "next");
		@SuppressWarnings("unchecked")
		List<Element> stopElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "stop");
		nextElements.addAll(stopElements);
		@SuppressWarnings("unchecked")
		List<Element> endElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "end");
		nextElements.addAll(endElements);

		for (Element nextElement : nextElements) {
			String onAttribute = nextElement.getAttribute("on");
			String nextAttribute = nextElement.getAttribute("to");
			if (hasNextAttribute && onAttribute.equals("*")) {
				parserContext.getReaderContext().error("Duplicate transition pattern found for '*' "
						+ "(only specify one of next= attribute at step level and next element with on='*')",
						element);
			}

			RuntimeBeanReference additionalState = null;
			
			String name = nextElement.getNodeName();
			if ("stop".equals(name) || "end".equals(name)) {

				String statusName = nextElement.getAttribute("status");
				String status = StringUtils.hasText(statusName) ? statusName : "STOPPED";
				String nextOnEnd = StringUtils.hasText(statusName) ? null : nextAttribute;

				BeanDefinitionBuilder endBuilder = 
					BeanDefinitionBuilder.genericBeanDefinition("org.springframework.batch.core.job.flow.support.state.EndState");
				endBuilder.addConstructorArgValue(status);
				String endName = "stop".equals(name) ? "end" + (endCounter++) : null;

				endBuilder.addConstructorArgValue(endName);
				additionalState = getStateTransitionReference(parserContext, endBuilder.getBeanDefinition(), onAttribute, nextOnEnd);
				nextAttribute = endName;
	
			}
			list.add(getStateTransitionReference(parserContext, stateDef, onAttribute, nextAttribute));
			if(additionalState != null)
			{
				//
				// Must be added after the state to ensure that the state is the first in the list
				//
				list.add(additionalState);
			}
		}

		if(hasNextAttribute && nextElements.isEmpty()) 
		{ 
		    list.add(getStateTransitionReference(parserContext, stateDef, "FAILED", null));
		} 

		if (list.isEmpty() && !hasNextAttribute) {
			list.add(getStateTransitionReference(parserContext, stateDef, null, null));
		}

		return list;
	}

	/**
	 * @param parserContext the parser context
	 * @param stateDefinition a reference to the state implementation
	 * @param on the pattern value
	 * @param next the next step id
	 * @return a bean definition for a {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 */
	public static RuntimeBeanReference getStateTransitionReference(ParserContext parserContext,
			BeanDefinition stateDefinition, String on, String next) {

		BeanDefinitionBuilder nextBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition("org.springframework.batch.core.job.flow.support.StateTransition");
		nextBuilder.addConstructorArgValue(stateDefinition);

		if (StringUtils.hasText(on)) {
			nextBuilder.addConstructorArgValue(on);
		}

		if (StringUtils.hasText(next)) {
			nextBuilder.setFactoryMethod("createStateTransition");
			nextBuilder.addConstructorArgValue(next);
		}
		else {
			nextBuilder.setFactoryMethod("createEndStateTransition");
		}

		// TODO: do we need to use RuntimeBeanReference?
		AbstractBeanDefinition nextDef = nextBuilder.getBeanDefinition();
		String nextDefName = parserContext.getReaderContext().generateBeanName(nextDef);
		BeanComponentDefinition nextDefComponent = new BeanComponentDefinition(nextDef, nextDefName);
		parserContext.registerBeanComponent(nextDefComponent);

		return new RuntimeBeanReference(nextDefName);

	}

	/**
	 * @param stepElement
	 * @param taskletRef
	 * @param parserContext
	 */
	private void handleTaskletRef(Element stepElement, String taskletRef, ParserContext parserContext) {

    	RootBeanDefinition bd = new RootBeanDefinition("org.springframework.batch.core.step.tasklet.TaskletStep", null, null);

        if (StringUtils.hasText(taskletRef)) {
            RuntimeBeanReference taskletBeanRef = new RuntimeBeanReference(taskletRef);
            bd.getPropertyValues().addPropertyValue("tasklet", taskletBeanRef);
        }

        String jobRepositoryRef = stepElement.getAttribute("job-repository");
        RuntimeBeanReference jobRepositoryBeanRef = new RuntimeBeanReference(jobRepositoryRef);
        bd.getPropertyValues().addPropertyValue("jobRepository", jobRepositoryBeanRef);

        String transactionManagerRef = stepElement.getAttribute("transaction-manager");
        RuntimeBeanReference transactionManagerBeanRef = new RuntimeBeanReference(transactionManagerRef);
        bd.getPropertyValues().addPropertyValue("transactionManager", transactionManagerBeanRef);
		
        handleListenersElement(stepElement, bd, parserContext, "stepExecutionListeners");
        
        bd.setRole(BeanDefinition.ROLE_SUPPORT);
        
        bd.setSource(parserContext.extractSource(stepElement));
		parserContext.registerBeanComponent(new BeanComponentDefinition(bd, stepElement.getAttribute("name")));

    }

	/**
	 * @param element
	 * @param parserContext
	 */
	private void handleTaskletElement(Element stepElement, Element element, ParserContext parserContext) {

    	RootBeanDefinition bd;
    	
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
		
        if (isFaultTolerant) {
        	bd = new RootBeanDefinition("org.springframework.batch.core.step.item.FaultTolerantStepFactoryBean", null, null);
        }
        else {
        	bd = new RootBeanDefinition("org.springframework.batch.core.step.item.SimpleStepFactoryBean", null, null);
        }

		// now, set the properties on the new bean 
        String startLimit = element.getAttribute("start-limit");
        if (StringUtils.hasText(startLimit)) {
            bd.getPropertyValues().addPropertyValue("startLimit", startLimit);
        }
        String allowStartIfComplete = element.getAttribute("allow-start-if-complete");
        if (StringUtils.hasText(allowStartIfComplete)) {
            bd.getPropertyValues().addPropertyValue("allowStartIfComplete", allowStartIfComplete);
        }

        String readerBeanId = element.getAttribute("reader");
        if (StringUtils.hasText(readerBeanId)) {
            RuntimeBeanReference readerRef = new RuntimeBeanReference(readerBeanId);
            bd.getPropertyValues().addPropertyValue("itemReader", readerRef);
        }

        String processorBeanId = element.getAttribute("processor");
        if (StringUtils.hasText(processorBeanId)) {
            RuntimeBeanReference processorRef = new RuntimeBeanReference(processorBeanId);
            bd.getPropertyValues().addPropertyValue("itemProcessor", processorRef);
        }

        String writerBeanId = element.getAttribute("writer");
        if (StringUtils.hasText(writerBeanId)) {
            RuntimeBeanReference writerRef = new RuntimeBeanReference(writerBeanId);
            bd.getPropertyValues().addPropertyValue("itemWriter", writerRef);
        }

        String taskExecutorBeanId = element.getAttribute("task-executor");
        if (StringUtils.hasText(taskExecutorBeanId)) {
            RuntimeBeanReference taskExecutorRef = new RuntimeBeanReference(taskExecutorBeanId);
            bd.getPropertyValues().addPropertyValue("taskExecutor", taskExecutorRef);
        }

        String jobRepository = stepElement.getAttribute("job-repository");
        RuntimeBeanReference jobRepositoryRef = new RuntimeBeanReference(jobRepository);
        bd.getPropertyValues().addPropertyValue("jobRepository", jobRepositoryRef);

        String transactionManager = stepElement.getAttribute("transaction-manager");
        RuntimeBeanReference tx = new RuntimeBeanReference(transactionManager);
        bd.getPropertyValues().addPropertyValue("transactionManager", tx);

        String commitInterval = element.getAttribute("commit-interval");
        if (StringUtils.hasText(commitInterval)) {
            bd.getPropertyValues().addPropertyValue("commitInterval", commitInterval);
        }

        if (StringUtils.hasText(skipLimit)) {
            bd.getPropertyValues().addPropertyValue("skipLimit", skipLimit);
        }

        if (StringUtils.hasText(retryLimit)) {
            bd.getPropertyValues().addPropertyValue("retryLimit", retryLimit);
        }

        if (StringUtils.hasText(cacheCapacity)) {
            bd.getPropertyValues().addPropertyValue("cacheCapacity", cacheCapacity);
        }

        String transactionAttribute = element.getAttribute("transaction-attribute");
        if (StringUtils.hasText(transactionAttribute)) {
            bd.getPropertyValues().addPropertyValue("transactionAttribute", transactionAttribute);
        }

        if (StringUtils.hasText(isReaderTransactionalQueue)) {
        	if (isFaultTolerant) {
        		bd.getPropertyValues().addPropertyValue("isReaderTransactionalQueue", isReaderTransactionalQueue);
        	}
        }

        handleExceptionElement(element, parserContext, bd, "skippable-exception-classes", "skippableExceptionClasses", isFaultTolerant);
        
        handleExceptionElement(element, parserContext, bd, "retryable-exception-classes", "retryableExceptionClasses", isFaultTolerant);
        
        handleExceptionElement(element, parserContext, bd, "fatal-exception-classes", "fatalExceptionClasses", isFaultTolerant);

        handleListenersElement(stepElement, bd, parserContext, "listeners");
        
        handleRetryListenersElement(element, bd, parserContext);
        
        handleStreamsElement(element, bd, parserContext);
        
        bd.setRole(BeanDefinition.ROLE_SUPPORT);
        
        bd.setSource(parserContext.extractSource(stepElement));
		parserContext.registerBeanComponent(new BeanComponentDefinition(bd, stepElement.getAttribute("name")));

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
		String exceptions = 
        	DomUtils.getChildElementValueByTagName(element, subElementName);
        if (StringUtils.hasLength(exceptions)) {
        	return true;
        }
		return false;
	}

	private void handleExceptionElement(Element element, ParserContext parserContext, BeanDefinition bd, 
			String subElementName, String propertyName, boolean isFaultTolerant) {
		String exceptions = 
        	DomUtils.getChildElementValueByTagName(element, subElementName);
        if (StringUtils.hasLength(exceptions)) {
        	if (isFaultTolerant) {
		        String[] exceptionArray = StringUtils.tokenizeToStringArray(
		        		StringUtils.delete(exceptions, ","), "\n");
		        if (exceptionArray.length > 0) {
		        	bd.getPropertyValues().addPropertyValue(propertyName, exceptionArray);
		        }
        	}
        	else {
				parserContext.getReaderContext().error(subElementName + " can only be specified for fault-tolerant " +
						"configurations providing skip-limit, retry-limit or cache-capacity", element);
        	}
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
	private void handleRetryListenersElement(Element element, BeanDefinition bd, ParserContext parserContext) {
		Element listenersElement = 
        	DomUtils.getChildElementByTagName(element, "retry-listeners");
		if (listenersElement != null) {
			CompositeComponentDefinition compositeDef =
				new CompositeComponentDefinition(listenersElement.getTagName(), parserContext.extractSource(element));
			parserContext.pushContainingComponent(compositeDef);
			List<Object> retryListenerBeans = new ArrayList<Object>(); 
			handleRetryListenerElements(parserContext, listenersElement,
					retryListenerBeans);
	        ManagedList arguments = new ManagedList();
	        arguments.addAll(retryListenerBeans);
        	bd.getPropertyValues().addPropertyValue("retryListeners", arguments);
        	parserContext.popAndRegisterContainingComponent();
		}
	}

	@SuppressWarnings("unchecked")
	private void handleRetryListenerElements(ParserContext parserContext,
			Element element, List<Object> beans) {
		List<Element> listenerElements = 
			DomUtils.getChildElementsByTagName(element, "listener");
		if (listenerElements != null) {
			for (Element listenerElement : listenerElements) {
				String id = listenerElement.getAttribute("id");
				String listenerRef = listenerElement.getAttribute("ref");
				String className = listenerElement.getAttribute("class");
				checkListenerElementAttributes(parserContext, element,
						listenerElement, id, listenerRef, className);
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
					parserContext.getReaderContext().error("Neither 'ref' or 'class' specified for <" + listenerElement.getTagName() + "> element", element);
				}
			}
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

	@SuppressWarnings("unchecked")
	private void handleStreamsElement(Element element, BeanDefinition bd, ParserContext parserContext) {
		Element streamsElement = 
        	DomUtils.getChildElementByTagName(element, "streams");
		if (streamsElement != null) {
			List<BeanReference> streamBeans = new ArrayList<BeanReference>(); 
			List<Element> streamElements = 
	        	DomUtils.getChildElementsByTagName(streamsElement, "stream");
			if (streamElements != null) {
				for (Element streamElement : streamElements) {
					String streamRef = streamElement.getAttribute("ref");
					if (StringUtils.hasText(streamRef)) {
				        BeanReference bean = new RuntimeBeanReference(streamRef);
						streamBeans.add(bean);
					}
					else {
						parserContext.getReaderContext().error("ref not specified for <" + streamElement.getTagName() + "> element", element);
					}
				}
			}
	        ManagedList arguments = new ManagedList();
	        arguments.addAll(streamBeans);
        	bd.getPropertyValues().addPropertyValue("streams", arguments);
		}
	}

}
