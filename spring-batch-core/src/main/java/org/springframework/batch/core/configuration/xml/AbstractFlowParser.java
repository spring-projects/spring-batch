/*
 * Copyright 2006-2008 the original author or authors.
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

import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * @author Dave Syer
 * 
 */
public abstract class AbstractFlowParser extends AbstractSingleBeanDefinitionParser {

	private static final String ID_ATTR = "id";

	private static final String STEP_ELE = "step";

	private static final String FLOW_ELE = "flow";

	private static final String DECISION_ELE = "decision";

	private static final String SPLIT_ELE = "split";

	private static final String NEXT_ATTR = "next";

	private static final String NEXT_ELE = "next";

	private static final String END_ELE = "end";

	private static final String FAIL_ELE = "fail";

	private static final String STOP_ELE = "stop";

	private static final String ON_ATTR = "on";

	private static final String TO_ATTR = "to";

	private static final String RESTART_ATTR = "restart";

	private static final String EXIT_CODE_ATTR = "exit-code";

	private static final InlineStepParser stepParser = new InlineStepParser();

	private static final FlowElementParser flowParser = new FlowElementParser();

	private static final DecisionParser decisionParser = new DecisionParser();

	// For generating unique state names for end transitions
	private static int endCounter = 0;

	private String jobFactoryRef;

