/*
 * Copyright 2013 the original author or authors.
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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser used to parse the batch.xml file as defined in JSR-352.  It is <em>not</em>
 * recommended to use the batch.xml approach with Spring to manage bean instantiation.
 * It is recommended that standard Spring bean configurations (via XML or Java Config)
 * be used.
 * 
 * @author Michael Minella
 * @since 3.0
 */
public class BatchParser extends AbstractBeanDefinitionParser {

	private static final Log logger = LogFactory.getLog(BatchParser.class);

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element,
			ParserContext parserContext) {
		BeanDefinitionRegistry registry = parserContext.getRegistry();

		parseRefElements(element, registry);

		return null;
	}

	private void parseRefElements(Element element,
			BeanDefinitionRegistry registry) {
		List<Element> beanElements = DomUtils.getChildElementsByTagName(element, "ref");

		if(beanElements.size() > 0) {
			for (Element curElement : beanElements) {
				AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(curElement.getAttribute("class"))
						.getBeanDefinition();

				beanDefinition.setScope("step");

				String beanName = curElement.getAttribute("id");

				if(!registry.containsBeanDefinition(beanName)) {
					registry.registerBeanDefinition(beanName, beanDefinition);
				} else {
					logger.info("Ignoring batch.xml bean definition for " + beanName + " because another bean of the same name has been registered");
				}
			}
		}

	}
}
