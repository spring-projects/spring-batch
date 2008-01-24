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

package org.springframework.batch.execution.repository.dao;

import java.util.List;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;

/**
 * Data Access Object for jobs.
 * 
 * @author Lucas Ward
 * 
 */
public interface JobDao {

	/**
	 * Create a job using the provided JobIdentifier as the natural key.
	 * 
	 * PostConditions: A valid job will be returned which has been persisted and
	 * contains an unique Id.
	 * 
	 * @param jobIdentifier
	 * @return Job
	 */
	public JobInstance createJobInstance(String jobName, JobParameters jobParameters);

	/**
	 * Find all jobs that match the given JobIdentifier. If no jobs matching the
	 * Identifier are found, then a list of size 0 will be returned.
	 * 
	 * @param jobIdentifier
	 * @return List of {@link JobInstance} objects matching
	 *         {@link JobIdentifier}
	 */
	public List findJobInstances(String jobName, JobParameters jobParameters);

	/**
	 * Update an existing Job.
	 * 
	 * Preconditions: Job must have an ID.
	 * 
	 * @param job
	 */
	public void update(JobInstance job);

	/**
	 * Save a new JobExecution.
	 * 
	 * Preconditions: JobExecution must have a JobId.
	 * 
	 * @param jobExecution
	 */
	public void save(JobExecution jobExecution);

	/**
	 * Update and existing JobExecution.
	 * 
	 * Preconditions: JobExecution must have an Id (which can be obtained by the
	 * save method) and a JobId.
	 * 
	 * @param jobExecution
	 */
	public void update(JobExecution jobExecution);

	/**
	 * Return the number of JobExecutions with the given Job Id
	 * 
	 * Preconditions: Job must have an id.
	 * 
	 * @param job
	 */
	public int getJobExecutionCount(Long jobId);

	/**
	 * Return list of JobExecutions for given job.
	 * 
	 * @param job
	 * @return list of jobExecutions.
	 */
	public List findJobExecutions(JobInstance job);
}
