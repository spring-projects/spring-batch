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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.StepState;
import org.springframework.batch.flow.StateTransition;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Internal parser for the &lt;step/&gt; elements inside a job. A step element
 * references a bean definition for a {@link Step} and goes on to (optionally)
 * list a set of transitions from that step to others with &lt;next on="pattern"
 * to="stepName"/&gt;. Used by the {@link JobParser}.
 * 
 * @see JobParser
 * 
 * @author Dave Syer
 * 
 */
public class StepParser {

	/**
	 * Parse the step and turn it into a list of transitions.
	 * 
	 * @param element the &lt;step/gt; element to parse
	 * @param parserContext the parser context for the bean factory
	 * @return a collection of bean definitions for {@link StateTransition}
	 * instances objects
	 */
	public Collection<RuntimeBeanReference> parse(Element element, ParserContext parserContext) {

		String refAttribute = element.getAttribute("name");

		Collection<RuntimeBeanReference> list = new ArrayList<RuntimeBeanReference>();

		String shortNextAttribute = element.getAttribute("next");
		boolean hasNextAttribute = StringUtils.hasText(shortNextAttribute);
		if (hasNextAttribute) {
			list.add(getStateTransitionReference(parserContext, new RuntimeBeanReference(refAttribute), null,
					shortNextAttribute));
		}

		@SuppressWarnings("unchecked")
		List<Element> nextElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "next");

		// If there are no next elements then this must be an end state
		if (nextElements.isEmpty() && !hasNextAttribute) {
			list.add(getStateTransitionReference(parserContext, new RuntimeBeanReference(refAttribute), null, null));
		}
		else {
			// Otherwise we need to capture the "to" state
			for (Element nextElement : nextElements) {
				String onAttribute = nextElement.getAttribute("on");
				String nextAttribute = nextElement.getAttribute("to");
				if (hasNextAttribute && onAttribute.equals("*")) {
					throw new BeanCreationException("Duplicate transition pattern found for '*' "
							+ "(only specify one of next= attribute at step level and next element with on='*')");
				}
				list.add(getStateTransitionReference(parserContext, new RuntimeBeanReference(refAttribute),
						onAttribute, nextAttribute));
			}
		}

		return list;

	}

	/**
	 * @param parserContext
	 * @param runtimeBeanReference
	 * @param onAttribute
	 * @param nextAttribute
	 * @return
	 */
	private RuntimeBeanReference getStateTransitionReference(ParserContext parserContext,
			RuntimeBeanReference runtimeBeanReference, String onAttribute, String nextAttribute) {
		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder.genericBeanDefinition(StepState.class);
		stateBuilder.addConstructorArgValue(runtimeBeanReference);
		return getStateTransitionReference(parserContext, stateBuilder.getBeanDefinition(), onAttribute,
				nextAttribute);
	}

	/**
	 * @param parserContext the parser context
	 * @param stateDefinition a reference to the state implementation
	 * @param on the pattern value
	 * @param next the next step id
	 * @return a bean definition for a {@link StateTransition}
	 */
	public static RuntimeBeanReference getStateTransitionReference(ParserContext parserContext,
			BeanDefinition stateDefinition, String on,
			String next) {

		BeanDefinitionBuilder nextBuilder = BeanDefinitionBuilder.genericBeanDefinition(StateTransition.class);
		nextBuilder.addConstructorArgValue(stateDefinition);

		if (StringUtils.hasText(on)) {
			nextBuilder.addConstructorArgValue(on);
		}

		if (StringUtils.hasText(next)) {
			nextBuilder.setFactoryMethod("createStateTransition");
			nextBuilder.addConstructorArgValue(next);
		}
		else {
			nextBuilder.setFactoryMethod("createEndStateTransition");
		}

		// TODO: do we need to use RuntimeBeanReference?
		AbstractBeanDefinition nextDef = nextBuilder.getBeanDefinition();
		String nextDefName = parserContext.getReaderContext().generateBeanName(nextDef);
		BeanComponentDefinition nextDefComponent = new BeanComponentDefinition(nextDef, nextDefName);
		parserContext.registerBeanComponent(nextDefComponent);

		return new RuntimeBeanReference(nextDefName);

	}

}
