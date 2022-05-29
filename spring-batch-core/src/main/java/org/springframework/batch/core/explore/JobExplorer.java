/*
 * Copyright 2006-2019 the original author or authors.
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
package org.springframework.batch.core.explore;

import java.util.List;
import java.util.Set;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.lang.Nullable;

/**
 * Entry point for browsing executions of running or historical jobs and steps. Since the
 * data may be re-hydrated from persistent storage, it may not contain volatile fields
 * that would have been present when the execution was active.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public interface JobExplorer {

	/**
	 * Fetch {@link JobInstance} values in descending order of creation (and therefore
	 * usually of first execution).
	 * @param jobName the name of the job to query
	 * @param start the start index of the instances to return
	 * @param count the maximum number of instances to return
	 * @return the {@link JobInstance} values up to a maximum of count values
	 */
	List<JobInstance> getJobInstances(String jobName, int start, int count);

	/**
	 * Find the last job instance by Id for the given job.
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
	 * Retrieve a {@link JobExecution} by its id. The complete object graph for this
	 * execution should be returned (unless otherwise indicated) including the parent
	 * {@link JobInstance} and associated {@link ExecutionContext} and
	 * {@link StepExecution} instances (also including their execution contexts).
	 * @param executionId the job execution id
	 * @return the {@link JobExecution} with this id, or null if not found
	 */
	@Nullable
	JobExecution getJobExecution(@Nullable Long executionId);

	/**
	 * Retrieve a {@link StepExecution} by its id and parent {@link JobExecution} id. The
	 * execution context for the step should be available in the result, and the parent
	 * job execution should have its primitive properties, but may not contain the job
	 * instance information.
	 * @param jobExecutionId the parent job execution id
	 * @param stepExecutionId the step execution id
	 * @return the {@link StepExecution} with this id, or null if not found
	 *
	 * @see #getJobExecution(Long)
	 */
	@Nullable
	StepExecution getStepExecution(@Nullable Long jobExecutionId, @Nullable Long stepExecutionId);

	/**
	 * @param instanceId {@link Long} id for the jobInstance to obtain.
	 * @return the {@link JobInstance} with this id, or null
	 */
	@Nullable
	JobInstance getJobInstance(@Nullable Long instanceId);

	/**
	 * Retrieve job executions by their job instance. The corresponding step executions
	 * may not be fully hydrated (e.g. their execution context may be missing), depending
	 * on the implementation. Use {@link #getStepExecution(Long, Long)} to hydrate them in
	 * that case.
	 * @param jobInstance the {@link JobInstance} to query
	 * @return the set of all executions for the specified {@link JobInstance}
	 */
	List<JobExecution> getJobExecutions(JobInstance jobInstance);

	/**
	 * Find the last {@link JobExecution} that has been created for a given
	 * {@link JobInstance}.
	 * @param jobInstance the {@link JobInstance}
	 * @return the last {@link JobExecution} that has been created for this instance or
	 * {@code null} if no job execution is found for the given job instance.
	 *
	 * @since 4.2
	 */
	@Nullable
	default JobExecution getLastJobExecution(JobInstance jobInstance) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieve running job executions. The corresponding step executions may not be fully
	 * hydrated (e.g. their execution context may be missing), depending on the
	 * implementation. Use {@link #getStepExecution(Long, Long)} to hydrate them in that
	 * case.
	 * @param jobName the name of the job
	 * @return the set of running executions for jobs with the specified name
	 */
	Set<JobExecution> findRunningJobExecutions(@Nullable String jobName);

	/**
	 * Query the repository for all unique {@link JobInstance} names (sorted
	 * alphabetically).
	 * @return the set of job names that have been executed
	 */
	List<String> getJobNames();

	/**
	 * Fetch {@link JobInstance} values in descending order of creation (and there for
	 * usually of first execution) with a 'like'/wildcard criteria.
	 * @param jobName the name of the job to query for.
	 * @param start the start index of the instances to return.
	 * @param count the maximum number of instances to return.
	 * @return a list of {@link JobInstance} for the job name requested.
	 */
	List<JobInstance> findJobInstancesByJobName(String jobName, int start, int count);

	/**
	 * Query the repository for the number of unique {@link JobInstance}s associated with
	 * the supplied job name.
	 * @param jobName the name of the job to query for
	 * @return the number of {@link JobInstance}s that exist within the associated job
	 * repository
	 * @throws NoSuchJobException thrown when there is no {@link JobInstance} for the
	 * jobName specified.
	 */
	int getJobInstanceCount(@Nullable String jobName) throws NoSuchJobException;

}
