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

import java.util.Collections;

import junit.framework.TestCase;

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.executor.JobExecutor;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.execution.bootstrap.support.ThreadInterruptJobExecutionListener;
import org.springframework.batch.execution.configuration.MapJobConfigurationRegistry;
import org.springframework.batch.execution.launch.JobExecutorFacade;
import org.springframework.batch.execution.launch.SimpleJobExecutorFacade;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.MapJobDao;
import org.springframework.batch.execution.repository.dao.MapStepDao;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * @author Dave Syer
 * 
 */
public class InterruptJobTests extends TestCase {

	public void testInterruptUsingListener() throws Exception {

		// final InterruptibleFacade facade = new InterruptibleFacade();
		// facade.setListener(new ThreadInterruptJobExecutionListener());
		final SimpleJobExecutorFacade facade = new SimpleJobExecutorFacade();
		facade.setJobExecutor(new InterruptibleJobExecutor());

		facade.setJobRepository(new SimpleJobRepository(new MapJobDao(),
				new MapStepDao()));

		facade.setJobExecutionListeners(Collections
				.singletonList(new ThreadInterruptJobExecutionListener()));

		MapJobConfigurationRegistry registry = new MapJobConfigurationRegistry();
		facade.setJobConfigurationLocator(registry);

		registry.register(new JobConfiguration("foo"));
		final SimpleJobIdentifier identifier = new SimpleJobIdentifier("foo");
		final JobExecution execution = facade.createExecutionFrom(identifier);

		TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		Runnable launcherRunnable = new Runnable() {
			public void run() {
				try {
					facade.start(execution);
				} catch (NoSuchJobConfigurationException e) {
					fail("Unexpected NoSuchJobConfigurationException");
				}
			}
		};

		taskExecutor.execute(launcherRunnable);

		// give the thread a second to start up
		Thread.sleep(100);
		assertTrue(facade.isRunning());
		facade.stop(execution);
		Thread.sleep(100);
		assertFalse(facade.isRunning());
	}

	/**
	 * Simple {@link JobExecutorFacade} that can be used to test thread
	 * interruption. Mimics the implementation of the
	 * {@link SimpleJobExecutorFacade} with the use of a listener, but silently
	 * allows the current thread to be interrupted.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private class InterruptibleJobExecutor implements JobExecutor {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.batch.core.executor.JobExecutor#run(org.springframework.batch.core.configuration.JobConfiguration,
		 *      org.springframework.batch.core.domain.JobExecution)
		 */
		public ExitStatus run(JobConfiguration configuration,
				JobExecution execution) throws BatchCriticalException {
			try {
				// 1 seconds should be long enough to allow the thread to be
				// run and for interrupt to be called;
				Thread.sleep(3000);
				return ExitStatus.FAILED;

			} catch (InterruptedException ex) {
				// thread interrupted, allow to exit normally
				return ExitStatus.FAILED;
			}
		}

	}

}
