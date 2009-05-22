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

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Post-process jobs and steps defined using the batch namespace to inject
 * dependencies.
 * 
 * @author Dan Garrette
 * @since 2.0.1
 */
public class CoreNamespacePostProcessor implements BeanPostProcessor, BeanFactoryPostProcessor, ApplicationContextAware {

	private static final String DEFAULT_JOB_REPOSITORY_NAME = "jobRepository";

	private static final String DEFAULT_TRANSACTION_MANAGER_NAME = "transactionManager";

	private static final String JOB_FACTORY_PROPERTY_NAME = "jobParserJobFactoryBeanRef";

	private static final String JOB_REPOSITORY_PROPERTY_NAME = "jobRepository";

	private ApplicationContext applicationContext;

	/**
	 * Automatically inject job-repository from a job into its steps. Only
	 * inject if the step is an AbstractStep or StepParserStepFactoryBean.
	 * 
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
			if (bd.hasAttribute(JOB_FACTORY_PROPERTY_NAME)) {
				MutablePropertyValues pvs = (MutablePropertyValues) bd.getPropertyValues();
				if (CoreNamespaceBeanDefinitionUtils.isAbstractStep(beanName, beanFactory)) {
					String jobName = (String) bd.getAttribute(JOB_FACTORY_PROPERTY_NAME);
					PropertyValue jobRepository = getJobRepository(jobName, beanFactory);
					if (jobRepository != null) {
						// Set the job's JobRepository onto the step
						pvs.addPropertyValue(jobRepository);
					}
					else {
						// No JobRepository found, so inject the default
						RuntimeBeanReference jobRepositoryBeanRef = new RuntimeBeanReference(
								DEFAULT_JOB_REPOSITORY_NAME);
						pvs.addPropertyValue(JOB_REPOSITORY_PROPERTY_NAME, jobRepositoryBeanRef);
					}
				}
			}
		}
	}

	/**
	 * @param jobName
	 * @param beanFactory
	 * @return The {@link PropertyValue} for the {@link JobRepository} of the
	 *         bean. Search parent hierarchy if necessary. Return null if none
	 *         is found.
	 */
	private PropertyValue getJobRepository(String jobName, ConfigurableListableBeanFactory beanFactory) {
		return CoreNamespaceBeanDefinitionUtils.getPropertyValue(jobName, JOB_REPOSITORY_PROPERTY_NAME, beanFactory);
	}

	/**
	 * Inject defaults into factory beans.
	 * <ul>
	 * <li>Inject "jobRepository" into any {@link JobParserJobFactoryBean}
	 * without a jobRepository.
	 * <li>Inject "transactionManager" into any
	 * {@link StepParserStepFactoryBean} without a transactionManager.
	 * </ul>
	 * 
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object,
	 *      java.lang.String)
	 */
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof JobParserJobFactoryBean) {
			JobParserJobFactoryBean fb = (JobParserJobFactoryBean) bean;
			JobRepository jobRepository = fb.getJobRepository();
			if (jobRepository == null) {
				fb.setJobRepository((JobRepository) applicationContext.getBean(DEFAULT_JOB_REPOSITORY_NAME));
			}
		}
		else if (bean instanceof StepParserStepFactoryBean) {
			StepParserStepFactoryBean<?, ?> fb = (StepParserStepFactoryBean<?, ?>) bean;
			JobRepository jobRepository = fb.getJobRepository();
			if (jobRepository == null) {
				fb.setJobRepository((JobRepository) applicationContext.getBean(DEFAULT_JOB_REPOSITORY_NAME));
			}
			PlatformTransactionManager transactionManager = fb.getTransactionManager();
			if (transactionManager == null) {
				fb.setTransactionManager((PlatformTransactionManager) applicationContext
						.getBean(DEFAULT_TRANSACTION_MANAGER_NAME));
			}
		}
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
}
