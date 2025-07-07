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

package org.springframework.batch.core.repository;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Isolation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Repository responsible for persistence of batch meta-data entities.
 * </p>
 *
 * @see JobInstance
 * @see JobExecution
 * @see StepExecution
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * @author David Turanski
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 */
public interface JobRepository extends JobExplorer {

	/*
	 * ===================================================================================
	 * Read operations
	 * ===================================================================================
	 */

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

	/*
	 * ===================================================================================
	 * Write operations
	 * ===================================================================================
	 */

	/**
	 * Create a new {@link JobInstance} with the name and job parameters provided.
	 * @param jobName logical name of the job
	 * @param jobParameters parameters used to execute the job
	 * @return the new {@link JobInstance}
	 */
	JobInstance createJobInstance(String jobName, JobParameters jobParameters);

	/**
	 * <p>
	 * Create a {@link JobExecution} for a given {@link Job} and {@link JobParameters}. If
	 * matching {@link JobInstance} already exists, the job must be restartable and it's
	 * last JobExecution must *not* be completed. If matching {@link JobInstance} does not
	 * exist yet it will be created.
	 * </p>
	 *
	 * <p>
	 * If this method is run in a transaction (as it normally would be) with isolation
	 * level at {@link Isolation#REPEATABLE_READ} or better, then this method should block
	 * if another transaction is already executing it (for the same {@link JobParameters}
	 * and job name). The first transaction to complete in this scenario obtains a valid
	 * {@link JobExecution}, and others throw {@link JobExecutionAlreadyRunningException}
	 * (or timeout). There are no such guarantees if the {@link JobInstanceDao} and
	 * {@link JobExecutionDao} do not respect the transaction isolation levels (e.g. if
	 * using a non-relational data-store, or if the platform does not support the higher
	 * isolation levels).
	 * </p>
	 * @param jobName the name of the job that is to be executed
	 * @param jobParameters the runtime parameters for the job
	 * @return a valid {@link JobExecution} for the arguments provided
	 * @throws JobExecutionAlreadyRunningException if there is a {@link JobExecution}
	 * already running for the job instance with the provided job and parameters.
	 * @throws JobRestartException if one or more existing {@link JobInstance}s is found
	 * with the same parameters and {@link Job#isRestartable()} is false.
	 * @throws JobInstanceAlreadyCompleteException if a {@link JobInstance} is found and
	 * was already completed successfully.
	 *
	 */
	JobExecution createJobExecution(String jobName, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException;

	/**
	 * Update the {@link JobExecution} (but not its {@link ExecutionContext}).
	 * <p>
	 * Preconditions: {@link JobExecution} must contain a valid {@link JobInstance} and be
	 * saved (have an id assigned).
	 * @param jobExecution {@link JobExecution} instance to be updated in the repo.
	 */
	void update(JobExecution jobExecution);

	/**
	 * Save the {@link StepExecution} and its {@link ExecutionContext}. ID will be
	 * assigned - it is not permitted that an ID be assigned before calling this method.
	 * Instead, it should be left blank, to be assigned by a {@link JobRepository}.
	 * <p>
	 * Preconditions: {@link StepExecution} must have a valid {@link Step}.
	 * @param stepExecution {@link StepExecution} instance to be added to the repo.
	 */
	void add(StepExecution stepExecution);

	/**
	 * Save a collection of {@link StepExecution}s and each {@link ExecutionContext}. The
	 * StepExecution ID will be assigned - it is not permitted that an ID be assigned
	 * before calling this method. Instead, it should be left blank, to be assigned by
	 * {@link JobRepository}.
	 * <p>
	 * Preconditions: {@link StepExecution} must have a valid {@link Step}.
	 * @param stepExecutions collection of {@link StepExecution} instances to be added to
	 * the repo.
	 */
	void addAll(Collection<StepExecution> stepExecutions);

	/**
	 * Update the {@link StepExecution} (but not its {@link ExecutionContext}).
	 * <p>
	 * Preconditions: {@link StepExecution} must be saved (have an id assigned).
	 * @param stepExecution {@link StepExecution} instance to be updated in the repo.
	 */
	void update(StepExecution stepExecution);

	/**
	 * Persist the updated {@link ExecutionContext}s of the given {@link StepExecution}.
	 * @param stepExecution {@link StepExecution} instance to be used to update the
	 * context.
	 */
	void updateExecutionContext(StepExecution stepExecution);

	/**
	 * Persist the updated {@link ExecutionContext} of the given {@link JobExecution}.
	 * @param jobExecution {@link JobExecution} instance to be used to update the context.
	 */
	void updateExecutionContext(JobExecution jobExecution);

	/**
	 * Delete the step execution along with its execution context.
	 * @param stepExecution the step execution to delete
	 * @since 5.0
	 */
	default void deleteStepExecution(StepExecution stepExecution) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Delete the job execution object graph (ie the job execution with its execution
	 * context, all related step executions and their executions contexts, as well as
	 * associated job parameters)
	 * @param jobExecution the job execution to delete
	 * @since 5.0
	 */
	default void deleteJobExecution(JobExecution jobExecution) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Delete the job instance object graph (ie the job instance with all associated job
	 * executions along with their respective object graphs as specified in
	 * {@link #deleteJobExecution(JobExecution)}).
	 * @param jobInstance the job instance to delete
	 * @since 5.0
	 */
	default void deleteJobInstance(JobInstance jobInstance) {
		throw new UnsupportedOperationException();
	}

}
