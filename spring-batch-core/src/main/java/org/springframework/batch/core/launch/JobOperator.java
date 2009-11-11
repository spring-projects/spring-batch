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
package org.springframework.batch.core.launch;

import java.util.List;
import java.util.Map;
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

/**
 * Low level interface for inspecting and controlling jobs with access only to
 * primitive and collection types. Suitable for a command-line client (e.g. that
 * launches a new process for each operation), or a remote launcher like a JMX
 * console.
 * 
 * @author Dave Syer
 * @since 2.0
 */
public interface JobOperator {

	/**
	 * List the {@link JobExecution JobExecutions} associated with a particular
	 * {@link JobInstance}, in reverse order of creation (and therefore usually
	 * of execution).
	 * 
	 * @param instanceId the id of a {@link JobInstance}
	 * @return the id values of all the {@link JobExecution JobExecutions}
	 * associated with this instance
	 * @throws NoSuchJobInstanceException
	 */
	List<Long> getExecutions(long instanceId) throws NoSuchJobInstanceException;

	/**
	 * List the {@link JobInstance JobInstances} for a given job name, in
	 * reverse order of creation (and therefore usually of first execution).
	 * 
	 * @param jobName the job name that all the instances have
	 * @param start the start index of the instances
	 * @param count the maximum number of values to return
	 * @return the id values of the {@link JobInstance JobInstances}
	 * @throws NoSuchJobException
	 */
	List<Long> getJobInstances(String jobName, int start, int count) throws NoSuchJobException;

	/**
	 * Get the id values of all the running {@link JobExecution JobExecutions}
	 * with the given job name.
	 * 
	 * @param jobName the name of the job to search under
	 * @return the id values of the running {@link JobExecution} instances
	 * @throws NoSuchJobException if there are no {@link JobExecution
	 * JobExecutions} with that job name
	 */
	Set<Long> getRunningExecutions(String jobName) throws NoSuchJobException;

	/**
	 * Get the {@link JobParameters} as an easily readable String.
	 * 
	 * @param executionId the id of an existing {@link JobExecution}
	 * @return the job parameters that were used to launch the associated
	 * instance
	 * @throws NoSuchJobExecutionException if the id was not associated with any
	 * {@link JobExecution}
	 */
	String getParameters(long executionId) throws NoSuchJobExecutionException;

	/**
	 * Start a new instance of a job with the parameters specified.
	 * 
	 * @param jobName the name of the {@link Job} to launch
	 * @param parameters the parameters to launch it with (comma or newline
	 * separated name=value pairs)
	 * @return the id of the {@link JobExecution} that is launched
	 * @throws NoSuchJobException if there is no {@link Job} with the specified
	 * name
	 * @throws JobInstanceAlreadyExistsException if a job instance with this
	 * name and parameters already exists
	 * @throws JobParametersInvalidException 
	 */
	Long start(String jobName, String parameters) throws NoSuchJobException, JobInstanceAlreadyExistsException, JobParametersInvalidException;

	/**
	 * Restart a failed or stopped {@link JobExecution}. Fails with an exception
	 * if the id provided does not exist or corresponds to a {@link JobInstance}
	 * that in normal circumstances already completed successfully.
	 * 
	 * @param executionId the id of a failed or stopped {@link JobExecution}
	 * @return the id of the {@link JobExecution} that was started
	 * 
	 * @throws JobInstanceAlreadyCompleteException if the job was already
	 * successfully completed
	 * @throws NoSuchJobExecutionException if the id was not associated with any
	 * {@link JobExecution}
	 * @throws NoSuchJobException if the {@link JobExecution} was found, but its
	 * corresponding {@link Job} is no longer available for launching
	 * @throws JobRestartException if there is a non-specific error with the
	 * restart (e.g. corrupt or inconsistent restart data)
	 * @throws JobParametersInvalidException if the parameters are not valid for
	 * this job
	 */
	Long restart(long executionId) throws JobInstanceAlreadyCompleteException, NoSuchJobExecutionException,
			NoSuchJobException, JobRestartException, JobParametersInvalidException;

	/**
	 * Launch the next in a sequence of {@link JobInstance} determined by the
	 * {@link JobParametersIncrementer} attached to the specified job. If the
	 * previous instance is still in a failed state, this method should still
	 * create a new instance and run it with different parameters (as long as
	 * the {@link JobParametersIncrementer} is working).<br/>
	 * <br/>
	 * 
	 * The last three exception described below should be extremely unlikely,
	 * but cannot be ruled out entirely. It points to some other thread or
	 * process trying to use this method (or a similar one) at the same time.
	 * 
	 * @param jobName the name of the job to launch
	 * @return the {@link JobExecution} id of the execution created when the job
	 * is launched
	 * @throws NoSuchJobException if there is no such job definition available
	 * @throws JobParametersNotFoundException if the parameters cannot be found
	 * @throws JobParametersInvalidException 
	 * @throws UnexpectedJobExecutionException 
	 * @throws UnexpectedJobExecutionException if an unexpected condition arises
	 */
	Long startNextInstance(String jobName) throws NoSuchJobException, JobParametersNotFoundException,
			JobRestartException, JobExecutionAlreadyRunningException, JobInstanceAlreadyCompleteException, UnexpectedJobExecutionException, JobParametersInvalidException;

	/**
	 * Send a stop signal to the {@link JobExecution} with the supplied id. The
	 * signal is successfully sent if this method returns true, but that doesn't
	 * mean that the job has stopped. The only way to be sure of that is to poll
	 * the job execution status.
	 * 
	 * @param executionId the id of a running {@link JobExecution}
	 * @return true if the message was successfully sent (does not guarantee
	 * that the job has stopped)
	 * @throws NoSuchJobExecutionException if there is no {@link JobExecution}
	 * with the id supplied
	 * @throws JobExecutionNotRunningException if the {@link JobExecution} is
	 * not running (so cannot be stopped)
	 */
	boolean stop(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException;

	/**
	 * Summarise the {@link JobExecution} with the supplied id, giving details
	 * of status, start and end times etc.
	 * 
	 * @param executionId the id of an existing {@link JobExecution}
	 * @return a String summarising the state of the job execution
	 * @throws NoSuchJobExecutionException if there is no {@link JobExecution}
	 * with the supplied id
	 */
	String getSummary(long executionId) throws NoSuchJobExecutionException;

	/**
	 * Summarise the {@link StepExecution} instances belonging to the
	 * {@link JobExecution} with the supplied id, giving details of status,
	 * start and end times etc.
	 * 
	 * @param executionId the id of an existing {@link JobExecution}
	 * @return a map of step execution id to String summarising the state of the
	 * execution
	 * @throws NoSuchJobExecutionException if there is no {@link JobExecution}
	 * with the supplied id
	 */
	Map<Long, String> getStepExecutionSummaries(long executionId) throws NoSuchJobExecutionException;

	/**
	 * List the available job names that can be launched with
	 * {@link #start(String, String)}.
	 * 
	 * @return a set of job names
	 */
	Set<String> getJobNames();

}
