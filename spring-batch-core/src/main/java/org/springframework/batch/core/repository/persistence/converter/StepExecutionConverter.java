/*
 * Copyright 2024 the original author or authors.
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

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.repository.persistence.ExecutionContext;
import org.springframework.batch.core.repository.persistence.ExitStatus;
import org.springframework.batch.core.repository.persistence.StepExecution;

/**
 * @author Mahmoud Ben Hassine
 * @since 5.2.0
 */
public class StepExecutionConverter {

	public org.springframework.batch.core.step.StepExecution toStepExecution(StepExecution source,
			JobExecution jobExecution) {
		org.springframework.batch.core.step.StepExecution stepExecution = new org.springframework.batch.core.step.StepExecution(
				source.getStepExecutionId(), source.getName(), jobExecution);
		stepExecution.setStatus(source.getStatus());
		stepExecution.setReadCount(source.getReadCount());
		stepExecution.setWriteCount(source.getWriteCount());
		stepExecution.setCommitCount(source.getCommitCount());
		stepExecution.setRollbackCount(source.getRollbackCount());
		stepExecution.setReadSkipCount(source.getReadSkipCount());
		stepExecution.setProcessSkipCount(source.getProcessSkipCount());
		stepExecution.setWriteSkipCount(source.getWriteSkipCount());
		stepExecution.setFilterCount(source.getFilterCount());
		stepExecution.setStartTime(source.getStartTime());
		stepExecution.setCreateTime(source.getCreateTime());
		stepExecution.setEndTime(source.getEndTime());
		stepExecution.setLastUpdated(source.getLastUpdated());
		stepExecution.setExitStatus(new org.springframework.batch.core.ExitStatus(source.getExitStatus().exitCode(),
				source.getExitStatus().exitDescription()));
		stepExecution.setExecutionContext(
				new org.springframework.batch.infrastructure.item.ExecutionContext(source.getExecutionContext().map()));
		if (source.isTerminateOnly()) {
			stepExecution.setTerminateOnly();
		}
		return stepExecution;
	}

	public StepExecution fromStepExecution(org.springframework.batch.core.step.StepExecution source) {
		StepExecution stepExecution = new StepExecution();
		stepExecution.setStepExecutionId(source.getId());
		stepExecution.setJobExecutionId(source.getJobExecutionId());
		stepExecution.setName(source.getStepName());
		stepExecution.setJobExecutionId(source.getJobExecutionId());
		stepExecution.setStatus(source.getStatus());
		stepExecution.setReadCount(source.getReadCount());
		stepExecution.setWriteCount(source.getWriteCount());
		stepExecution.setCommitCount(source.getCommitCount());
		stepExecution.setRollbackCount(source.getRollbackCount());
		stepExecution.setReadSkipCount(source.getReadSkipCount());
		stepExecution.setProcessSkipCount(source.getProcessSkipCount());
		stepExecution.setWriteSkipCount(source.getWriteSkipCount());
		stepExecution.setFilterCount(source.getFilterCount());
		stepExecution.setStartTime(source.getStartTime());
		stepExecution.setCreateTime(source.getCreateTime());
		stepExecution.setEndTime(source.getEndTime());
		stepExecution.setLastUpdated(source.getLastUpdated());
		stepExecution.setExitStatus(
				new ExitStatus(source.getExitStatus().getExitCode(), source.getExitStatus().getExitDescription()));
		org.springframework.batch.infrastructure.item.ExecutionContext executionContext = source.getExecutionContext();
		stepExecution.setExecutionContext(new ExecutionContext(executionContext.toMap(), executionContext.isDirty()));
		stepExecution.setTerminateOnly(source.isTerminateOnly());
		return stepExecution;
	}

}
