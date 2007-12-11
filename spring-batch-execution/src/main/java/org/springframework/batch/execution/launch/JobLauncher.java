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
package org.springframework.batch.execution.launch;

import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;

/**
 * Simple interface for controlling jobs, including possible ad-hoc executions,
 * based on different runtime identifiers.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */

public interface JobLauncher {

	/**
	 * Start a job execution with default name and other runtime information
	 * generated on the fly.<br/>
	 * 
	 * @return the exit code from the job if it returns synchronously. If the
	 *         implementation is asynchronous, the status might well be unknown.
	 * @throws JobExecutionAlreadyRunningException 
	 * 
	 */
	public JobExecution run() throws NoSuchJobConfigurationException, JobExecutionAlreadyRunningException;

	/**
	 * Start a job execution with the given name and other runtime information
	 * generated on the fly. The name is used to locate a job configuration, and
	 * the other runtime information is used to identify the job instance.
	 * 
	 * @param name
	 *            the name to assign to the job configuration
	 * @return the exit code from the job if it returns synchronously. If the
	 *         implementation is asynchronous, the status might well be unknown.
	 * 
	 * @throws NoSuchJobConfigurationException
	 * @throws JobExecutionAlreadyRunningException 
	 */
	public JobExecution run(String jobName)
			throws NoSuchJobConfigurationException, JobExecutionAlreadyRunningException;

	/**
	 * Start a job execution with the given runtime information.
	 * 
	 * @return the exit code from the job if it returns synchronously. If the
	 *         implementation is asynchronous, the status might well be unknown.
	 * 
	 * @throws NoSuchJobConfigurationException
	 */
	public JobExecution run(JobIdentifier jobIdentifier)
			throws NoSuchJobConfigurationException, JobExecutionAlreadyRunningException;

	/**
	 * Stop the current job executions if there are any. If not, no action will
	 * be taken.
	 * 
	 * @see org.springframework.context.Lifecycle#stop()
	 */
	public void stop();

	/**
	 * Check whether or not any job execution is currently running.
	 * 
	 * @return true if this launcher started a job or jobs and one can be
	 *         determined to be in an active state.
	 */
	public boolean isRunning();

}
