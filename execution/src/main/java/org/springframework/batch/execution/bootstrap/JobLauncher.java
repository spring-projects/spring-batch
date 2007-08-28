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
package org.springframework.batch.execution.bootstrap;

import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.runtime.JobIdentifier;
import org.springframework.batch.execution.facade.JobExecutorFacade;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Simple interface for controlling jobs from the job configuration registry,
 * including possible ad-hoc executions, based on different runtime identifiers.
 * Implementations should concentrate on managing jobs and delegate the
 * launching to a {@link JobExecutorFacade}.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */

public interface JobLauncher {

	/**
	 * Start a job execution with default name and other runtime information
	 * generated on the fly.<br/>
	 * 
	 * @return the exit code from the job if it returns synchronously.
	 * 
	 */
	public ExitStatus run() throws NoSuchJobConfigurationException;

	/**
	 * Start a job execution with the given name and other runtime information
	 * generated on the fly.
	 * 
	 * @param name
	 *            the name to assign to the job
	 * @return the exit code from the job if it returns synchronously.
	 * @throws NoSuchJobConfigurationException
	 */
	public ExitStatus run(String jobName)
			throws NoSuchJobConfigurationException;

	/**
	 * Start a job execution with the given runtime information.
	 * 
	 * @return the exit code from the job if it returns synchronously.
	 * @throws NoSuchJobConfigurationException
	 */
	public ExitStatus run(JobIdentifier jobIdentifier)
			throws NoSuchJobConfigurationException;

	/**
	 * Stop the current job executions if there are any. If not, no action will
	 * be taken.
	 * 
	 * @see org.springframework.context.Lifecycle#stop()
	 */
	public void stop();

	/**
	 * Return whether or not a job execution is currently running.
	 */
	public boolean isRunning();

}
