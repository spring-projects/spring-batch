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
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.execution.facade.JobExecutorFacade;
import org.springframework.batch.execution.facade.NoSuchJobExecutionException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.util.Assert;

/**
 * Simple bootstrapping mechanism for running a single job execution in a
 * {@link JobExecutorFacade}.
 * 
 * <p>
 * This simple implementation does not run the job asynchronously, so the start
 * method will not return before the job ends. However, the job execution can be
 * interrupted via the stop method in another thread.
 * </p>
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @since 2.1
 */
public class SimpleJobLauncher extends AbstractJobLauncher {

	private volatile Thread processingThread;
	private int running = 0;

	/**
	 * Check whether or not the container is currently running. This is done by
	 * checking the thread to see if it is still alive.
	 */
	protected boolean isRunning(JobIdentifier jobIdentifier) {
		return processingThread != null && processingThread.isAlive();
	}

	/**
	 * Start the provided facade. The current thread will first be saved. This
	 * may seem odd at first, however, this simple bootstrap requires that only
	 * one thread can kick off a container, and that the first thread that calls
	 * start is the 'processing thread'. If the container has already been
	 * started, no exception will be thrown.
	 * 
	 * @throws NoSuchJobConfigurationException
	 * @see Lifecycle#start().
	 * 
	 * @throws IllegalStateException
	 *             if JobConfiguration is null.
	 */
	protected ExitStatus doRun(JobIdentifier jobIdentifier,
			Runnable exitCallback) throws NoSuchJobConfigurationException {

		Assert.notNull(jobIdentifier, "JobIdentifier must not be null.");
		Assert.isTrue(running == 0,
				"This launcher can run only one job at at time.");

		/*
		 * There is no reason to kick off a new thread, since only one thread
		 * should be processing at once. However, a handle to the thread is
		 * maintained to allow for interrupt
		 */
		processingThread = Thread.currentThread();
		try {
			running++;
			return jobExecutorFacade.start(jobIdentifier);
		} finally {
			running--;
			exitCallback.run();
		}

	}

	/**
	 * Interrupt the thread that is running the job.
	 * 
	 * @see org.springframework.batch.execution.bootstrap.AbstractJobLauncher#doStop(org.springframework.batch.core.domain.JobIdentifier)
	 */
	protected void doStop(JobIdentifier runtimeInformation)
			throws NoSuchJobExecutionException {
		if (isRunning()) {
			processingThread.interrupt();
		}
	}

}
