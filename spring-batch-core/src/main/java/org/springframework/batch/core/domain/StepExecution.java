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
import java.util.Properties;

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

	private Date startTime = new Date(System.currentTimeMillis());

	private Date endTime = null;

	private Properties statistics = new Properties();

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
	 */
	public StepExecution(StepInstance step, JobExecution jobExecution, Long id) {
		super(id);
		this.step = step;
		this.jobExecution = jobExecution;
	}
	
	public StepExecution(StepInstance step, JobExecution jobExecution){
		this(step, jobExecution, null);
	}

	public void incrementCommitCount() {
		commitCount++;
	}

	public void incrementTaskCount() {
		taskCount++;
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

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
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

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public BatchStatus getStatus() {
		return status;
	}

	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	public Long getStepId() {
		if (step != null) {
			return step.getId();
		}
		return null;
	}

	/**
	 * Accessor for the job execution id.
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
	 * @return the step
	 */
	public StepInstance getStep() {
		return step;
	}

	/**
	 * Accessor for the execution context information of the enclosing job.
	 * @return the {@link jobExecutionContext} that was used to start this step
	 * execution.
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
		statistics = contribution.getStatistics();
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

}