	/**
	 * Convenience method for subclasses to set the job factory reference if it
	 * is available (null is fine, but the quality of error reports is better if
	 * it is available).
	 * 
	 * @param jobFactoryRef
	 */
	protected void setJobFactoryRef(String jobFactoryRef) {
		this.jobFactoryRef = jobFactoryRef;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see AbstractSingleBeanDefinitionParser#getBeanClass(Element)
	 */
	@Override
	protected Class<?> getBeanClass(Element element) {
		return SimpleFlowFactoryBean.class;
	}

	/**
	 * @param element the top level element containing a flow definition
	 * @param parserContext the {@link ParserContext}
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

		List<BeanDefinition> stateTransitions = new ArrayList<BeanDefinition>();

		SplitParser splitParser = new SplitParser(jobFactoryRef);
		CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(),
				parserContext.extractSource(element));
		parserContext.pushContainingComponent(compositeDef);

		boolean stepExists = false;
		Map<String, Set<String>> reachableElementMap = new HashMap<String, Set<String>>();
		String startElement = null;
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node instanceof Element) {
				String nodeName = node.getLocalName();
				Element child = (Element) node;
				if (nodeName.equals(STEP_ELE)) {
					stateTransitions.addAll(stepParser.parse(child, parserContext, jobFactoryRef));
					stepExists = true;
				}
				else if (nodeName.equals(DECISION_ELE)) {
					stateTransitions.addAll(decisionParser.parse(child, parserContext));
				}
				else if (nodeName.equals(FLOW_ELE)) {
					stateTransitions.addAll(flowParser.parse(child, parserContext));
					stepExists = true;
				}
				else if (nodeName.equals(SPLIT_ELE)) {
					stateTransitions.addAll(splitParser
							.parse(child, new ParserContext(parserContext.getReaderContext(), parserContext
									.getDelegate(), builder.getBeanDefinition())));
					stepExists = true;
				}

				if (Arrays.asList(STEP_ELE, DECISION_ELE, SPLIT_ELE, FLOW_ELE).contains(nodeName)) {
					reachableElementMap.put(child.getAttribute(ID_ATTR), findReachableElements(child));
					if (startElement == null) {
						startElement = child.getAttribute(ID_ATTR);
					}
				}
			}
		}

		String flowName = (String) builder.getRawBeanDefinition().getAttribute("flowName");
		if (!stepExists && !StringUtils.hasText(element.getAttribute("parent"))) {
			parserContext.getReaderContext().error("The flow [" + flowName + "] must contain at least one step, flow or split",
					element);
		}

		// Ensure that all elements are reachable
		Set<String> allReachableElements = new HashSet<String>();
		findAllReachableElements(startElement, reachableElementMap, allReachableElements);
		for (String elementId : reachableElementMap.keySet()) {
			if (!allReachableElements.contains(elementId)) {
				parserContext.getReaderContext().error("The element [" + elementId + "] is unreachable", element);
			}
		}

		ManagedList managedList = new ManagedList();
		@SuppressWarnings( { "unchecked", "unused" })
		boolean dummy = managedList.addAll(stateTransitions);
		builder.addPropertyValue("stateTransitions", managedList);

	}

	/**
	 * Find all of the elements that are pointed to by this element.
	 * 
	 * @param element
	 * @return a collection of reachable element names
	 */
	private Set<String> findReachableElements(Element element) {
		Set<String> reachableElements = new HashSet<String>();

		String nextAttribute = element.getAttribute(NEXT_ATTR);
		if (StringUtils.hasText(nextAttribute)) {
			reachableElements.add(nextAttribute);
		}

		@SuppressWarnings("unchecked")
		List<Element> nextElements = DomUtils.getChildElementsByTagName(element, NEXT_ELE);
		for (Element nextElement : nextElements) {
			String toAttribute = nextElement.getAttribute(TO_ATTR);
			reachableElements.add(toAttribute);
		}

		@SuppressWarnings("unchecked")
		List<Element> stopElements = DomUtils.getChildElementsByTagName(element, STOP_ELE);
		for (Element stopElement : stopElements) {
			String restartAttribute = stopElement.getAttribute(RESTART_ATTR);
			reachableElements.add(restartAttribute);
		}

		return reachableElements;
	}

	/**
	 * Find all of the elements reachable from the startElement.
	 * 
	 * @param startElement
	 * @param reachableElementMap
	 * @param accumulator a collection of reachable element names
	 */
	private void findAllReachableElements(String startElement, Map<String, Set<String>> reachableElementMap,
			Set<String> accumulator) {
		Set<String> reachableIds = reachableElementMap.get(startElement);
		accumulator.add(startElement);
		if (reachableIds != null) {
			for (String reachable : reachableIds) {
				// don't explore a previously explored element; prevent loop
				if (!accumulator.contains(reachable)) {
					findAllReachableElements(reachable, reachableElementMap, accumulator);
				}
			}
		}
	}

	/**
	 * @param parserContext the parser context for the bean factory
	 * @param stateDef The bean definition for the current state
	 * @param element the &lt;step/gt; element to parse
	 * @return a collection of
	 * {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 * references
	 */
	protected static Collection<BeanDefinition> getNextElements(ParserContext parserContext, BeanDefinition stateDef,
			Element element) {
		return getNextElements(parserContext, null, stateDef, element);
	}

	/**
	 * @param parserContext the parser context for the bean factory
	 * @param stepId the id of the current state if it is a step state, null
	 * otherwise
	 * @param stateDef The bean definition for the current state
	 * @param element the &lt;step/gt; element to parse
	 * @return a collection of
	 * {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 * references
	 */
	protected static Collection<BeanDefinition> getNextElements(ParserContext parserContext, String stepId,
			BeanDefinition stateDef, Element element) {

		Collection<BeanDefinition> list = new ArrayList<BeanDefinition>();

		String shortNextAttribute = element.getAttribute(NEXT_ATTR);
		boolean hasNextAttribute = StringUtils.hasText(shortNextAttribute);
		if (hasNextAttribute) {
			list.add(getStateTransitionReference(parserContext, stateDef, null, shortNextAttribute));
		}

		boolean transitionElementExists = false;
		List<String> patterns = new ArrayList<String>();
		for (String transitionName : new String[] { NEXT_ELE, STOP_ELE, END_ELE, FAIL_ELE }) {
			@SuppressWarnings("unchecked")
			List<Element> transitionElements = DomUtils.getChildElementsByTagName(element, transitionName);
			for (Element transitionElement : transitionElements) {
				verifyUniquePattern(transitionElement, patterns, element, parserContext);
				list.addAll(parseTransitionElement(transitionElement, stepId, stateDef, parserContext));
				transitionElementExists = true;
			}
		}

		if (!transitionElementExists) {
			list.addAll(createTransition(FlowExecutionStatus.FAILED, FlowExecutionStatus.FAILED.getName(), null, null,
					stateDef, parserContext, false));
			list.addAll(createTransition(FlowExecutionStatus.UNKNOWN, FlowExecutionStatus.UNKNOWN.getName(), null, null,
					stateDef, parserContext, false));
			if (!hasNextAttribute) {
				list.addAll(createTransition(FlowExecutionStatus.COMPLETED, null, null, null, stateDef, parserContext,
						false));
			}
		}
		else if (hasNextAttribute) {
			parserContext.getReaderContext().error(
					"The <" + element.getNodeName() + "/> may not contain a '" + NEXT_ATTR
							+ "' attribute and a transition element", element);
		}

		return list;
	}

	/**
	 * @param transitionElement The element to parse
	 * @param patterns a list of patterns on state transitions for this element
	 * @param element
	 * @param parserContext the parser context for the bean factory
	 */
	private static void verifyUniquePattern(Element transitionElement, List<String> patterns, Element element,
			ParserContext parserContext) {
		String onAttribute = transitionElement.getAttribute(ON_ATTR);
		if (patterns.contains(onAttribute)) {
			parserContext.getReaderContext().error("Duplicate transition pattern found for '" + onAttribute + "'",
					element);
		}
		patterns.add(onAttribute);
	}

	/**
	 * @param transitionElement The element to parse
	 * @param stateDef The bean definition for the current state
	 * @param parserContext the parser context for the bean factory
	 * @param a collection of
	 * {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 * references
	 */
	private static Collection<BeanDefinition> parseTransitionElement(Element transitionElement, String stateId,
			BeanDefinition stateDef, ParserContext parserContext) {

		FlowExecutionStatus status = getBatchStatusFromEndTransitionName(transitionElement.getNodeName());
		String onAttribute = transitionElement.getAttribute(ON_ATTR);
		String restartAttribute = transitionElement.getAttribute(RESTART_ATTR);
		String nextAttribute = transitionElement.getAttribute(TO_ATTR);
		if (!StringUtils.hasText(nextAttribute)) {
			nextAttribute = restartAttribute;
		}
		boolean abandon = stateId != null && StringUtils.hasText(restartAttribute) && !restartAttribute.equals(stateId);
		String exitCodeAttribute = transitionElement.getAttribute(EXIT_CODE_ATTR);

		return createTransition(status, onAttribute, nextAttribute, exitCodeAttribute, stateDef, parserContext, abandon);
	}

	/**
	 * @param status The batch status that this transition will set. Use
	 * BatchStatus.UNKNOWN if not applicable.
	 * @param on The pattern that this transition should match. Use null for
	 * "no restriction" (same as "*").
	 * @param next The state to which this transition should go. Use null if not
	 * applicable.
	 * @param exitCode The exit code that this transition will set. Use null to
	 * default to batchStatus.
	 * @param stateDef The bean definition for the current state
	 * @param parserContext the parser context for the bean factory
	 * @param a collection of
	 * {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 * references
	 */
	private static Collection<BeanDefinition> createTransition(FlowExecutionStatus status, String on, String next,
			String exitCode, BeanDefinition stateDef, ParserContext parserContext, boolean abandon) {

		BeanDefinition endState = null;

		if (status.isEnd()) {

			BeanDefinitionBuilder endBuilder = BeanDefinitionBuilder
					.genericBeanDefinition("org.springframework.batch.core.job.flow.support.state.EndState");

			boolean exitCodeExists = StringUtils.hasText(exitCode);

			endBuilder.addConstructorArgValue(status);

			endBuilder.addConstructorArgValue(exitCodeExists ? exitCode : status.getName());

			String endName = (status == FlowExecutionStatus.STOPPED ? STOP_ELE
					: status == FlowExecutionStatus.FAILED ? FAIL_ELE : END_ELE)
					+ (endCounter++);
			endBuilder.addConstructorArgValue(endName);

			endBuilder.addConstructorArgValue(abandon);

			String nextOnEnd = exitCodeExists ? null : next;
			endState = getStateTransitionReference(parserContext, endBuilder.getBeanDefinition(), null, nextOnEnd);
			next = endName;

		}

		Collection<BeanDefinition> list = new ArrayList<BeanDefinition>();
		list.add(getStateTransitionReference(parserContext, stateDef, on, next));
		if (endState != null) {
			//
			// Must be added after the state to ensure that the state is the
			// first in the list
			//
			list.add(endState);
		}
		return list;
	}

	/**
	 * @param elementName An end transition element name
	 * @return the BatchStatus corresponding to the transition name
	 */
	private static FlowExecutionStatus getBatchStatusFromEndTransitionName(String elementName) {
        elementName = stripNamespace(elementName);
		if (STOP_ELE.equals(elementName)) {
			return FlowExecutionStatus.STOPPED;
		}
		else if (END_ELE.equals(elementName)) {
			return FlowExecutionStatus.COMPLETED;
		}
		else if (FAIL_ELE.equals(elementName)) {
			return FlowExecutionStatus.FAILED;
		}
		else {
			return FlowExecutionStatus.UNKNOWN;
		}
	}

    /**
     * Strip the namespace from the element name if it exists.
     */
    private static String stripNamespace(String elementName){
        if(elementName.startsWith("batch:")){
            return elementName.substring(6);
        }
        else{
            return elementName;
        }
    }

	/**
	 * @param parserContext the parser context
	 * @param stateDefinition a reference to the state implementation
	 * @param on the pattern value
	 * @param next the next step id
	 * @return a bean definition for a
	 * {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 */
	public static BeanDefinition getStateTransitionReference(ParserContext parserContext,
			BeanDefinition stateDefinition, String on, String next) {

		BeanDefinitionBuilder nextBuilder = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.batch.core.job.flow.support.StateTransition");
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

		return nextBuilder.getBeanDefinition();

	}

}
