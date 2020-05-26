/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.jsr.configuration.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.configuration.xml.AbstractFlowParser;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.jsr.job.flow.support.JsrFlow;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parses flows as defined in JSR-352.  The current state parses a flow
 * as it is within a regular Spring Batch job/flow.
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 */
public class FlowParser extends AbstractFlowParser {
	private static final String NEXT_ATTRIBUTE = "next";
	private static final String EXIT_STATUS_ATTRIBUTE = "exit-status";
	private static final List<String> TRANSITION_TYPES = new ArrayList<>();

	static {
		TRANSITION_TYPES.add(NEXT_ELE);
		TRANSITION_TYPES.add(STOP_ELE);
		TRANSITION_TYPES.add(END_ELE);
		TRANSITION_TYPES.add(FAIL_ELE);
	}

	private String flowName;
	private String jobFactoryRef;
	private StepParser stepParser = new StepParser();

	/**
	 * @param flowName The name of the flow
	 * @param jobFactoryRef The bean name for the job factory
	 */
	public FlowParser(String flowName, String jobFactoryRef) {
		super.setJobFactoryRef(jobFactoryRef);
		this.jobFactoryRef = jobFactoryRef;
		this.flowName = flowName;
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return JsrFlowFactoryBean.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.getRawBeanDefinition().setAttribute("flowName", flowName);
		builder.addPropertyValue("name", flowName);
		builder.addPropertyValue("flowType", JsrFlow.class);

		List<BeanDefinition> stateTransitions = new ArrayList<>();

		Map<String, Set<String>> reachableElementMap = new HashMap<>();
		String startElement = null;
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node instanceof Element) {
				String nodeName = node.getLocalName();
				Element child = (Element) node;
				if (nodeName.equals(STEP_ELE)) {
					stateTransitions.addAll(stepParser.parse(child, parserContext, builder));
				} else if(nodeName.equals(SPLIT_ELE)) {
					stateTransitions.addAll(new JsrSplitParser(flowName).parse(child, parserContext));
				} else if(nodeName.equals(DECISION_ELE)) {
					stateTransitions.addAll(new JsrDecisionParser().parse(child, parserContext, flowName));
				} else if(nodeName.equals(FLOW_ELE)) {
					stateTransitions.addAll(parseFlow(child, parserContext, builder));
				}
			}
		}

		Set<String> allReachableElements = new HashSet<>();
		findAllReachableElements(startElement, reachableElementMap, allReachableElements);
		for (String elementId : reachableElementMap.keySet()) {
			if (!allReachableElements.contains(elementId)) {
				parserContext.getReaderContext().error("The element [" + elementId + "] is unreachable", element);
			}
		}

		ManagedList<BeanDefinition> managedList = new ManagedList<>();
		managedList.addAll(stateTransitions);
		builder.addPropertyValue("stateTransitions", managedList);
	}

	private Collection<BeanDefinition> parseFlow(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String idAttribute = element.getAttribute(ID_ATTRIBUTE);

		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.batch.core.job.flow.support.state.FlowState");

		FlowParser flowParser = new FlowParser(idAttribute, jobFactoryRef);

		stateBuilder.addConstructorArgValue(flowParser.parse(element, parserContext));
		stateBuilder.addConstructorArgValue(idAttribute);

		builder.getRawBeanDefinition().setAttribute("flowName", idAttribute);
		builder.addPropertyValue("name", idAttribute);

		doParse(element, parserContext, builder);
		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		return FlowParser.getNextElements(parserContext, null, stateBuilder.getBeanDefinition(), element);
	}

	public static Collection<BeanDefinition> getNextElements(ParserContext parserContext, BeanDefinition stateDef,
			Element element) {
		return getNextElements(parserContext, null, stateDef, element);
	}

	public static Collection<BeanDefinition> getNextElements(ParserContext parserContext, String stepId,
			BeanDefinition stateDef, Element element) {

		Collection<BeanDefinition> list = new ArrayList<>();

		boolean transitionElementExists = false;
		boolean failedTransitionElementExists = false;

		List<Element> childElements = DomUtils.getChildElements(element);
		for(Element childElement : childElements) {
			if(isChildElementTransitionElement(childElement)) {
				list.addAll(parseTransitionElement(childElement, stepId, stateDef, parserContext));
				failedTransitionElementExists = failedTransitionElementExists || hasFailedTransitionElement(childElement);
				transitionElementExists = true;
			}
		}

		String shortNextAttribute = element.getAttribute(NEXT_ATTRIBUTE);
		boolean hasNextAttribute = StringUtils.hasText(shortNextAttribute);

		if (!transitionElementExists) {
			list.addAll(createTransition(FlowExecutionStatus.FAILED, FlowExecutionStatus.FAILED.getName(), null, null,
					stateDef, parserContext, false));
			list.addAll(createTransition(FlowExecutionStatus.UNKNOWN, FlowExecutionStatus.UNKNOWN.getName(), null, null,
					stateDef, parserContext, false));
		}

		if (hasNextAttribute) {
			if (transitionElementExists && !failedTransitionElementExists) {
				list.addAll(createTransition(FlowExecutionStatus.FAILED, FlowExecutionStatus.FAILED.getName(), null, null,
						stateDef, parserContext, false));
			}

			list.add(getStateTransitionReference(parserContext, stateDef, null, shortNextAttribute));
		} else {
			list.addAll(createTransition(FlowExecutionStatus.COMPLETED, FlowExecutionStatus.COMPLETED.getName(), null, null, stateDef, parserContext,
					false));
		}

		return list;
	}

	private static boolean isChildElementTransitionElement(Element childElement) {
		return TRANSITION_TYPES.contains(childElement.getLocalName());
	}

	private static boolean hasFailedTransitionElement(Element childName) {
		return FAIL_ELE.equals(childName.getLocalName());
	}

	protected static Collection<BeanDefinition> parseTransitionElement(Element transitionElement, String stateId,
			BeanDefinition stateDef, ParserContext parserContext) {
		FlowExecutionStatus status = getBatchStatusFromEndTransitionName(transitionElement.getNodeName());
		String onAttribute = transitionElement.getAttribute(ON_ATTR);
		String restartAttribute = transitionElement.getAttribute(RESTART_ATTR);
		String nextAttribute = transitionElement.getAttribute(TO_ATTR);

		if (!StringUtils.hasText(nextAttribute)) {
			nextAttribute = restartAttribute;
		}
		String exitCodeAttribute = transitionElement.getAttribute(EXIT_STATUS_ATTRIBUTE);

		return createTransition(status, onAttribute, nextAttribute, restartAttribute, exitCodeAttribute, stateDef, parserContext, false);
	}

	/**
	 * @param status The batch status that this transition will set. Use
	 * BatchStatus.UNKNOWN if not applicable.
	 * @param on The pattern that this transition should match. Use null for
	 * "no restriction" (same as "*").
	 * @param next The state to which this transition should go. Use null if not
	 * applicable.
	 * @param restart The restart attribute this transition will set.
	 * @param exitCode The exit code that this transition will set. Use null to
	 * default to batchStatus.
	 * @param stateDef The bean definition for the current state
	 * @param parserContext the parser context for the bean factory
	 * @param abandon the abandon state this transition will set.
	 * @return a collection of
	 * {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 * references
	 */
	protected static Collection<BeanDefinition> createTransition(FlowExecutionStatus status, String on, String next,
			String restart, String exitCode, BeanDefinition stateDef, ParserContext parserContext, boolean abandon) {

		BeanDefinition endState = null;

		if (status.isEnd()) {

			BeanDefinitionBuilder endBuilder = BeanDefinitionBuilder
					.genericBeanDefinition("org.springframework.batch.core.jsr.job.flow.support.state.JsrEndState");

			boolean exitCodeExists = StringUtils.hasText(exitCode);

			endBuilder.addConstructorArgValue(status);

			endBuilder.addConstructorArgValue(exitCodeExists ? exitCode : status.getName());

			String endName = (status == FlowExecutionStatus.STOPPED ? STOP_ELE
					: status == FlowExecutionStatus.FAILED ? FAIL_ELE : END_ELE)
					+ (endCounter++);
			endBuilder.addConstructorArgValue(endName);

			endBuilder.addConstructorArgValue(restart);

			endBuilder.addConstructorArgValue(abandon);

			endBuilder.addConstructorArgReference("jobRepository");

			String nextOnEnd = exitCodeExists ? null : next;
			endState = getStateTransitionReference(parserContext, endBuilder.getBeanDefinition(), null, nextOnEnd);
			next = endName;

		}

		Collection<BeanDefinition> list = new ArrayList<>();
		list.add(getStateTransitionReference(parserContext, stateDef, on, next));

		if(StringUtils.hasText(restart)) {
			list.add(getStateTransitionReference(parserContext, stateDef, on + ".RESTART", restart));
		}

		if (endState != null) {
			//
			// Must be added after the state to ensure that the state is the
			// first in the list
			//
			list.add(endState);
		}
		return list;
	}
}
