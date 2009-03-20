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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * utility methods used in parsing of the batch core namespace
 * 
 * @author Thomas Risberg
 */
public class CoreNamespaceUtils {
	
	public static final String STEP_SCOPE_PROCESSOR_BEAN_NAME =
		"org.springframework.batch.core.scope.internalStepScope";

	public static final String STEP_SCOPE_PROCESSOR_CLASS_NAME =
		"org.springframework.batch.core.scope.StepScope";


	protected static void checkForStepScope(ParserContext parserContext, Object source) {
		
		boolean foundStepScope = false;
		String[] beanNames = parserContext.getRegistry().getBeanDefinitionNames();
		for (String beanName : beanNames) {
			BeanDefinition bd = parserContext.getRegistry().getBeanDefinition(beanName);
			if (STEP_SCOPE_PROCESSOR_CLASS_NAME.equals(bd.getBeanClassName())) {
				foundStepScope = true;
				break;
			}
		}
		if (!foundStepScope) {
			BeanDefinitionBuilder stepScopeBuilder = 
				BeanDefinitionBuilder.genericBeanDefinition(STEP_SCOPE_PROCESSOR_CLASS_NAME);
			AbstractBeanDefinition abd = stepScopeBuilder.getBeanDefinition();
			abd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			abd.setSource(source);
			parserContext.getRegistry().registerBeanDefinition(STEP_SCOPE_PROCESSOR_BEAN_NAME, abd);
		}
	}

	/**
	 * Should this element be treated as incomplete? If it has a parent or is
	 * abstract, then it may not have all properties.
	 * 
	 * @param element
	 * @return TRUE if the element is abstract or has a parent
	 */
	public static boolean isUnderspecified(Element element) {
		return Boolean.valueOf(element.getAttribute("abstract")) || StringUtils.hasText(element.getAttribute("parent"));
	}
	
}
