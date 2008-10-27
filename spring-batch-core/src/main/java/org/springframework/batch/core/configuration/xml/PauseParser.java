/*
 * Copyright 2006-2007 the original author or authors.
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

import org.springframework.batch.core.job.flow.PauseState;
import org.springframework.batch.flow.StateTransition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Internal parser for the &lt;pause/&gt; elements inside a job. A pause element
 * causes the job flow to end and will resume on the next execution at the next
 * state. Used by the {@link JobParser}.
 * 
 * @see JobParser
 * 
 * @author Dave Syer
 * 
 */
public class PauseParser {

	/**
	 * Parse the pause and turn it into a transition.
	 * 
	 * @param element the &lt;pause/gt; element to parse
	 * @param parserContext the parser context for the bean factory
	 * @return a bean definitions for a {@link StateTransition}
	 * instances objects
	 */
	public RuntimeBeanReference parse(Element element, ParserContext parserContext) {

		String nextAttribute = element.getAttribute("next");
		String idAttribute = element.getAttribute("id");

		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder.genericBeanDefinition(PauseState.class);
		stateBuilder.addConstructorArgValue(idAttribute);
		return StepParser.getStateTransitionReference(parserContext, stateBuilder.getBeanDefinition(), "*",
				nextAttribute);

	}
}
