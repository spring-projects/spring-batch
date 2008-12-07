/*
 * Copyright 2006-2007 the original author or authors.
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

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.support.StateTransition;
import org.springframework.batch.core.job.flow.support.state.EndState;
import org.springframework.batch.core.job.flow.support.state.StepState;
import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.batch.core.listener.StepListenerMetaData;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
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
 * references a bean definition for a {@link Step} and goes on to (optionally)
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
	 * @return a collection of bean definitions for {@link StateTransition}
	 * instances objects
	 */
	public Collection<RuntimeBeanReference> parse(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder.genericBeanDefinition(StepState.class);
		String stepRef = element.getAttribute("name");

		@SuppressWarnings("unchecked")
		List<Element> simpleTaskElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "simple-task");
		@SuppressWarnings("unchecked")
		List<Element> processTaskElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "item-task");
		if (simpleTaskElements.size() > 0) {
			Object task = parseSimpleTask(simpleTaskElements.get(0), parserContext);
			stateBuilder.addConstructorArgValue(stepRef);
			stateBuilder.addConstructorArgValue(task);
		}
		else if (processTaskElements.size() > 0) {
			Object task = parseProcessTask(processTaskElements.get(0), parserContext);
			stateBuilder.addConstructorArgValue(stepRef);
			stateBuilder.addConstructorArgValue(task);
		}
		else if (StringUtils.hasText(stepRef)) {
				RuntimeBeanReference stateDef = new RuntimeBeanReference(stepRef);
				stateBuilder.addConstructorArgValue(stateDef);
		}
		else {
			throw new BeanCreationException("Error creating Step for " + element);
		}
		return getNextElements(parserContext, stateBuilder.getBeanDefinition(), element);

	}

	/**
	 * @param parserContext
	 * @param stateDef
	 * @param element
	 * @return a collection of {@link StateTransition} references
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
				throw new BeanCreationException("Duplicate transition pattern found for '*' "
						+ "(only specify one of next= attribute at step level and next element with on='*')");
			}

			String name = nextElement.getNodeName();
			if ("stop".equals(name) || "end".equals(name)) {

				String statusName = nextElement.getAttribute("status");
				BatchStatus status = StringUtils.hasText(statusName) ? BatchStatus.valueOf(statusName)
						: BatchStatus.STOPPED;
				String nextOnEnd = StringUtils.hasText(statusName) ? null : nextAttribute;

				BeanDefinitionBuilder endBuilder = BeanDefinitionBuilder.genericBeanDefinition(EndState.class);
				endBuilder.addConstructorArgValue(status);
				String endName = "end" + endCounter;
				endCounter++;

				endBuilder.addConstructorArgValue(endName);
				list.add(getStateTransitionReference(parserContext, endBuilder.getBeanDefinition(), onAttribute, nextOnEnd));
				nextAttribute = endName;
	
			}
			list.add(getStateTransitionReference(parserContext, stateDef, onAttribute, nextAttribute));
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
	 * @return a bean definition for a {@link StateTransition}
	 */
	public static RuntimeBeanReference getStateTransitionReference(ParserContext parserContext,
			BeanDefinition stateDefinition, String on, String next) {

		BeanDefinitionBuilder nextBuilder = BeanDefinitionBuilder.genericBeanDefinition(StateTransition.class);
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
	 * @param element
	 * @param parserContext
	 * @return the TaskletStep bean
	 */
	protected RootBeanDefinition parseSimpleTask(Element element, ParserContext parserContext) {

    	RootBeanDefinition bd = new RootBeanDefinition("org.springframework.batch.core.step.tasklet.TaskletStep", null, null);

        String taskletBeanId = element.getAttribute("tasklet");
        if (StringUtils.hasText(taskletBeanId)) {
            RuntimeBeanReference taskletRef = new RuntimeBeanReference(taskletBeanId);
            bd.getPropertyValues().addPropertyValue("tasklet", taskletRef);
        }

        String jobRepository = element.getAttribute("job-repository");
        RuntimeBeanReference jobRepositoryRef = new RuntimeBeanReference(jobRepository);
        bd.getPropertyValues().addPropertyValue("jobRepository", jobRepositoryRef);

        String transactionManager = element.getAttribute("transaction-manager");
        RuntimeBeanReference tx = new RuntimeBeanReference(transactionManager);
        bd.getPropertyValues().addPropertyValue("transactionManager", tx);
		
        handleListenersElement(element, bd, parserContext, "stepExecutionListeners");
        
        bd.setRole(BeanDefinition.ROLE_SUPPORT);
        
        return bd;

    }

	/**
	 * @param element
	 * @param parserContext
	 * @return the TaskletStep bean
	 */
	protected RootBeanDefinition parseProcessTask(Element element, ParserContext parserContext) {

    	RootBeanDefinition bd;
    	
    	boolean isFaultTolerant = false;

    	// TODO determine if step should be fault-tolerant
		String faultTolerant = element.getAttribute("fault-tolerant");
		
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

        String jobRepository = element.getAttribute("job-repository");
        RuntimeBeanReference jobRepositoryRef = new RuntimeBeanReference(jobRepository);
        bd.getPropertyValues().addPropertyValue("jobRepository", jobRepositoryRef);

        String transactionManager = element.getAttribute("transaction-manager");
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

        handleExceptionElement(element, bd, "skippable-exception-classes", "skippableExceptionClasses", isFaultTolerant);
        
        handleExceptionElement(element, bd, "retryable-exception-classes", "retryableExceptionClasses", isFaultTolerant);
        
        handleExceptionElement(element, bd, "fatal-exception-classes", "fatalExceptionClasses", isFaultTolerant);

        handleListenersElement(element, bd, parserContext, "listeners");
        
        handleRetryListenersElement(element, bd, parserContext);
        
        handleStreamsElement(element, bd, parserContext);
        
        bd.setRole(BeanDefinition.ROLE_SUPPORT);
        
		String id = parserContext.getReaderContext().generateBeanName(bd);
		parserContext.getRegistry().registerBeanDefinition(id, bd);
        
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
		String exceptions = 
        	DomUtils.getChildElementValueByTagName(element, subElementName);
        if (StringUtils.hasLength(exceptions)) {
        	return true;
        }
		return false;
	}

	private void handleExceptionElement(Element element, RootBeanDefinition bd, 
			String subElementName, String propertyName, boolean isFaultTolerant) {
		String exceptions = 
        	DomUtils.getChildElementValueByTagName(element, subElementName);
        if (StringUtils.hasLength(exceptions)) {
        	if (!isFaultTolerant) {
				throw new BeanCreationException(subElementName + " can only be specified if fault-tolerant is set to \"true\"");
        	}
	        String[] exceptionArray = StringUtils.tokenizeToStringArray(
	        		StringUtils.delete(exceptions, ","), "\n");
	        if (exceptionArray.length > 0) {
	        	bd.getPropertyValues().addPropertyValue(propertyName, exceptionArray);
	        }
        }
	}

	@SuppressWarnings("unchecked")
	private void handleListenersElement(Element element, RootBeanDefinition bd, ParserContext parserContext, String property) {
		Element listenersElement = 
        	DomUtils.getChildElementByTagName(element, "listeners");
		if (listenersElement != null) {
			List<BeanReference> listenerBeans = new ArrayList<BeanReference>(); 
			handleStepListenerElements(parserContext, listenersElement,
					listenerBeans);
	        ManagedList arguments = new ManagedList();
	        arguments.addAll(listenerBeans);
        	bd.getPropertyValues().addPropertyValue(property, arguments);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleRetryListenersElement(Element element, RootBeanDefinition bd, ParserContext parserContext) {
		Element retryListenersElement = 
        	DomUtils.getChildElementByTagName(element, "retry-listeners");
		if (retryListenersElement != null) {
			List<BeanReference> retryListenerBeans = new ArrayList<BeanReference>(); 
			handleListenerElements(parserContext, retryListenersElement,
					retryListenerBeans);
	        ManagedList arguments = new ManagedList();
	        arguments.addAll(retryListenerBeans);
        	bd.getPropertyValues().addPropertyValue("retryListeners", arguments);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleListenerElements(ParserContext parserContext,
			Element element, List<BeanReference> beans) {
		List<Element> listenerElements = 
			DomUtils.getChildElementsByTagName(element, "listener");
		if (listenerElements != null) {
			for (Element listenerElement : listenerElements) {
				String id = listenerElement.getAttribute("id");
				String listenerRef = listenerElement.getAttribute("ref");
				String className = listenerElement.getAttribute("class");
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
					throw new BeanCreationException("Both 'id' or 'ref' plus 'class' specified; use 'class' with an optional 'id' or just 'ref' for <" + 
							listenerElement.getTagName() + "> element with attributes: " + attributes);
				}
				if (StringUtils.hasText(listenerRef)) {
			        BeanReference bean = new RuntimeBeanReference(listenerRef);
					beans.add(bean);
				}
				else if (StringUtils.hasText(className)) {
					RootBeanDefinition beanDef = new RootBeanDefinition(className, null, null);
					if (!StringUtils.hasText(id)) {
						id = parserContext.getReaderContext().generateBeanName(beanDef);
					}
					parserContext.getRegistry().registerBeanDefinition(id, beanDef);
			        BeanReference bean = new RuntimeBeanReference(id);
					beans.add(bean);
				}
				else {
					throw new BeanCreationException("Neither 'ref' or 'class' specified for <" + listenerElement.getTagName() + "> element");
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void handleStepListenerElements(ParserContext parserContext,
			Element element, List<BeanReference> beans) {
		List<Element> listenerElements = 
			DomUtils.getChildElementsByTagName(element, "listener");
		if (listenerElements != null) {
			for (Element listenerElement : listenerElements) {
				BeanDefinitionBuilder listenerBuilder = BeanDefinitionBuilder.genericBeanDefinition(StepListenerFactoryBean.class);
				String id = listenerElement.getAttribute("id");
				String listenerRef = listenerElement.getAttribute("ref");
				String className = listenerElement.getAttribute("class");
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
					throw new BeanCreationException("Both 'id' or 'ref' plus 'class' specified; use 'class' with an optional 'id' or just 'ref' for <" + 
							listenerElement.getTagName() + "> element with attributes: " + attributes);
				}
				if (StringUtils.hasText(listenerRef)) {
			        listenerBuilder.addPropertyReference("delegate", listenerRef);
				}
				else if (StringUtils.hasText(className)) {
					RootBeanDefinition beanDef = new RootBeanDefinition(className, null, null);
					String delegateId = parserContext.getReaderContext().generateBeanName(beanDef);
					parserContext.getRegistry().registerBeanDefinition(delegateId, beanDef);
					listenerBuilder.addPropertyReference("delegate", delegateId);
				}
				else {
					throw new BeanCreationException("Neither 'ref' or 'class' specified for <" + listenerElement.getTagName() + "> element");
				}
				
				ManagedMap metaDataMap = new ManagedMap();
				for(StepListenerMetaData metaData: StepListenerMetaData.values()){
					String listenerMethod = listenerElement.getAttribute(metaData.getPropertyName());
					if(StringUtils.hasText(listenerMethod)){
						metaDataMap.put(metaData.getPropertyName(), listenerMethod);
					}
				}
				listenerBuilder.addPropertyValue("metaDataMap", metaDataMap);
				
				AbstractBeanDefinition beanDef = listenerBuilder.getBeanDefinition();
				if (!StringUtils.hasText(id)) {
					id = parserContext.getReaderContext().generateBeanName(beanDef);
				}
				parserContext.getRegistry().registerBeanDefinition(id, beanDef);
		        BeanReference bean = new RuntimeBeanReference(id);
				beans.add(bean);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void handleStreamsElement(Element element, RootBeanDefinition bd, ParserContext parserContext) {
		Element streamsElement = 
        	DomUtils.getChildElementByTagName(element, "streams");
		if (streamsElement != null) {
			List<BeanReference> streamBeans = new ArrayList<BeanReference>(); 
			List<Element> streamElements = 
	        	DomUtils.getChildElementsByTagName(streamsElement, "stream");
			if (streamElements != null) {
				for (Element listenerElement : streamElements) {
					String listenerRef = listenerElement.getAttribute("ref");
					if (StringUtils.hasText(listenerRef)) {
				        BeanReference bean = new RuntimeBeanReference(listenerRef);
						streamBeans.add(bean);
					}
					else {
						throw new BeanCreationException("ref not specified for <" + listenerElement.getTagName() + "> element");
					}
				}
			}
	        ManagedList arguments = new ManagedList();
	        arguments.addAll(streamBeans);
        	bd.getPropertyValues().addPropertyValue("streams", arguments);
		}
	}

}
