/*
 * Copyright 2006-2007 the original author or authors.
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

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class BeanDefinitionUtils {

	/**
	 * @param beanName a bean definition name
	 * @param propertyName the name of the property
	 * @param beanFactory a {@link BeanFactory}
	 * @return The {@link PropertyValue} for the property of the bean. Search
	 *         parent hierarchy if necessary. Return null if none is found.
	 */
	public static PropertyValue getPropertyValue(String beanName, String propertyName, ConfigurableListableBeanFactory beanFactory) {
		return beanFactory.getMergedBeanDefinition(beanName).getPropertyValues().getPropertyValue(propertyName);
	}

	/**
	 * @param beanName a bean definition name
	 * @param attributeName the name of the property
	 * @param beanFactory a {@link BeanFactory}
	 * @return The value for the attribute of the bean. Search parent hierarchy
	 *         if necessary. Return null if none is found.
	 */
	public static Object getAttribute(String beanName, String attributeName, ConfigurableListableBeanFactory beanFactory) {
		return beanFactory.getMergedBeanDefinition(beanName).getAttribute(attributeName);
	}
}
