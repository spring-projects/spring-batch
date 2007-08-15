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

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.runtime.JobIdentifier;
import org.springframework.batch.core.runtime.JobIdentifierFactory;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.execution.JobExecutorFacade;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;

public class SimpleJobLauncherTests extends TestCase {

	public void testAutoStartContainer() throws Exception {

		MockControl containerControl = MockControl.createControl(JobExecutorFacade.class);
		JobExecutorFacade mockContainer;

		AbstractJobLauncher bootstrap = new SimpleJobLauncher();
		final SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier("foo");
		bootstrap.setJobRuntimeInformationFactory(new JobIdentifierFactory() {
			public JobIdentifier getJobIdentifier(String name) {
				return runtimeInformation;
			}
		});
		mockContainer = (JobExecutorFacade) containerControl.getMock();
		bootstrap.setBatchContainer(mockContainer);

		JobConfiguration jobConfiguration = new JobConfiguration("foo");
		bootstrap.setJobConfigurationName(jobConfiguration.getName());

		mockContainer.start(runtimeInformation);
		containerControl.replay();

		bootstrap.setAutoStart(true);

		bootstrap.onApplicationEvent(new ContextRefreshedEvent(new GenericApplicationContext()));
		// It ran and then stopped...
		assertFalse(bootstrap.isRunning());

		containerControl.verify();
	}

	public void testApplicationEventNotContextRefresh() throws Exception {

		MockControl containerControl = MockControl.createControl(JobExecutorFacade.class);
		JobExecutorFacade mockContainer;

		AbstractJobLauncher bootstrap = new SimpleJobLauncher();
		mockContainer = (JobExecutorFacade) containerControl.getMock();
		bootstrap.setBatchContainer(mockContainer);

		containerControl.replay();

		bootstrap.setAutoStart(true);

		bootstrap.onApplicationEvent(new ApplicationEvent(new GenericApplicationContext()) {
		});
		assertFalse(bootstrap.isRunning());

		containerControl.verify();
	}

	public void testStartWithNoConfiguration() throws Exception {
		final AbstractJobLauncher bootstrap = new SimpleJobLauncher();
		try {
			bootstrap.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().indexOf("required") >= 0);
		}
	}

	public void testInitializeWithNoConfiguration() throws Exception {
		final AbstractJobLauncher bootstrap = new SimpleJobLauncher();
		try {
			bootstrap.start();
			// should do nothing
		}
		catch (Exception e) {
			fail("Unexpected IllegalStateException");
		}
	}

	public void testStartTwiceNotFatal() throws Exception {
		AbstractJobLauncher bootstrap = new SimpleJobLauncher();
		final SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier("foo");
		bootstrap.setJobRuntimeInformationFactory(new JobIdentifierFactory() {
			public JobIdentifier getJobIdentifier(String name) {
				return runtimeInformation;
			}
		});
		InterruptibleContainer container = new InterruptibleContainer();
		bootstrap.setBatchContainer(container);
		bootstrap.setJobConfigurationName(new JobConfiguration("foo").getName());
		bootstrap.start();
		bootstrap.start();
		// Both jobs finished running because they were not launched in a new
		// Thread
		assertFalse(bootstrap.isRunning());
	}

	public void testInterruptContainer() throws Exception {
		final AbstractJobLauncher bootstrap = new SimpleJobLauncher();
		final SimpleJobIdentifier runtimeInformation = new SimpleJobIdentifier("foo");
		bootstrap.setJobRuntimeInformationFactory(new JobIdentifierFactory() {
			public JobIdentifier getJobIdentifier(String name) {
				return runtimeInformation;
			}
		});

		InterruptibleContainer container = new InterruptibleContainer();
		bootstrap.setBatchContainer(container);
		bootstrap.setJobConfigurationName(new JobConfiguration("foo").getName());

		Thread bootstrapThread = new Thread() {
			public void run() {
				bootstrap.start();
			}
		};

		bootstrapThread.start();

		// give the thread a second to start up
		Thread.sleep(100);
		assertTrue(bootstrap.isRunning());
		bootstrap.stop();
		Thread.sleep(100);
		assertFalse(bootstrap.isRunning());
	}

	public void testStopOnUnstartedContainer() {

		AbstractJobLauncher bootstrap = new SimpleJobLauncher();

		assertFalse(bootstrap.isRunning());
		// no exception should be thrown if stop is called on unstarted
		// container
		// this is to fullfill the contract outlined in Lifecycle#stop().
		bootstrap.stop();
	}

	private class InterruptibleContainer implements JobExecutorFacade {
		/*
		 * (non-Javadoc)
		 * @see org.springframework.batch.container.BatchContainer#start()
		 */
		public void start() {
			try {
				// 1 seconds should be long enough to allow the thread to be
				// started and
				// for interrupt to be called;
				Thread.sleep(300);
			}
			catch (InterruptedException ex) {
				// thread interrupted, allow to exit normally
			}
		}

		public void start(JobIdentifier runtimeInformation) {
			start();
		}

		public void stop(JobIdentifier runtimeInformation) {
			// not needed
		}
		
		public boolean isRunning() {
			// not needed
			return false;
		}
	}

}
