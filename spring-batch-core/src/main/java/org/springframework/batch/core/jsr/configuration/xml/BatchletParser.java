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

import org.springframework.batch.core.jsr.configuration.support.BatchArtifactType;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;batchlet /&gt; tag defined in JSR-352.  The current state
 * of this parser parses a batchlet element into a {@link Tasklet} (the ref
 * attribute is expected to point to an implementation of Tasklet).
 * 
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 */
public class BatchletParser extends AbstractSingleBeanDefinitionParser {
	private static final String REF = "ref";

	public void parseBatchlet(Element batchletElement, AbstractBeanDefinition bd, ParserContext parserContext, String stepName) {
		bd.setBeanClass(StepFactoryBean.class);
		bd.setAttribute("isNamespaceStep", false);

		String taskletRef = batchletElement.getAttribute(REF);

		if (StringUtils.hasText(taskletRef)) {
			bd.getPropertyValues().addPropertyValue("stepTasklet", new RuntimeBeanReference(taskletRef));
		}

		bd.setRole(BeanDefinition.ROLE_SUPPORT);
		bd.setSource(parserContext.extractSource(batchletElement));

		new PropertyParser(taskletRef, parserContext, BatchArtifactType.STEP_ARTIFACT, stepName).parseProperties(batchletElement);
	}
}
