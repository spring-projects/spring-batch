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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.batch.core.job.flow.support.state.StepState;
import org.springframework.batch.core.jsr.StepContextFactoryBean;
import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;step /&gt; element defined by JSR-352.
 * 
 * @author Michael Minella
 * @author Glenn Renfro
 * @since 3.0
 */
public class StepParser extends AbstractSingleBeanDefinitionParser {

	private static final String CHUNK_ELEMENT = "chunk";
	private static final String BATCHLET_ELEMENT = "batchlet";
	private static final String ALLOW_START_IF_COMPLETE_ATTRIBUTE = "allow-start-if-complete";
	private static final String START_LIMIT_ATTRIBUTE = "start-limit";
	private static final String SPLIT_ID_ATTRIBUTE = "id";

	protected Collection<BeanDefinition>  parse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		BeanDefinitionBuilder defBuilder = BeanDefinitionBuilder.genericBeanDefinition();
		AbstractBeanDefinition bd = defBuilder.getRawBeanDefinition();
		bd.setBeanClass(StepFactoryBean.class);

		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder.genericBeanDefinition(StepState.class);

		String stepName = element.getAttribute(SPLIT_ID_ATTRIBUTE);
		builder.addPropertyValue("name", stepName);
		parserContext.registerBeanComponent(new BeanComponentDefinition(bd, stepName));
		stateBuilder.addConstructorArgReference(stepName);

		String startLimit = element.getAttribute(START_LIMIT_ATTRIBUTE);
		if(StringUtils.hasText(startLimit)) {
			bd.getPropertyValues().addPropertyValue("startLimit", startLimit);
		}

		String allowStartIfComplete = element.getAttribute(ALLOW_START_IF_COMPLETE_ATTRIBUTE);
		if(StringUtils.hasText(allowStartIfComplete)) {
			bd.getPropertyValues().addPropertyValue("allowStartIfComplete",
					allowStartIfComplete);
		}

		new ListnerParser(StepListenerFactoryBean.class, "listeners").parseListeners(element, parserContext, bd);

		// look at all nested elements
		NodeList children = element.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node nd = children.item(i);

			if (nd instanceof Element) {
				Element nestedElement = (Element) nd;
				String name = nestedElement.getLocalName();

				if(name.equalsIgnoreCase(BATCHLET_ELEMENT)) {
					new BatchletParser().parseBatchlet(element, nestedElement, bd, parserContext);
				} else if(name.equals(CHUNK_ELEMENT)) {
					new ChunkParser().parse(nestedElement, bd, parserContext);
				}
			}
		}

		AbstractBeanDefinition stepContextBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(StepContextFactoryBean.class)
				.getBeanDefinition();

		stepContextBeanDefinition.setScope("step");

		parserContext.getRegistry().registerBeanDefinition(stepName + "stepContext", stepContextBeanDefinition);

		return FlowParser.getNextElements(parserContext, stepName, stateBuilder.getBeanDefinition(), element);
	}
}
