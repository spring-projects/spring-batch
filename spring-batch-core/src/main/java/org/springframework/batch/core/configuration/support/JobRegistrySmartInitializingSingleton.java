/*
 * Copyright 2024-2025 the original author or authors.
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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NullUnmarked;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.Assert;

/**
 * A {@link SmartInitializingSingleton} that registers {@link Job} beans with a
 * {@link JobRegistry}. Include a bean of this type along with your job configuration and
 * use the same {@link JobRegistry} as a {@link JobLocator} when you need to locate a
 * {@link Job} to launch.
 *
 * @author Henning Pöttker
 * @since 5.1.1
 * @deprecated since 6.0 with no replacement. Register a {@link MapJobRegistry} as a bean,
 * and it will automatically register all {@link Job} beans in the application context.
 */
@NullUnmarked
@Deprecated(since = "6.0", forRemoval = true)
public class JobRegistrySmartInitializingSingleton
		implements SmartInitializingSingleton, BeanFactoryAware, InitializingBean, DisposableBean {

	private static final Log logger = LogFactory.getLog(JobRegistrySmartInitializingSingleton.class);

	// It doesn't make sense for this to have a default value...
	private JobRegistry jobRegistry = null;

	private final Collection<String> jobNames = new HashSet<>();

	private String groupName = null;

	private ListableBeanFactory beanFactory;

	/**
	 * Default constructor.
	 */
	public JobRegistrySmartInitializingSingleton() {
	}

	/**
	 * Convenience constructor for setting the {@link JobRegistry}.
	 * @param jobRegistry the {@link JobRegistry} to register the {@link Job}s with
	 */
	public JobRegistrySmartInitializingSingleton(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

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
	 * @param jobRegistry the {@link JobRegistry} to register the {@link Job}s with
	 */
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof ListableBeanFactory listableBeanFactory) {
			this.beanFactory = listableBeanFactory;
		}
	}

	/**
	 * Make sure the registry is set before use.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(jobRegistry != null, "JobRegistry must not be null");
	}

	/**
	 * Unregister all the {@link Job} instances that were registered by this smart
	 * initializing singleton.
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

	@Override
	public void afterSingletonsInstantiated() {
		if (beanFactory == null) {
			return;
		}
		Map<String, Job> jobs = beanFactory.getBeansOfType(Job.class, false, false);
		for (var entry : jobs.entrySet()) {
			postProcessAfterInitialization(entry.getValue(), entry.getKey());
		}
	}

	private void postProcessAfterInitialization(Job job, String beanName) {
		try {
			String groupName = this.groupName;
			if (beanFactory instanceof DefaultListableBeanFactory defaultListableBeanFactory
					&& beanFactory.containsBean(beanName)) {
				groupName = getGroupName(defaultListableBeanFactory.getBeanDefinition(beanName), job);
			}
			job = groupName == null ? job : new GroupAwareJob(groupName, job);
			String name = job.getName();
			if (logger.isDebugEnabled()) {
				logger.debug("Registering job: " + name);
			}
			jobRegistry.register(job);
			jobNames.add(name);
		}
		catch (DuplicateJobException e) {
			throw new FatalBeanException("Cannot register job configuration", e);
		}
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

}
