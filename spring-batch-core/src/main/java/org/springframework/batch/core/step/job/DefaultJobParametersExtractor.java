/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.core.step.job;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

/**
 * Simple implementation of {@link JobParametersExtractor} which pulls
 * parameters with named keys out of the step execution context and the job
 * parameters of the surrounding job.
 * 
 * @author Dave Syer
 * 
 */
public class DefaultJobParametersExtractor implements JobParametersExtractor {

	private Set<String> keys = new HashSet<String>();

	private boolean useAllParentParameters = true;

	/**
	 * The key names to pull out of the execution context or job parameters, if
	 * they exist. If a key doesn't exist in the execution context then the job
	 * parameters from the enclosing job execution are tried, and if there is
	 * nothing there either then no parameter is extracted. Key names ending
	 * with <code>(long)</code>, <code>(int)</code>, <code>(double)</code>,
	 * <code>(date)</code> or <code>(string)</code> will be assumed to refer to
	 * values of the respective type and assigned to job parameters accordingly
	 * (there will be an error if they are not of the right type). Without a
	 * special suffix in that form a parameter is assumed to be of type String.
	 * 
	 * @param keys the keys to set
	 */
	public void setKeys(String[] keys) {
		this.keys = new HashSet<String>(Arrays.asList(keys));
	}

	/**
	 * @see JobParametersExtractor#getJobParameters(StepExecution)
	 */
	public JobParameters getJobParameters(Job job, StepExecution stepExecution) {
		JobParametersBuilder builder = new JobParametersBuilder();
		Map<String, JobParameter> jobParameters = stepExecution.getJobParameters().getParameters();
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		if (useAllParentParameters) {
			for (String key : jobParameters.keySet()) {
				builder.addParameter(key, jobParameters.get(key));
			}
		}
		for (String key : keys) {
			if (key.endsWith("(long)")) {
				key = key.replace("(long)", "");
				if (executionContext.containsKey(key)) {
					builder.addLong(key, executionContext.getLong(key));
				}
				else if (jobParameters.containsKey(key)) {
					builder.addLong(key, (Long) jobParameters.get(key).getValue());
				}
			}
			else if (key.endsWith("(int)")) {
				key = key.replace("(int)", "");
				if (executionContext.containsKey(key)) {
					builder.addLong(key, (long) executionContext.getInt(key));
				}
				else if (jobParameters.containsKey(key)) {
					builder.addLong(key, (Long) jobParameters.get(key).getValue());
				}
			}
			else if (key.endsWith("(double)")) {
				key = key.replace("(double)", "");
				if (executionContext.containsKey(key)) {
					builder.addDouble(key, executionContext.getDouble(key));
				}
				else if (jobParameters.containsKey(key)) {
					builder.addDouble(key, (Double) jobParameters.get(key).getValue());
				}
			}
			else if (key.endsWith("(string)")) {
				key = key.replace("(string)", "");
				if (executionContext.containsKey(key)) {
					builder.addString(key, executionContext.getString(key));
				}
				else if (jobParameters.containsKey(key)) {
					builder.addString(key, (String) jobParameters.get(key).getValue());
				}
			}
			else if (key.endsWith("(date)")) {
				key = key.replace("(date)", "");
				if (executionContext.containsKey(key)) {
					builder.addDate(key, (Date) executionContext.get(key));
				}
				else if (jobParameters.containsKey(key)) {
					builder.addDate(key, (Date) jobParameters.get(key).getValue());
				}
			}
			else {
				if (executionContext.containsKey(key)) {
					builder.addString(key, executionContext.get(key).toString());
				}
				else if (jobParameters.containsKey(key)) {
					builder.addString(key, jobParameters.get(key).getValue().toString());
				}
			}
		}
		return builder.toJobParameters();
	}

}
