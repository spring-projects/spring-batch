/*
 * Copyright 2006-2020 the original author or authors.
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
package org.springframework.batch.core.job.builder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.listener.JobListenerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.support.ReflectionUtils;

/**
 * A base class and utility for other job builders providing access to common properties like job repository.
 * 
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * 
 * @since 2.2
 */
public abstract class JobBuilderHelper<B extends JobBuilderHelper<B>> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final CommonJobProperties properties;

	public JobBuilderHelper(String name) {
		this.properties = new CommonJobProperties();
		properties.name = name;
	}

	/**
	 * Create a new builder initialized with any properties in the parent. The parent is copied, so it can be re-used.
	 * 
	 * @param parent a parent helper containing common step properties
	 */
	protected JobBuilderHelper(JobBuilderHelper<?> parent) {
		this.properties = new CommonJobProperties(parent.properties);
	}

	/**
	 * Add a job parameters validator.
	 * 
	 * @param jobParametersValidator a job parameters validator
	 * @return this to enable fluent chaining
	 */
	public B validator(JobParametersValidator jobParametersValidator) {
		properties.jobParametersValidator = jobParametersValidator;
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	/**
	 * Add a job parameters incrementer.
	 * 
	 * @param jobParametersIncrementer a job parameters incrementer
	 * @return this to enable fluent chaining
	 */
	public B incrementer(JobParametersIncrementer jobParametersIncrementer) {
		properties.jobParametersIncrementer = jobParametersIncrementer;
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	/**
	 * Sets the job repository for the job.
	 * 
	 * @param jobRepository the job repository (mandatory)
	 * @return this to enable fluent chaining
	 */
	public B repository(JobRepository jobRepository) {
		properties.jobRepository = jobRepository;
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	/**
	 * Registers objects using the annotation based listener configuration.
	 *
	 * @param listener the object that has a method configured with listener annotation
	 * @return this for fluent chaining
	 */
	public B listener(Object listener) {
		Set<Method> jobExecutionListenerMethods = new HashSet<>();
		jobExecutionListenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), BeforeJob.class));
		jobExecutionListenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), AfterJob.class));

		if(jobExecutionListenerMethods.size() > 0) {
			JobListenerFactoryBean factory = new JobListenerFactoryBean();
			factory.setDelegate(listener);
			properties.addJobExecutionListener((JobExecutionListener) factory.getObject());
		}

		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	/**
	 * Register a job execution listener.
	 * 
	 * @param listener a job execution listener
	 * @return this to enable fluent chaining
	 */
	public B listener(JobExecutionListener listener) {
		properties.addJobExecutionListener(listener);
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	/**
	 * Set a flag to prevent restart an execution of this job even if it has failed.
	 * 
	 * @return this to enable fluent chaining
	 */
	public B preventRestart() {
		properties.restartable = false;
		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	protected String getName() {
		return properties.name;
	}

	protected JobRepository getJobRepository() {
		return properties.jobRepository;
	}

	protected boolean isRestartable() {
		return properties.restartable;
	}

	protected void enhance(Job target) {

		if (target instanceof AbstractJob) {

			AbstractJob job = (AbstractJob) target;
			job.setJobRepository(properties.getJobRepository());

			JobParametersIncrementer jobParametersIncrementer = properties.getJobParametersIncrementer();
			if (jobParametersIncrementer != null) {
				job.setJobParametersIncrementer(jobParametersIncrementer);
			}
			JobParametersValidator jobParametersValidator = properties.getJobParametersValidator();
			if (jobParametersValidator != null) {
				job.setJobParametersValidator(jobParametersValidator);
			}

			Boolean restartable = properties.getRestartable();
			if (restartable != null) {
				job.setRestartable(restartable);
			}

			List<JobExecutionListener> listeners = properties.getJobExecutionListeners();
			if (!listeners.isEmpty()) {
				job.setJobExecutionListeners(listeners.toArray(new JobExecutionListener[0]));
			}

		}

	}

	public static class CommonJobProperties {

		private Set<JobExecutionListener> jobExecutionListeners = new LinkedHashSet<>();

		private boolean restartable = true;

		private JobRepository jobRepository;

		private JobParametersIncrementer jobParametersIncrementer;

		private JobParametersValidator jobParametersValidator;

		public CommonJobProperties() {
		}

		public CommonJobProperties(CommonJobProperties properties) {
			this.name = properties.name;
			this.restartable = properties.restartable;
			this.jobRepository = properties.jobRepository;
			this.jobExecutionListeners = new LinkedHashSet<>(properties.jobExecutionListeners);
			this.jobParametersIncrementer = properties.jobParametersIncrementer;
			this.jobParametersValidator = properties.jobParametersValidator;
		}

		public JobParametersIncrementer getJobParametersIncrementer() {
			return jobParametersIncrementer;
		}

		public void setJobParametersIncrementer(JobParametersIncrementer jobParametersIncrementer) {
			this.jobParametersIncrementer = jobParametersIncrementer;
		}

		public JobParametersValidator getJobParametersValidator() {
			return jobParametersValidator;
		}

		public void setJobParametersValidator(JobParametersValidator jobParametersValidator) {
			this.jobParametersValidator = jobParametersValidator;
		}

		public JobRepository getJobRepository() {
			return jobRepository;
		}

		public void setJobRepository(JobRepository jobRepository) {
			this.jobRepository = jobRepository;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<JobExecutionListener> getJobExecutionListeners() {
			return new ArrayList<>(jobExecutionListeners);
		}

		public void addStepExecutionListeners(List<JobExecutionListener> jobExecutionListeners) {
			this.jobExecutionListeners.addAll(jobExecutionListeners);
		}

		public void addJobExecutionListener(JobExecutionListener jobExecutionListener) {
			this.jobExecutionListeners.add(jobExecutionListener);
		}

		public boolean getRestartable() {
			return restartable;
		}

		public void setRestartable(boolean restartable) {
			this.restartable = restartable;
		}

		private String name;

	}

}
