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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * @author Dave Syer
 * 
 */
public class InlineFlowParser extends AbstractFlowParser {
	
	private final String flowName;

	/**
	 * Construct a {@link InlineFlowParser} with the specified name and using the
	 * provided job repository ref.
	 * 
	 * @param flowName the name of the flow
	 * @param jobFactoryRef the reference to the {@link JobParserJobFactoryBean}
	 * from the enclosing tag
	 */
	public InlineFlowParser(String flowName, String jobFactoryRef) {
		this.flowName = flowName;
		setJobFactoryRef(jobFactoryRef);

	}

	/**
	 * @param element the top level element containing a flow definition
	 * @param parserContext the {@link ParserContext}
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

		builder.getRawBeanDefinition().setAttribute("flowName", flowName);
		builder.addPropertyValue("name", flowName);
		super.doParse(element, parserContext, builder);
		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		parserContext.popAndRegisterContainingComponent();

	}

}
