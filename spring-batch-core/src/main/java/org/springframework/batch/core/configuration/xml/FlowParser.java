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

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Dave Syer
 * 
 */
public class FlowParser {

	/**
	 * @param element the top level element containing a flow definition
	 * @param parserContext the {@link ParserContext}
	 * @param flowName the name of the flow
	 * @return a bean definition for a {@link org.springframework.batch.core.job.flow.Flow}
	 */
	public AbstractBeanDefinition parse(Element element, ParserContext parserContext, String flowName) {
		List<RuntimeBeanReference> stateTransitions = new ArrayList<RuntimeBeanReference>();

		StepParser stepParser = new StepParser();
		DecisionParser decisionParser = new DecisionParser();
		SplitParser splitParser = new SplitParser();
		
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node instanceof Element) {
				String nodeName = node.getLocalName();
				if(nodeName.equals("step"))
				{
					stateTransitions.addAll(stepParser.parse((Element)node, parserContext));
				}
				else if(nodeName.equals("decision"))
				{
					stateTransitions.addAll(decisionParser.parse((Element)node, parserContext));
				}
				else if(nodeName.equals("split"))
				{
					stateTransitions.addAll(splitParser.parse((Element)node, parserContext));
				}
			}
		}

		BeanDefinitionBuilder flowBuilder = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.batch.core.job.flow.support.SimpleFlow");
		flowBuilder.addConstructorArgValue(flowName);
		ManagedList managedList = new ManagedList();
		@SuppressWarnings( { "unchecked", "unused" })
		boolean dummy = managedList.addAll(stateTransitions);
		flowBuilder.addPropertyValue("stateTransitions", managedList);
		AbstractBeanDefinition flowDef = flowBuilder.getBeanDefinition();
		parserContext.getReaderContext().registerWithGeneratedName(flowDef);

		return flowDef;

	}

}
