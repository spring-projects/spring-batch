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

import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;decision /&gt; element as specified in JSR-352.  The current state
 * parses a decision element and assumes that it refers to a {@link JobExecutionDecider}
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 */
public class DecisionParser {

	private static final String ID_ATTRIBUTE = "id";
	private static final String REF_ATTRIBUTE = "ref";

	public Collection<BeanDefinition> parse(Element element, ParserContext parserContext) {
		String refAttribute = element.getAttribute(REF_ATTRIBUTE);
		String idAttribute = element.getAttribute(ID_ATTRIBUTE);

		BeanDefinitionBuilder stateBuilder =
				BeanDefinitionBuilder.genericBeanDefinition("org.springframework.batch.core.jsr.configuration.xml.DecisionStateFactoryBean");
		stateBuilder.addPropertyReference("decider", refAttribute);
		stateBuilder.addPropertyValue("name", idAttribute);

		new PropertyParser(refAttribute, parserContext).parseProperties(element);

		return FlowParser.getNextElements(parserContext, stateBuilder.getBeanDefinition(), element);
	}
}
