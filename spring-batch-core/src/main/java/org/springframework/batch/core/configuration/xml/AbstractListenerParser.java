/*
 * Copyright 2009-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.listener.AbstractListenerFactoryBean;
import org.springframework.batch.core.listener.ListenerMetaData;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * @author Dan Garrette
 * @since 2.0
 * @see StepListenerParser
 * @see JobExecutionListenerParser
 */
public abstract class AbstractListenerParser {

	private static final String ID_ATTR = "id";

	private static final String REF_ATTR = "ref";

	private static final String BEAN_ELE = "bean";

	private static final String REF_ELE = "ref";

	public AbstractBeanDefinition parse(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(getBeanClass());
		doParse(element, parserContext, builder);
		return builder.getBeanDefinition();
	}

	public void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addPropertyValue("delegate", parseListenerElement(element, parserContext, builder.getRawBeanDefinition()));

		ManagedMap<String, String> metaDataMap = new ManagedMap<>();
		for (String metaDataPropertyName : getMethodNameAttributes()) {
			String listenerMethod = element.getAttribute(metaDataPropertyName);
			if (StringUtils.hasText(listenerMethod)) {
				metaDataMap.put(metaDataPropertyName, listenerMethod);
			}
		}
		builder.addPropertyValue("metaDataMap", metaDataMap);
	}

	public static BeanMetadataElement parseListenerElement(Element element, ParserContext parserContext, BeanDefinition enclosing) {
		String listenerRef = element.getAttribute(REF_ATTR);
		List<Element> beanElements = DomUtils.getChildElementsByTagName(element, BEAN_ELE);
		List<Element> refElements = DomUtils.getChildElementsByTagName(element, REF_ELE);

		verifyListenerAttributesAndSubelements(listenerRef, beanElements, refElements, element, parserContext);

		if (StringUtils.hasText(listenerRef)) {
			return new RuntimeBeanReference(listenerRef);
		}
		else if (beanElements.size() == 1) {
			Element beanElement = beanElements.get(0);
			BeanDefinitionHolder beanDefinitionHolder = parserContext.getDelegate().parseBeanDefinitionElement(
					beanElement, enclosing);
			parserContext.getDelegate().decorateBeanDefinitionIfRequired(beanElement, beanDefinitionHolder);
			return beanDefinitionHolder;
		}
		else {
			return (BeanMetadataElement) parserContext.getDelegate().parsePropertySubElement(refElements.get(0), null);
		}
	}

	private static void verifyListenerAttributesAndSubelements(String listenerRef, List<Element> beanElements,
			List<Element> refElements, Element element, ParserContext parserContext) {
		int total = (StringUtils.hasText(listenerRef) ? 1 : 0) + beanElements.size() + refElements.size();
		if (total != 1) {
			StringBuilder found = new StringBuilder();
			if (total == 0) {
				found.append("None");
			}
			else {
				if (StringUtils.hasText(listenerRef)) {
					found.append("'" + REF_ATTR + "' attribute, ");
				}
				if (beanElements.size() == 1) {
					found.append("<" + BEAN_ELE + "/> element, ");
				}
				else if (beanElements.size() > 1) {
					found.append(beanElements.size() + " <" + BEAN_ELE + "/> elements, ");
				}
				if (refElements.size() == 1) {
					found.append("<" + REF_ELE + "/> element, ");
				}
				else if (refElements.size() > 1) {
					found.append(refElements.size() + " <" + REF_ELE + "/> elements, ");
				}
				found.delete(found.length() - 2, found.length());
			}

			String id = element.getAttribute(ID_ATTR);
			parserContext.getReaderContext().error(
					"The <" + element.getTagName() + (StringUtils.hasText(id) ? " id=\"" + id + "\"" : "")
							+ "/> element must have exactly one of: '" + REF_ATTR + "' attribute, <" + BEAN_ELE
							+ "/> attribute, or <" + REF_ELE + "/> element.  Found: " + found + ".", element);
		}
	}

	private List<String> getMethodNameAttributes() {
		List<String> methodNameAttributes = new ArrayList<>();
		for (ListenerMetaData metaData : getMetaDataValues()) {
			methodNameAttributes.add(metaData.getPropertyName());
		}
		return methodNameAttributes;
	}

	/**
	 * @return The {@link Class} for the implementation of {@link AbstractListenerFactoryBean}.
	 */
	protected abstract Class<? extends AbstractListenerFactoryBean<?>> getBeanClass();

	/**
	 * @return The array of {@link ListenerMetaData}.
	 */
	protected abstract ListenerMetaData[] getMetaDataValues();

}
