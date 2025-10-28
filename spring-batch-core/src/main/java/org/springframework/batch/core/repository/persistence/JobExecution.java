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
package org.springframework.batch.core.repository.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.BatchStatus;

/**
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 * @since 5.2.0
 */
public class JobExecution {

	private long jobExecutionId;

	private long jobInstanceId;

	private Set<JobParameter<?>> jobParameters = new HashSet<>();

	private List<StepExecution> stepExecutions = new ArrayList<>();

	private BatchStatus status;

	private LocalDateTime startTime;

	private LocalDateTime createTime;

	private LocalDateTime endTime;

	private LocalDateTime lastUpdated;

	private ExitStatus exitStatus;

	private ExecutionContext executionContext;

	public JobExecution() {
	}

	public long getJobInstanceId() {
		return jobInstanceId;
	}

	public void setJobInstanceId(long jobInstanceId) {
		this.jobInstanceId = jobInstanceId;
	}

	public long getJobExecutionId() {
		return jobExecutionId;
	}

	public void setJobExecutionId(long jobExecutionId) {
		this.jobExecutionId = jobExecutionId;
	}

	public Set<JobParameter<?>> getJobParameters() {
		return jobParameters;
	}

	public void setJobParameters(Set<JobParameter<?>> jobParameters) {
		this.jobParameters = jobParameters;
	}

	public List<StepExecution> getStepExecutions() {
		return stepExecutions;
	}

	public void setStepExecutions(List<StepExecution> stepExecutions) {
		this.stepExecutions = stepExecutions;
	}

	public BatchStatus getStatus() {
		return status;
	}

	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public LocalDateTime getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(LocalDateTime lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public ExitStatus getExitStatus() {
		return exitStatus;
	}

	public void setExitStatus(ExitStatus exitStatus) {
		this.exitStatus = exitStatus;
	}

	public ExecutionContext getExecutionContext() {
		return executionContext;
	}

	public void setExecutionContext(ExecutionContext executionContext) {
		this.executionContext = executionContext;
	}

	@Override
	public String toString() {
		return "JobExecution{jobExecutionId=" + jobExecutionId + ", jobInstanceId=" + jobInstanceId + ", jobParameters="
				+ jobParameters + ", stepExecutions=" + stepExecutions + ", status=" + status + ", startTime="
				+ startTime + ", createTime=" + createTime + ", endTime=" + endTime + ", lastUpdated=" + lastUpdated
				+ ", exitStatus=" + exitStatus + ", executionContext=" + executionContext + '}';
	}

}
