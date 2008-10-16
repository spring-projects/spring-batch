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
import org.springframework.batch.core.job.StepTransition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Internal parser for the &lt;step/&gt; elements inside a job. A step element
 * references a bean definition for a {@link Step} and goes on to (optionally)
 * list a set of transitions from that step to others with &lt;next on="pattern"
 * to="stepName"/&gt;.  Used by the {@link JobParser}.
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
	 * @return a collection of bean definitions for {@link StepTransition} objects
	 */
	public Collection<RuntimeBeanReference> parse(Element element, ParserContext parserContext) {

		String refAttribute = element.getAttribute("name");

		Collection<RuntimeBeanReference> list = new ArrayList<RuntimeBeanReference>();
		@SuppressWarnings("unchecked")
		List<Element> nextElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "next");

		// If there are no next elements then this must be an end state
		if (nextElements.isEmpty()) {
			list.add(getStepTransitionReference(parserContext, new RuntimeBeanReference(refAttribute), "*", null));
		}
		else {
			// Otherwise we need to capture the "to" state
			for (Element nextElement : nextElements) {
				String onAttribute = nextElement.getAttribute("on");
				String nextAttribute = nextElement.getAttribute("to");
				list.add(getStepTransitionReference(parserContext, new RuntimeBeanReference(refAttribute), onAttribute,
						nextAttribute));
			}
		}

		return list;

	}

	/**
	 * @param parserContext the parser context
	 * @param runtimeBeanReference a reference to the step implementation
	 * @param on the pattern value
	 * @param next the next step id
	 * @return a bean definition for a {@link StepTransition}
	 */
	private RuntimeBeanReference getStepTransitionReference(ParserContext parserContext,
			RuntimeBeanReference runtimeBeanReference, String on, String next) {

		RootBeanDefinition nextDef = new RootBeanDefinition(StepTransition.class);
		nextDef.getConstructorArgumentValues().addIndexedArgumentValue(0, runtimeBeanReference);
		nextDef.getConstructorArgumentValues().addIndexedArgumentValue(1, on);

		if (StringUtils.hasText(next)) {
			nextDef.getConstructorArgumentValues().addIndexedArgumentValue(2, next);
		}

		// TODO: do we need to use RuntimeBeanReference?
		String nextDefName = parserContext.getReaderContext().generateBeanName(nextDef);
		BeanComponentDefinition nextDefComponent = new BeanComponentDefinition(nextDef, nextDefName);
		parserContext.registerBeanComponent(nextDefComponent);

		return new RuntimeBeanReference(nextDefName);

	}

}
