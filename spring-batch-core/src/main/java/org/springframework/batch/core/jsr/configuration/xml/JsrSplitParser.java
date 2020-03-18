/*
 * Copyright 2013 the original author or authors.
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

import java.util.Collection;
import java.util.List;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parses a &lt;split /&gt; element as defined in JSR-352.
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 */
public class JsrSplitParser {
	private static final String TASK_EXECUTOR_PROPERTY_NAME = "taskExecutor";
	private static final String JSR_352_SPLIT_TASK_EXECUTOR_BEAN_NAME = "jsr352splitTaskExecutor";

	private String jobFactoryRef;

	public JsrSplitParser(String jobFactoryRef) {
		this.jobFactoryRef = jobFactoryRef;
	}

	public Collection<BeanDefinition> parse(Element element, ParserContext parserContext) {

		String idAttribute = element.getAttribute("id");

		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.batch.core.jsr.job.flow.support.state.JsrSplitState");

		List<Element> flowElements = DomUtils.getChildElementsByTagName(element, "flow");

		if (flowElements.size() < 2) {
			parserContext.getReaderContext().error("A <split/> must contain at least two 'flow' elements.", element);
		}

		Collection<Object> flows = new ManagedList<>();
		int i = 0;
		for (Element nextElement : flowElements) {
			FlowParser flowParser = new FlowParser(idAttribute + "." + i, jobFactoryRef);
			flows.add(flowParser.parse(nextElement, parserContext));
			i++;
		}

		stateBuilder.addConstructorArgValue(flows);
		stateBuilder.addConstructorArgValue(idAttribute);

		PropertyValue propertyValue = getSplitTaskExecutorPropertyValue(parserContext.getRegistry());
		stateBuilder.addPropertyValue(propertyValue.getName(), propertyValue.getValue());

		return FlowParser.getNextElements(parserContext, null, stateBuilder.getBeanDefinition(), element);
	}

	protected PropertyValue getSplitTaskExecutorPropertyValue(BeanDefinitionRegistry beanDefinitionRegistry) {
		PropertyValue propertyValue;

		if (hasBeanDefinition(beanDefinitionRegistry, JSR_352_SPLIT_TASK_EXECUTOR_BEAN_NAME)) {
			propertyValue = new PropertyValue(TASK_EXECUTOR_PROPERTY_NAME, new RuntimeBeanReference(JSR_352_SPLIT_TASK_EXECUTOR_BEAN_NAME));
		} else {
			propertyValue = new PropertyValue(TASK_EXECUTOR_PROPERTY_NAME, new SimpleAsyncTaskExecutor());
		}

		return propertyValue;
	}

	private boolean hasBeanDefinition(BeanDefinitionRegistry beanDefinitionRegistry, String beanName) {
		return beanDefinitionRegistry.containsBeanDefinition(beanName);
	}
}
