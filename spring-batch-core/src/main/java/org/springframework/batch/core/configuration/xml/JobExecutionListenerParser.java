/*
 * Copyright 2002-2008 the original author or authors.
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

import static org.springframework.util.StringUtils.hasText;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * {@link BeanDefinitionParser} for
 * {@link org.springframework.batch.core.JobExecutionListener}s
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class JobExecutionListenerParser {

	@SuppressWarnings("unchecked")
	public ManagedList parse(Element element, ParserContext parserContext) {
		List<BeanDefinition> listeners = new ArrayList<BeanDefinition>();
		List<Element> listenerElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "listener");
		for (Element listenerElement : listenerElements) {
			BeanDefinitionBuilder listenerBuilder = BeanDefinitionBuilder
					.genericBeanDefinition("org.springframework.batch.core.listener.JobListenerFactoryBean");
			String id = listenerElement.getAttribute("id");
			String listenerRef = listenerElement.getAttribute("ref");
			String className = listenerElement.getAttribute("class");
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
						"Both 'ref' and 'class' specified; use 'class' with an optional 'id' or just 'ref' for <"
								+ listenerElement.getTagName() + "> element with attributes: " + attributes,
						listenerElement);
			}

			if (hasText(listenerRef)) {
				listenerBuilder.addPropertyReference("delegate", listenerRef);
			}
			else if (hasText(className)) {
				RootBeanDefinition beanDef = new RootBeanDefinition(className, null, null);
				listenerBuilder.addPropertyValue("delegate", beanDef);
			}
			else {
				parserContext.getReaderContext().error(
						"Neither 'ref' or 'class' specified for <" + listenerElement.getTagName() + "> element",
						listenerElement);
			}

			ManagedMap metaDataMap = new ManagedMap();
			String beforeMethod = listenerElement.getAttribute("before-method");
			if (StringUtils.hasText(beforeMethod)) {
				metaDataMap.put("beforeMethod", beforeMethod);
			}

			String afterMethod = listenerElement.getAttribute("after-method");
			if (StringUtils.hasText(beforeMethod)) {
				metaDataMap.put("afterMethod", afterMethod);
			}
			listenerBuilder.addPropertyValue("metaDataMap", metaDataMap);
			AbstractBeanDefinition beanDef = listenerBuilder.getBeanDefinition();
			listeners.add(beanDef);
		}

		ManagedList managedList = new ManagedList();
		managedList.addAll(listeners);

		return managedList;
	}

}
