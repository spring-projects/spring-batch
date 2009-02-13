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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
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

/**
 * @author Dave Syer
 * 
 */
public class FlowParser extends AbstractSingleBeanDefinitionParser {

	private static final String NEXT = "next";
	private static final String END = "end";
	private static final String FAIL = "fail";
	private static final String STOP = "stop";

	// For generating unique state names for end transitions
	private static int endCounter = 0;

	private final String flowName;
	private final String jobRepositoryRef;

	/**
	 * Construct a {@link FlowParser} with the specified name and using the provided job repository ref.
	 * @param flowName the name of the flow
	 * @param jobRepositoryRef the reference to the jobRepository from the enclosing tag
	 */
	public FlowParser(String flowName, String jobRepositoryRef) {
		this.flowName = flowName;
		this.jobRepositoryRef = jobRepositoryRef;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see AbstractSingleBeanDefinitionParser#getBeanClass(Element)
	 */
	@Override
	protected Class<SimpleFlow> getBeanClass(Element element) {
		return SimpleFlow.class;
	}

	/**
	 * @param element the top level element containing a flow definition
	 * @param parserContext the {@link ParserContext}
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		List<BeanDefinition> stateTransitions = new ArrayList<BeanDefinition>();

		InlineStepParser stepParser = new InlineStepParser();
		DecisionParser decisionParser = new DecisionParser();
		SplitParser splitParser = new SplitParser(jobRepositoryRef);
		CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(),
				parserContext.extractSource(element));
		parserContext.pushContainingComponent(compositeDef);

		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node instanceof Element) {
				String nodeName = node.getLocalName();
				if (nodeName.equals("step")) {
					stateTransitions.addAll(stepParser.parse((Element) node, parserContext, jobRepositoryRef));
				}
				else if (nodeName.equals("decision")) {
					stateTransitions.addAll(decisionParser.parse((Element) node, parserContext));
				}
				else if (nodeName.equals("split")) {
					stateTransitions.addAll(splitParser.parse((Element) node, new ParserContext(parserContext
							.getReaderContext(), parserContext.getDelegate(), builder.getBeanDefinition())));
				}
			}
		}

		builder.addConstructorArgValue(flowName);
		ManagedList managedList = new ManagedList();
		@SuppressWarnings( { "unchecked", "unused" })
		boolean dummy = managedList.addAll(stateTransitions);
		builder.addPropertyValue("stateTransitions", managedList);

		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		parserContext.popAndRegisterContainingComponent();

	}

	/**
	 * @param parserContext the parser context for the bean factory
	 * @param stateDef The bean definition for the current state
	 * @param element the &lt;step/gt; element to parse
	 * @return a collection of
	 *         {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 *         references
	 */
	protected static Collection<BeanDefinition> getNextElements(ParserContext parserContext, BeanDefinition stateDef,
			Element element) {

		Collection<BeanDefinition> list = new ArrayList<BeanDefinition>();

		String shortNextAttribute = element.getAttribute(NEXT);
		boolean hasNextAttribute = StringUtils.hasText(shortNextAttribute);
		if (hasNextAttribute) {
			list.add(getStateTransitionReference(parserContext, stateDef, null, shortNextAttribute));
		}

		boolean transitionElementExists = false;
		List<String> patterns = new ArrayList<String>();
		for (String transitionName : new String[] { NEXT, STOP, END, FAIL }) {
			@SuppressWarnings("unchecked")
			List<Element> transitionElements = (List<Element>) DomUtils.getChildElementsByTagName(element,
					transitionName);
			for (Element transitionElement : transitionElements) {
				verifyUniquePattern(transitionElement, patterns, element, parserContext);
				list.addAll(parseTransitionElement(transitionElement, stateDef, parserContext));
				transitionElementExists = true;
			}
		}

		if (!transitionElementExists) {
			list.addAll(createTransition(BatchStatus.INCOMPLETE, ExitStatus.FAILED.getExitCode(), null, null,
					stateDef, parserContext));
			if (!hasNextAttribute) {
				list.addAll(createTransition(BatchStatus.COMPLETED, null, null, null, stateDef, parserContext));
			}
		}
		else if (hasNextAttribute) {
			parserContext.getReaderContext().error("Step may not contain a 'next' attribute and a transition element",
					element);
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
		String onAttribute = transitionElement.getAttribute("on");
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
	 *            {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 *            references
	 */
	private static Collection<BeanDefinition> parseTransitionElement(Element transitionElement,
			BeanDefinition stateDef, ParserContext parserContext) {

		BatchStatus batchStatus = getBatchStatusFromEndTransitionName(transitionElement.getNodeName());
		String onAttribute = transitionElement.getAttribute("on");
		String nextAttribute = transitionElement.getAttribute("to");
		nextAttribute = StringUtils.hasText(nextAttribute) ? nextAttribute : transitionElement.getAttribute("restart");
		String statusAttribute = transitionElement.getAttribute("status");

		return createTransition(batchStatus, onAttribute, nextAttribute, statusAttribute, stateDef, parserContext);
	}

	/**
	 * @param batchStatus The batch status that this transition will set. Use
	 *            BatchStatus.UNKNOWN if not applicable.
	 * @param on The pattern that this transition should match. Use null for
	 *            "no restriction" (same as "*").
	 * @param next The state to which this transition should go. Use null if not
	 *            applicable.
	 * @param exitCode The exit code that this transition will set. Use null to
	 *            default to batchStatus.
	 * @param stateDef The bean definition for the current state
	 * @param parserContext the parser context for the bean factory
	 * @param a collection of
	 *            {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 *            references
	 */
	private static Collection<BeanDefinition> createTransition(BatchStatus batchStatus, String on, String next,
			String exitCode, BeanDefinition stateDef, ParserContext parserContext) {

		BeanDefinition endState = null;

		if (batchStatus == BatchStatus.FAILED || batchStatus == BatchStatus.COMPLETED
				|| batchStatus == BatchStatus.INCOMPLETE) {

			BeanDefinitionBuilder endBuilder = BeanDefinitionBuilder
					.genericBeanDefinition("org.springframework.batch.core.job.flow.support.state.EndState");
			endBuilder.addConstructorArgValue(batchStatus);

			boolean exitCodeExists = StringUtils.hasText(exitCode);
			endBuilder.addConstructorArgValue(exitCodeExists ? new ExitStatus(exitCode)
					: convertToExitStatus(batchStatus));

			String endName = "end" + (endCounter++);
			endBuilder.addConstructorArgValue(endName);

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
	private static BatchStatus getBatchStatusFromEndTransitionName(String elementName) {
		if (STOP.equals(elementName)) {
			return BatchStatus.INCOMPLETE;
		}
		else if (END.equals(elementName)) {
			return BatchStatus.COMPLETED;
		}
		else if (FAIL.equals(elementName)) {
			return BatchStatus.FAILED;
		}
		else {
			return BatchStatus.UNKNOWN;
		}
	}

	/**
	 * @param batchStatus A BatchStatus
	 * @return the ExitStatus corresponding to the BatchStatus
	 */
	private static ExitStatus convertToExitStatus(BatchStatus batchStatus) {
		if (batchStatus == BatchStatus.INCOMPLETE) {
			return ExitStatus.FAILED;
		}
		else {
			return new ExitStatus(batchStatus.toString());
		}
	}

	/**
	 * @param parserContext the parser context
	 * @param stateDefinition a reference to the state implementation
	 * @param on the pattern value
	 * @param next the next step id
	 * @return a bean definition for a {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 */
	public static BeanDefinition getStateTransitionReference(ParserContext parserContext,
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

		return nextBuilder.getBeanDefinition();

	}

}
