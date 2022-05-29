/*
 * Copyright 2006-2022 the original author or authors.
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

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

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

	private Map<String, JobParameter> parameterMap;

	private JobExplorer jobExplorer;

	/**
	 * Default constructor. Initializes the builder with empty parameters.
	 */
	public JobParametersBuilder() {
		this.parameterMap = new LinkedHashMap<>();
	}

	/**
	 * @param jobExplorer {@link JobExplorer} used for looking up previous job parameter
	 * information.
	 */
	public JobParametersBuilder(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
		this.parameterMap = new LinkedHashMap<>();
	}

	/**
	 * Copy constructor. Initializes the builder with the supplied parameters.
	 * @param jobParameters {@link JobParameters} instance used to initialize the builder.
	 */
	public JobParametersBuilder(JobParameters jobParameters) {
		this(jobParameters, null);
	}

	/**
	 * Constructor to add conversion capabilities to support JSR-352. Per the spec, it is
	 * expected that all keys and values in the provided {@link Properties} instance are
	 * {@link String} objects.
	 * @param properties the job parameters to be used.
	 */
	public JobParametersBuilder(Properties properties) {
		this.parameterMap = new LinkedHashMap<>();

		if (properties != null) {
			for (Map.Entry<Object, Object> curProperty : properties.entrySet()) {
				this.parameterMap.put((String) curProperty.getKey(),
						new JobParameter((String) curProperty.getValue(), false));
			}
		}
	}

	/**
	 * Copy constructor. Initializes the builder with the supplied parameters.
	 * @param jobParameters {@link JobParameters} instance used to initialize the builder.
	 * @param jobExplorer {@link JobExplorer} used for looking up previous job parameter
	 * information.
	 */
	public JobParametersBuilder(JobParameters jobParameters, JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
		this.parameterMap = new LinkedHashMap<>(jobParameters.getParameters());
	}

	/**
	 * Add a new identifying String parameter for the given key.
	 * @param key The parameter accessor.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addString(String key, @NonNull String parameter) {
		this.parameterMap.put(key, new JobParameter(parameter, true));
		return this;
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
		this.parameterMap.put(key, new JobParameter(parameter, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link Date} parameter for the given key.
	 * @param key The parameter accessor.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addDate(String key, @NonNull Date parameter) {
		this.parameterMap.put(key, new JobParameter(parameter, true));
		return this;
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
		this.parameterMap.put(key, new JobParameter(parameter, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link Long} parameter for the given key.
	 * @param key The parameter accessor.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLong(String key, @NonNull Long parameter) {
		this.parameterMap.put(key, new JobParameter(parameter, true));
		return this;
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
		this.parameterMap.put(key, new JobParameter(parameter, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link Double} parameter for the given key.
	 * @param key The parameter accessor.
	 * @param parameter The runtime parameter. Must not be {@code null}.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addDouble(String key, @NonNull Double parameter) {
		this.parameterMap.put(key, new JobParameter(parameter, true));
		return this;
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
		this.parameterMap.put(key, new JobParameter(parameter, identifying));
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
	public JobParametersBuilder addParameter(String key, JobParameter jobParameter) {
		Assert.notNull(jobParameter, "JobParameter must not be null");
		this.parameterMap.put(key, jobParameter);
		return this;
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
		Map<String, JobParameter> nextParametersMap = new HashMap<>(nextParameters.getParameters());
		// append new parameters (overriding those with the same key)
		nextParametersMap.putAll(this.parameterMap);
		this.parameterMap = nextParametersMap;
		return this;
	}

}
