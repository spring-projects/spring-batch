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

import java.util.Collection;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Internal parser for the &lt;flow/&gt; elements inside a job..
 * 
 * @see JobParser
 * 
 * @author Dave Syer
 * 
 */
public class FlowElementParser {

	private static final String ID_ATTR = "id";

	private static final String REF_ATTR = "parent";

	/**
	 * Parse the flow and turn it into a list of transitions.
	 * 
	 * @param element the &lt;flow/gt; element to parse
	 * @param parserContext the parser context for the bean factory
	 * @return a collection of bean definitions for
	 * {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 * instances objects
	 */
	public Collection<BeanDefinition> parse(Element element, ParserContext parserContext) {

		String refAttribute = element.getAttribute(REF_ATTR);
		String idAttribute = element.getAttribute(ID_ATTR);

		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.batch.core.job.flow.support.state.FlowState");

		AbstractBeanDefinition flowDefinition = new GenericBeanDefinition();
		flowDefinition.setParentName(refAttribute);
		MutablePropertyValues propertyValues = flowDefinition.getPropertyValues();
		propertyValues.addPropertyValue("name", idAttribute);
		stateBuilder.addConstructorArgValue(flowDefinition);
		stateBuilder.addConstructorArgValue(idAttribute);
		return InlineFlowParser.getNextElements(parserContext, stateBuilder.getBeanDefinition(), element);

	}
}
