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
 * Entry point for browsing the executions of running or historical jobs and steps. Since the
 * data may be re-hydrated from persistent storage, it cannot contain volatile fields
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
	 * Fetch {@link JobInstance} values in descending order of creation (and, therefore,
	 * usually, of first execution).
	 * @param jobName The name of the job to query.
	 * @param start The start index of the instances to return.
	 * @param count The maximum number of instances to return.
	 * @return the {@link JobInstance} values up to a maximum of count values.
	 */
	List<JobInstance> getJobInstances(String jobName, int start, int count);

	/**
	 * Find the last job instance, by ID, for the given job.
	 * @param jobName The name of the job.
	 * @return the last job instance by Id if any or {@code null} otherwise.
	 *
	 * @since 4.2
	 */
	@Nullable
	default JobInstance getLastJobInstance(String jobName) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieve a {@link JobExecution} by its ID. The complete object graph for this
	 * execution should be returned (unless otherwise indicated), including the parent
	 * {@link JobInstance} and associated {@link ExecutionContext} and
	 * {@link StepExecution} instances (also including their execution contexts).
	 * @param executionId The job execution ID.
	 * @return the {@link JobExecution} that has this ID or {@code null} if not found.
	 */
	@Nullable
	JobExecution getJobExecution(@Nullable Long executionId);

	/**
	 * Retrieve a {@link StepExecution} by its ID and parent {@link JobExecution} ID. The
	 * execution context for the step should be available in the result, and the parent
	 * job execution should have its primitive properties, but it may not contain the job
	 * instance information.
	 * @param jobExecutionId The parent job execution ID.
	 * @param stepExecutionId The step execution ID.
	 * @return the {@link StepExecution} that has this ID or {@code null} if not found.
	 *
	 * @see #getJobExecution(Long)
	 */
	@Nullable
	StepExecution getStepExecution(@Nullable Long jobExecutionId, @Nullable Long stepExecutionId);

	/**
	 * @param instanceId {@link Long} The ID for the {@link jobInstance} to obtain.
	 * @return the {@code JobInstance} that has this ID, or {@code null} if not found.
	 */
	@Nullable
	JobInstance getJobInstance(@Nullable Long instanceId);

	/**
	 * Retrieve job executions by their job instance. The corresponding step executions
	 * may not be fully hydrated (for example, their execution context may be missing), depending
	 * on the implementation. In that case, use {@link #getStepExecution(Long, Long)} to hydrate them.
	 * @param jobInstance The {@link JobInstance} to query.
	 * @return the set of all executions for the specified {@link JobInstance}.
	 */
	List<JobExecution> getJobExecutions(JobInstance jobInstance);

	/**
	 * Find the last {@link JobExecution} that has been created for a given
	 * {@link JobInstance}.
	 * @param jobInstance The {@code JobInstance} for which to find the last {@code JobExecution}.
	 * @return the last {@code JobExecution} that has been created for this instance or
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
	 * hydrated (for example, their execution context may be missing), depending on the
	 * implementation. In that case, use {@link #getStepExecution(Long, Long)} to hydrate them.
	 * @param jobName The name of the job.
	 * @return the set of running executions for jobs with the specified name.
	 */
	Set<JobExecution> findRunningJobExecutions(@Nullable String jobName);

	/**
	 * Query the repository for all unique {@link JobInstance} names (sorted
	 * alphabetically).
	 * @return the set of job names that have been executed.
	 */
	List<String> getJobNames();

	/**
	 * Fetch {@link JobInstance} values in descending order of creation (and, therefore,
	 * usually of first execution) with a 'like' or wildcard criteria.
	 * @param jobName The name of the job for which to query.
	 * @param start The start index of the instances to return.
	 * @param count The maximum number of instances to return.
	 * @return a list of {@link JobInstance} for the requested job name.
	 */
	List<JobInstance> findJobInstancesByJobName(String jobName, int start, int count);

	/**
	 * Query the repository for the number of unique {@link JobInstance} objects associated with
	 * the supplied job name.
	 * @param jobName The name of the job for which to query.
	 * @return the number of {@link JobInstance}s that exist within the associated job
	 * repository.
	 * @throws {@code NoSuchJobException} thrown when there is no {@link JobInstance} for the
	 * jobName specified.
	 */
	int getJobInstanceCount(@Nullable String jobName) throws NoSuchJobException;

}
