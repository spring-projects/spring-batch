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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class CoreNamespaceBeanDefinitionUtils {

	/**
	 * @param bd a {@link BeanDefinition}
	 * @param beanFactory a {@link BeanFactory}
	 * @return TRUE if the bean represents an {@link AbstractStep} (or
	 *         {@link StepParserStepFactoryBean}).
	 */
	public static boolean isAbstractStep(BeanDefinition bd, ConfigurableListableBeanFactory beanFactory) {
		return isBeanClassAssignable(bd, new Class<?>[] { StepParserStepFactoryBean.class, AbstractStep.class },
				beanFactory);
	}

	/**
	 * @param bd a {@link BeanDefinition}
	 * @param types an array of {@link Class} objects.
	 * @param beanFactory a {@link BeanFactory}
	 * @return TRUE if the given {@link BeanDefinition}'s bean class is one of
	 *         the given types (or a subtype thereof).
	 */
	public static boolean isBeanClassAssignable(BeanDefinition bd, Class<?>[] types,
			ConfigurableListableBeanFactory beanFactory) {
		Class<?> stepClass = getClass(bd, beanFactory);
		for (Class<?> type : types) {
			if (ClassUtils.isAssignable(type, stepClass)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param bd a {@link BeanDefinition}
	 * @param beanFactory a {@link BeanFactory}
	 * @return The class of the bean. Search parent hierarchy if necessary.
	 *         Return null if none is found.
	 */
	public static Class<?> getClass(BeanDefinition bd, ConfigurableListableBeanFactory beanFactory) {
		// Get the declared class of the bean
		String className = bd.getBeanClassName();
		if (StringUtils.hasText(className)) {
			try {
				return ClassUtils.forName(className);
			}
			catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			// Search the parent until you find it
			String parentName = bd.getParentName();
			if (StringUtils.hasText(parentName)) {
				return getClass(beanFactory.getBeanDefinition(parentName), beanFactory);
			}
			else {
				return null;
			}
		}
	}

	/**
	 * @param bd a {@link BeanDefinition}
	 * @param propertyName the name of the property
	 * @param beanFactory a {@link BeanFactory}
	 * @return The {@link PropertyValue} for the {@link JobRepository} of the
	 *         bean. Search parent hierarchy if necessary. Return null if none
	 *         is found.
	 */
	public static PropertyValue getPropertyValue(BeanDefinition bd, String propertyName,
			ConfigurableListableBeanFactory beanFactory) {
		PropertyValues jobDefPvs = bd.getPropertyValues();
		if (jobDefPvs.contains(propertyName)) {
			// return the property
			return jobDefPvs.getPropertyValue(propertyName);
		}
		else {
			// Search the parent until you find it
			String parentName = bd.getParentName();
			if (StringUtils.hasText(parentName)) {
				return getPropertyValue(beanFactory.getBeanDefinition(parentName), propertyName, beanFactory);
			}
			else {
				return null;
			}
		}
	}
}
