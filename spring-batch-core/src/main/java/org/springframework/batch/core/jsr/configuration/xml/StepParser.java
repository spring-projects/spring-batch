/*
 * Copyright 2013-2018 the original author or authors.
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

import org.springframework.batch.core.jsr.configuration.support.BatchArtifactType;
import org.springframework.batch.core.jsr.job.flow.support.state.JsrStepState;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;

/**
 * Parser for the &lt;step /&gt; element defined by JSR-352.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Chris Schaefer
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public class StepParser extends AbstractSingleBeanDefinitionParser {
	private static final String CHUNK_ELEMENT = "chunk";
	private static final String BATCHLET_ELEMENT = "batchlet";
	private static final String ALLOW_START_IF_COMPLETE_ATTRIBUTE = "allow-start-if-complete";
	private static final String START_LIMIT_ATTRIBUTE = "start-limit";
	private static final String SPLIT_ID_ATTRIBUTE = "id";
	private static final String PARTITION_ELEMENT = "partition";

	protected Collection<BeanDefinition> parse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		BeanDefinitionBuilder defBuilder = BeanDefinitionBuilder.genericBeanDefinition();
		AbstractBeanDefinition bd = defBuilder.getRawBeanDefinition();
		bd.setBeanClass(StepFactoryBean.class);
		bd.getPropertyValues().addPropertyValue("batchPropertyContext", new RuntimeBeanReference("batchPropertyContext"));

		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder.genericBeanDefinition(JsrStepState.class);

		String stepName = element.getAttribute(SPLIT_ID_ATTRIBUTE);
		builder.addPropertyValue("name", stepName);

		parserContext.registerBeanComponent(new BeanComponentDefinition(bd, stepName));
		stateBuilder.addConstructorArgReference(stepName);

		String startLimit = element.getAttribute(START_LIMIT_ATTRIBUTE);
		if(StringUtils.hasText(startLimit)) {
			bd.getPropertyValues().addPropertyValue("startLimit", startLimit);
		}

		String allowStartIfComplete = element.getAttribute(ALLOW_START_IF_COMPLETE_ATTRIBUTE);
		boolean allowStartIfCompleteValue = false;
		if(StringUtils.hasText(allowStartIfComplete)) {
			bd.getPropertyValues().addPropertyValue("allowStartIfComplete",
					allowStartIfComplete);
			allowStartIfCompleteValue = Boolean.valueOf(allowStartIfComplete);
		}

		new ListenerParser(JsrStepListenerFactoryBean.class, "listeners").parseListeners(element, parserContext, bd, stepName);
		new PropertyParser(stepName, parserContext, BatchArtifactType.STEP, stepName).parseProperties(element);

		// look at all nested elements
		NodeList children = element.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node nd = children.item(i);

			if (nd instanceof Element) {
				Element nestedElement = (Element) nd;
				String name = nestedElement.getLocalName();

				if(name.equalsIgnoreCase(BATCHLET_ELEMENT)) {
					new BatchletParser().parseBatchlet(nestedElement, bd, parserContext, stepName);
				} else if(name.equals(CHUNK_ELEMENT)) {
					new ChunkParser().parse(nestedElement, bd, parserContext, stepName);
				} else if(name.equals(PARTITION_ELEMENT)) {
					new PartitionParser(stepName, allowStartIfCompleteValue).parse(nestedElement, bd, parserContext, stepName);
				}
			}
		}

		return FlowParser.getNextElements(parserContext, stepName, stateBuilder.getBeanDefinition(), element);
	}
}
