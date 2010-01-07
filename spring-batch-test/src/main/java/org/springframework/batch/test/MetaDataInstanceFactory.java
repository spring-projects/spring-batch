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
package org.springframework.batch.test;

import java.util.Collection;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.PropertiesConverter;

/**
 * Convenience methods for creating test instances of {@link JobExecution},
 * {@link JobInstance} and {@link StepExecution}.
 * 
 * @author Dave Syer
 * 
 */
public class MetaDataInstanceFactory {

	/**
	 * The default name for a job ("job")
	 */
	public static final String DEFAULT_JOB_NAME = "job";

	/**
	 * The default id for a job instance (12L)
	 */
	public static final long DEFAULT_JOB_INSTANCE_ID = 12L;

	/**
	 * The default id for a job execution (123L)
	 */
	public static final long DEFAULT_JOB_EXECUTION_ID = 123L;

	/**
	 * The default name for a step ("step")
	 */
	public static final String DEFAULT_STEP_NAME = "step";

	/**
	 * The default id for a step execution (1234L)
	 */
	public static final long DEFAULT_STEP_EXECUTION_ID = 1234L;

	/**
	 * Create a {@link JobInstance} with the parameters provided.
	 * 
	 * @param jobName the name of the job
	 * @param instanceId the Id of the {@link JobInstance}
	 * @param jobParameters comma or new line separated name=value pairs
	 * @return a {@link JobInstance}
	 */
	public static JobInstance createJobInstance(String jobName, Long instanceId, String jobParameters) {
		JobParameters params = new DefaultJobParametersConverter().getJobParameters(PropertiesConverter
				.stringToProperties(jobParameters));
		return createJobInstance(jobName, instanceId, params);
	}

	/**
	 * Create a {@link JobInstance} with the parameters provided.
	 * 
	 * @param jobName the name of the job
	 * @param instanceId the Id of the {@link JobInstance}
	 * @param jobParameters an instance of {@link JobParameters}
	 * @return a {@link JobInstance}
	 */
	public static JobInstance createJobInstance(String jobName, Long instanceId, JobParameters jobParameters) {
		return new JobInstance(instanceId, jobParameters, jobName);
	}

	/**
	 * Create a {@link JobInstance} with the parameters provided.
	 * 
	 * @param jobName the name of the job
	 * @param instanceId the Id of the {@link JobInstance}
	 * @return a {@link JobInstance} with empty {@link JobParameters}
	 */
	public static JobInstance createJobInstance(String jobName, Long instanceId) {
		return new JobInstance(instanceId, new JobParameters(), jobName);
	}

	/**
	 * Create a {@link JobInstance} with default parameters.
	 * 
	 * @return a {@link JobInstance} with name=DEFAULT_JOB_NAME,
	 * id=DEFAULT_JOB_INSTANCE_ID and empty parameters
	 */
	public static JobInstance createJobInstance() {
		return new JobInstance(DEFAULT_JOB_INSTANCE_ID, new JobParameters(), DEFAULT_JOB_NAME);
	}

	/**
	 * Create a {@link JobExecution} with default parameters.
	 * 
	 * @return a {@link JobExecution} with id=DEFAULT_JOB_EXECUTION_ID
	 */
	public static JobExecution createJobExecution() {
		return createJobExecution(DEFAULT_JOB_EXECUTION_ID);
	}

	/**
	 * Create a {@link JobExecution} with the parameters provided.
	 * 
	 * @param executionId the id for the {@link JobExecution}
	 * @return a {@link JobExecution} with valid {@link JobInstance}
	 */
	public static JobExecution createJobExecution(Long executionId) {
		return createJobExecution(DEFAULT_JOB_NAME, DEFAULT_JOB_INSTANCE_ID, executionId);
	}

	/**
	 * Create a {@link JobExecution} with the parameters provided.
	 * 
	 * @param jobName the name of the job
	 * @param instanceId the id for the {@link JobInstance}
	 * @param executionId the id for the {@link JobExecution}
	 * @return a {@link JobExecution} with empty {@link JobParameters}
	 */
	public static JobExecution createJobExecution(String jobName, Long instanceId, Long executionId) {
		return createJobExecution(jobName, instanceId, executionId, new JobParameters());
	}

	/**
	 * Create a {@link JobExecution} with the parameters provided.
	 * 
	 * @param jobName the name of the job
	 * @param instanceId the Id of the {@link JobInstance}
	 * @param executionId the id for the {@link JobExecution}
	 * @param jobParameters comma or new line separated name=value pairs
	 * @return a {@link JobExecution}
	 */
	public static JobExecution createJobExecution(String jobName, Long instanceId, Long executionId,
			String jobParameters) {
		JobParameters params = new DefaultJobParametersConverter().getJobParameters(PropertiesConverter
				.stringToProperties(jobParameters));
		return createJobExecution(jobName, instanceId, executionId, params);
	}

