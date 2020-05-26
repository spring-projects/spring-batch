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
package org.springframework.batch.core.jsr.partition.support;

import org.springframework.batch.core.jsr.configuration.xml.StepFactoryBean;
import org.springframework.batch.core.jsr.partition.JsrPartitionHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;

import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionReducer;

/**
 * In order for property resolution to occur correctly within the scope of a JSR-352
 * batch job, initialization of job level artifacts must occur on the same thread that
 * the job is executing.  To allow this to occur, {@link PartitionMapper},
 * {@link PartitionReducer}, and {@link PartitionAnalyzer} are all configured to
 * lazy initialization (equivalent to lazy-init="true").
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrBeanScopeBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	private JobLevelBeanLazyInitializer initializer;

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	@Override
	public void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (initializer == null) {
			this.initializer = new JobLevelBeanLazyInitializer(beanFactory);
		}

		String[] beanNames = beanFactory.getBeanDefinitionNames();

		for (String curName : beanNames) {
			initializer.visitBeanDefinition(beanFactory.getBeanDefinition(curName));
		}
	}

	/**
	 * Looks for beans that may have dependencies that need to be lazily initialized and
	 * configures the corresponding {@link BeanDefinition} accordingly.
	 *
	 * @author Michael Minella
	 * @since 3.0
	 */
	public static class JobLevelBeanLazyInitializer {

		private ConfigurableListableBeanFactory beanFactory;

		public JobLevelBeanLazyInitializer(ConfigurableListableBeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		public void visitBeanDefinition(BeanDefinition beanDefinition) {
			String beanClassName = beanDefinition.getBeanClassName();

			if(StepFactoryBean.class.getName().equals(beanClassName)) {
				PropertyValue [] values = beanDefinition.getPropertyValues().getPropertyValues();
				for (PropertyValue propertyValue : values) {
					if(propertyValue.getName().equalsIgnoreCase("partitionReducer")) {
						RuntimeBeanReference ref = (RuntimeBeanReference) propertyValue.getValue();
						beanFactory.getBeanDefinition(ref.getBeanName()).setLazyInit(true);
					}
				}
			}

			if(JsrPartitionHandler.class.getName().equals(beanClassName)) {
				PropertyValue [] values = beanDefinition.getPropertyValues().getPropertyValues();
				for (PropertyValue propertyValue : values) {
					String propertyName = propertyValue.getName();
					if(propertyName.equalsIgnoreCase("partitionMapper") || propertyName.equalsIgnoreCase("partitionAnalyzer")) {
						RuntimeBeanReference ref = (RuntimeBeanReference) propertyValue.getValue();
						beanFactory.getBeanDefinition(ref.getBeanName()).setLazyInit(true);
					}
				}
			}
		}
	}
}
