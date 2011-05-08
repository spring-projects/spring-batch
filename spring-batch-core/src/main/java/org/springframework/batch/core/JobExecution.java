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

package org.springframework.batch.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.batch.item.ExecutionContext;

/**
 * Batch domain object representing the execution of a job.
 * 
 * @author Lucas Ward
 * 
 */
public class JobExecution extends Entity {

	private JobInstance jobInstance;

	private volatile Collection<StepExecution> stepExecutions = new CopyOnWriteArraySet<StepExecution>();

	private volatile BatchStatus status = BatchStatus.STARTING;

	private volatile Date startTime = null;

	private volatile Date createTime = new Date(System.currentTimeMillis());

	private volatile Date endTime = null;

	private volatile Date lastUpdated = null;

	private volatile ExitStatus exitStatus = ExitStatus.UNKNOWN;

	private volatile ExecutionContext executionContext = new ExecutionContext();

	private transient volatile List<Throwable> failureExceptions = new CopyOnWriteArrayList<Throwable>();

	/**
	 * Because a JobExecution isn't valid unless the job is set, this
	 * constructor is the only valid one from a modelling point of view.
	 * 
	 * @param job the job of which this execution is a part
	 */
	public JobExecution(JobInstance job, Long id) {
		super(id);
		this.jobInstance = job;
	}

	/**
	 * Constructor for transient (unsaved) instances.
	 * 
	 * @param job the enclosing {@link JobInstance}
	 */
	public JobExecution(JobInstance job) {
		this(job, null);
	}

	public JobExecution(Long id) {
		super(id);
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setJobInstance(JobInstance jobInstance) {
		this.jobInstance = jobInstance;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
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

	/**
	 * Set the value of the status field.
	 * 
	 * @param status the status to set
	 */
	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	/**
	 * Upgrade the status field if the provided value is greater than the
	 * existing one. Clients using this method to set the status can be sure
	 * that they don't overwrite a failed status with an successful one.
	 * 
	 * @param status the new status value
	 */
	public void upgradeStatus(BatchStatus status) {
		this.status = this.status.upgradeTo(status);
	}

	/**
	 * Convenience getter for for the id of the enclosing job. Useful for DAO
	 * implementations.
	 * 
	 * @return the id of the enclosing job
	 */
	public Long getJobId() {
		if (jobInstance != null) {
			return jobInstance.getId();
		}
		return null;
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
	 * @return the Job that is executing.
	 */
	public JobInstance getJobInstance() {
		return jobInstance;
	}

	/**
	 * Accessor for the step executions.
	 * 
	 * @return the step executions that were registered
	 */
	public Collection<StepExecution> getStepExecutions() {
		return Collections.unmodifiableList(new ArrayList<StepExecution>(stepExecutions));
	}

	/**
	 * Register a step execution with the current job execution.
	 * @param stepName the name of the step the new execution is associated with
	 */
	public StepExecution createStepExecution(String stepName) {
		StepExecution stepExecution = new StepExecution(stepName, this);
		this.stepExecutions.add(stepExecution);
		return stepExecution;
	}

	/**
	 * Test if this {@link JobExecution} indicates that it is running. It should
	 * be noted that this does not necessarily mean that it has been persisted
	 * as such yet.
	 * @return true if the end time is null
	 */
	public boolean isRunning() {
		return endTime == null;
	}

	/**
	 * Test if this {@link JobExecution} indicates that it has been signalled to
	 * stop.
	 * @return true if the status is {@link BatchStatus#STOPPING}
	 */
	public boolean isStopping() {
		return status == BatchStatus.STOPPING;
	}

	/**
	 * Signal the {@link JobExecution} to stop. Iterates through the associated
	 * {@link StepExecution}s, calling {@link StepExecution#setTerminateOnly()}.
	 * 
	 */
	public void stop() {
		for (StepExecution stepExecution : stepExecutions) {
			stepExecution.setTerminateOnly();
		}
		status = BatchStatus.STOPPING;
	}

	/**
	 * Sets the {@link ExecutionContext} for this execution
	 * 
	 * @param executionContext the context
	 */
	public void setExecutionContext(ExecutionContext executionContext) {
		this.executionContext = executionContext;
	}

	/**
	 * Returns the {@link ExecutionContext} for this execution. The content is
	 * expected to be persisted after each step completion (successful or not).
	 * 
	 * @return the context
	 */
	public ExecutionContext getExecutionContext() {
		return executionContext;
	}

	/**
	 * @return the time when this execution was created.
	 */
	public Date getCreateTime() {
		return createTime;
	}

	/**
	 * @param createTime creation time of this execution.
	 */
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	/**
	 * Package private method for re-constituting the step executions from
	 * existing instances.
	 * @param stepExecution
	 */
	void addStepExecution(StepExecution stepExecution) {
		stepExecutions.add(stepExecution);
	}

	/**
	 * Get the date representing the last time this JobExecution was updated in
	 * the JobRepository.
	 * 
	 * @return Date representing the last time this JobExecution was updated.
	 */
	public Date getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * Set the last time this JobExecution was updated.
	 * 
	 * @param lastUpdated
	 */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public List<Throwable> getFailureExceptions() {
		return failureExceptions;
	}

	/**
	 * Add the provided throwable to the failure exception list.
	 * 
	 * @param t
	 */
	public synchronized void addFailureException(Throwable t) {
		this.failureExceptions.add(t);
	}

	/**
	 * Return all failure causing exceptions for this JobExecution, including
	 * step executions.
	 * 
	 * @return List<Throwable> containing all exceptions causing failure for
	 * this JobExecution.
	 */
	public synchronized List<Throwable> getAllFailureExceptions() {

		Set<Throwable> allExceptions = new HashSet<Throwable>(failureExceptions);
		for (StepExecution stepExecution : stepExecutions) {
			allExceptions.addAll(stepExecution.getFailureExceptions());
		}

		return new ArrayList<Throwable>(allExceptions);
	}

	/**
	 * Deserialise and ensure transient fields are re-instantiated when read
	 * back
	 */
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		failureExceptions = new ArrayList<Throwable>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.core.domain.Entity#toString()
	 */
	public String toString() {
		return super.toString()
				+ String.format(", startTime=%s, endTime=%s, lastUpdated=%s, status=%s, exitStatus=%s, job=[%s]",
						startTime, endTime, lastUpdated, status, exitStatus, jobInstance);
	}

	/**
	 * Add some step executions.  For internal use only.
	 * @param stepExecutions step executions to add to the current list
	 */
	public void addStepExecutions(List<StepExecution> stepExecutions) {
		if (stepExecutions!=null) {
			this.stepExecutions.removeAll(stepExecutions);
			this.stepExecutions.addAll(stepExecutions);
		}
	}

}