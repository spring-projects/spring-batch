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

package org.springframework.batch.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Helper class for creating {@link JobParameters}. Useful because all
 * {@link JobParameter} objects are immutable and must be instantiated separately to
 * ensure type safety. Once created, it can be used in the same was a
 * {@link java.lang.StringBuilder} (except that order is irrelevant), by adding various
 * parameter types and creating a valid {@link JobParameters} object once finished.<br>
 * <br>
 * Using the {@code identifying} flag indicates if the parameter should be used in the
 * identification of a {@link JobInstance} object. That flag defaults to {@code true}.
 *
 * @author Lucas Ward
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @since 1.0
 * @see JobParameters
 * @see JobParameter
 */
public class JobParametersBuilder {

	private Map<String, JobParameter<?>> parameterMap;

	private JobExplorer jobExplorer;

	/**
	 * Default constructor. Initializes the builder with empty parameters.
	 */
	public JobParametersBuilder() {
		this.parameterMap = new HashMap<>();
	}

	/**
	 * @param jobExplorer {@link JobExplorer} used for looking up previous job parameter
	 * information.
	 */
	public JobParametersBuilder(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
		this.parameterMap = new HashMap<>();
	}

	/**
	 * Copy constructor. Initializes the builder with the supplied parameters.
	 * @param jobParameters {@link JobParameters} instance used to initialize the builder.
	 */
	public JobParametersBuilder(JobParameters jobParameters) {
		this(jobParameters, null);
	}

	/**
	 * Copy constructor. Initializes the builder with the supplied parameters.
	 * @param jobParameters {@link JobParameters} instance used to initialize the builder.
	 * @param jobExplorer {@link JobExplorer} used for looking up previous job parameter
	 * information.
	 */
	public JobParametersBuilder(JobParameters jobParameters, JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
		this.parameterMap = new HashMap<>(jobParameters.getParameters());
	}

	/**
	 * Add a new identifying String parameter for the given key.
	 * @param key The parameter accessor.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addString(String key, @NonNull String parameter) {
		return addString(key, parameter, true);
	}

	/**
	 * Add a new String parameter for the given key.
	 * @param key The parameter accessor.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @param identifying The indicates if the parameter is used as part of identifying a
	 * job instance.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addString(String key, @NonNull String parameter, boolean identifying) {
		Assert.notNull(parameter, "Value for parameter '" + key + "' must not be null");
		this.parameterMap.put(key, new JobParameter<>(parameter, String.class, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link Date} parameter for the given key.
	 * @param key The parameter accessor.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addDate(String key, @NonNull Date parameter) {
		return addDate(key, parameter, true);
	}

	/**
	 * Add a new {@link Date} parameter for the given key.
	 * @param key The parameter accessor.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @param identifying Indicates if the parameter is used as part of identifying a job
	 * instance
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addDate(String key, @NonNull Date parameter, boolean identifying) {
		Assert.notNull(parameter, "Value for parameter '" + key + "' must not be null");
		this.parameterMap.put(key, new JobParameter<>(parameter, Date.class, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link LocalDate} parameter for the given key.
	 * @param key The parameter name.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLocalDate(String key, @NonNull LocalDate parameter) {
		return addLocalDate(key, parameter, true);
	}

	/**
	 * Add a new {@link LocalDate} parameter for the given key.
	 * @param key The parameter name.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @param identifying Indicates if the parameter is used as part of identifying a job
	 * instance
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLocalDate(String key, @NonNull LocalDate parameter, boolean identifying) {
		Assert.notNull(parameter, "Value for parameter '" + key + "' must not be null");
		this.parameterMap.put(key, new JobParameter<>(parameter, LocalDate.class, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link LocalTime} parameter for the given key.
	 * @param key The parameter name.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLocalTime(String key, @NonNull LocalTime parameter) {
		return addLocalTime(key, parameter, true);
	}

	/**
	 * Add a new {@link LocalTime} parameter for the given key.
	 * @param key The parameter name.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @param identifying Indicates if the parameter is used as part of identifying a job
	 * instance
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLocalTime(String key, @NonNull LocalTime parameter, boolean identifying) {
		Assert.notNull(parameter, "Value for parameter '" + key + "' must not be null");
		this.parameterMap.put(key, new JobParameter<>(parameter, LocalTime.class, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link LocalDateTime} parameter for the given key.
	 * @param key The parameter name.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLocalDateTime(String key, @NonNull LocalDateTime parameter) {
		return addLocalDateTime(key, parameter, true);
	}

	/**
	 * Add a new {@link LocalDateTime} parameter for the given key.
	 * @param key The parameter name.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @param identifying Indicates if the parameter is used as part of identifying a job
	 * instance
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLocalDateTime(String key, @NonNull LocalDateTime parameter, boolean identifying) {
		Assert.notNull(parameter, "Value for parameter '" + key + "' must not be null");
		this.parameterMap.put(key, new JobParameter<>(parameter, LocalDateTime.class, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link Long} parameter for the given key.
	 * @param key The parameter accessor.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLong(String key, @NonNull Long parameter) {
		return addLong(key, parameter, true);
	}

	/**
	 * Add a new {@link Long} parameter for the given key.
	 * @param key The parameter accessor.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @param identifying Indicates if the parameter is used as part of identifying a job
	 * instance.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLong(String key, @NonNull Long parameter, boolean identifying) {
		Assert.notNull(parameter, "Value for parameter '" + key + "' must not be null");
		this.parameterMap.put(key, new JobParameter<>(parameter, Long.class, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link Double} parameter for the given key.
	 * @param key The parameter accessor.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addDouble(String key, @NonNull Double parameter) {
		return addDouble(key, parameter, true);
	}

	/**
	 * Add a new {@link Double} parameter for the given key.
	 * @param key The parameter accessor.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @param identifying Indicates if the parameter is used as part of identifying a job
	 * instance.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addDouble(String key, @NonNull Double parameter, boolean identifying) {
		Assert.notNull(parameter, "Value for parameter '" + key + "' must not be null");
		this.parameterMap.put(key, new JobParameter<>(parameter, Double.class, identifying));
		return this;
	}

	/**
	 * Conversion method that takes the current state of this builder and returns it as a
	 * {@code JobParameters} object.
	 * @return a valid {@link JobParameters} object.
	 */
	public JobParameters toJobParameters() {
		return new JobParameters(this.parameterMap);
	}

