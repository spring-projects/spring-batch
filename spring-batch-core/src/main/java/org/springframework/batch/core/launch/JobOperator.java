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
package org.springframework.batch.core.launch;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.lang.Nullable;

/**
 * High level interface for operating batch jobs.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public interface JobOperator extends JobLauncher {

	/**
	 * List the available job names that can be launched with
	 * {@link #start(String, Properties)}.
	 * @return a set of job names
	 */
	Set<String> getJobNames();

	/**
	 * Start a new instance of a job with the parameters specified.
	 * @param jobName the name of the {@link Job} to launch
	 * @param parameters the parameters to launch it with
	 * @return the id of the {@link JobExecution} that is launched
	 * @throws NoSuchJobException if there is no {@link Job} with the specified name
	 * @throws JobInstanceAlreadyExistsException if a job instance with this name and
	 * parameters already exists
	 * @throws JobParametersInvalidException thrown if any of the job parameters are
	 * invalid.
	 * @deprecated since 6.0 in favor of {@link #start(Job, JobParameters)}. Scheduled for
	 * removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	default Long start(String jobName, Properties parameters)
			throws NoSuchJobException, JobInstanceAlreadyExistsException, JobParametersInvalidException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Start a new instance of a job with the specified parameters.
	 * @param job the {@link Job} to start
	 * @param jobParameters the {@link JobParameters} to start the job with
	 * @return the {@link JobExecution} that was started
	 * @throws NoSuchJobException if the given {@link Job} is not registered
	 * @throws JobParametersInvalidException thrown if any of the job parameters are
	 * @throws JobExecutionAlreadyRunningException if the JobInstance identified by the
	 * properties already has an execution running. invalid.
	 * @throws JobRestartException if the execution would be a re-start, but a re-start is
	 * either not allowed or not needed.
	 * @throws JobInstanceAlreadyCompleteException if the job has been run before with the
	 * same parameters and completed successfully
	 * @throws IllegalArgumentException if the job or job parameters are null.
	 */
	default JobExecution start(Job job, JobParameters jobParameters)
			throws NoSuchJobException, JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
			JobRestartException, JobParametersInvalidException {
		return run(job, jobParameters);
	}

	/**
	 * Restart a failed or stopped {@link JobExecution}. Fails with an exception if the id
	 * provided does not exist or corresponds to a {@link JobInstance} that in normal
	 * circumstances already completed successfully.
	 * @param executionId the id of a failed or stopped {@link JobExecution}
	 * @return the id of the {@link JobExecution} that was started
	 * @throws JobInstanceAlreadyCompleteException if the job was already successfully
	 * completed
	 * @throws NoSuchJobExecutionException if the id was not associated with any
	 * {@link JobExecution}
	 * @throws NoSuchJobException if the {@link JobExecution} was found, but its
	 * corresponding {@link Job} is no longer available for launching
	 * @throws JobRestartException if there is a non-specific error with the restart (e.g.
	 * corrupt or inconsistent restart data)
	 * @throws JobParametersInvalidException if the parameters are not valid for this job
	 */
	Long restart(long executionId) throws JobInstanceAlreadyCompleteException, NoSuchJobExecutionException,
			NoSuchJobException, JobRestartException, JobParametersInvalidException;

	/**
	 * Launch the next in a sequence of {@link JobInstance} determined by the
	 * {@link JobParametersIncrementer} attached to the specified job. If the previous
	 * instance is still in a failed state, this method should still create a new instance
	 * and run it with different parameters (as long as the
	 * {@link JobParametersIncrementer} is working).<br>
	 * <br>
	 *
	 * The last three exception described below should be extremely unlikely, but cannot
	 * be ruled out entirely. It points to some other thread or process trying to use this
	 * method (or a similar one) at the same time.
	 * @param jobName the name of the job to launch
	 * @return the {@link JobExecution} id of the execution created when the job is
	 * launched
	 * @throws NoSuchJobException if there is no such job definition available
	 * @throws JobParametersNotFoundException if the parameters cannot be found
	 * @throws JobParametersInvalidException thrown if some of the job parameters are
	 * invalid.
	 * @throws UnexpectedJobExecutionException if an unexpected condition arises
	 * @throws JobRestartException thrown if a job is restarted illegally.
	 * @throws JobExecutionAlreadyRunningException thrown if attempting to restart a job
	 * that is already executing.
	 * @throws JobInstanceAlreadyCompleteException thrown if attempting to restart a
	 * completed job.
	 */
	Long startNextInstance(String jobName) throws NoSuchJobException, JobParametersNotFoundException,
			JobRestartException, JobExecutionAlreadyRunningException, JobInstanceAlreadyCompleteException,
			UnexpectedJobExecutionException, JobParametersInvalidException;

	/**
	 * Send a stop signal to the {@link JobExecution} with the supplied id. The signal is
	 * successfully sent if this method returns true, but that doesn't mean that the job
	 * has stopped. The only way to be sure of that is to poll the job execution status.
	 * @param executionId the id of a running {@link JobExecution}
	 * @return true if the message was successfully sent (does not guarantee that the job
	 * has stopped)
	 * @throws NoSuchJobExecutionException if there is no {@link JobExecution} with the id
	 * supplied
	 * @throws JobExecutionNotRunningException if the {@link JobExecution} is not running
	 * (so cannot be stopped)
	 */
	boolean stop(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException;

	/**
	 * Mark the {@link JobExecution} as ABANDONED. If a stop signal is ignored because the
	 * process died this is the best way to mark a job as finished with (as opposed to
	 * STOPPED). An abandoned job execution cannot be restarted by the framework.
	 * @param jobExecutionId the job execution id to abort
	 * @return the {@link JobExecution} that was aborted
	 * @throws NoSuchJobExecutionException thrown if there is no job execution for the
	 * jobExecutionId.
	 * @throws JobExecutionAlreadyRunningException if the job is running (it should be
	 * stopped first)
	 */
	JobExecution abandon(long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionAlreadyRunningException;

	/**
	 * List the {@link JobExecution JobExecutions} associated with a particular
	 * {@link JobInstance}, in reverse order of creation (and therefore usually of
	 * execution).
	 * @param instanceId the id of a {@link JobInstance}
	 * @return the id values of all the {@link JobExecution JobExecutions} associated with
	 * this instance
	 * @throws NoSuchJobInstanceException if the {@link JobInstance} associated with the
	 * {@code instanceId} cannot be found.
	 * @deprecated Since 6.0 in favor of
	 * {@link org.springframework.batch.core.repository.JobRepository#getJobExecutions(JobInstance)}.
	 * Scheduled for removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	List<Long> getExecutions(long instanceId) throws NoSuchJobInstanceException;

	/**
	 * List the {@link JobInstance JobInstances} for a given job name, in reverse order of
	 * creation (and therefore usually of first execution).
	 * @param jobName the job name that all the instances have
	 * @param start the start index of the instances
	 * @param count the maximum number of values to return
	 * @return the id values of the {@link JobInstance JobInstances}
	 * @throws NoSuchJobException is thrown if no {@link JobInstance}s for the jobName
	 * exist.
	 * @deprecated Since 6.0 in favor of
	 * {@link org.springframework.batch.core.repository.JobRepository#getJobInstances(String, int, int)}.
	 * Scheduled for removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	List<Long> getJobInstances(String jobName, int start, int count) throws NoSuchJobException;

	/**
	 * @param jobName {@link String} name of the job.
	 * @param jobParameters {@link JobParameters} parameters for the job instance.
	 * @return the {@link JobInstance} with the given name and parameters, or
	 * {@code null}.
	 * @deprecated Since 6.0 in favor of
	 * {@link org.springframework.batch.core.repository.JobRepository#getJobInstance(String, JobParameters)}.
	 * Scheduled for removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	@Nullable
	default JobInstance getJobInstance(String jobName, JobParameters jobParameters) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the id values of all the running {@link JobExecution JobExecutions} with the
	 * given job name.
	 * @param jobName the name of the job to search under
	 * @return the id values of the running {@link JobExecution} instances
	 * @throws NoSuchJobException if there are no {@link JobExecution JobExecutions} with
	 * that job name
	 * @deprecated Since 6.0 in favor of
	 * {@link org.springframework.batch.core.repository.JobRepository#findRunningJobExecutions(String)}.
	 * Scheduled for removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	Set<Long> getRunningExecutions(String jobName) throws NoSuchJobException;

	/**
	 * Get the {@link JobParameters} as a human readable String (new line separated
	 * key=value pairs).
	 * @param executionId the id of an existing {@link JobExecution}
	 * @return the job parameters that were used to launch the associated instance
	 * @throws NoSuchJobExecutionException if the id was not associated with any
	 * {@link JobExecution}
	 * @deprecated Since 6.0 in favor of the <code>getJobParameters()</code> method of
	 * {@link org.springframework.batch.core.repository.JobRepository#getJobExecution(Long)}.
	 * Scheduled for removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	String getParameters(long executionId) throws NoSuchJobExecutionException;

	/**
	 * Summarise the {@link JobExecution} with the supplied id, giving details of status,
	 * start and end times etc.
	 * @param executionId the id of an existing {@link JobExecution}
	 * @return a String summarising the state of the job execution
	 * @throws NoSuchJobExecutionException if there is no {@link JobExecution} with the
	 * supplied id
	 * @deprecated Since 6.0 in favor of the <code>toString()</code> method of
	 * {@link org.springframework.batch.core.repository.JobRepository#getJobExecution(Long)}.
	 * Scheduled for removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	String getSummary(long executionId) throws NoSuchJobExecutionException;

	/**
	 * Summarise the {@link StepExecution} instances belonging to the {@link JobExecution}
	 * with the supplied id, giving details of status, start and end times etc.
	 * @param executionId the id of an existing {@link JobExecution}
	 * @return a map of step execution id to String summarising the state of the execution
	 * @throws NoSuchJobExecutionException if there is no {@link JobExecution} with the
	 * supplied id
	 * @deprecated Since 6.0 in favor of the <code>getStepExecutions()</code> method of
	 * {@link org.springframework.batch.core.repository.JobRepository#getJobExecution(Long)}.
	 * Scheduled for removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	Map<Long, String> getStepExecutionSummaries(long executionId) throws NoSuchJobExecutionException;

}
