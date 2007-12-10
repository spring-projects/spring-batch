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
package org.springframework.batch.execution.configuration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.springframework.batch.core.configuration.DuplicateJobConfigurationException;
import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.JobConfigurationLocator;
import org.springframework.batch.core.configuration.JobConfigurationRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.Assert;

/**
 * A {@link BeanPostProcessor} that registers {@link JobConfiguration} beans
 * with a {@link JobConfigurationRegistry}. Include a bean of this type along
 * with your job configuration, and use the same
 * {@link JobConfigurationRegistry} as a {@link JobConfigurationLocator} when
 * you need to locate a {@link JobConfigurationLocator} to launch.
 * 
 * @author Dave Syer
 * 
 */
public class JobConfigurationRegistryBeanPostProcessor implements BeanPostProcessor, InitializingBean, DisposableBean {

	// It doesn't make sense for this to have a default value...
	private JobConfigurationRegistry jobConfigurationRegistry = null;

	private Collection jobConfigurations = new HashSet();

	/**
	 * Injection setter for {@link JobConfigurationRegistry}.
	 * 
	 * @param jobConfigurationRegistry the jobConfigurationRegistry to set
	 */
	public void setJobConfigurationRegistry(JobConfigurationRegistry jobConfigurationRegistry) {
		this.jobConfigurationRegistry = jobConfigurationRegistry;
	}

	/**
	 * Make sure the registry is set before use.
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jobConfigurationRegistry, "JobConfigurationRegistry must not be null");
	}

	/**
	 * De-register all the {@link JobConfiguration} instances that were
	 * regsistered by this post processor.
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		for (Iterator iter = jobConfigurations.iterator(); iter.hasNext();) {
			JobConfiguration jobConfiguration = (JobConfiguration) iter.next();
			jobConfigurationRegistry.unregister(jobConfiguration);
		}
		jobConfigurations.clear();
	}

	/**
	 * If the bean is an instance of {@link JobConfiguration} then register it.
	 * @throws FatalBeanException if there is a
	 * {@link DuplicateJobConfigurationException}.
	 * 
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object,
	 * java.lang.String)
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof JobConfiguration) {
			JobConfiguration jobConfiguration = (JobConfiguration) bean;
			try {
				jobConfigurationRegistry.register(jobConfiguration);
				jobConfigurations.add(jobConfiguration);
			}
			catch (DuplicateJobConfigurationException e) {
				throw new FatalBeanException("Cannot register job configuration", e);
			}
		}
		return bean;
	}

	/**
	 * Do nothing.
	 * 
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object,
	 * java.lang.String)
	 */
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
