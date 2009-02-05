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
import java.util.List;

import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Dave Syer
 * 
 */
public class FlowParser extends AbstractSingleBeanDefinitionParser {

	private final String flowName;

	/**
	 * Construct a {@link FlowParser} with the specified name.
	 * @param flowName the name of the flow
	 */
	public FlowParser(String flowName) {
		this.flowName = flowName;

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
		List<RuntimeBeanReference> stateTransitions = new ArrayList<RuntimeBeanReference>();

		StepParser stepParser = new StepParser();
		DecisionParser decisionParser = new DecisionParser();
		SplitParser splitParser = new SplitParser();
		CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(),
				parserContext.extractSource(element));
		parserContext.pushContainingComponent(compositeDef);

		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node instanceof Element) {
				String nodeName = node.getLocalName();
				if (nodeName.equals("step")) {
					stateTransitions.addAll(stepParser.parse((Element) node, parserContext));
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

}
