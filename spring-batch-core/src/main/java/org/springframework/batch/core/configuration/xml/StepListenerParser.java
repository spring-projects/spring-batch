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

import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Internal parser for a step listener element. Builds a
 * {@link StepListenerFactoryBean} using attributes from the configuration.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public class StepListenerParser {

	/**
	 * Parse the step and turn it into a list of transitions.
	 * 
	 * @param element the &lt;step/gt; element to parse
	 * @param parserContext the parser context for the bean factory
	 */
	@SuppressWarnings("unchecked")
	public AbstractBeanDefinition parse(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder listenerBuilder = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.batch.core.listener.StepListenerFactoryBean");
		String id = element.getAttribute("id");
		String listenerRef = element.getAttribute("ref");
		String className = element.getAttribute("class");
		checkListenerElementAttributes(parserContext, element, id, listenerRef, className);
		if (StringUtils.hasText(listenerRef)) {
			listenerBuilder.addPropertyReference("delegate", listenerRef);
		}
		else if (StringUtils.hasText(className)) {
			RootBeanDefinition beanDef = new RootBeanDefinition(className, null, null);
			listenerBuilder.addPropertyValue("delegate", beanDef);
		}
		else {
			parserContext.getReaderContext().error(
					"Neither 'ref' or 'class' specified for <" + element.getTagName() + "> element", element);
		}

		ManagedMap metaDataMap = new ManagedMap();
		String[] methodNameAttributes = new String[] { "before-step-method", "after-step-method",
				"before-chunk-method", "after-chunk-method", "before-read-method", "after-read-method",
				"on-read-error-method", "before-process-method", "after-process-method", "on-process-error-method",
				"before-write-method", "after-write-method", "on-write-error-method", "on-skip-in-read-method",
				"on-skip-in-process-method", "on-skip-in-write-method" };
		for (String metaDataPropertyName : methodNameAttributes) {
			String listenerMethod = element.getAttribute(metaDataPropertyName);
			if (StringUtils.hasText(listenerMethod)) {
				metaDataMap.put(metaDataPropertyName, listenerMethod);
			}
		}
		listenerBuilder.addPropertyValue("metaDataMap", metaDataMap);

		AbstractBeanDefinition beanDef = listenerBuilder.getBeanDefinition();
		if (!StringUtils.hasText(id)) {
			id = parserContext.getReaderContext().generateBeanName(beanDef);
		}

		return beanDef;
	}

	private void checkListenerElementAttributes(ParserContext parserContext, Element listenerElement, String id,
			String listenerRef, String className) {
		if ((StringUtils.hasText(id) || StringUtils.hasText(className)) && StringUtils.hasText(listenerRef)) {
			NamedNodeMap attributeNodes = listenerElement.getAttributes();
			StringBuilder attributes = new StringBuilder();
			for (int i = 0; i < attributeNodes.getLength(); i++) {
				if (i > 0) {
					attributes.append(" ");
				}
				attributes.append(attributeNodes.item(i));
			}
			parserContext.getReaderContext().error(
					"Both 'ref' and " + (StringUtils.hasText(id) ? "'id'" : "'class'")
							+ " specified; use 'class' with an optional 'id' or just 'ref' for <"
							+ listenerElement.getTagName() + "> element specified with attributes: " + attributes,
					listenerElement);
		}
	}

}
