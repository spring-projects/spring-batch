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

package org.springframework.batch.core.domain;

import java.sql.Timestamp;
import java.util.Properties;

/**
 * Batch domain object representation the execution of a step. Unlike
 * JobExecution, there are four additional properties: luwCount, commitCount,
 * rollbackCount and statistics. These values represent how many times a step
 * has iterated through logical units of work, how many times it has been
 * committed, and any other statistics the developer wishes to store,
 * respectively.
 * 
 * @author Lucas Ward
 * 
 */
public class StepExecution extends Entity {

	// TODO declare transient or make serializable
	private BatchStatus status = BatchStatus.STARTING;

	private int taskCount = 0;

	private int commitCount = 0;

	private int rollbackCount = 0;

	private Timestamp startTime = new Timestamp(System.currentTimeMillis());

	private Timestamp endTime = null;

	private Properties statistics = new Properties();

	private Long stepId;

	private Long jobExecutionId;

	private int exitCode;
	
	/**
	 * Package private constructor for Hibernate
	 */
	StepExecution() {
		super();
	}

	public StepExecution(Long stepId, Long jobExecutionId) {
		this();
		this.stepId = stepId;
		this.jobExecutionId = jobExecutionId;
	}

	public void incrementCommitCount() {
		commitCount++;
	}

	public void incrementTaskCount() {
		taskCount++;
	}

	public void incrementRollbackCount() {
		rollbackCount++;
	}

	public Properties getStatistics() {
		return statistics;
	}

	public void setStatistics(Properties statistics) {
		this.statistics = statistics;
	}

	public Integer getCommitCount() {
		return new Integer(commitCount);
	}

	public void setCommitCount(int commitCount) {
		this.commitCount = commitCount;
	}

	public Timestamp getEndTime() {
		return endTime;
	}

	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
	}

	public Integer getTaskCount() {
		return new Integer(taskCount);
	}

	public void setTaskCount(int taskCount) {
		this.taskCount = taskCount;
	}

	public void setRollbackCount(int rollbackCount) {
		this.rollbackCount = rollbackCount;
	}

	public Integer getRollbackCount() {
		return new Integer(rollbackCount);
	}

	public Timestamp getStartTime() {
		return startTime;
	}

	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}

	public BatchStatus getStatus() {
		return status;
	}

	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	public Long getStepId() {
		return stepId;
	}

	/**
	 * Accessor for the job execution id.
	 * @return the jobExecutionId
	 */
	public Long getJobExecutionId() {
		return jobExecutionId;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.domain.Entity#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (stepId==null && jobExecutionId==null || !(obj instanceof StepExecution) || getId()!=null) {			
			return super.equals(obj);
		}
		StepExecution other = (StepExecution) obj;
		if (stepId==null) {
			return jobExecutionId.equals(other.getJobExecutionId());
		}
		return stepId.equals(other.getStepId()) && (jobExecutionId==null || jobExecutionId.equals(other.getJobExecutionId())); 
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.domain.Entity#hashCode()
	 */
	public int hashCode() {
		return super.hashCode() + 31*(stepId!=null ? stepId.hashCode() : 0) + 91*(jobExecutionId!=null ? jobExecutionId.hashCode() : 0);
	}
		
	public String toString() {
		return super.toString() + ", taskCount=" + taskCount + ", commitCount=" + commitCount + ", rollbackCount="
				+ rollbackCount;
	}


	/**
	 * @param exitCode
	 */
	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

	/**
	 * @return the exitCode
	 */
	public int getExitCode() {
		return exitCode;
	}

}
