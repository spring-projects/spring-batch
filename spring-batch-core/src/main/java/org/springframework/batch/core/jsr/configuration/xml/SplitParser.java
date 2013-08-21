/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parses a &lt;split /&gt; element as defined in JSR-352.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class SplitParser {

	private String jobFactoryRef;

	public SplitParser(String jobFactoryRef) {
		this.jobFactoryRef = jobFactoryRef;
	}

	public Collection<BeanDefinition> parse(Element element, ParserContext parserContext) {

		String idAttribute = element.getAttribute("id");

		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.batch.core.job.flow.support.state.SplitState");

		List<Element> flowElements = DomUtils.getChildElementsByTagName(element, "flow");

		if (flowElements.size() < 2) {
			parserContext.getReaderContext().error("A <split/> must contain at least two 'flow' elements.", element);
		}

		Collection<Object> flows = new ManagedList<Object>();
		int i = 0;
		String prefix = idAttribute;
		for (Element nextElement : flowElements) {
			FlowParser flowParser = new FlowParser(prefix + "." + i, jobFactoryRef);
			flows.add(flowParser.parse(nextElement, parserContext));
			i++;
		}

		stateBuilder.addConstructorArgValue(flows);
		stateBuilder.addConstructorArgValue(prefix);

		return FlowParser.getNextElements(parserContext, null, stateBuilder.getBeanDefinition(), element);
	}
}
