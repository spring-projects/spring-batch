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

package org.springframework.batch.core.repository;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.item.ExecutionContext;

/**
 * <p>
 * Repository for storing batch jobs and steps. Before using any methods, a Job
 * must first be obtained using the findOrCreateJob method. Once a Job and it's
 * related steps are obtained, they can be updated. It should be noted that any
 * reconstituted steps are expected to contain restart data <strong>if the step
 * says it wants to be restored after a restart, and {@link ExecutionContext}
 * exists.</strong>
 * </p>
 * 
 * Once a Job/Steps has been created, Job and Step executions can be created and
 * associated with a job, by setting the JobId and StepId respectively. Once
 * these Id's are set, an execution can be persisted. If the object is in a
 * transient state (i.e. it has no id of it's own) then an ID will be created
 * for that specific execution, and then stored ('saved'). (NOTE: The
 * relationship between a Job/Step and Job/StepExecutions is 1:N.) If an ID does
 * exist, then the execution will be stored ('updated').
 * 
 * 
 * @author Lucas Ward
 * 
 */
public interface JobRepository {

	/**
	 * Find or create a job execution for a given {@link JobIdentifier} and
	 * configuration. If the job that is uniquely identified by
	 * {@link JobIdentifier} already exists, its persisted values (including ID)
	 * will be returned in a new {@link JobInstance} object. If no previous run
	 * is found, a new job will be created and returned.
	 * @param jobParameters the runtime parameters for the job
	 * @param jobConfiguration describes the configuration for this job
	 * 
	 * @return a valid job execution for the identifier provided
	 * @throws JobExecutionAlreadyRunningException if there is a
	 * {@link JobExecution} alrady running for the job instance that would
	 * otherwise be returned
	 * @throws JobRestartException if more than one JobInstance if found or if
	 * JobInstance.getJobExecutionCount() is greater than Job.getStartLimit()
	 * 
	 */
	public JobExecution createJobExecution(Job job, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException;

	/**
	 * Save or Update a {@link JobExecution}. If no ID is found a new instance
	 * will be saved. If an ID does exist it will be updated. The ID should only
	 * be assigned to a {@link JobExecution} by calling this method - it should
	 * be left blank on the first call, and assigned by the
	 * {@link JobRepository}.
	 * 
	 * Preconditions: {@link JobExecution} must contain a valid
	 * {@link JobInstance}.
	 * 
	 * @param jobInstance
	 */
	public void saveOrUpdate(JobExecution jobExecution);

	/**
	 * Save or update a {@link StepExecution}. If no ID is found a new instance
	 * will be created (and saved). If an ID does exist it will be updated. It
	 * is not advisable that an ID be assigned before calling this method.
	 * Instead, it should be left blank, to be assigned by a
	 * {@link JobRepository}. The {@link ExecutionContext} of the
	 * {@link StepExecution} is <em>not</em> saved: see
	 * {@link #saveExecutionContext(StepExecution)}.
	 * 
	 * Preconditions: {@link StepExecution} must have a valid {@link Step}.
	 * 
	 * @param jobInstance
	 */
	public void saveOrUpdate(StepExecution stepExecution);

	/**
	 * Save the {@link ExecutionContext} of the given {@link StepExecution}.
	 * Implementations are allowed to ensure that the {@link StepExecution} is
	 * already saved by calling {@link #saveOrUpdate(StepExecution)} before
	 * saving the {@link ExecutionContext}.
	 * 
	 * @param stepExecution the {@link StepExecution} containing the
	 * {@link ExecutionContext} to be saved.
	 */
	void saveOrUpdateExecutionContext(StepExecution stepExecution);

	/**
	 * @return the last execution of step for the given job instance.
	 */
	public StepExecution getLastStepExecution(JobInstance jobInstance, Step step);

	/**
	 * @return the execution count of the step within the given job instance.
	 */
	public int getStepExecutionCount(JobInstance jobInstance, Step step);

}
