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
package org.springframework.batch.core.jsr.configuration.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.PriorityOrdered;

/**
 * After the {@link BeanFactory} is created, this post processor will evaluate to see
 * if any of the beans referenced from a job definition (as defined by JSR-352) point
 * to class names instead of bean names.  If this is the case, a new {@link BeanDefinition}
 * is added with the name of the class as the bean name.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class ThreadLocalClassloaderBeanPostProcessor implements BeanFactoryPostProcessor, PriorityOrdered {
	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String[] beanNames = beanFactory.getBeanDefinitionNames();

		for (String curName : beanNames) {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(curName);
			PropertyValue[] values = beanDefinition.getPropertyValues().getPropertyValues();

			for (PropertyValue propertyValue : values) {
				Object value = propertyValue.getValue();

				if(value instanceof RuntimeBeanReference) {
					RuntimeBeanReference ref = (RuntimeBeanReference) value;
					if(!beanFactory.containsBean(ref.getBeanName())) {
						AbstractBeanDefinition newBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(ref.getBeanName()).getBeanDefinition();
						newBeanDefinition.setScope("step");
						((DefaultListableBeanFactory) beanFactory).registerBeanDefinition(ref.getBeanName(), newBeanDefinition);
					}
				}
			}
		}
	}

	/**
	 * Sets this {@link BeanFactoryPostProcessor} to the lowest precedence so that
	 * it is executed as late as possible in the chain of {@link BeanFactoryPostProcessor}s
	 */
	@Override
	public int getOrder() {
		return PriorityOrdered.LOWEST_PRECEDENCE;
	}
}