	/**
	 * Create a {@link JobExecution} with the parameters provided.
	 * 
	 * @param jobName the name of the job
	 * @param instanceId the Id of the {@link JobInstance}
	 * @param executionId the id for the {@link JobExecution}
	 * @param jobParameters an instance of {@link JobParameters}
	 * @return a {@link JobExecution}
	 */
	public static JobExecution createJobExecution(String jobName, Long instanceId, Long executionId,
			JobParameters jobParameters) {
		return new JobExecution(createJobInstance(jobName, instanceId, jobParameters), executionId);
	}

	/**
	 * Create a {@link StepExecution} with default parameters.
	 * 
	 * @return a {@link StepExecution} with stepName="step" and
	 * id=DEFAULT_STEP_EXECUTION_ID
	 */
	public static StepExecution createStepExecution() {
		return createStepExecution(DEFAULT_STEP_NAME, DEFAULT_STEP_EXECUTION_ID);
	}

	/**
	 * Create a {@link StepExecution} with the parameters provided.
	 * 
	 * @param stepName the stepName for the {@link StepExecution}
	 * @param executionId the id for the {@link StepExecution}
	 * @return a {@link StepExecution} with a {@link JobExecution} having
	 * default properties
	 */
	public static StepExecution createStepExecution(String stepName, Long executionId) {
		return createStepExecution(createJobExecution(), stepName, executionId);
	}

	/**
	 * Create a {@link StepExecution} with the parameters provided.
	 * 
	 * @param stepName the stepName for the {@link StepExecution}
	 * @param executionId the id for the {@link StepExecution}
	 * @return a {@link StepExecution} with the given {@link JobExecution}
	 */
	public static StepExecution createStepExecution(JobExecution jobExecution, String stepName, Long executionId) {
		StepExecution stepExecution = jobExecution.createStepExecution(stepName);
		stepExecution.setId(executionId);
		return stepExecution;
	}

	/**
	 * Create a {@link JobExecution} with the parameters provided with attached
	 * step executions.
	 * 
	 * @param executionId the {@link JobExecution} id
	 * @param stepNames the names of the step executions
	 * @return a {@link JobExecution} with step executions as specified, each
	 * with a unique id
	 */
	public static JobExecution createJobExecutionWithStepExecutions(Long executionId, Collection<String> stepNames) {
		JobExecution jobExecution = createJobExecution(DEFAULT_JOB_NAME, DEFAULT_JOB_INSTANCE_ID, executionId);
		Long stepExecutionId = DEFAULT_STEP_EXECUTION_ID;
		for (String stepName : stepNames) {
			createStepExecution(jobExecution, stepName, stepExecutionId);
			stepExecutionId++;
		}
		return jobExecution;
	}

	/**
	 * Create a {@link StepExecution} and all its parent entities with default
	 * values, but using the {@link ExecutionContext} and {@link JobParameters}
	 * provided.
	 * 
	 * @param jobParameters come {@link JobParameters}
	 * @param executionContext some {@link ExecutionContext}
	 * 
	 * @return a {@link StepExecution} with the execution context provided
	 */
	public static StepExecution createStepExecution(JobParameters jobParameters, ExecutionContext executionContext) {
		StepExecution stepExecution = createStepExecution(jobParameters);
		stepExecution.setExecutionContext(executionContext);
		return stepExecution;
	}

	/**
	 * Create a {@link StepExecution} and all its parent entities with default
	 * values, but using the {@link JobParameters} provided.
	 * 
	 * @param jobParameters some {@link JobParameters}
	 * @return a {@link StepExecution} with the job parameters provided
	 */
	public static StepExecution createStepExecution(JobParameters jobParameters) {
		JobExecution jobExecution = createJobExecution(DEFAULT_JOB_NAME, DEFAULT_JOB_INSTANCE_ID,
				DEFAULT_JOB_EXECUTION_ID, jobParameters);
		return jobExecution.createStepExecution(DEFAULT_STEP_NAME);
	}

	/**
	 * Create a {@link StepExecution} and all its parent entities with default
	 * values, but using the {@link ExecutionContext} provided.
	 * 
	 * @param executionContext some {@link ExecutionContext}
	 * @return a {@link StepExecution} with the execution context provided
	 */
	public static StepExecution createStepExecution(ExecutionContext executionContext) {
		StepExecution stepExecution = createStepExecution();
		stepExecution.setExecutionContext(executionContext);
		return stepExecution;
	}

}
