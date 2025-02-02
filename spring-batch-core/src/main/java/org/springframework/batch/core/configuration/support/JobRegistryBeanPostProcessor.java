/*
 * Copyright 2006-2024 the original author or authors.
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
package org.springframework.batch.core.configuration.support;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.Assert;

/**
 * A {@link BeanPostProcessor} that registers {@link Job} beans with a
 * {@link JobRegistry}. Include a bean of this type along with your job configuration and
 * use the same {@link JobRegistry} as a {@link JobLocator} when you need to locate a
 * {@link Job} to launch.
 * <p>
 * An alternative to this class is {@link JobRegistrySmartInitializingSingleton}, which is
 * recommended in cases where this class may cause early bean initializations. You must
 * include at most one of either of them as a bean.
 *
 * @deprecated since 5.2 in favor of {@link JobRegistrySmartInitializingSingleton}.
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
@Deprecated(since = "5.2")
public class JobRegistryBeanPostProcessor
		implements BeanPostProcessor, BeanFactoryAware, InitializingBean, DisposableBean {

	private static final Log logger = LogFactory.getLog(JobRegistryBeanPostProcessor.class);

	// It doesn't make sense for this to have a default value...
	private JobRegistry jobRegistry = null;

	private final Collection<String> jobNames = new HashSet<>();

	private String groupName = null;

	private DefaultListableBeanFactory beanFactory;

	/**
	 * The group name for jobs registered by this component. Optional (defaults to null,
	 * which means that jobs are registered with their bean names). Useful where there is
	 * a hierarchy of application contexts all contributing to the same
	 * {@link JobRegistry}: child contexts can then define an instance with a unique group
	 * name to avoid clashes between job names.
	 * @param groupName the groupName to set
	 */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/**
	 * Injection setter for {@link JobRegistry}.
	 * @param jobRegistry the jobConfigurationRegistry to set
	 */
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof DefaultListableBeanFactory) {
			this.beanFactory = (DefaultListableBeanFactory) beanFactory;
		}
	}

	/**
	 * Make sure the registry is set before use.
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(jobRegistry != null, "JobRegistry must not be null");
	}

	/**
	 * Unregister all the {@link Job} instances that were registered by this post
	 * processor.
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	@Override
	public void destroy() throws Exception {
		for (String name : jobNames) {
			if (logger.isDebugEnabled()) {
				logger.debug("Unregistering job: " + name);
			}
			jobRegistry.unregister(name);
		}
		jobNames.clear();
	}

	/**
	 * If the bean is an instance of {@link Job}, then register it.
	 * @throws FatalBeanException if there is a {@link DuplicateJobException}.
	 *
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object,
	 * java.lang.String)
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Job job) {
			try {
				String groupName = this.groupName;
				if (beanFactory != null && beanFactory.containsBean(beanName)) {
					groupName = getGroupName(beanFactory.getBeanDefinition(beanName), job);
				}
				job = groupName == null ? job : new GroupAwareJob(groupName, job);
				ReferenceJobFactory jobFactory = new ReferenceJobFactory(job);
				String name = jobFactory.getJobName();
				if (logger.isDebugEnabled()) {
					logger.debug("Registering job: " + name);
				}
				jobRegistry.register(jobFactory);
				jobNames.add(name);
			}
			catch (DuplicateJobException e) {
				throw new FatalBeanException("Cannot register job configuration", e);
			}
			return job;
		}
		return bean;
	}

	/**
	 * Determine a group name for the job to be registered. The default implementation
	 * returns the {@link #setGroupName(String) groupName} configured. Provides an
	 * extension point for specialised subclasses.
	 * @param beanDefinition the bean definition for the job
	 * @param job the job
	 * @return a group name for the job (or null if not needed)
	 */
	protected String getGroupName(BeanDefinition beanDefinition, Job job) {
		return groupName;
	}

	/**
	 * Do nothing.
	 *
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object,
	 * java.lang.String)
	 */
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
