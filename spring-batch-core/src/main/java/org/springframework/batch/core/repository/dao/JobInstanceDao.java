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

package org.springframework.batch.core.repository.dao;

import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;

/**
 * Data Access Object for job instances.
 * 
 * @author Lucas Ward
 * @author Robert Kasanicky
 * 
 */
public interface JobInstanceDao {

	/**
	 * Create a JobInstance with given name and parameters.
	 * 
	 * PreConditions: JobInstance for given name and parameters must not already
	 * exist
	 * 
	 * PostConditions: A valid job instance will be returned which has been
	 * persisted and contains an unique Id.
	 * 
	 * @param jobName
	 * @param jobParameters
	 * @return JobInstance
	 */
	JobInstance createJobInstance(String jobName, JobParameters jobParameters);

	/**
	 * Find the job instance that matches the given name and parameters. If no
	 * matching job instances are found, then returns null.
	 * 
	 * @param jobName the name of the job
	 * @param jobParameters the parameters with which the job was executed
	 * @return {@link JobInstance} object matching the job name and
	 * {@link JobParameters} or null
	 */
	JobInstance getJobInstance(String jobName, JobParameters jobParameters);

	/**
	 * Fetch the job instance with the provided identifier.
	 * 
	 * @param instanceId the job identifier
	 * @return the job instance with this identifier or null if it doesn't exist
	 */
	JobInstance getJobInstance(Long instanceId);

	/**
	 * Fetch the JobInstance for the provided JobExecution.
	 * 
	 * @param jobExecution the JobExecution
	 * @return the JobInstance for the provided execution or null if it doesn't exist.
	 */
	JobInstance getJobInstance(JobExecution jobExecution);
	
	/**
	 * Fetch the last job instances with the provided name, sorted backwards by
	 * primary key.
	 * 
	 * 
	 * @param jobName the job name
	 * @param start the start index of the instances to return
	 * @param count the maximum number of objects to return
	 * @return the job instances with this name or empty if none
	 */
	List<JobInstance> getJobInstances(String jobName, int start, int count);

	/**
	 * Retrieve the names of all job instances sorted alphabetically - i.e. jobs
	 * that have ever been executed.
	 * @return the names of all job instances
	 */
	List<String> getJobNames();

}
