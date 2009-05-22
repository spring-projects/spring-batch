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

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class CoreNamespaceBeanDefinitionUtils {

	/**
	 * @param name the name of a bean definition in the bean factory
	 * @param beanFactory a {@link BeanFactory}
	 * @return TRUE if the bean represents an {@link AbstractStep} (or
	 * {@link StepParserStepFactoryBean}).
	 */
	public static boolean isAbstractStep(String name, ConfigurableListableBeanFactory beanFactory) {
		if (beanFactory.isFactoryBean(name)) {
			return beanFactory.isTypeMatch(BeanFactory.FACTORY_BEAN_PREFIX + name, StepParserStepFactoryBean.class);
		}
		return beanFactory.isTypeMatch(name, AbstractStep.class);
	}

	/**
	 * @param name a bean definition name
	 * @param propertyName the name of the property
	 * @param beanFactory a {@link BeanFactory}
	 * @return The {@link PropertyValue} for the {@link JobRepository} of the
	 * bean. Search parent hierarchy if necessary. Return null if none is found.
	 */
	public static PropertyValue getPropertyValue(String name, String propertyName,
			ConfigurableListableBeanFactory beanFactory) {
		PropertyValues jobDefPvs = beanFactory.getMergedBeanDefinition(name).getPropertyValues();
		return jobDefPvs.getPropertyValue(propertyName);
	}

}
