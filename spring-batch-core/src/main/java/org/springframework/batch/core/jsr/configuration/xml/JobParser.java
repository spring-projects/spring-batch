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

import org.springframework.batch.core.configuration.xml.CoreNamespaceUtils;
import org.springframework.batch.core.jsr.StepContextFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parses a &lt;job /&gt; tag as defined in JSR-352.  Current state parses into
 * the standard Spring Batch artifacts.
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 */
public class JobParser extends AbstractSingleBeanDefinitionParser {
	private static final String ID_ATTRIBUTE = "id";
	private static final String RESTARTABLE_ATTRIBUTE = "restartable";

	@Override
	protected Class<JobFactoryBean> getBeanClass(Element element) {
		return JobFactoryBean.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		CoreNamespaceUtils.autoregisterBeansForNamespace(parserContext, parserContext.extractSource(element));
		JsrNamespaceUtils.autoregisterJsrBeansForNamespace(parserContext);

		String jobName = element.getAttribute(ID_ATTRIBUTE);
		builder.addConstructorArgValue(jobName);

		String restartableAttribute = element.getAttribute(RESTARTABLE_ATTRIBUTE);
		if (StringUtils.hasText(restartableAttribute)) {
			builder.addPropertyValue("restartable", restartableAttribute);
		}

		BeanDefinition flowDef = new FlowParser(jobName, jobName).parse(element, parserContext);
		builder.addPropertyValue("flow", flowDef);

		AbstractBeanDefinition stepContextBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(StepContextFactoryBean.class)
				.getBeanDefinition();

		stepContextBeanDefinition.setScope("step");

		parserContext.getRegistry().registerBeanDefinition("stepContextFactory", stepContextBeanDefinition);

		new ListnerParser(JobListenerFactoryBean.class, "jobExecutionListeners").parseListeners(element, parserContext, builder);
		new PropertyParser(PropertyParser.JOB_ARTIFACT_PROPERTY_PREFIX + jobName, parserContext).parseProperties(element);
	}
}
