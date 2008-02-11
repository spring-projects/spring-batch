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

import java.util.Date;

import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Batch domain object representation the execution of a step. Unlike
 * JobExecution, there are four additional properties: luwCount, commitCount,
 * rollbackCount and statistics. These values represent how many times a step
 * has iterated through logical units of work, how many times it has been
 * committed, and any other statistics the developer wishes to store,
 * respectively.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class StepExecution extends Entity {

	private JobExecution jobExecution;

	private StepInstance step;

	private BatchStatus status = BatchStatus.STARTING;

	private int taskCount = 0;

	private int commitCount = 0;

	private int rollbackCount = 0;
	
	private int skipCount = 0;
	
	private int retryCount = 0;

	private Date startTime = new Date(System.currentTimeMillis());

	private Date endTime = null;

	private ExecutionAttributes executionAttributes = new ExecutionAttributes();

	private ExitStatus exitStatus = ExitStatus.UNKNOWN;

	private boolean terminateOnly;

	/**
	 * Package private constructor for Hibernate
	 */
	StepExecution() {
		super();
	}

	/**
	 * Constructor with mandatory properties.
	 * 
	 * @param step the step to which this execution belongs
	 * @param jobExecution the current job execution
	 * @param id the id of this execution
	 */
	public StepExecution(StepInstance step, JobExecution jobExecution, Long id) {
		super(id);
		this.step = step;
		this.jobExecution = jobExecution;
	}

	/**
	 * Constructor that substitutes in null for the execution id
	 * 
	 * @param step the step to which this execution belongs
	 * @param jobExecution the current job execution
	 */
	public StepExecution(StepInstance step, JobExecution jobExecution) {
		this(step, jobExecution, null);
	}

	/**
	 * Increments the number of commits in this execution
	 */
	public void incrementCommitCount() {
		commitCount++;
	}

	/**
	 * Increments the number of tasks in this execution
	 */
	public void incrementTaskCount() {
		taskCount++;
	}

	/**
	 * Returns the {@link ExecutionAttributes} for this execution
	 * 
	 * @return the attributes
	 */
	public ExecutionAttributes getExecutionAttributes() {
		return executionAttributes;
	}

	/**
	 * Sets the {@link ExecutionAttributes} for this execution
	 * 
	 * @param executionAttributes the attributes
	 */
	public void setExecutionAttributes(ExecutionAttributes executionAttributes) {
		this.executionAttributes = executionAttributes;
	}

	/**
	 * Returns the current number of commits for this execution
	 * 
	 * @return the current number of commits
	 */
	public Integer getCommitCount() {
		return new Integer(commitCount);
	}

	/**
	 * Sets the current number of commits for this execution
	 * 
	 * @param commitCount the current number of commits
	 */
	public void setCommitCount(int commitCount) {
		this.commitCount = commitCount;
	}

	/**
	 * Returns the time that this execution ended
	 * 
	 * @return the time that this execution ended
	 */
	public Date getEndTime() {
		return endTime;
	}

	/**
	 * Sets the time that this execution ended
	 * 
	 * @param endTime the time that this execution ended
	 */
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	/**
	 * Returns the current number of tasks for this execution
	 * 
	 * @return the current number of tasks for this execution
	 */
	public Integer getTaskCount() {
		return new Integer(taskCount);
	}

	/**
	 * Sets the current number of tasks for this execution
	 * 
	 * @param taskCount the current number of tasks for this execution
	 */
	public void setTaskCount(int taskCount) {
		this.taskCount = taskCount;
	}

	/**
	 * Sets the current number of rollbacks for this execution
	 * 
	 * @param rollbackCount the current number of rollbacks for this execution
	 */
	public void setRollbackCount(int rollbackCount) {
		this.rollbackCount = rollbackCount;
	}

	/**
	 * Returns the current number of rollbacks for this execution
	 * 
	 * @return the current number of rollbacks for this execution
	 */
	public Integer getRollbackCount() {
		return new Integer(rollbackCount);
	}

	/**
	 * Gets the time this execution started
	 * 
	 * @return the time this execution started
	 */
	public Date getStartTime() {
		return startTime;
	}

	/**
	 * Sets the time this execution started
	 * 
	 * @param startTime the time this execution started
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	/**
	 * Returns the current status of this step
	 * 
	 * @return the current status of this step
	 */
	public BatchStatus getStatus() {
		return status;
	}

	/**
	 * Sets the current status of this step
	 * 
	 * @param status the current status of this step
	 */
	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	/**
	 * Returns the id for this step
	 * 
	 * @return the id for this step
	 */
	public Long getStepId() {
		if (step != null) {
			return step.getId();
		}
		return null;
	}

	/**
	 * Accessor for the job execution id.
	 * 
	 * @return the jobExecutionId
	 */
	public Long getJobExecutionId() {
		if (jobExecution != null) {
			return jobExecution.getId();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.container.common.domain.Entity#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		Object stepId = getStepId();
		Object jobExecutionId = getJobExecutionId();
		if (stepId == null && jobExecutionId == null || !(obj instanceof StepExecution) || getId() == null) {
			return super.equals(obj);
		}
		StepExecution other = (StepExecution) obj;
		if (stepId == null) {
			return jobExecutionId.equals(other.getJobExecutionId());
		}
		return stepId.equals(other.getStepId())
		        && (jobExecutionId == null || jobExecutionId.equals(other.getJobExecutionId()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.container.common.domain.Entity#hashCode()
	 */
	public int hashCode() {
		Object stepId = getStepId();
		Object jobExecutionId = getJobExecutionId();
		return super.hashCode() + 31 * (stepId != null ? stepId.hashCode() : 0) + 91
		        * (jobExecutionId != null ? jobExecutionId.hashCode() : 0);
	}

	public String toString() {
		return super.toString() + ", name=" + getName() + ", taskCount=" + taskCount + ", commitCount=" + commitCount
		        + ", rollbackCount=" + rollbackCount;
	}

	private String getName() {
		return step == null ? null : step.getName();
	}

	/**
	 * @param exitStatus
	 */
	public void setExitStatus(ExitStatus exitStatus) {
		this.exitStatus = exitStatus;
	}

	/**
	 * @return the exitCode
	 */
	public ExitStatus getExitStatus() {
		return exitStatus;
	}

	/**
	 * Accessor for the step governing this execution.
	 * 
	 * @return the step
	 */
	public StepInstance getStep() {
		return step;
	}

	/**
	 * Accessor for the execution context information of the enclosing job.
	 * 
	 * @return the {@link JobExecution} that was used to start this step
	 *         execution.
	 */
	public JobExecution getJobExecution() {
		return jobExecution;
	}

	/**
	 * Factory method for {@link StepContribution}.
	 * 
	 * @return a new {@link StepContribution}
	 */
	public StepContribution createStepContribution() {
		return new StepContribution(this);
	}

	/**
	 * On successful execution just before a chunk commit, this method should be
	 * called. Synchronizes access to the {@link StepExecution} so that changes
	 * are atomic.
	 * 
	 * @param contribution
	 */
	public synchronized void apply(StepContribution contribution) {
		taskCount += contribution.getTaskCount();
		executionAttributes = contribution.getExecutionAttributes();
		commitCount += contribution.getCommitCount();
	}

	/**
	 * On unsuccessful execution after a chunk has rolled back. Synchronizes
	 * access to the {@link StepExecution} so that changes are atomic.
	 */
	public synchronized void rollback() {
		rollbackCount++;
	}

	/**
	 * @return flag to indicate that an execution should halt
	 */
	public boolean isTerminateOnly() {
		return this.terminateOnly;
	}

	/**
	 * Set a flag that will signal to an execution environment that this
	 * execution (and its surrounding job) wishes to exit.
	 */
	public void setTerminateOnly() {
		this.terminateOnly = true;
	}
	
	public void setSkipCount(int skipCount) {
		this.skipCount = skipCount;
	}
	
	public int getSkipCount() {
		return skipCount;
	}
	
	public void incrementSkipCount(){
		skipCount++;
	}
	
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
	
	public int getRetryCount() {
		return retryCount;
	}

}
