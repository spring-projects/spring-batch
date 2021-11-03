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

	/**
	 * {@link Log} used by the builder helper.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	private final CommonJobProperties properties;

	/**
	 * Constructor for {@link JobBuilderHelper} that initializes the name property.
	 * @param name name to be used by the builder helper.
	 */
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

	/**
	 * @return name used by the ubilder.
	 */
	protected String getName() {
		return properties.name;
	}

	/**
	 * @return {@link JobRepository} used by the builder helper.
	 */
	protected JobRepository getJobRepository() {
		return properties.jobRepository;
	}

	/**
	 * @return restartable flag used by the builder helper.
	 */
	protected boolean isRestartable() {
		return properties.restartable;
	}

	/**
	 * Sets the {@link JobParametersIncrementer}, {@link JobParametersIncrementer}, {@link JobExecutionListener}s and restartable flag for the job.
	 * @param target {@link Job} to be updated.
	 */
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

	/**
	 * Properties used in creating {@link Job}s.
	 */
	public static class CommonJobProperties {

		private Set<JobExecutionListener> jobExecutionListeners = new LinkedHashSet<>();

		private boolean restartable = true;

		private JobRepository jobRepository;

		private JobParametersIncrementer jobParametersIncrementer;

		private JobParametersValidator jobParametersValidator;

		/**
		 * Default constructor for {@link CommonJobProperties}.
		 */
		public CommonJobProperties() {
		}

		/**
		 * Constructor that sets the properties used by the builder helper.
		 * @param properties the {@link CommonJobProperties} used by the builder helper.
		 */
		public CommonJobProperties(CommonJobProperties properties) {
			this.name = properties.name;
			this.restartable = properties.restartable;
			this.jobRepository = properties.jobRepository;
			this.jobExecutionListeners = new LinkedHashSet<>(properties.jobExecutionListeners);
			this.jobParametersIncrementer = properties.jobParametersIncrementer;
			this.jobParametersValidator = properties.jobParametersValidator;
		}

		/**
		 * @return the {@link JobParametersIncrementer} used by the builder helper.
		 */
		public JobParametersIncrementer getJobParametersIncrementer() {
			return jobParametersIncrementer;
		}

		/**
		 * Set the {@link JobParametersIncrementer} to be used by the builder helper.
		 * @param jobParametersIncrementer the {@link JobParametersIncrementer} to be used by the builder helper.
		 */
		public void setJobParametersIncrementer(JobParametersIncrementer jobParametersIncrementer) {
			this.jobParametersIncrementer = jobParametersIncrementer;
		}

		/**
		 * @return the {@link JobParametersValidator} used by the validator
		 */
		public JobParametersValidator getJobParametersValidator() {
			return jobParametersValidator;
		}

		/**
		 * Set the {@link JobParametersValidator} to be used by the builder helper.
		 * @param jobParametersValidator the {@link JobParametersValidator} to be used by the builder helper.
		 */
		public void setJobParametersValidator(JobParametersValidator jobParametersValidator) {
			this.jobParametersValidator = jobParametersValidator;
		}

		/**
		 * @return current {@link JobRepository} used by the builder helper.
		 */
		public JobRepository getJobRepository() {
			return jobRepository;
		}

		/**
		 * Set the {@link JobRepository} to be used by the builder helper.
		 * @param jobRepository the {@link JobRepository} to be used by the builder helper.
		 */
		public void setJobRepository(JobRepository jobRepository) {
			this.jobRepository = jobRepository;
		}

		/**
		 * @return the name used by the builder helper.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Set the name of job created by builder.
		 * @param name the name to use.
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return {@link List} of {@link JobExecutionListener}s.
		 */
		public List<JobExecutionListener> getJobExecutionListeners() {
			return new ArrayList<>(jobExecutionListeners);
		}

		/**
		 * Add {@link List} of {@link JobExecutionListener}s to the list of listeners for  {@link Job}.
		 * @param jobExecutionListeners {@link List} of {@link JobExecutionListener}s to be used by the builder helper.
		 */
		public void addStepExecutionListeners(List<JobExecutionListener> jobExecutionListeners) {
			this.jobExecutionListeners.addAll(jobExecutionListeners);
		}

		/**
		 * Add {@link JobExecutionListener} to the list of listeners for  {@link Job}.
		 * @param jobExecutionListener the {@link JobExecutionListener} to be used by the builder helper.
		 */
		public void addJobExecutionListener(JobExecutionListener jobExecutionListener) {
			this.jobExecutionListeners.add(jobExecutionListener);
		}

		/**
		 * @return true if restartable else false.
		 */
		public boolean getRestartable() {
			return restartable;
		}

		/**
		 * Set the value for restartable.
		 * @param restartable true if to be restartable else false.
		 */
		public void setRestartable(boolean restartable) {
			this.restartable = restartable;
		}

		private String name;

	}

}
