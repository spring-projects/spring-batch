/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.core.job;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Entity;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;

/**
 * Batch domain object representing the execution of a job.
 *
 * @author Lucas Ward
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Dimitrios Liapis
 * @author Taeik Lim
 *
 */
public class JobExecution extends Entity {

	private final JobParameters jobParameters;

	private JobInstance jobInstance;

	private final List<StepExecution> stepExecutions = Collections.synchronizedList(new LinkedList<>());

	private BatchStatus status = BatchStatus.STARTING;

	private LocalDateTime createTime = LocalDateTime.now();

	private @Nullable LocalDateTime startTime = null;

	private @Nullable LocalDateTime endTime = null;

	private @Nullable LocalDateTime lastUpdated = null;

	private ExitStatus exitStatus = ExitStatus.UNKNOWN;

	private ExecutionContext executionContext = new ExecutionContext();

	private final List<Throwable> failureExceptions = new CopyOnWriteArrayList<>();

	/**
	 * Create a new {@link JobExecution} instance. Because a JobExecution is not valid
	 * unless the job instance is set, this constructor is the only valid one from a
	 * modeling point of view.
	 * @param jobInstance The job instance of which this execution is a part.
	 * @param id of the {@code JobExecution}.
	 * @param jobParameters A {@link JobParameters} instance for this
	 * {@code JobExecution}.
	 */
	// TODO add execution context parameter
	public JobExecution(long id, JobInstance jobInstance, JobParameters jobParameters) {
		super(id);
		this.jobInstance = jobInstance;
		this.jobParameters = jobParameters;
	}

	/**
	 * @return The current {@link JobParameters}.
	 */
	public JobParameters getJobParameters() {
		return this.jobParameters;
	}

	/**
	 * @return The current end time.
	 */
	@Nullable public LocalDateTime getEndTime() {
		return endTime;
	}

	/**
	 * Set the {@link JobInstance} used by the {@link JobExecution}.
	 * @param jobInstance The {@link JobInstance} used by the {@link JobExecution}.
	 */
	public void setJobInstance(JobInstance jobInstance) {
		this.jobInstance = jobInstance;
	}

	/**
	 * Set the end time.
	 * @param endTime The {@link LocalDateTime} to be used for the end time.
	 */
	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	/**
	 * @return The current start time.
	 */
	@Nullable public LocalDateTime getStartTime() {
		return startTime;
	}

	/**
	 * Set the start time.
	 * @param startTime The {@link LocalDateTime} to be used for the start time.
	 */
	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return The current {@link BatchStatus}.
	 */
	public BatchStatus getStatus() {
		return status;
	}

	/**
	 * Set the value of the {@code status} field.
	 * @param status The status to set.
	 */
	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	/**
	 * Upgrade the {@code status} field if the provided value is greater than the existing
	 * one. Clients using this method to set the status can be sure to not overwrite a
	 * failed status with a successful one.
	 * @param status The new status value.
	 */
	public void upgradeStatus(BatchStatus status) {
		this.status = this.status.upgradeTo(status);
	}

	/**
	 * Convenience getter for the {@code id} of the enclosing job instance. Useful for DAO
	 * implementations.
	 * @return the {@code id} of the enclosing job instance.
	 */
	// TODO why is that needed for DAO implementations? should not be needed with the new
	// model
	public long getJobInstanceId() {
		return this.jobInstance.getId();
	}

	/**
	 * @param exitStatus The {@link ExitStatus} instance to be used for job execution.
	 */
	public void setExitStatus(ExitStatus exitStatus) {
		this.exitStatus = exitStatus;
	}

	/**
	 * @return the {@code exitStatus}.
	 */
	public ExitStatus getExitStatus() {
		return exitStatus;
	}

	/**
	 * @return the Job instance that is executing.
	 */
	public JobInstance getJobInstance() {
		return this.jobInstance;
	}

	/**
	 * Accessor for the step executions.
	 * @return the step executions that were registered.
	 */
	public Collection<StepExecution> getStepExecutions() {
		return List.copyOf(this.stepExecutions);
	}

	/**
	 * Test if this {@link JobExecution} indicates that it is running. Note that this does
	 * not necessarily mean that it has been persisted.
	 * @return {@code true} if the status is one of the running statuses.
	 * @see BatchStatus#isRunning()
	 */
	public boolean isRunning() {
		return status.isRunning();
	}

	/**
	 * Test if this {@link JobExecution} indicates that it has been signalled to stop.
	 * @return {@code true} if the status is {@link BatchStatus#STOPPING}.
	 */
	public boolean isStopping() {
		return status == BatchStatus.STOPPING;
	}

	/**
	 * Sets the {@link ExecutionContext} for this execution.
	 * @param executionContext The context.
	 */
	public void setExecutionContext(ExecutionContext executionContext) {
		this.executionContext = executionContext;
	}

	/**
	 * Returns the {@link ExecutionContext} for this execution. The content is expected to
	 * be persisted after each step completion (successful or not).
	 * @return The {@link ExecutionContext}.
	 */
	public ExecutionContext getExecutionContext() {
		return executionContext;
	}

	/**
	 * @return the time when this execution was created.
	 */
	public LocalDateTime getCreateTime() {
		return createTime;
	}

	/**
	 * @param createTime The creation time of this execution.
	 */
	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

	/**
	 * Add a step execution.
	 * @param stepExecution The {@code stepExecution} execution to be added.
	 */
	public void addStepExecution(StepExecution stepExecution) {
		this.stepExecutions.add(stepExecution);
	}

	/**
	 * Add some step executions.
	 * @param stepExecutions The step executions to add to the current list.
	 */
	public void addStepExecutions(List<StepExecution> stepExecutions) {
		this.stepExecutions.addAll(stepExecutions);
	}

	/**
	 * Get the date representing the last time this {@code JobExecution} was updated in
	 * the {@link org.springframework.batch.core.repository.JobRepository}.
	 * @return a {@link LocalDateTime} object representing the last time this
	 * {@code JobExecution} was updated.
	 */
	@Nullable public LocalDateTime getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * Set the last time this {@code JobExecution} was updated.
	 * @param lastUpdated The {@link LocalDateTime} instance to which to set the job
	 * execution's {@code lastUpdated} attribute.
	 */
	public void setLastUpdated(LocalDateTime lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	/**
	 * Retrieve a list of exceptions.
	 * @return the {@link List} of {@link Throwable} objects.
	 */
	public List<Throwable> getFailureExceptions() {
		return failureExceptions;
	}

	/**
	 * Add the provided throwable to the failure exception list.
	 * @param t A {@link Throwable} instance to be added failure exception list.
	 */
	public synchronized void addFailureException(Throwable t) {
		this.failureExceptions.add(t);
	}

	/**
	 * Return all failure causing exceptions for this {@code JobExecution}, including step
	 * executions.
	 * @return a {@code List<Throwable>} containing all exceptions causing failure for
	 * this {@code JobExecution}.
	 */
	public synchronized List<Throwable> getAllFailureExceptions() {

		Set<Throwable> allExceptions = new HashSet<>(failureExceptions);
		for (StepExecution stepExecution : stepExecutions) {
			allExceptions.addAll(stepExecution.getFailureExceptions());
		}

		return new ArrayList<>(allExceptions);
	}

	@Override
	public String toString() {
		return super.toString() + String.format(
				", startTime=%s, endTime=%s, lastUpdated=%s, status=%s, exitStatus=%s, job=[%s], jobParameters=[%s]",
				startTime, endTime, lastUpdated, status, exitStatus, jobInstance, jobParameters);
	}

}
