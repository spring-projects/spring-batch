/*
 * Copyright 2006-2025 the original author or authors.
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

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
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
	 * <p>
	 * PreConditions: JobInstance for given name and parameters must not already exist
	 * <p>
	 * PostConditions: A valid job instance will be returned which has been persisted and
	 * contains an unique Id.
	 * @param jobName {@link String} containing the name of the job.
	 * @param jobParameters {@link JobParameters} containing the parameters for the
	 * JobInstance.
	 * @return JobInstance {@link JobInstance} instance that was created.
	 */
	JobInstance createJobInstance(String jobName, JobParameters jobParameters);

	/**
	 * Find the job instance that matches the given name and parameters. If no matching
	 * job instances are found, then returns null.
	 * @param jobName the name of the job
	 * @param jobParameters the parameters with which the job was executed
	 * @return {@link JobInstance} object matching the job name and {@link JobParameters}
	 * or {@code null}
	 */
	@Nullable
	JobInstance getJobInstance(String jobName, JobParameters jobParameters);

	/**
	 * Fetch the job instance with the provided identifier.
	 * @param instanceId the job identifier
	 * @return the job instance with this identifier or {@code null} if it doesn't exist
	 */
	@Nullable
	JobInstance getJobInstance(long instanceId);

	/**
	 * Fetch the JobInstance for the provided JobExecution.
	 * @param jobExecution the JobExecution
	 * @return the JobInstance for the provided execution or {@code null} if it doesn't
	 * exist.
	 */
	@Nullable
	// TODO what is the added value of this? client should call
	// jobExecution.getJobInstance()
	JobInstance getJobInstance(JobExecution jobExecution);

	/**
	 * Fetch the last job instances with the provided name, sorted backwards by primary
	 * key.
	 * <p>
	 * if using the JdbcJobInstance, you can provide the jobName with a wildcard (e.g.
	 * *Job) to return 'like' job names. (e.g. *Job will return 'someJob' and 'otherJob')
	 * @param jobName the job name
	 * @param start the start index of the instances to return
	 * @param count the maximum number of objects to return
	 * @return the job instances with this name or empty if none
	 */
	List<JobInstance> getJobInstances(String jobName, int start, int count);

	/**
	 * Fetch all job instances for the given job name.
	 * @param jobName the job name
	 * @return the job instances for the given name empty if none
	 * @since 6.0
	 */
	default List<JobInstance> getJobInstances(String jobName) {
		return getJobInstanceIds(jobName).stream().map(jobInstanceId -> getJobInstance(jobInstanceId)).toList();
	}

	/**
	 * Fetch the last job instance by Id for the given job.
	 * @param jobName name of the job
	 * @return the last job instance by Id if any or null otherwise
	 *
	 * @since 4.2
	 */
	@Nullable
	default JobInstance getLastJobInstance(String jobName) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Fetch all job instance ids for the given job name.
	 * @param jobName name of the job
	 * @return the list of job instance ids, or an empty list if none
	 * @since 6.0
	 */
	default List<Long> getJobInstanceIds(String jobName) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieve the names of all job instances sorted alphabetically - i.e. jobs that have
	 * ever been executed.
	 * @return the names of all job instances
	 */
	// FIXME javadoc: i.e. jobs that have * ever been executed ?
	List<String> getJobNames();

	/**
	 * Fetch the last job instances with the provided name, sorted backwards by primary
	 * key, using a 'like' criteria
	 * @param jobName {@link String} containing the name of the job.
	 * @param start int containing the offset of where list of job instances results
	 * should begin.
	 * @param count int containing the number of job instances to return.
	 * @return a list of {@link JobInstance} for the job name requested.
	 * @deprecated Since v6.0 and scheduled for removal in v6.2. Use
	 * {@link #getJobInstances(String)}
	 */
	@Deprecated(forRemoval = true)
	List<JobInstance> findJobInstancesByName(String jobName, int start, int count);

	/**
	 * Query the repository for the number of unique {@link JobInstance}s associated with
	 * the supplied job name.
	 * @param jobName the name of the job to query for
	 * @return the number of {@link JobInstance}s that exist within the associated job
	 * repository
	 * @throws NoSuchJobException thrown if no Job has the jobName specified.
	 */
	long getJobInstanceCount(String jobName) throws NoSuchJobException;

	/**
	 * Delete the job instance. This method is not expected to delete the associated job
	 * executions. If this is needed, clients of this method should do that manually.
	 * @param jobInstance the job instance to delete
	 * @since 5.0
	 */
	default void deleteJobInstance(JobInstance jobInstance) {
		throw new UnsupportedOperationException();
	}

}
