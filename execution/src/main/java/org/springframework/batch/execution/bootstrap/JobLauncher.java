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
import org.springframework.batch.execution.JobExecutorFacade;
import org.springframework.batch.execution.NoSuchJobExecutionException;
import org.springframework.context.Lifecycle;

/**
 * Simple interface for controlling a {@link JobExecutorFacade} for a single job
 * configuration, and also possibly ad-hoc executions, based on different
 * runtime information. Implementations should concentrate on launching and
 * controlling a single job, as configured in a {@link JobExecutorFacade} instance.
 * 
 * @author Dave Syer
 * @since 2.1
 */
public interface JobLauncher extends Lifecycle {

	/**
	 * Return whether or not a job execution is currently running.
	 */
	boolean isRunning();

	/**
	 * Start a job execution with the given runtime information.
	 * @throws NoSuchJobConfigurationException
	 */
	void start(JobIdentifier runtimeInformation) throws NoSuchJobConfigurationException;

	/**
	 * Start a job execution with the given name and other runtime information
	 * generated on the fly.
	 * 
	 * @param name the name to assign to the job
	 * @throws NoSuchJobConfigurationException
	 */
	void start(String name) throws NoSuchJobConfigurationException;

	/**
	 * Start a job execution with default name and other runtime information
	 * generated on the fly.<br/>
	 * 
	 * Because {@link Lifecycle#start()} does not throw checked exceptions this
	 * also does not, so an error message and stack trace will be logged if the
	 * required job(s) cannot be started.
	 * 
	 * @see org.springframework.context.Lifecycle#start()
	 */
	public void start();

	/**
	 * Stop the job execution that was started with this runtime information.
	 * @param runtimeInformation the {@link JobIdentifier}.
	 * @throws NoSuchJobExecutionException 
	 */
	void stop(JobIdentifier runtimeInformation) throws NoSuchJobExecutionException;

	/**
	 * Stop all currently executing jobs matching the given name. All jobs
	 * started with {@link JobIdentifier} having this name will be
	 * stopped.
	 * @throws NoSuchJobExecutionException 
	 */
	void stop(String name) throws NoSuchJobExecutionException;

	/**
	 * Stop the current job executions if there are any. If not, no action will
	 * be taken.
	 * @throws NoSuchJobExecutionException 
	 * 
	 * @see org.springframework.context.Lifecycle#stop()
	 */
	public void stop();
}
