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
package org.springframework.batch.execution.bootstrap.support;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.execution.launch.JobExecutionListener;
import org.springframework.batch.execution.launch.JobExecutionListenerSupport;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.util.Assert;

/**
 * {@link JobExecutionListener} that will interrupt the Thread that the job was
 * started in when the stop signal comes. Use only for a standalone process, not
 * in an application server container.
 * 
 * @author Dave Syer
 * 
 */
public class ThreadInterruptJobExecutionListener extends
		JobExecutionListenerSupport {

	private volatile Thread processingThread;
	private int running = 0;

	/**
	 * Save the current thread so it can be interrupted later. This may seem odd
	 * at first, however, a simple bootstrap requires that only one thread can
	 * kick off a container, and that the first thread that calls start is the
	 * 'processing thread'. If the container has already been started, no
	 * exception will be thrown.
	 * 
	 * @see org.springframework.batch.execution.launch.JobExecutionListenerSupport#before(org.springframework.batch.core.domain.JobExecution)
	 */
	public void before(JobExecution execution) {
		Assert.isTrue(running == 0,
				"This listener only supports one job at at time.");
		running++;
		/*
		 * There is no reason to kick off a new thread, since only one thread
		 * should be processing at once. However, a handle to the thread is
		 * maintained to allow for interrupt
		 */
		processingThread = Thread.currentThread();
	}

	/**
	 * Interrupt the thread that is running the job if the {@link ExitStatus}
	 * indicates that it is still running.
	 * 
	 * @see org.springframework.batch.execution.launch.JobExecutionListenerSupport#stop(org.springframework.batch.core.domain.JobExecution)
	 */
	public void stop(JobExecution execution) {
		if (execution==null || execution.getExitStatus().isRunning()) {
			processingThread.interrupt();
		}
	}

	/**
	 * internal housekeeping.
	 * 
	 * @see org.springframework.batch.execution.launch.JobExecutionListenerSupport#after(org.springframework.batch.core.domain.JobExecution)
	 */
	public void after(JobExecution execution) {
		running--;
	}

}
