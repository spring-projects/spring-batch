/*
 * Copyright 2006-2018 the original author or authors.
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

package org.springframework.batch.core.repository.dao;

import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.lang.Nullable;

/**
 * Data Access Object for job instances.
 *
 * @author Lucas Ward
 * @author Robert Kasanicky
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
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
	 * @param jobName {@link String} containing the name of the job.
	 * @param jobParameters {@link JobParameters} containing the parameters for
	 * the JobInstance.
	 * @return JobInstance {@link JobInstance} instance that was created.
	 */
	JobInstance createJobInstance(String jobName, JobParameters jobParameters);

	/**
	 * Find the job instance that matches the given name and parameters. If no
	 * matching job instances are found, then returns null.
	 *
	 * @param jobName the name of the job
	 * @param jobParameters the parameters with which the job was executed
	 * @return {@link JobInstance} object matching the job name and
	 * {@link JobParameters} or {@code null}
	 */
	@Nullable
	JobInstance getJobInstance(String jobName, JobParameters jobParameters);

	/**
	 * Fetch the job instance with the provided identifier.
	 *
	 * @param instanceId the job identifier
	 * @return the job instance with this identifier or {@code null} if it doesn't exist
	 */
	@Nullable
	JobInstance getJobInstance(@Nullable Long instanceId);

	/**
	 * Fetch the JobInstance for the provided JobExecution.
	 *
	 * @param jobExecution the JobExecution
	 * @return the JobInstance for the provided execution or {@code null} if it doesn't exist.
	 */
	@Nullable
	JobInstance getJobInstance(JobExecution jobExecution);

	/**
	 * Fetch the last job instances with the provided name, sorted backwards by
	 * primary key.
	 *
	 * if using the JdbcJobInstance, you can provide the jobName with a wildcard
	 * (e.g. *Job) to return 'like' job names. (e.g. *Job will return 'someJob' 
	 * and 'otherJob')
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
	 *
	 * @return the names of all job instances
	 */
	List<String> getJobNames();
	
	/**
	 * Fetch the last job instances with the provided name, sorted backwards by
	 * primary key, using a 'like' criteria
	 * 
	 * @param jobName {@link String} containing the name of the job.
	 * @param start int containing the offset of where list of job instances
	 * results should begin.
	 * @param count int containing the number of job instances to return.
	 * @return a list of {@link JobInstance} for the job name requested.
	 */
	List<JobInstance> findJobInstancesByName(String jobName, int start, int count);


	/**
	 * Query the repository for the number of unique {@link JobInstance}s
	 * associated with the supplied job name.
	 *
	 * @param jobName the name of the job to query for
	 * @return the number of {@link JobInstance}s that exist within the
	 * associated job repository
	 *
	 * @throws NoSuchJobException thrown if no Job has the jobName specified.
	 */
	int getJobInstanceCount(@Nullable String jobName) throws NoSuchJobException;

}
