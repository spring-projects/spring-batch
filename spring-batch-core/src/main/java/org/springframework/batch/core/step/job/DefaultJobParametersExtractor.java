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
package org.springframework.batch.core.step.job;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.Assert;

/**
 * Simple implementation of {@link JobParametersExtractor} which pulls parameters with
 * named keys out of the step execution context and the job parameters of the surrounding
 * job.
 *
 * @author Dave Syer
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 *
 */
public class DefaultJobParametersExtractor implements JobParametersExtractor {

	private Set<String> keys = new HashSet<>();

	private boolean useAllParentParameters = true;

	private JobParametersConverter jobParametersConverter = new DefaultJobParametersConverter();

	/**
	 * The key names to pull out of the execution context or job parameters, if they
	 * exist. If a key doesn't exist in the execution context then the job parameters from
	 * the enclosing job execution are tried, and if there is nothing there either then no
	 * parameter is extracted.
	 * @param keys the keys to set
	 */
	public void setKeys(String[] keys) {
		this.keys = new HashSet<>(Arrays.asList(keys));
	}

	/**
	 * @see JobParametersExtractor#getJobParameters(Job, StepExecution)
	 */
	@Override
	public JobParameters getJobParameters(Job job, StepExecution stepExecution) {
		JobParametersBuilder builder = new JobParametersBuilder();
		JobParameters jobParameters = stepExecution.getJobParameters();
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		if (useAllParentParameters) {
			for (JobParameter<?> jobParameter : jobParameters) {
				builder.addJobParameter(jobParameter);
			}
		}
		Properties properties = new Properties();
		for (String key : keys) {
			if (executionContext.containsKey(key)) {
				properties.setProperty(key, executionContext.getString(key));
			}
			else if (jobParameters.getParameter(key) != null) {
				builder.addJobParameter(jobParameters.getParameter(key));
			}
		}
		builder.addJobParameters(convert(properties));
		return builder.toJobParameters();
	}

	/**
	 * setter to support switching off all parent parameters
	 * @param useAllParentParameters if false do not include parent parameters. True if
	 * all parent parameters need to be included.
	 */
	public void setUseAllParentParameters(boolean useAllParentParameters) {
		this.useAllParentParameters = useAllParentParameters;
	}

	/**
	 * Set the {@link JobParametersConverter} to use.
	 * @param jobParametersConverter the converter to use. Must not be {@code null}.
	 * @deprecated since 6.0 in favor of {@link #convert(Properties)}, scheduled for
	 * removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public void setJobParametersConverter(JobParametersConverter jobParametersConverter) {
		Assert.notNull(jobParametersConverter, "jobParametersConverter must not be null");
		this.jobParametersConverter = jobParametersConverter;
	}

	/**
	 * Convert the given {@link Properties} to {@link JobParameters}.
	 * @param properties the properties to convert
	 * @return the converted job parameters
	 *
	 * @since 6.0
	 */
	protected JobParameters convert(Properties properties) {
		return this.jobParametersConverter.getJobParameters(properties);
	}

}
