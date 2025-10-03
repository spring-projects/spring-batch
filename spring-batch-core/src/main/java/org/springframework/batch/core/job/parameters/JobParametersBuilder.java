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

package org.springframework.batch.core.job.parameters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.batch.core.job.JobInstance;
import org.springframework.util.Assert;

/**
 * Helper class for creating {@link JobParameters}. Useful because all
 * {@link JobParameter} objects are immutable and must be instantiated separately to
 * ensure type safety. Once created, it can be used in the same way as a
 * {@link java.lang.StringBuilder} (except that order is irrelevant), by adding various
 * parameter types and creating a valid {@link JobParameters} object once finished.
 * <p>
 * Job parameters must have unique names within a {@link JobParameters} instance.
 * Therefore, adding a parameter with the same name as an existing parameter will cause
 * the existing parameter to be replaced with the new one.
 * <p>
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

	private final Set<JobParameter<?>> parameters;

	/**
	 * Default constructor. Initializes the builder with empty parameters.
	 */
	public JobParametersBuilder() {
		this.parameters = new HashSet<>();
	}

	/**
	 * Copy constructor. Initializes the builder with the supplied parameters. Existing
	 * parameters with the same name will be overridden.
	 * @param jobParameters {@link JobParameters} instance used to initialize the builder.
	 */
	public JobParametersBuilder(JobParameters jobParameters) {
		this.parameters = new HashSet<>(jobParameters.parameters());
	}

	/**
	 * Add a new identifying String parameter for the given key. <strong>Note: Adding a
	 * parameter with the same name as an existing parameter will cause the existing
	 * parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addString(String name, String value) {
		return addString(name, value, true);
	}

	/**
	 * Add a new String parameter for the given key. <strong>Note: Adding a parameter with
	 * the same name as an existing parameter will cause the existing parameter to be
	 * replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @param identifying The indicates if the parameter is used as part of identifying a
	 * job instance.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addString(String name, String value, boolean identifying) {
		Assert.notNull(value, "Value for parameter '" + name + "' must not be null");
		addJobParameter(new JobParameter<>(name, value, String.class, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link Date} parameter for the given key. <strong>Note:
	 * Adding a parameter with the same name as an existing parameter will cause the
	 * existing parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addDate(String name, Date value) {
		return addDate(name, value, true);
	}

	/**
	 * Add a new {@link Date} parameter for the given key. <strong>Note: Adding a
	 * parameter with the same name as an existing parameter will cause the existing
	 * parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @param identifying Indicates if the parameter is used as part of identifying a job
	 * instance
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addDate(String name, Date value, boolean identifying) {
		Assert.notNull(value, "Value for parameter '" + name + "' must not be null");
		addJobParameter(new JobParameter<>(name, value, Date.class, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link LocalDate} parameter for the given key. <strong>Note:
	 * Adding a parameter with the same name as an existing parameter will cause the
	 * existing parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLocalDate(String name, LocalDate value) {
		return addLocalDate(name, value, true);
	}

	/**
	 * Add a new {@link LocalDate} parameter for the given key. <strong>Note: Adding a
	 * parameter with the same name as an existing parameter will cause the existing
	 * parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @param identifying Indicates if the parameter is used as part of identifying a job
	 * instance
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLocalDate(String name, LocalDate value, boolean identifying) {
		Assert.notNull(value, "Value for parameter '" + name + "' must not be null");
		addJobParameter(new JobParameter<>(name, value, LocalDate.class, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link LocalTime} parameter for the given key. <strong>Note:
	 * Adding a parameter with the same name as an existing parameter will cause the
	 * existing parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLocalTime(String name, LocalTime value) {
		return addLocalTime(name, value, true);
	}

	/**
	 * Add a new {@link LocalTime} parameter for the given key. <strong>Note: Adding a
	 * parameter with the same name as an existing parameter will cause the existing
	 * parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @param identifying Indicates if the parameter is used as part of identifying a job
	 * instance
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLocalTime(String name, LocalTime value, boolean identifying) {
		Assert.notNull(value, "Value for parameter '" + name + "' must not be null");
		addJobParameter(new JobParameter<>(name, value, LocalTime.class, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link LocalDateTime} parameter for the given key.
	 * <strong>Note: Adding a parameter with the same name as an existing parameter will
	 * cause the existing parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLocalDateTime(String name, LocalDateTime value) {
		return addLocalDateTime(name, value, true);
	}

	/**
	 * Add a new {@link LocalDateTime} parameter for the given key. <strong>Note: Adding a
	 * parameter with the same name as an existing parameter will cause the existing
	 * parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @param identifying Indicates if the parameter is used as part of identifying a job
	 * instance
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLocalDateTime(String name, LocalDateTime value, boolean identifying) {
		Assert.notNull(value, "Value for parameter '" + name + "' must not be null");
		addJobParameter(new JobParameter<>(name, value, LocalDateTime.class, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link Long} parameter for the given key. <strong>Note:
	 * Adding a parameter with the same name as an existing parameter will cause the
	 * existing parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLong(String name, Long value) {
		return addLong(name, value, true);
	}

	/**
	 * Add a new {@link Long} parameter for the given key. <strong>Note: Adding a
	 * parameter with the same name as an existing parameter will cause the existing
	 * parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @param identifying Indicates if the parameter is used as part of identifying a job
	 * instance.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addLong(String name, Long value, boolean identifying) {
		Assert.notNull(value, "Value for parameter '" + name + "' must not be null");
		addJobParameter(new JobParameter<>(name, value, Long.class, identifying));
		return this;
	}

	/**
	 * Add a new identifying {@link Double} parameter for the given key. <strong>Note:
	 * Adding a parameter with the same name as an existing parameter will cause the
	 * existing parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addDouble(String name, Double value) {
		return addDouble(name, value, true);
	}

	/**
	 * Add a new {@link Double} parameter for the given key. <strong>Note: Adding a
	 * parameter with the same name as an existing parameter will cause the existing
	 * parameter to be replaced with the new one.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @param identifying Indicates if the parameter is used as part of identifying a job
	 * instance.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addDouble(String name, Double value, boolean identifying) {
		Assert.notNull(value, "Value for parameter '" + name + "' must not be null");
		addJobParameter(new JobParameter<>(name, value, Double.class, identifying));
		return this;
	}

	/**
	 * Conversion method that takes the current state of this builder and returns it as a
	 * {@code JobParameters} object.
	 * @return a valid {@link JobParameters} object.
	 */
	public JobParameters toJobParameters() {
		return new JobParameters(this.parameters);
	}

	/**
	 * Add a new {@link JobParameter} for the given key. <strong>Note: Adding a parameter
	 * with the same name as an existing parameter will cause the existing parameter to be
	 * replaced with the new one.</strong>
	 * @param jobParameter The runtime parameter.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addJobParameter(JobParameter<?> jobParameter) {
		Assert.notNull(jobParameter, "JobParameter must not be null");
		this.parameters.remove(jobParameter);
		this.parameters.add(jobParameter);
		return this;
	}

	/**
	 * Add a job parameter. <strong>Note: Adding a parameter with the same name as an
	 * existing parameter will cause the existing parameter to be replaced with the new
	 * parameter.</strong>
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @param type the type of the parameter
	 * @param identifying true if the parameter is identifying. false otherwise
	 * @return a reference to this object.
	 * @param <T> the type of the parameter
	 * @since 5.0
	 */
	public <T> JobParametersBuilder addJobParameter(String name, T value, Class<T> type, boolean identifying) {
		Assert.notNull(value, "Value for parameter '" + name + "' must not be null");
		return addJobParameter(new JobParameter<>(name, value, type, identifying));
	}

	/**
	 * Add an identifying job parameter. <strong>Note: Adding a parameter with the same
	 * name as an existing parameter will cause the existing parameter to be replaced with
	 * the new one.</strong>
	 * @param name the name of the parameter
	 * @param value the value of the parameter.
	 * @param type the type of the parameter
	 * @return a reference to this object.
	 * @param <T> the type of the parameter
	 * @since 5.0
	 */
	public <T> JobParametersBuilder addJobParameter(String name, T value, Class<T> type) {
		return addJobParameter(name, value, type, true);
	}

	/**
	 * Copy job parameters into the current state. <strong>Note: Parameters with the same
	 * name will be overridden.</strong>
	 * @param jobParameters The parameters to copy in.
	 * @return a reference to this object.
	 */
	public JobParametersBuilder addJobParameters(JobParameters jobParameters) {
		Assert.notNull(jobParameters, "jobParameters must not be null");
		for (JobParameter<?> jobParameter : jobParameters) {
			addJobParameter(jobParameter);
		}
		return this;
	}

}
