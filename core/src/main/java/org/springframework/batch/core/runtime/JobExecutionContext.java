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
package org.springframework.batch.core.runtime;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Context for an executing job. Maintains invariants and provides communication
 * channel for all components requiring information about the job and its steps.
 * 
 * @author Dave Syer
 * 
 */
public class JobExecutionContext {

	private JobIdentifier jobIdentifier;
	
	private final JobInstance job;
	
	private final JobExecution jobExecution;
	
	private Collection stepExecutions = new HashSet();

	private Collection stepContexts = new HashSet();

	private Collection chunkContexts = new HashSet();

	/**
	 * Constructor with all the mandatory properties.
	 * 
	 * @param jobIdentifier
	 */
	public JobExecutionContext(JobIdentifier jobIdentifier, JobInstance job) {
		super();
		this.jobIdentifier = jobIdentifier;
		this.job = job;
		this.jobExecution = new JobExecution(job.getId());
		this.jobExecution.setStartTime(new Timestamp(System.currentTimeMillis()));
	}

	/**
	 * Accessor for the potentially multiple chunk contexts that are in
	 * progress. In a single-threaded, sequential execution there would normally
	 * be only one current chunk, but in more complicated scenarios there might
	 * be multiple active contexts.
	 * @return all the chunk contexts that have been registered and not
	 * unregistered. A collection opf {@link RepeatContext} objects.
	 */
	public Collection getChunkContexts() {
		synchronized (chunkContexts) {
			return new HashSet(chunkContexts);
		}
	}

	/**
	 * Accessor for the runtime information of this execution.
	 * @return the {@link JobRuntimeInformation} that was used to start this job
	 * execution.
	 */
	public JobIdentifier getJobIdentifier() {
		return jobIdentifier;
	}

	/**
	 * Accessor for the potentially multiple step contexts that are in progress.
	 * In a single-threaded, sequential execution there would normally be only
	 * one current step, but in more complicated scenarios there might be
	 * multiple active contexts.
	 * @return all the step contexts that have been registered and not
	 * unregistered. A collection of {@link RepeatContext} objects.
	 */
	public Collection getStepContexts() {
		synchronized (stepContexts) {
			return new HashSet(stepContexts);
		}
	}

	/**
	 * Called at the start of a step, before any business logic is processed.
	 * @param context the current step context.
	 */
	public void registerStepContext(RepeatContext stepContext) {
		synchronized (stepContexts) {
			this.stepContexts.add(stepContext);
		}
	}

	/**
	 * Called at the end of a step, after all business logic is processed, or in
	 * the case of a failure.
	 * @param context the current step context.
	 */
	public void unregisterStepContext(RepeatContext stepContext) {
		synchronized (stepContexts) {
			this.stepContexts.remove(stepContext);
		}
	}

	/**
	 * Called at the start of a chunk, before any business logic is processed.
	 * @param context the current chunk context.
	 */
	public void registerChunkContext(RepeatContext chunkContext) {
		synchronized (chunkContexts) {
			this.chunkContexts.add(chunkContext);
		}
	}

	/**
	 * Called at the end of a chunk, after all business logic is processed, or
	 * in the case of a failure.
	 * @param context the current chunk context.
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
	 * @return the current job execution.
	 */
	public JobExecution getJobExecution() {
		return jobExecution;
	}

	/**
	 * Accessor for the step executions. 
	 * @return the step executions that were registered
	 */
	public Collection getStepExecutions() {
		return stepExecutions;
	}

	/**
	 * Register a step execution with the current job execution.
	 * @param stepExecution
	 */
	public void registerStepExecution(StepExecution stepExecution) {
		this.stepExecutions.add(stepExecution);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof JobExecutionContext)) {
			return super.equals(obj);
		}
		JobExecutionContext other = (JobExecutionContext) obj;
		return job.equals(other.getJob()) && jobExecution.equals(other.getJobExecution());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return 23*job.hashCode() + 61*jobExecution.hashCode();
	}


	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "identifier=" + jobIdentifier + "; steps=" + stepContexts + "; chunks=" + chunkContexts;
	}

}
