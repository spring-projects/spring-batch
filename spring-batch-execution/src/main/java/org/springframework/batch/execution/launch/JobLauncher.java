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

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.NoSuchJobException;
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
	 * Start a job execution with the given runtime information.
	 * 
	 * @return the exit code from the job if it returns synchronously. If the
	 *         implementation is asynchronous, the status might well be unknown.
	 * 
	 * @throws NoSuchJobException
	 */
	public JobExecution run(JobIdentifier jobIdentifier)
			throws NoSuchJobException, JobExecutionAlreadyRunningException;

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
