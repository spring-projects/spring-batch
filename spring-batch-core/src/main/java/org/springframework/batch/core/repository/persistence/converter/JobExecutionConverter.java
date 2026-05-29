/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.batch.core.repository.persistence.converter;

import java.util.HashSet;
import java.util.Set;

import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.persistence.ExecutionContext;
import org.springframework.batch.core.repository.persistence.ExitStatus;
import org.springframework.batch.core.repository.persistence.JobExecution;
import org.springframework.batch.core.repository.persistence.JobParameter;

/**
 * @author Mahmoud Ben Hassine
 * @since 5.2.0
 */
public class JobExecutionConverter {

	private final JobParameterConverter jobParameterConverter = new JobParameterConverter();

	private final StepExecutionConverter stepExecutionConverter = new StepExecutionConverter();

	public org.springframework.batch.core.job.JobExecution toJobExecution(JobExecution source,
			JobInstance jobInstance) {
		Set<org.springframework.batch.core.job.parameters.JobParameter<?>> parameters = new HashSet<>();
		source.getJobParameters()
			.forEach(parameter -> parameters.add(this.jobParameterConverter.toJobParameter(parameter)));
		org.springframework.batch.core.job.JobExecution jobExecution = new org.springframework.batch.core.job.JobExecution(
				source.getJobExecutionId(), jobInstance, new JobParameters(parameters));
		jobExecution.addStepExecutions(source.getStepExecutions()
			.stream()
			.map(stepExecution -> this.stepExecutionConverter.toStepExecution(stepExecution, jobExecution))
			.toList());
		jobExecution.setStatus(source.getStatus());
		jobExecution.setStartTime(source.getStartTime());
		jobExecution.setCreateTime(source.getCreateTime());
		jobExecution.setEndTime(source.getEndTime());
		jobExecution.setLastUpdated(source.getLastUpdated());
		jobExecution.setExitStatus(new org.springframework.batch.core.ExitStatus(source.getExitStatus().exitCode(),
				source.getExitStatus().exitDescription()));
		jobExecution.setExecutionContext(
				new org.springframework.batch.infrastructure.item.ExecutionContext(source.getExecutionContext().map()));
		return jobExecution;
	}

	public JobExecution fromJobExecution(org.springframework.batch.core.job.JobExecution source) {
		JobExecution jobExecution = new JobExecution();
		jobExecution.setJobExecutionId(source.getId());
		jobExecution.setJobInstanceId(source.getJobInstance().getId());
		Set<JobParameter<?>> parameters = new HashSet<>();
		source.getJobParameters()
			.parameters()
			.forEach(parameter -> parameters.add(this.jobParameterConverter.fromJobParameter(parameter)));
		jobExecution.setJobParameters(parameters);
		jobExecution.setStepExecutions(
				source.getStepExecutions().stream().map(this.stepExecutionConverter::fromStepExecution).toList());
		jobExecution.setStatus(source.getStatus());
		jobExecution.setStartTime(source.getStartTime());
		jobExecution.setCreateTime(source.getCreateTime());
		jobExecution.setEndTime(source.getEndTime());
		jobExecution.setLastUpdated(source.getLastUpdated());
		jobExecution.setExitStatus(
				new ExitStatus(source.getExitStatus().getExitCode(), source.getExitStatus().getExitDescription()));
		org.springframework.batch.infrastructure.item.ExecutionContext executionContext = source.getExecutionContext();
		jobExecution.setExecutionContext(new ExecutionContext(executionContext.toMap(), executionContext.isDirty()));
		return jobExecution;
	}

}
