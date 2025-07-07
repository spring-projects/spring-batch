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
package org.springframework.batch.core.repository.explore;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.lang.Nullable;

/**
 * Entry point for browsing the executions of running or historical jobs and steps. Since
 * the data may be re-hydrated from persistent storage, it cannot contain volatile fields
 * that would have been present when the execution was active.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 * @since 2.0
 * @deprecated since 6.0 in favor of {@link JobRepository}. Scheduled for removal in 6.2
 * or later.
 */
@Deprecated(since = "6.0", forRemoval = true)
public interface JobExplorer {

	/*
	 * ===================================================================================
	 * Job operations
	 * ===================================================================================
	 */

	/**
	 * Query the repository for all unique {@link JobInstance} names (sorted
	 * alphabetically).
	 * @return the list of job names that have been executed.
	 */
	default List<String> getJobNames() {
		return Collections.emptyList();
	}

	/*
	 * ===================================================================================
	 * Job instance operations
	 * ===================================================================================
	 */

	/**
	 * Fetch {@link JobInstance} values in descending order of creation (and, therefore,
	 * usually, of first execution).
	 * @param jobName The name of the job to query.
	 * @param start The start index of the instances to return.
	 * @param count The maximum number of instances to return.
	 * @return the {@link JobInstance} values up to a maximum of count values.
	 */
	default List<JobInstance> getJobInstances(String jobName, int start, int count) {
		return Collections.emptyList();
	}

	/**
	 * Fetch {@link JobInstance} values in descending order of creation (and, therefore,
	 * usually of first execution) with a 'like' or wildcard criteria.
	 * @param jobName The name of the job for which to query.
	 * @param start The start index of the instances to return.
	 * @param count The maximum number of instances to return.
	 * @return a list of {@link JobInstance} for the requested job name.
	 * @deprecated Since v6.0 and scheduled for removal in v6.2. Use
	 * {@link #getJobInstances(String, int, int)}
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	default List<JobInstance> findJobInstancesByJobName(String jobName, int start, int count) {
		return Collections.emptyList();
	}

	/**
	 * Fetch the last job instances with the provided name, sorted backwards by primary
	 * key, using a 'like' criteria
	 * @param jobName {@link String} containing the name of the job.
	 * @param start int containing the offset of where list of job instances results
	 * should begin.
	 * @param count int containing the number of job instances to return.
	 * @return a list of {@link JobInstance} for the job name requested.
	 * @since 5.0
	 * @deprecated since v6.0 and scheduled for removal in v6.2. Use
	 * {@link #getJobInstances(String, int, int)}
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	default List<JobInstance> findJobInstancesByName(String jobName, int start, int count) {
		return Collections.emptyList();
	}

	/**
	 * Check if an instance of this job already exists with the parameters provided.
	 * @param jobName the name of the job
	 * @param jobParameters the parameters to match
	 * @return true if a {@link JobInstance} already exists for this job name and job
	 * parameters
	 * @deprecated Since v6.0 and scheduled for removal in v6.2. Use
	 * {@link #getJobInstance(String, JobParameters)} and check for {@code null} result
	 * instead.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	default boolean isJobInstanceExists(String jobName, JobParameters jobParameters) {
		return getJobInstance(jobName, jobParameters) != null;
	}

	/**
	 * @param instanceId {@link Long} The ID for the {@link JobInstance} to obtain.
	 * @return the {@code JobInstance} that has this ID, or {@code null} if not found.
	 */
	@Nullable
	default JobInstance getJobInstance(@Nullable Long instanceId) {
		throw new UnsupportedOperationException();
	}

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
	 * @param jobName {@link String} name of the job.
	 * @param jobParameters {@link JobParameters} parameters for the job instance.
	 * @return the {@link JobInstance} with the given name and parameters, or
	 * {@code null}.
	 *
	 * @since 5.0
	 */
	@Nullable
	default JobInstance getJobInstance(String jobName, JobParameters jobParameters) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Query the repository for the number of unique {@link JobInstance} objects
	 * associated with the supplied job name.
	 * @param jobName The name of the job for which to query.
	 * @return the number of {@link JobInstance}s that exist within the associated job
	 * repository.
	 * @throws NoSuchJobException thrown when there is no {@link JobInstance} for the
	 * jobName specified.
	 */
	default long getJobInstanceCount(@Nullable String jobName) throws NoSuchJobException {
		throw new UnsupportedOperationException();
	}

