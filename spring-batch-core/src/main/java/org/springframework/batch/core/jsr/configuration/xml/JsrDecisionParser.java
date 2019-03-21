/*
 * Copyright 2013-2014 the original author or authors.
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

import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.jsr.configuration.support.BatchArtifactType;
import org.springframework.batch.core.jsr.job.flow.support.state.JsrStepState;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;decision /&gt; element as specified in JSR-352.  The current state
 * parses a decision element and assumes that it refers to a {@link JobExecutionDecider}
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 */
public class JsrDecisionParser {

	private static final String ID_ATTRIBUTE = "id";
	private static final String REF_ATTRIBUTE = "ref";

	public Collection<BeanDefinition> parse(Element element, ParserContext parserContext, String jobFactoryRef) {
		BeanDefinitionBuilder factoryBuilder = BeanDefinitionBuilder.genericBeanDefinition();
		AbstractBeanDefinition factoryDefinition = factoryBuilder.getRawBeanDefinition();
		factoryDefinition.setBeanClass(DecisionStepFactoryBean.class);

		BeanDefinitionBuilder stateBuilder = BeanDefinitionBuilder.genericBeanDefinition(JsrStepState.class);

		String idAttribute = element.getAttribute(ID_ATTRIBUTE);

		parserContext.registerBeanComponent(new BeanComponentDefinition(factoryDefinition, idAttribute));
		stateBuilder.addConstructorArgReference(idAttribute);

		String refAttribute = element.getAttribute(REF_ATTRIBUTE);
		factoryDefinition.getPropertyValues().add("decider", new RuntimeBeanReference(refAttribute));
		factoryDefinition.getPropertyValues().add("name", idAttribute);

		if(StringUtils.hasText(jobFactoryRef)) {
			factoryDefinition.setAttribute("jobParserJobFactoryBeanRef", jobFactoryRef);
		}

		new PropertyParser(refAttribute, parserContext, BatchArtifactType.STEP_ARTIFACT, idAttribute).parseProperties(element);

		return FlowParser.getNextElements(parserContext, stateBuilder.getBeanDefinition(), element);
	}
}
