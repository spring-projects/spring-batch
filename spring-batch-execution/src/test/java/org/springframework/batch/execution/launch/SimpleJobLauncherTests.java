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

import junit.framework.TestCase;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.NoSuchJobException;
import org.springframework.batch.core.runtime.SimpleJobIdentifierFactory;

public class SimpleJobLauncherTests extends TestCase {

	public void testStartWithNoConfiguration() throws Exception {
		final SimpleJobLauncher launcher = new SimpleJobLauncher();
		try {
			launcher.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().indexOf("required") >= 0);
		}
	}

	public void testInitializeWithNoConfiguration() throws Exception {
		final SimpleJobLauncher launcher = new SimpleJobLauncher();
		try {
			launcher.run();
			// should do nothing
			fail("Expected NoSuchJobConfigurationException");
		} catch (NoSuchJobException e) {
			assertTrue("Message should mention null job name: "
					+ e.getMessage(), e.getMessage().toLowerCase().indexOf(
					"null") >= 0);
		}
	}

	public void testRunTwiceNotFatal() throws Exception {
		SimpleJobLauncher launcher = new SimpleJobLauncher();
		launcher.setJobIdentifierFactory(new SimpleJobIdentifierFactory());
		InterruptibleFacade jobExecutorFacade = new InterruptibleFacade();
		launcher.setJobExecutorFacade(jobExecutorFacade);
		launcher.setJobName(new Job("foo").getName());
		launcher.run();
		assertFalse(launcher.isRunning());
		launcher.run();
		// Both jobs finished running because they were not launched in a new
		// Thread
		assertFalse(launcher.isRunning());
	}

	public void testStopOnNotRunningLauncher() {

		SimpleJobLauncher launcher = new SimpleJobLauncher();

		assertFalse(launcher.isRunning());
		// no exception should be thrown if stop is called on
		// a launcher that is not running.
		launcher.stop();
	}

	private class InterruptibleFacade implements JobExecutorFacade {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.batch.container.BatchContainer#run()
		 */
		public void run() {
			try {
				// 1 seconds should be long enough to allow the thread to be
				// run and for interrupt to be called;
				Thread.sleep(300);
				// return ExitStatus.FAILED;

			} catch (InterruptedException ex) {
				// thread interrupted, allow to exit normally
				// return ExitStatus.FAILED;
			}
		}

		public void start(JobExecution execution)
				throws NoSuchJobException {
			run();
		}

		public JobExecution createExecutionFrom(JobIdentifier jobIdentifier)
				throws NoSuchJobException {
			return new JobExecution(new JobInstance(jobIdentifier));
		}

		public void stop(JobExecution execution) {
			// not needed
		}

		public boolean isRunning() {
			// not needed
			return false;
		}
	}

}