	/*
	 * ===================================================================================
	 * Job execution operations
	 * ===================================================================================
	 */

	/**
	 * Retrieve a {@link JobExecution} by its ID. The complete object graph for this
	 * execution should be returned (unless otherwise indicated), including the parent
	 * {@link JobInstance} and associated {@link ExecutionContext} and
	 * {@link StepExecution} instances (also including their execution contexts).
	 * @param executionId The job execution ID.
	 * @return the {@link JobExecution} that has this ID or {@code null} if not found.
	 */
	@Nullable
	default JobExecution getJobExecution(@Nullable Long executionId) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieve job executions by their job instance. The corresponding step executions
	 * may not be fully hydrated (for example, their execution context may be missing),
	 * depending on the implementation. In that case, use
	 * {@link #getStepExecution(Long, Long)} to hydrate them.
	 * @param jobInstance The {@link JobInstance} to query.
	 * @return the list of all executions for the specified {@link JobInstance}.
	 */
	default List<JobExecution> getJobExecutions(JobInstance jobInstance) {
		return Collections.emptyList();
	}

	/**
	 * Return all {@link JobExecution}s for given {@link JobInstance}, sorted backwards by
	 * creation order (so the first element is the most recent).
	 * @param jobInstance parent {@link JobInstance} of the {@link JobExecution}s to find.
	 * @return {@link List} containing JobExecutions for the jobInstance.
	 * @since 5.0
	 * @deprecated since v6.0 and scheduled for removal in v6.2. Use
	 * {@link #getJobExecutions(JobInstance)}
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	default List<JobExecution> findJobExecutions(JobInstance jobInstance) {
		return Collections.emptyList();
	}

	/**
	 * Find the last {@link JobExecution} that has been created for a given
	 * {@link JobInstance}.
	 * @param jobInstance The {@code JobInstance} for which to find the last
	 * {@code JobExecution}.
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
	 * @param jobName the name of the job that might have run
	 * @param jobParameters parameters identifying the {@link JobInstance}
	 * @return the last execution of job if exists, null otherwise
	 */
	@Nullable
	default JobExecution getLastJobExecution(String jobName, JobParameters jobParameters) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieve running job executions. The corresponding step executions may not be fully
	 * hydrated (for example, their execution context may be missing), depending on the
	 * implementation. In that case, use {@link #getStepExecution(Long, Long)} to hydrate
	 * them.
	 * @param jobName The name of the job.
	 * @return the set of running executions for jobs with the specified name.
	 */
	default Set<JobExecution> findRunningJobExecutions(@Nullable String jobName) {
		return Collections.emptySet();
	}

	/*
	 * ===================================================================================
	 * Step execution operations
	 * ===================================================================================
	 */

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
	default StepExecution getStepExecution(@Nullable Long jobExecutionId, @Nullable Long stepExecutionId) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param jobInstance {@link JobInstance} instance containing the step executions.
	 * @param stepName the name of the step execution that might have run.
	 * @return the last execution of step for the given job instance.
	 */
	@Nullable
	default StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param jobInstance {@link JobInstance} instance containing the step executions.
	 * @param stepName the name of the step execution that might have run.
	 * @return the execution count of the step within the given job instance.
	 */
	default long getStepExecutionCount(JobInstance jobInstance, String stepName) {
		throw new UnsupportedOperationException();
	}

}