	/**
	 * Add a new {@link JobParameter} for the given key.
	 * @param key The parameter accessor.
	 * @param jobParameter The runtime parameter.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addJobParameter(String key, JobParameter<?> jobParameter) {
		Assert.notNull(jobParameter, "JobParameter must not be null");
		this.parameterMap.put(key, jobParameter);
		return this;
	}

	/**
	 * Add a job parameter.
	 * @param name the name of the parameter
	 * @param value the value of the parameter. Must not be {@code null}.
	 * @param type the type of the parameter
	 * @param identifying true if the parameter is identifying. false otherwise
	 * @return a reference to this object.
	 * @param <T> the type of the parameter
	 * @since 5.0
	 */
	public <T> JobParametersBuilder addJobParameter(String name, @NonNull T value, Class<T> type, boolean identifying) {
		Assert.notNull(value, "Value for parameter '" + name + "' must not be null");
		return addJobParameter(name, new JobParameter<>(value, type, identifying));
	}

	/**
	 * Add an identifying job parameter.
	 * @param name the name of the parameter
	 * @param value the value of the parameter. Must not be {@code null}.
	 * @param type the type of the parameter
	 * @return a reference to this object.
	 * @param <T> the type of the parameter
	 * @since 5.0
	 */
	public <T> JobParametersBuilder addJobParameter(String name, @NonNull T value, Class<T> type) {
		return addJobParameter(name, value, type, true);
	}

	/**
	 * Copy job parameters into the current state.
	 * @param jobParameters The parameters to copy in.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addJobParameters(JobParameters jobParameters) {
		Assert.notNull(jobParameters, "jobParameters must not be null");

		this.parameterMap.putAll(jobParameters.getParameters());

		return this;
	}

	/**
	 * Initializes the {@link JobParameters} based on the state of the {@link Job}. This
	 * should be called after all parameters have been entered into the builder. All
	 * parameters already set on this builder instance are appended to those retrieved
	 * from the job incrementer, overriding any with the same key (this is the same
	 * behavior as
	 * {@link org.springframework.batch.core.launch.support.CommandLineJobRunner} with the
	 * {@code -next} option and
	 * {@link org.springframework.batch.core.launch.JobOperator#startNextInstance(String)}).
	 * @param job The job for which the {@link JobParameters} are being constructed.
	 * @return a reference to this object.
	 *
	 * @since 4.0
	 */
	public JobParametersBuilder getNextJobParameters(Job job) {
		Assert.state(this.jobExplorer != null, "A JobExplorer is required to get next job parameters");
		Assert.notNull(job, "Job must not be null");
		Assert.notNull(job.getJobParametersIncrementer(),
				"No job parameters incrementer found for job=" + job.getName());

		String name = job.getName();
		JobParameters nextParameters;
		JobInstance lastInstance = this.jobExplorer.getLastJobInstance(name);
		JobParametersIncrementer incrementer = job.getJobParametersIncrementer();
		if (lastInstance == null) {
			// Start from a completely clean sheet
			nextParameters = incrementer.getNext(new JobParameters());
		}
		else {
			JobExecution previousExecution = this.jobExplorer.getLastJobExecution(lastInstance);
			if (previousExecution == null) {
				// Normally this will not happen - an instance exists with no executions
				nextParameters = incrementer.getNext(new JobParameters());
			}
			else {
				nextParameters = incrementer.getNext(previousExecution.getJobParameters());
			}
		}

		// start with parameters from the incrementer
		Map<String, JobParameter<?>> nextParametersMap = new HashMap<>(nextParameters.getParameters());
		// append new parameters (overriding those with the same key)
		nextParametersMap.putAll(this.parameterMap);
		this.parameterMap = nextParametersMap;
		return this;
	}

}
