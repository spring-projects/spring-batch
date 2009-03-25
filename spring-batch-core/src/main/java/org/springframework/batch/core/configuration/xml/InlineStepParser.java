/*
 * Copyright 2006-2009 the original author or authors.
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

import static org.springframework.batch.core.configuration.xml.FlowParser.END_ELE;
import static org.springframework.batch.core.configuration.xml.FlowParser.FAIL_ELE;
import static org.springframework.batch.core.configuration.xml.FlowParser.NEXT_ATTR;
import static org.springframework.batch.core.configuration.xml.FlowParser.NEXT_ELE;
import static org.springframework.batch.core.configuration.xml.FlowParser.STOP_ELE;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.job.flow.support.state.StepState;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Internal parser for the &lt;step/&gt; elements inside a job. A step element
 * references a bean definition for a
 * {@link org.springframework.batch.core.Step} and goes on to (optionally) list
 * a set of transitions from that step to others with &lt;next on="pattern"
 * to="stepName"/&gt;. Used by the {@link JobParser}.
 * 
 * @see JobParser
 * 
 * @author Dave Syer
 * @author Thomas Risberg
 * @since 2.0
 */
public class InlineStepParser extends AbstractStepParser {

	private static final String REF_ATTR = "ref";

	private static final String TX_MANAGER_ATTR = "transaction-manager";

	/**
	 * Parse the step and turn it into a list of transitions.
	 * 
	 * @param element the &lt;step/gt; element to parse
	 * @param parserContext the parser context for the bean factory
	 * @param jobRepositoryRef the reference to the jobRepository from the
	 *            enclosing tag
	 * @return a collection of bean definitions for
	 *         {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 *         instances objects
	 */
	public Collection<BeanDefinition> parse(Element element, ParserContext parserContext, String jobRepositoryRef) {

		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder.genericBeanDefinition(StepState.class);
		String stepId = element.getAttribute(ID_ATTR);
		String stepRef = element.getAttribute(REF_ATTR);

		if (StringUtils.hasText(stepRef)) {
			this.checkStepRef(element, parserContext);
			BeanDefinitionBuilder stepBuilder = BeanDefinitionBuilder.genericBeanDefinition(DelegatingStep.class);
			stepBuilder.addConstructorArgValue(stepId);
			stepBuilder.addConstructorArgReference(stepRef);
			AbstractBeanDefinition bd = stepBuilder.getBeanDefinition();
			bd.setSource(parserContext.extractSource(element));
			parserContext.getRegistry().registerBeanDefinition(stepId, bd);
			stateBuilder.addConstructorArgReference(stepId);
		}
		else {
			AbstractBeanDefinition bd = parseTasklet(element, parserContext, jobRepositoryRef);
			parserContext.registerBeanComponent(new BeanComponentDefinition(bd, stepId));
			stateBuilder.addConstructorArgReference(stepId);
		}
		return FlowParser.getNextElements(parserContext, stepId, stateBuilder.getBeanDefinition(), element);

	}

	private void checkStepRef(Element element, ParserContext parserContext) {
		List<String> legalAttributes = Arrays.asList(ID_ATTR, REF_ATTR, NEXT_ATTR, TX_MANAGER_ATTR);
		NamedNodeMap allAttributes = element.getAttributes();
		for (int i = 0; i < allAttributes.getLength(); i++) {
			String attribute = allAttributes.item(i).getNodeName();
			if (!legalAttributes.contains(attribute)) {
				cantBeCombinedWithRef(attribute, "attribute", element, parserContext);
			}
		}

		List<String> legalElements = Arrays.asList(NEXT_ELE, END_ELE, FAIL_ELE, STOP_ELE);
		NodeList allElements = element.getChildNodes();
		for (int i = 0; i < allElements.getLength(); i++) {
			Node child = allElements.item(i);
			if (child instanceof Element) {
				String childName = child.getNodeName();
				if (!legalElements.contains(childName)) {
					cantBeCombinedWithRef(childName, "element", element, parserContext);
				}
			}
		}
	}

	private void cantBeCombinedWithRef(String itemName, String itemType, Element element, ParserContext parserContext) {
		parserContext.getReaderContext().error(
				"The '" + itemName + "' " + itemType + " can't be combined with the '" + REF_ATTR + "=\""
						+ element.getAttribute(REF_ATTR) + "\"' attribute specification for <" + element.getNodeName()
						+ ">", element);
	}

}
