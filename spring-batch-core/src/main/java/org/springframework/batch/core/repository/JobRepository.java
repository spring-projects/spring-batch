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

package org.springframework.batch.core.repository;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Isolation;

import java.util.Collection;

/**
 * <p>
 * Repository responsible for persistence of batch meta-data entities.
 * </p>
 *
 * @see JobInstance
 * @see JobExecution
 * @see StepExecution
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * @author David Turanski
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
public interface JobRepository {

	/**
	 * Check if an instance of this job already exists with the parameters
	 * provided.
	 *
	 * @param jobName the name of the job
	 * @param jobParameters the parameters to match
	 * @return true if a {@link JobInstance} already exists for this job name
	 * and job parameters
	 */
	boolean isJobInstanceExists(String jobName, JobParameters jobParameters);

	/**
	 * Create a new {@link JobInstance} with the name and job parameters provided.
	 *
	 * @param jobName logical name of the job
	 * @param jobParameters parameters used to execute the job
	 * @return the new {@link JobInstance}
	 */
	JobInstance createJobInstance(String jobName, JobParameters jobParameters);

	/**
	 * Create a new {@link JobExecution} based upon the {@link JobInstance} it's associated
	 * with, the {@link JobParameters} used to execute it with and the location of the configuration
	 * file that defines the job.
	 *
	 * @param jobInstance {@link JobInstance} instance to initialize the new JobExecution.
	 * @param jobParameters {@link JobParameters} instance to initialize the new JobExecution.
	 * @param jobConfigurationLocation {@link String} instance to initialize the new JobExecution.
	 * @return the new {@link JobExecution}.
	 */
	JobExecution createJobExecution(JobInstance jobInstance, JobParameters jobParameters, String jobConfigurationLocation);

	/**
	 * <p>
	 * Create a {@link JobExecution} for a given {@link Job} and
	 * {@link JobParameters}. If matching {@link JobInstance} already exists,
	 * the job must be restartable and it's last JobExecution must *not* be
	 * completed. If matching {@link JobInstance} does not exist yet it will be
	 * created.
	 * </p>
	 *
	 * <p>
	 * If this method is run in a transaction (as it normally would be) with
	 * isolation level at {@link Isolation#REPEATABLE_READ} or better, then this
	 * method should block if another transaction is already executing it (for
	 * the same {@link JobParameters} and job name). The first transaction to
	 * complete in this scenario obtains a valid {@link JobExecution}, and
	 * others throw {@link JobExecutionAlreadyRunningException} (or timeout).
	 * There are no such guarantees if the {@link JobInstanceDao} and
	 * {@link JobExecutionDao} do not respect the transaction isolation levels
	 * (e.g. if using a non-relational data-store, or if the platform does not
	 * support the higher isolation levels).
	 * </p>
	 *
	 * @param jobName the name of the job that is to be executed
	 *
	 * @param jobParameters the runtime parameters for the job
	 *
	 * @return a valid {@link JobExecution} for the arguments provided
	 *
	 * @throws JobExecutionAlreadyRunningException if there is a
	 * {@link JobExecution} already running for the job instance with the
	 * provided job and parameters.
	 * @throws JobRestartException if one or more existing {@link JobInstance}s
	 * is found with the same parameters and {@link Job#isRestartable()} is
	 * false.
	 * @throws JobInstanceAlreadyCompleteException if a {@link JobInstance} is
	 * found and was already completed successfully.
	 *
	 */
	JobExecution createJobExecution(String jobName, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException;

	/**
	 * Update the {@link JobExecution} (but not its {@link ExecutionContext}).
	 *
	 * Preconditions: {@link JobExecution} must contain a valid
	 * {@link JobInstance} and be saved (have an id assigned).
	 *
	 * @param jobExecution {@link JobExecution} instance to be updated in the repo.
	 */
	void update(JobExecution jobExecution);

	/**
	 * Save the {@link StepExecution} and its {@link ExecutionContext}. ID will
	 * be assigned - it is not permitted that an ID be assigned before calling
	 * this method. Instead, it should be left blank, to be assigned by a
	 * {@link JobRepository}.
	 *
	 * Preconditions: {@link StepExecution} must have a valid {@link Step}.
	 *
	 * @param stepExecution {@link StepExecution} instance to be added to the repo.
	 */
	void add(StepExecution stepExecution);

	/**
	 * Save a collection of {@link StepExecution}s and each {@link ExecutionContext}. The
	 * StepExecution ID will be assigned - it is not permitted that an ID be assigned before calling
	 * this method. Instead, it should be left blank, to be assigned by {@link JobRepository}.
	 *
	 * Preconditions: {@link StepExecution} must have a valid {@link Step}.
	 *
	 * @param stepExecutions collection of {@link StepExecution} instances to be added to the repo.
	 */
	void addAll(Collection<StepExecution> stepExecutions);

	/**
	 * Update the {@link StepExecution} (but not its {@link ExecutionContext}).
	 *
	 * Preconditions: {@link StepExecution} must be saved (have an id assigned).
	 *
	 * @param stepExecution {@link StepExecution} instance to be updated in the repo.
	 */
	void update(StepExecution stepExecution);

	/**
	 * Persist the updated {@link ExecutionContext}s of the given
	 * {@link StepExecution}.
	 *
	 * @param stepExecution {@link StepExecution} instance to be used to update the context.
	 */
	void updateExecutionContext(StepExecution stepExecution);

	/**
	 * Persist the updated {@link ExecutionContext} of the given
	 * {@link JobExecution}.
	 * @param jobExecution {@link JobExecution} instance to be used to update the context.
	 */
	void updateExecutionContext(JobExecution jobExecution);

	/**
	 * @param jobInstance {@link JobInstance} instance containing the step executions.
	 * @param stepName the name of the step execution that might have run.
	 * @return the last execution of step for the given job instance.
	 */
	@Nullable
	StepExecution getLastStepExecution(JobInstance jobInstance, String stepName);

	/**
	 * @param jobInstance {@link JobInstance} instance containing the step executions.
	 * @param stepName the name of the step execution that might have run.
	 * @return the execution count of the step within the given job instance.
	 */
	int getStepExecutionCount(JobInstance jobInstance, String stepName);

	/**
	 * @param jobName the name of the job that might have run
	 * @param jobParameters parameters identifying the {@link JobInstance}
	 * @return the last execution of job if exists, null otherwise
	 */
	@Nullable
	JobExecution getLastJobExecution(String jobName, JobParameters jobParameters);

}
