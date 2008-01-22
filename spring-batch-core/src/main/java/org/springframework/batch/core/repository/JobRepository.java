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
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobInstanceProperties;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;

/**
 * <p>
 * Repository for storing batch jobs and steps. Before using any methods, a Job
 * must first be obtained using the findOrCreateJob method. Once a Job and it's
 * related steps are obtained, they can be updated. It should be noted that any
 * reconstituted steps are expected to contain restart data <strong>if the
 * RestartPolicy associated with the step returns true, and RestartData exists.</strong>
 * </p>
 * 
 * Once a Job/Steps has been created, Job and Step executions can be created and
 * associated with a job, by setting the JobId and StepId respectively. Once
 * these Id's are set, an execution can be persisted. If the object is in a
 * transient state (i.e. it has no id of it's own) then an ID will be created
 * for that specific execution, and then stored ('saved'). (NOTE: The
 * relationship between a Job/Step and Job/StepExecutions is 1:N) If an ID does
 * exist, then the execution will be stored ('updated').
 * 
 * 
 * @author Lucas Ward
 * 
 */
public interface JobRepository {

	/**
	 * Find or create a job execution for a given {@link JobIdentifier} and configuration.
	 * If the job that is uniquely identified by {@link JobIdentifier} already
	 * exists, its persisted values (including ID) will be returned in a new
	 * {@link JobInstance} object. If no previous run is found, a new job will
	 * be created and returned.
	 * @param jobInstanceProperties TODO
	 * @param jobConfiguration
	 *            describes the configuration for this job
	 * 
	 * @return a valid job execution for the identifier provided
	 * @throws JobExecutionAlreadyRunningException
	 *             if there is a {@link JobExecution} alrady running for the
	 *             job instance that would otherwise be returned
	 * 
	 */
	public JobExecution createJobExecution(Job job,
			JobInstanceProperties jobInstanceProperties)
			throws JobExecutionAlreadyRunningException;

	/**
	 * Update a Job.
	 * 
	 * Preconditions: Job must contain a valid ID. This can be ensured by first
	 * obtaining a job from findOrCreateJob.
	 * 
	 * @param job
	 * @see JobInstance
	 */
	public void update(JobInstance jobInstance);

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
	 * Update a step.
	 * 
	 * Preconditions: {@link StepInstance} must contain a valid ID. This can be
	 * ensured by first obtaining a {@link JobInstance} from findOrCreateJob,
	 * and accessing it's step list.
	 * 
	 * @param step
	 * @see StepInstance
	 */
	public void update(StepInstance stepInstance);

	/**
	 * Save or Update a StepExecution. If no ID is found a new instance will be
	 * created. (saved). If an ID does exist it will be updated. It is not
	 * advisable that an ID be assigned to a JobExecution before calling this
	 * method. Instead, it should be left blank, to be assigned by a
	 * JobRepository.
	 * 
	 * Preconditions: StepExecution must have a valid StepId.
	 * 
	 * @param jobInstance
	 */
	public void saveOrUpdate(StepExecution stepExecution);

}
