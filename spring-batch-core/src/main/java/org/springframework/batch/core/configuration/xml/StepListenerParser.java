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

import java.util.List;

import org.springframework.batch.core.listener.AbstractListenerFactoryBean;
import org.springframework.batch.core.listener.ListenerMetaData;
import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.batch.core.listener.StepListenerMetaData;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for a step listener element. Builds a {@link StepListenerFactoryBean}
 * using attributes from the configuration.
 * 
 * @author Dan Garrette
 * @since 2.0
 * @see AbstractListenerParser
 */
public class StepListenerParser extends AbstractListenerParser {
	
	private static final String LISTENERS_ELE = "listeners";

	private static final String MERGE_ATTR = "merge";

	private final ListenerMetaData[] listenerMetaData;

	public StepListenerParser() {
		this(StepListenerMetaData.values());
	}

	public StepListenerParser(ListenerMetaData[] listenerMetaData) {
		this.listenerMetaData = listenerMetaData;
	}

	protected Class<? extends AbstractListenerFactoryBean> getBeanClass() {
		return StepListenerFactoryBean.class;
	}

	protected ListenerMetaData[] getMetaDataValues() {
		return listenerMetaData;
	}

	@SuppressWarnings("unchecked")
	public void handleListenersElement(Element stepElement, BeanDefinition beanDefinition,
			ParserContext parserContext) {
		MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
		List<Element> listenersElements = DomUtils.getChildElementsByTagName(stepElement, LISTENERS_ELE);
		if (listenersElements.size() == 1) {
			Element listenersElement = listenersElements.get(0);
			CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(listenersElement.getTagName(),
					parserContext.extractSource(stepElement));
			parserContext.pushContainingComponent(compositeDef);
			ManagedList listenerBeans = new ManagedList();
			if (propertyValues.contains("listeners")) {
				listenerBeans = (ManagedList) propertyValues.getPropertyValue("listeners").getValue();
			}
			listenerBeans.setMergeEnabled(listenersElement.hasAttribute(MERGE_ATTR)
					&& Boolean.valueOf(listenersElement.getAttribute(MERGE_ATTR)));
			List<Element> listenerElements = DomUtils.getChildElementsByTagName(listenersElement, "listener");
			if (listenerElements != null) {
				for (Element listenerElement : listenerElements) {
					listenerBeans.add(parse(listenerElement, parserContext));
				}
			}
			propertyValues.addPropertyValue("listeners", listenerBeans);
			parserContext.popAndRegisterContainingComponent();
		}
	}

}
