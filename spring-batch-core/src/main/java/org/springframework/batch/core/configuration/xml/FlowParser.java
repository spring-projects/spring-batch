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
import org.springframework.util.xml.DomUtils;

import org.w3c.dom.Element;

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

		@SuppressWarnings("unchecked")
		List<Element> stepElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "step");
		StepParser stepParser = new StepParser();
		for (Element stepElement : stepElements) {
			stateTransitions.addAll(stepParser.parse(stepElement, parserContext));
		}

		@SuppressWarnings("unchecked")
		List<Element> decisionElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "decision");
		DecisionParser decisionParser = new DecisionParser();
		for (Element stepElement : decisionElements) {
			stateTransitions.addAll(decisionParser.parse(stepElement, parserContext));
		}

		@SuppressWarnings("unchecked")
		List<Element> splitElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "split");
		SplitParser splitParser = new SplitParser();
		for (Element stepElement : splitElements) {
			stateTransitions.addAll(splitParser.parse(stepElement, parserContext));
		}

		BeanDefinitionBuilder flowBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition("org.springframework.batch.core.job.flow.support.SimpleFlow");
		flowBuilder.addConstructorArgValue(flowName );
		ManagedList managedList = new ManagedList();
		@SuppressWarnings( { "unchecked", "unused" })
		boolean dummy = managedList.addAll(stateTransitions);
		flowBuilder.addPropertyValue("stateTransitions", managedList);
		AbstractBeanDefinition flowDef = flowBuilder.getBeanDefinition();
		parserContext.getReaderContext().registerWithGeneratedName(flowDef);

		return flowDef;

	}

}
