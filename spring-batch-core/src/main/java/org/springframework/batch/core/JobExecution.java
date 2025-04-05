/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.lang.Nullable;

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

	private volatile Collection<StepExecution> stepExecutions = Collections.synchronizedSet(new LinkedHashSet<>());

	private volatile BatchStatus status = BatchStatus.STARTING;

	private volatile LocalDateTime startTime = null;

	private volatile LocalDateTime createTime = LocalDateTime.now();

	private volatile LocalDateTime endTime = null;

	private volatile LocalDateTime lastUpdated = null;

	private volatile ExitStatus exitStatus = ExitStatus.UNKNOWN;

	private volatile ExecutionContext executionContext = new ExecutionContext();

	private transient volatile List<Throwable> failureExceptions = new CopyOnWriteArrayList<>();

	/**
	 * Constructor that sets the state of the instance to the {@link JobExecution}
	 * parameter.
	 * @param original The {@link JobExecution} to be copied.
	 */
	public JobExecution(JobExecution original) {
		this.jobParameters = original.getJobParameters();
		this.jobInstance = original.getJobInstance();
		this.stepExecutions = original.getStepExecutions();
		this.status = original.getStatus();
		this.startTime = original.getStartTime();
		this.createTime = original.getCreateTime();
		this.endTime = original.getEndTime();
		this.lastUpdated = original.getLastUpdated();
		this.exitStatus = original.getExitStatus();
		this.executionContext = original.getExecutionContext();
		this.failureExceptions = original.getFailureExceptions();
		this.setId(original.getId());
		this.setVersion(original.getVersion());
	}

	/**
	 * Because a JobExecution is not valid unless the job is set, this constructor is the
	 * only valid one from a modeling point of view.
	 * @param job The job of which this execution is a part.
	 * @param id A {@link Long} that represents the {@code id} for the
	 * {@code JobExecution}.
	 * @param jobParameters A {@link JobParameters} instance for this
	 * {@code JobExecution}.
	 */
	public JobExecution(JobInstance job, Long id, @Nullable JobParameters jobParameters) {
		super(id);
		this.jobInstance = job;
		this.jobParameters = jobParameters == null ? new JobParameters() : jobParameters;
	}

	/**
	 * Constructor for transient (unsaved) instances.
	 * @param job The enclosing {@link JobInstance}.
	 * @param jobParameters The {@link JobParameters} instance for this
	 * {@code JobExecution}.
	 */
	public JobExecution(JobInstance job, JobParameters jobParameters) {
		this(job, null, jobParameters);
	}

	/**
	 * Constructor that accepts the job execution {@code id} and {@link JobParameters}.
	 * @param id The job execution {@code id}.
	 * @param jobParameters The {@link JobParameters} for the {@link JobExecution}.
	 */
	public JobExecution(Long id, JobParameters jobParameters) {
		this(null, id, jobParameters);
	}

	/**
	 * Constructor that accepts the job execution {@code id}.
	 * @param id The job execution {@code id}.
	 */
	public JobExecution(Long id) {
		this(null, id, null);
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
	@Nullable
	public LocalDateTime getEndTime() {
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
	@Nullable
	public LocalDateTime getStartTime() {
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
	 * Convenience getter for the {@code id} of the enclosing job. Useful for DAO
	 * implementations.
	 * @return the {@code id} of the enclosing job.
	 */
	public Long getJobId() {
		if (jobInstance != null) {
			return jobInstance.getId();
		}
		return null;
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
	 * @return the Job that is executing.
	 */
	public JobInstance getJobInstance() {
		return jobInstance;
	}

	/**
	 * Accessor for the step executions.
	 * @return the step executions that were registered.
	 */
	public Collection<StepExecution> getStepExecutions() {
		return List.copyOf(stepExecutions);
	}

	/**
	 * Register a step execution with the current job execution.
	 * @param stepName the name of the step the new execution is associated with.
	 * @return an empty {@link StepExecution} associated with this {@code JobExecution}.
	 */
	public StepExecution createStepExecution(String stepName) {
		StepExecution stepExecution = new StepExecution(stepName, this);
		this.stepExecutions.add(stepExecution);
		return stepExecution;
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
	 * Package-private method for re-constituting the step executions from existing
	 * instances.
	 * @param stepExecution The {@code stepExecution} execution to be added.
	 */
	void addStepExecution(StepExecution stepExecution) {
		stepExecutions.add(stepExecution);
	}

	/**
	 * Get the date representing the last time this {@code JobExecution} was updated in
	 * the {@link org.springframework.batch.core.repository.JobRepository}.
	 * @return a {@link LocalDateTime} object representing the last time this
	 * {@code JobExecution} was updated.
	 */
	@Nullable
	public LocalDateTime getLastUpdated() {
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

	/**
	 * Deserialize and ensure transient fields are re-instantiated when read back.
	 * @param stream instance of {@link ObjectInputStream}.
	 * @throws IOException if an error occurs during read.
	 * @throws ClassNotFoundException thrown if the class is not found.
	 */
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		failureExceptions = new ArrayList<>();
	}

	@Override
	public String toString() {
		return super.toString() + String.format(
				", startTime=%s, endTime=%s, lastUpdated=%s, status=%s, exitStatus=%s, job=[%s], jobParameters=[%s]",
				startTime, endTime, lastUpdated, status, exitStatus, jobInstance, jobParameters);
	}

	/**
	 * Add some step executions. For internal use only.
	 * @param stepExecutions The step executions to add to the current list.
	 */
	public void addStepExecutions(List<StepExecution> stepExecutions) {
		if (stepExecutions != null) {
			this.stepExecutions.removeAll(stepExecutions);
			this.stepExecutions.addAll(stepExecutions);
		}
	}

}
