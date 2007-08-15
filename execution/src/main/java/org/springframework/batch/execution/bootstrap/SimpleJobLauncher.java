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
import org.springframework.context.Lifecycle;

/**
 * Simple bootstrapping mechanism for running a single job execution in a
 * {@link JobExecutorFacade}.
 * 
 * <p>
 * This simple implementation does not run the job asynchronously, so the start
 * method will not return before the job ends. However, the job execution to be
 * interrupted via the stop method in another thread.
 * </p>
 * 
 * @see Lifecycle
 * @author Lucas Ward
 * @author Dave Syer
 * @since 2.1
 */
public class SimpleJobLauncher extends AbstractJobLauncher {

	private volatile Thread processingThread;

	private volatile boolean running = false;

	/**
	 * Return whether or not the container is currently running. This is done by
	 * checking the thread to see if it is still alive.
	 */
	public boolean isRunning() {
		return running && processingThread != null && processingThread.isAlive();
	}

	/**
	 * Start the provided container. The current thread will first be saved.
	 * This may seem odd at first, however, this simple bootstrap requires that
	 * only one thread can kick off a container, and that the first thread that
	 * calls start is the 'processing thread'. If the container has already been
	 * started, no exception will be thrown.
	 * @throws NoSuchJobConfigurationException 
	 * @see Lifecycle#start().
	 * 
	 * @throws IllegalStateException if JobConfiguration is null.
	 */
	protected void doStart(JobIdentifier jobIdentifier) throws NoSuchJobConfigurationException {

		/*
		 * There is no reason to kick off a new thread, since only one thread
		 * should be processing at once. However, a handle to the thread should
		 * be maintained to allow for interrupt
		 */
		processingThread = Thread.currentThread();
		// TODO: push this out to a method call in parent inside synchronized
		// block?
		running = true;
		try {
			batchContainer.start(jobIdentifier);
		}
		finally {
			running = false;
			unregister(jobIdentifier);
		}

	}

	/**
	 * Stop the job if it is running by interrupting its thread. If no job is
	 * running, no action will be taken.
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.context.Lifecycle#stop()
	 */
	protected void doStop() {

		if (isRunning()) {
			processingThread.interrupt();
			running = false;
		}
	}

	/**
	 * Delegates to {@link #doStop()}. Since there is only one job running in
	 * this launcher this is OK.
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.context.Lifecycle#stop()
	 */
	protected void doStop(JobIdentifier runtimeInformation) {
		doStop();
	}
}
