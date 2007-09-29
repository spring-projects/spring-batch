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

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.runtime.JobIdentifier;

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
	 * Find or create a job for a given Job identifier or configuration. If the
	 * job that is uniquely identified by JobIdentifier already exists, it's
	 * persisted values (including ID) will be returned in a new Job object. If
	 * no previous run is found, a new job will be created and returned.
	 * 
	 * @param jobConfiguration
	 *            describes the configuration for this job
	 * @param jobIdentifier
	 *            identifies this particular run of the configuration across
	 *            possible restarts
	 * 
	 * @return a valid job
	 * 
	 */
	public JobInstance findOrCreateJob(JobConfiguration jobConfiguration,
			JobIdentifier jobIdentifier);

	/**
	 * Update a Job.
	 * 
	 * Preconditions: Job must contain a valid ID. This can be ensured by first
	 * obtaining a job from findOrCreateJob.
	 * 
	 * @param job
	 * @see JobInstance
	 */
	public void update(JobInstance job);

	/**
	 * Save or Update a JobExecution. If no ID is found a new instance will be
	 * created. (saved). If an ID does exist it will be updated. It is not
	 * advisable that an ID be assigned to a JobExecution before calling this
	 * method. Instead, it should be left blank, to be assigned by a
	 * JobRepository.
	 * 
	 * Preconditions: JobExecution must contain a valid JobId.
	 * 
	 * @param jobInstance
	 */
	public void saveOrUpdate(JobExecution jobExecution);

	/**
	 * Update a step.
	 * 
	 * Preconditions: Step must contain a valid ID. This can be ensured by first
	 * obtaining a Job from findOrCreateJob, and accessing it's step list.
	 * 
	 * @param step
	 * @see StepInstance
	 */
	public void update(StepInstance step);

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
