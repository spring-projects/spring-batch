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
import java.util.Collection;
import java.util.HashSet;

import org.springframework.batch.core.runtime.JobIdentifier;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Batch domain object representing the execution of a job.
 * 
 * @author Lucas Ward
 * 
 */
public class JobExecution extends Entity {

	private JobInstance job;

	private transient Collection stepExecutions = new HashSet();

	private transient Collection stepContexts = new HashSet();

	private transient Collection chunkContexts = new HashSet();

	private BatchStatus status = BatchStatus.STARTING;

	private Timestamp startTime = new Timestamp(System.currentTimeMillis());

	private Timestamp endTime = null;

	private ExitStatus exitStatus = ExitStatus.UNKNOWN;

	// Package private constructor for Hibernate
	JobExecution() {
	}

	/**
	 * Because a JobExecution isn't valid unless the job is set, this
	 * constructor is the only valid one from a modelling point of view.
	 * 
	 * @param job
	 *            the job of which this execution is a part
	 */
	public JobExecution(JobInstance job, Long id) {
		this.job = job;
		setId(id);
	}
	
	/**
	 * Constructor for transient (unsaved) instances.
	 * 
	 * @param job the enclosing {@link JobInstance}
	 */
	public JobExecution(JobInstance job) {
		this(job, null);
	}

	public Timestamp getEndTime() {
		return endTime;
	}

	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
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

	/**
	 * Convenience getter for for the id of the enclosing job. Useful for DAO
	 * implementations.
	 * 
	 * @return the id of the enclosing job
	 */
	public Long getJobId() {
		if (job != null) {
			return job.getId();
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
	 * Accessor for the potentially multiple chunk contexts that are in
	 * progress. In a single-threaded, sequential execution there would normally
	 * be only one current chunk, but in more complicated scenarios there might
	 * be multiple active contexts.
	 * 
	 * @return all the chunk contexts that have been registered and not
	 *         unregistered. A collection of {@link RepeatContext} objects.
	 */
	public Collection getChunkContexts() {
		synchronized (chunkContexts) {
			return new HashSet(chunkContexts);
		}
	}

	/**
	 * Accessor for the runtime information of this execution.
	 * 
	 * @return the {@link JobRuntimeInformation} that was used to start this job
	 *         execution.
	 */
	public JobIdentifier getJobIdentifier() {
		return job.getIdentifier();
	}

	/**
	 * Accessor for the potentially multiple step contexts that are in progress.
	 * In a single-threaded, sequential execution there would normally be only
	 * one current step, but in more complicated scenarios there might be
	 * multiple active contexts.
	 * 
	 * @return all the step contexts that have been registered and not
	 *         unregistered. A collection of {@link RepeatContext} objects.
	 */
	public Collection getStepContexts() {
		synchronized (stepContexts) {
			return new HashSet(stepContexts);
		}
	}

	/**
	 * Called at the start of a step, before any business logic is processed.
	 * 
	 * @param context
	 *            the current step context.
	 */
	public void registerStepContext(RepeatContext stepContext) {
		synchronized (stepContexts) {
			this.stepContexts.add(stepContext);
		}
	}

	/**
	 * Called at the end of a step, after all business logic is processed, or in
	 * the case of a failure.
	 * 
	 * @param context
	 *            the current step context.
	 */
	public void unregisterStepContext(RepeatContext stepContext) {
		synchronized (stepContexts) {
			this.stepContexts.remove(stepContext);
		}
	}

	/**
	 * Called at the start of a chunk, before any business logic is processed.
	 * 
	 * @param context
	 *            the current chunk context.
	 */
	public void registerChunkContext(RepeatContext chunkContext) {
		synchronized (chunkContexts) {
			this.chunkContexts.add(chunkContext);
		}
	}

	/**
	 * Called at the end of a chunk, after all business logic is processed, or
	 * in the case of a failure.
	 * 
	 * @param context
	 *            the current chunk context.
	 */
	public void unregisterChunkContext(RepeatContext chunkContext) {
		synchronized (chunkContexts) {
			this.chunkContexts.remove(chunkContext);
		}
	}

	/**
	 * @return the Job that is executing.
	 */
	public JobInstance getJob() {
		return job;
	}

	/**
	 * Accessor for the step executions.
	 * 
	 * @return the step executions that were registered
	 */
	public Collection getStepExecutions() {
		return stepExecutions;
	}

	/**
	 * Register a step execution with the current job execution.
	 * 
	 * @param stepExecution
	 */
	public void registerStepExecution(StepExecution stepExecution) {
		this.stepExecutions.add(stepExecution);
	}
}
