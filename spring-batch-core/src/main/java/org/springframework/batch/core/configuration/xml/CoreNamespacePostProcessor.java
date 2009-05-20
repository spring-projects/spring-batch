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
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

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
			MutablePropertyValues pvs = (MutablePropertyValues) bd.getPropertyValues();
			if (pvs.contains(JOB_FACTORY_PROPERTY_NAME)) {
				if (isAbstractStep(bd, beanFactory)) {
					String jobName = (String) pvs.getPropertyValue(JOB_FACTORY_PROPERTY_NAME).getValue();
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
				pvs.removePropertyValue(JOB_FACTORY_PROPERTY_NAME);
			}
		}
	}

	/**
	 * @param bd
	 * @param beanFactory
	 * @return TRUE if the bean represents an AbstractStep (or
	 *         StepParserStepFactoryBean).
	 */
	private boolean isAbstractStep(BeanDefinition bd, ConfigurableListableBeanFactory beanFactory) {
		Class<?> stepClass = getClass(bd, beanFactory);
		return StepParserStepFactoryBean.class.isAssignableFrom(stepClass)
				|| AbstractStep.class.isAssignableFrom(stepClass);
	}

	/**
	 * @param bd
	 * @param beanFactory
	 * @return The class of the bean. Search parent hierarchy if necessary.
	 *         Return null if none is found.
	 */
	private Class<?> getClass(BeanDefinition bd, ConfigurableListableBeanFactory beanFactory) {
		// Get the declared class of the bean
		String className = bd.getBeanClassName();
		if (StringUtils.hasText(className)) {
			try {
				return Class.forName(className);
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
	 * @param jobName
	 * @param beanFactory
	 * @return The {@link PropertyValue} for the {@link JobRepository} of the
	 *         bean. Search parent hierarchy if necessary. Return null if none
	 *         is found.
	 */
	private PropertyValue getJobRepository(String jobName, ConfigurableListableBeanFactory beanFactory) {
		BeanDefinition jobDef = beanFactory.getBeanDefinition(jobName);
		PropertyValues jobDefPvs = jobDef.getPropertyValues();
		if (jobDefPvs.contains(JOB_REPOSITORY_PROPERTY_NAME)) {
			// return the job repository property
			return jobDefPvs.getPropertyValue(JOB_REPOSITORY_PROPERTY_NAME);
		}
		else {
			// Search the parent until you find it
			String parentName = jobDef.getParentName();
			if (StringUtils.hasText(parentName)) {
				return getJobRepository(parentName, beanFactory);
			}
			else {
				return null;
			}
		}
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
