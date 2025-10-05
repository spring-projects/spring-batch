/*
 * Copyright 2006-2025 the original author or authors.
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NullUnmarked;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

/**
 * Simple, thread-safe, map-based implementation of {@link JobRegistry}. This registry is
 * a {@link SmartInitializingSingleton} that is automatically populated with all
 * {@link Job} beans in the {@link ApplicationContext}.
 *
 * @author Dave Syer
 * @author Robert Fischer
 * @author Mahmoud Ben Hassine
 */
@NullUnmarked // FIXME how to fix nullability checks for the applicationContext field?
public class MapJobRegistry implements JobRegistry, SmartInitializingSingleton, ApplicationContextAware {

	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The map holding the registered jobs.
	 */
	private final ConcurrentMap<String, Job> map = new ConcurrentHashMap<>();

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterSingletonsInstantiated() {
		Map<String, Job> jobBeans = this.applicationContext.getBeansOfType(Job.class);
		this.map.putAll(jobBeans);
	}

	@Override
	public void register(Job job) throws DuplicateJobException {
		Assert.notNull(job, "job must not be null");
		String jobName = job.getName();
		Assert.notNull(jobName, "Job name must not be null");
		Job previousValue = this.map.putIfAbsent(jobName, job);
		if (previousValue != null) {
			throw new DuplicateJobException("A job with this name [" + jobName + "] was already registered");
		}
	}

	@Override
	public void unregister(String name) {
		Assert.notNull(name, "Job name must not be null");
		this.map.remove(name);
	}

	@Override
	public Job getJob(String name) {
		return this.map.get(name);
	}

	/**
	 * Provides an unmodifiable view of job names.
	 */
	@Override
	public Set<String> getJobNames() {
		return Collections.unmodifiableSet(this.map.keySet());
	}

}
