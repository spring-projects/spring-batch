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
package org.springframework.batch.core.configuration.support;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.Assert;

/**
 * A {@link BeanPostProcessor} that registers {@link Job} beans
 * with a {@link JobRegistry}. Include a bean of this type along
 * with your job configuration, and use the same
 * {@link JobRegistry} as a {@link JobLocator} when
 * you need to locate a {@link JobLocator} to launch.
 * 
 * @author Dave Syer
 * 
 */
public class JobRegistryBeanPostProcessor implements BeanPostProcessor, InitializingBean, DisposableBean {

	// It doesn't make sense for this to have a default value...
	private JobRegistry jobRegistry = null;

	private Collection<String> jobNames = new HashSet<String>();

	/**
	 * Injection setter for {@link JobRegistry}.
	 * 
	 * @param jobRegistry the jobConfigurationRegistry to set
	 */
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	/**
	 * Make sure the registry is set before use.
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jobRegistry, "JobRegistry must not be null");
	}

	/**
	 * De-register all the {@link Job} instances that were
	 * regsistered by this post processor.
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		for (String name : jobNames) {
			jobRegistry.unregister(name);
		}
		jobNames.clear();
	}

	/**
	 * If the bean is an instance of {@link Job} then register it.
	 * @throws FatalBeanException if there is a
	 * {@link DuplicateJobException}.
	 * 
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object,
	 * java.lang.String)
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Job) {
			Job job = (Job) bean;
			try {
				jobRegistry.register(new ReferenceJobFactory(job));
				jobNames.add(job.getName());
			}
			catch (DuplicateJobException e) {
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
