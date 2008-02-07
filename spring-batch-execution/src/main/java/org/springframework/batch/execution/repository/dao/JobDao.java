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
import org.springframework.dao.IncorrectResultSizeDataAccessException;

/**
 * Data Access Object for jobs.
 * 
 * @author Lucas Ward
 * 
 */
public interface JobDao {

	/**
	 * Create a JobInstance with given name and parameters.
	 * 
	 * PostConditions: A valid job will be returned which has been persisted and
	 * contains an unique Id.
	 * 
	 * @param jobName
	 * @param jobParameters
	 * @return JobInstance
	 */
	JobInstance createJobInstance(String jobName, JobParameters jobParameters);

	/**
	 * Find all job instances that match the given name and parameters. If no
	 * matching job instances are found, then a list of size 0 will be
	 * returned.
	 * 
	 * @param jobName
	 * @param jobParameters
	 * @return List of {@link JobInstance} objects matching
	 * {@link JobIdentifier}
	 */
	List findJobInstances(String jobName, JobParameters jobParameters);

	/**
	 * Update an existing JobInstance.
	 * 
	 * Preconditions: jobInstance must have an ID.
	 * 
	 * @param jobInstance
	 */
	void updateJobInstance(JobInstance jobInstance);

	/**
	 * Save a new JobExecution.
	 * 
	 * Preconditions: jobExecution must have a jobInstanceId.
	 * 
	 * @param jobExecution
	 */
	void saveJobExecution(JobExecution jobExecution);

	/**
	 * Update and existing JobExecution.
	 * 
	 * Preconditions: jobExecution must have an Id (which can be obtained by the
	 * save method) and a jobInstanceId.
	 * 
	 * @param jobExecution
	 */
	void updateJobExecution(JobExecution jobExecution);

	/**
	 * Return the number of JobExecutions with the given jobInstanceId
	 * 
	 * Preconditions: jobInstance must have an id.
	 * 
	 * @param jobInstanceId
	 */
	int getJobExecutionCount(Long jobInstanceId);

	/**
	 * Return list of JobExecutions for given JobInstance.
	 * 
	 * @param jobInstance
	 * @return list of jobExecutions.
	 */
	List findJobExecutions(JobInstance jobInstance);

	/**
	 * Given an id, return the matching JobExecution.
	 * 
	 * @param jobExecutionId - id of the execution to be returned.
	 * @return {@link JobExecution} matching the id.
	 * @throws {@link IncorrectResultSizeDataAccessException} if more than one
	 * execution is found for the given id.
	 */
	JobExecution getJobExecution(Long jobExecutionId);
}
