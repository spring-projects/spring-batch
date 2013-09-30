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

import java.util.List;

import org.springframework.batch.core.jsr.configuration.support.BatchArtifact;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parses the various listeners defined in JSR-352.  Current state assumes
 * the ref attributes point to implementations of Spring Batch interfaces
 * and not JSR interfaces
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 */
public class ListenerParser {
	private static final String REF_ATTRIBUTE = "ref";
	private static final String LISTENER_ELEMENT = "listener";
	private static final String LISTENERS_ELEMENT = "listeners";
	@SuppressWarnings("rawtypes")
	private Class listenerType;
	private String propertyKey;

	@SuppressWarnings("rawtypes")
	public ListenerParser(Class listenerType, String propertyKey) {
		this.propertyKey = propertyKey;
		this.listenerType = listenerType;
	}

	public void parseListeners(Element element, ParserContext parserContext, AbstractBeanDefinition bd, String stepName) {
		ManagedList<AbstractBeanDefinition> listeners = parseListeners(element, parserContext, stepName);

		if(listeners.size() > 0) {
			bd.getPropertyValues().add(propertyKey, listeners);
		}
	}

	public void parseListeners(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		ManagedList<AbstractBeanDefinition> listeners = parseListeners(element, parserContext, "");

		if(listeners.size() > 0) {
			builder.addPropertyValue(propertyKey, listeners);
		}
	}

	private ManagedList<AbstractBeanDefinition> parseListeners(Element element, ParserContext parserContext, String stepName) {
		List<Element> listenersElements = DomUtils.getChildElementsByTagName(element, LISTENERS_ELEMENT);

		ManagedList<AbstractBeanDefinition> listeners = new ManagedList<AbstractBeanDefinition>();

		if (listenersElements.size() == 1) {
			Element listenersElement = listenersElements.get(0);
			CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(listenersElement.getTagName(),
					parserContext.extractSource(element));
			parserContext.pushContainingComponent(compositeDef);
			listeners.setMergeEnabled(false);
			List<Element> listenerElements = DomUtils.getChildElementsByTagName(listenersElement, LISTENER_ELEMENT);
			for (Element listenerElement : listenerElements) {
				String beanName = listenerElement.getAttribute(REF_ATTRIBUTE);

				BeanDefinitionBuilder bd = BeanDefinitionBuilder.genericBeanDefinition(listenerType);
				bd.addPropertyValue("delegate", new RuntimeBeanReference(beanName));

				listeners.add(bd.getBeanDefinition());

				new PropertyParser(beanName, parserContext, getBatchArtifactType(stepName), stepName).parseProperties(listenerElement);
			}
			parserContext.popAndRegisterContainingComponent();
		}
		else if (listenersElements.size() > 1) {
			parserContext.getReaderContext().error(
					"The '<listeners/>' element may not appear more than once in a single " + element.getLocalName(), element);
		}

		return listeners;
	}

	private BatchArtifact.BatchArtifactType getBatchArtifactType(String stepName) {
		return (stepName != null && !"".equals(stepName)) ? BatchArtifact.BatchArtifactType.STEP_ARTIFACT
			: BatchArtifact.BatchArtifactType.ARTIFACT;
	}
}
