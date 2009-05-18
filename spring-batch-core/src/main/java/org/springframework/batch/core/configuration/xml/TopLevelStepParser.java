/*
 * Copyright 2006-2008 the original author or authors.
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

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser for the lt;step/gt; top level element in the Batch namespace. Sets up
 * and returns a bean definition for a
 * {@link org.springframework.batch.core.Step}.
 * 
 * @author Thomas Risberg
 * 
 */
public class TopLevelStepParser extends AbstractBeanDefinitionParser {

	private static final StandaloneStepParser stepParser = new StandaloneStepParser();

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {

		CoreNamespaceUtils.autoregisterBeansForNamespace(parserContext, parserContext.extractSource(element));

		return stepParser.parse(element, parserContext);

	}

}
