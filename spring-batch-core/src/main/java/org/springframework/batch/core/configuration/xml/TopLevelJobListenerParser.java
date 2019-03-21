/*
 * Copyright 2009-2014 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.listener.AbstractListenerFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parse &lt;job-listener/&gt; elements in the batch namespace.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public class TopLevelJobListenerParser extends AbstractSingleBeanDefinitionParser {

	private static final JobExecutionListenerParser jobListenerParser = new JobExecutionListenerParser();

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		CoreNamespaceUtils.autoregisterBeansForNamespace(parserContext, element);
		jobListenerParser.doParse(element, parserContext, builder);
	}

	@Override
	protected Class<? extends AbstractListenerFactoryBean<?>> getBeanClass(Element element) {
		return jobListenerParser.getBeanClass();
	}

}
