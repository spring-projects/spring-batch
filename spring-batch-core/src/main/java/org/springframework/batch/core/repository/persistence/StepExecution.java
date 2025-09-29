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
package org.springframework.batch.core.repository.persistence;

import java.time.LocalDateTime;

import org.springframework.batch.core.BatchStatus;

/**
 * @author Mahmoud Ben Hassine
 * @since 5.2.0
 */
public class StepExecution {

	private String id;

	private long stepExecutionId;

	private long jobExecutionId;

	private String name;

	private BatchStatus status;

	private long readCount;

	private long writeCount;

	private long commitCount;

	private long rollbackCount;

	private long readSkipCount;

	private long processSkipCount;

	private long writeSkipCount;

	private long filterCount;

	private LocalDateTime startTime;

	private LocalDateTime createTime;

	private LocalDateTime endTime;

	private LocalDateTime lastUpdated;

	private ExecutionContext executionContext;

	private ExitStatus exitStatus;

	private boolean terminateOnly;

	public StepExecution() {
	}

	public String getId() {
		return id;
	}

	public long getStepExecutionId() {
		return stepExecutionId;
	}

	public void setStepExecutionId(long stepExecutionId) {
		this.stepExecutionId = stepExecutionId;
	}

	public long getJobExecutionId() {
		return jobExecutionId;
	}

	public void setJobExecutionId(long jobExecutionId) {
		this.jobExecutionId = jobExecutionId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BatchStatus getStatus() {
		return status;
	}

	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	public long getReadCount() {
		return readCount;
	}

	public void setReadCount(long readCount) {
		this.readCount = readCount;
	}

	public long getWriteCount() {
		return writeCount;
	}

	public void setWriteCount(long writeCount) {
		this.writeCount = writeCount;
	}

	public long getCommitCount() {
		return commitCount;
	}

	public void setCommitCount(long commitCount) {
		this.commitCount = commitCount;
	}

	public long getRollbackCount() {
		return rollbackCount;
	}

	public void setRollbackCount(long rollbackCount) {
		this.rollbackCount = rollbackCount;
	}

	public long getReadSkipCount() {
		return readSkipCount;
	}

	public void setReadSkipCount(long readSkipCount) {
		this.readSkipCount = readSkipCount;
	}

	public long getProcessSkipCount() {
		return processSkipCount;
	}

	public void setProcessSkipCount(long processSkipCount) {
		this.processSkipCount = processSkipCount;
	}

	public long getWriteSkipCount() {
		return writeSkipCount;
	}

	public void setWriteSkipCount(long writeSkipCount) {
		this.writeSkipCount = writeSkipCount;
	}

	public long getFilterCount() {
		return filterCount;
	}

	public void setFilterCount(long filterCount) {
		this.filterCount = filterCount;
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

	public ExecutionContext getExecutionContext() {
		return executionContext;
	}

	public void setExecutionContext(ExecutionContext executionContext) {
		this.executionContext = executionContext;
	}

	public ExitStatus getExitStatus() {
		return exitStatus;
	}

	public void setExitStatus(ExitStatus exitStatus) {
		this.exitStatus = exitStatus;
	}

	public boolean isTerminateOnly() {
		return terminateOnly;
	}

	public void setTerminateOnly(boolean terminateOnly) {
		this.terminateOnly = terminateOnly;
	}

	@Override
	public String toString() {
		return "StepExecution{" + "id='" + id + '\'' + ", stepExecutionId=" + stepExecutionId + ", jobExecutionId='"
				+ jobExecutionId + '\'' + ", name='" + name + '\'' + ", status=" + status + ", readCount=" + readCount
				+ ", writeCount=" + writeCount + ", commitCount=" + commitCount + ", rollbackCount=" + rollbackCount
				+ ", readSkipCount=" + readSkipCount + ", processSkipCount=" + processSkipCount + ", writeSkipCount="
				+ writeSkipCount + ", filterCount=" + filterCount + ", startTime=" + startTime + ", createTime="
				+ createTime + ", endTime=" + endTime + ", lastUpdated=" + lastUpdated + ", executionContext="
				+ executionContext + ", exitStatus=" + exitStatus + ", terminateOnly=" + terminateOnly + '}';
	}

}
