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
import java.util.List;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Internal parser for the &lt;split/&gt; elements inside a job. A split element
 * optionally references a bean definition for a {@link TaskExecutor} and goes
 * on to list a set of transitions to other states with &lt;next on="pattern"
 * to="stepName"/&gt;. Used by the {@link JobParser}.
 * 
 * @see JobParser
 * 
 * @author Dave Syer
 * 
 */
public class SplitParser {

	/**
	 * 
	 */
	private static final String PARENT_ATTR = "parent";

	private final String jobFactoryRef;

	/**
	 * Construct a {@link InlineFlowParser} using the provided job repository
	 * ref.
	 * 
	 * @param jobFactoryRef the reference to the {@link JobParserJobFactoryBean}
	 * from the enclosing tag
	 */
	public SplitParser(String jobFactoryRef) {
		this.jobFactoryRef = jobFactoryRef;
	}

	/**
	 * Parse the split and turn it into a list of transitions.
	 * 
	 * @param element the &lt;split/gt; element to parse
	 * @param parserContext the parser context for the bean factory
	 * @return a collection of bean definitions for
	 * {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 * instances objects
	 */
	public Collection<BeanDefinition> parse(Element element, ParserContext parserContext) {

		String idAttribute = element.getAttribute("id");

		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.batch.core.job.flow.support.state.SplitState");

		String taskExecutorBeanId = element.getAttribute("task-executor");
		if (StringUtils.hasText(taskExecutorBeanId)) {
			RuntimeBeanReference taskExecutorRef = new RuntimeBeanReference(taskExecutorBeanId);
			stateBuilder.addPropertyValue("taskExecutor", taskExecutorRef);
		}

		@SuppressWarnings("unchecked")
		List<Element> flowElements = DomUtils.getChildElementsByTagName(element, "flow");

		if (flowElements.size() < 2) {
			parserContext.getReaderContext().error("A <split/> must contain at least two 'flow' elements.", element);
		}

		@SuppressWarnings("unchecked")
		Collection<Object> flows = new ManagedList();
		int i = 0;
		String prefix = idAttribute;
		for (Element nextElement : flowElements) {
			String ref = nextElement.getAttribute(PARENT_ATTR);
			if (StringUtils.hasText(ref)) {
				if (nextElement.getElementsByTagName("*").getLength() > 0) {
					parserContext.getReaderContext().error(
							"A <flow/> in a <split/> must have ref= or nested <flow/>, but not both.", nextElement);
				}
				AbstractBeanDefinition flowDefinition = new GenericBeanDefinition();
				flowDefinition.setParentName(ref);
				MutablePropertyValues propertyValues = flowDefinition.getPropertyValues();
				propertyValues.addPropertyValue("name", prefix + "." + i);
				flows.add(flowDefinition);
			}
			else {
				InlineFlowParser flowParser = new InlineFlowParser(prefix + "." + i, jobFactoryRef);
				flows.add(flowParser.parse(nextElement, parserContext));
			}
			i++;
		}

		stateBuilder.addConstructorArgValue(flows);
		stateBuilder.addConstructorArgValue(prefix);

		return InlineFlowParser.getNextElements(parserContext, stateBuilder.getBeanDefinition(), element);

	}

}
