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
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Interface which defines a facade for running jobs. The interface is
 * intentionally minimal and package private. It is convenient to be able to
 * test a {@link JobLauncher} with stub implementations of this interface.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
interface JobExecutorFacade {

	/**
	 * Start a job execution identifiable by the {@link JobIdentifier}.
	 * Implementations normally require a job configuration to be locatable
	 * corresponding to the {@link JobIdentifier}, preferably matching them at
	 * least by name.
	 * 
	 * @param jobIdentifier
	 * 
	 * @throws NoSuchJobConfigurationException
	 */
	ExitStatus start(JobIdentifier jobIdentifier)
			throws NoSuchJobConfigurationException;

	/**
	 * Stop the job execution that was started with this runtime information.
	 * 
	 * @param jobIdentifier
	 *            the {@link JobIdentifier}.
	 * @throws NoSuchJobExecutionException
	 *             if a job with this runtime information is not running
	 */
	void stop(JobIdentifier jobIdentifier) throws NoSuchJobExecutionException;

	/**
	 * Simple check for whether or not there are jobs in progress. Can be used
	 * by clients to wait for all jobs to finish. Finer grained monitoring and
	 * reporting can be implemented using the persistent execution details
	 * (normally in a database), provided they are maintained by the
	 * implementation.
	 * 
	 * @return true if any jobs are active.
	 */
	boolean isRunning();

}
