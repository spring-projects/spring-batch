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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.core.runtime.SimpleJobIdentifierFactory;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.timer.TimerTaskExecutor;

public class TaskExecutorJobLauncherTests extends TestCase {

	private SimpleJobLauncher launcher = new SimpleJobLauncher();

	protected void setUp() throws Exception {
		super.setUp();
		launcher.setJobIdentifierFactory(new SimpleJobIdentifierFactory());
	}

	public void testStopContainer() throws Exception {

		// Important (otherwise start() does not return!)
		launcher.setTaskExecutor(new SimpleAsyncTaskExecutor());

		InterruptibleContainer container = new InterruptibleContainer();
		launcher.setJobExecutorFacade(container);
		launcher.setJobConfigurationName(new JobConfiguration("foo").getName());

		JobExecution execution = launcher.run();
		// give the thread some time to start up...
		Thread.sleep(100);
		assertTrue(launcher.isRunning());
		launcher.stop();
		// ...and to shut down:
		Thread.sleep(400);
		assertFalse(launcher.isRunning());
		assertEquals("COMPLETED_BY_TEST", execution.getExitStatus().getExitCode());
	}

	public void testStopContainerWhenJobNotRunning() throws Exception {

		final List list = new ArrayList();
		
		// Important (otherwise start() does not return!)
		TimerTaskExecutor taskExecutor = new TimerTaskExecutor(new Timer() {
			public void schedule(final TimerTask task, long delay) {
				TimerTask wrapper = new TimerTask() {
					public void run() {
						list.add(task);
						task.run();
					}
				};
				super.schedule(wrapper, 400);
			}
		});
		taskExecutor.afterPropertiesSet();
		launcher.setTaskExecutor(taskExecutor);

		InterruptibleContainer container = new InterruptibleContainer();
		launcher.setJobExecutorFacade(container);
		launcher.setJobConfigurationName("foo");

		JobExecution execution = launcher.run();
		// give the thread some time to start up...
		Thread.sleep(100);
		// The launcher thinks it has started the job...
		assertTrue(launcher.isRunning());
		// ...but the task has not been started yet
		assertEquals(0, list.size());
		launcher.stop();
		// ...and to shut down:
		Thread.sleep(1000);
		assertFalse(launcher.isRunning());
		// The timer task has been started...
		assertEquals(1, list.size());
		// ...but the job is not executed
		assertEquals(ExitStatus.UNKNOWN, execution.getExitStatus());
	}

	public void testRunTwice() throws Exception {

		// Important (otherwise start() does not return!)
		launcher.setTaskExecutor(new SimpleAsyncTaskExecutor());

		InterruptibleContainer container = new InterruptibleContainer();
		launcher.setJobExecutorFacade(container);
		launcher.setJobConfigurationName(new JobConfiguration("foo").getName());

		launcher.run();
		// give the thread some time to start up:
		Thread.sleep(100);
		assertTrue(launcher.isRunning());
		try {
			launcher.run();
			fail("Expected JobExecutionAlreadyRunningException");
		} catch (JobExecutionAlreadyRunningException e) {
			// expected
		}
		// give the thread some time to start up...
		Thread.sleep(100);
		launcher.stop();
		// ...and to shut down:
		Thread.sleep(400);
		assertFalse(launcher.isRunning());
	}

	public void testNormalApplicationEventNotRecognized() throws Exception {
		launcher.onApplicationEvent(new ApplicationEvent("foo") {
		});
		// nothing happens
	}

	public void testStatisticsRetrieved() throws Exception {
		MockControl control = MockControl
				.createControl(JobExecutorFacadeWithStatistics.class);
		JobExecutorFacadeWithStatistics batchContainer = (JobExecutorFacadeWithStatistics) control
				.getMock();
		launcher.setJobExecutorFacade(batchContainer);

		Properties properties = PropertiesConverter.stringToProperties("a=b");
		control.expectAndReturn(batchContainer.getStatistics(), properties);

		control.replay();
		assertEquals(properties, launcher.getStatistics());
		control.verify();
	}

	public void testStatisticsNotRetrieved() throws Exception {
		MockControl control = MockControl
				.createControl(JobExecutorFacade.class);
		JobExecutorFacade batchContainer = (JobExecutorFacade) control
				.getMock();
		launcher.setJobExecutorFacade(batchContainer);

		Properties properties = new Properties();
		control.replay();
		assertEquals(properties, launcher.getStatistics());
		control.verify();
	}

	public void testPublishApplicationEvent() throws Exception {
		final List list = new ArrayList();
		launcher.setApplicationEventPublisher(new ApplicationEventPublisher() {
			public void publishEvent(ApplicationEvent event) {
				list.add(event);
			}
		});

		MockControl control = MockControl
				.createControl(JobExecutorFacade.class);
		JobExecutorFacade facade = (JobExecutorFacade) control.getMock();
		launcher.setJobExecutorFacade(facade);
		SimpleJobIdentifier jobRuntimeInformation = new SimpleJobIdentifier(
				"spam");
		JobExecution execution = new JobExecution(new JobInstance(
				jobRuntimeInformation));
		control.expectAndReturn(facade
				.createExecutionFrom(jobRuntimeInformation), execution);
		facade.start(execution);
		control.setThrowable(new NoSuchJobConfigurationException("SPAM"));

		control.replay();
		launcher.run(jobRuntimeInformation);
		assertEquals(1, list.size());
		control.verify();
	}

	private class InterruptibleContainer implements JobExecutorFacade {
		private volatile boolean running = true;

		private void start() {
			while (running) {
				try {
					// 1 seconds should be long enough to allow the thread to be
					// started and
					// for interrupt to be called;
					Thread.sleep(300);
				} catch (InterruptedException ex) {
					// thread interrupted, allow to exit normally
				}
			}
		}

		public void start(JobExecution execution)
				throws NoSuchJobConfigurationException {
			start();
			execution.setExitStatus(new ExitStatus(false, "COMPLETED_BY_TEST"));
		}

		public JobExecution createExecutionFrom(JobIdentifier jobIdentifier)
				throws NoSuchJobConfigurationException {
			return new JobExecution(new JobInstance(jobIdentifier));
		}

		public void stop(JobExecution execution) {
			running = false;
		}

		public boolean isRunning() {
			// not needed
			return false;
		}
	}

	private interface JobExecutorFacadeWithStatistics extends
			JobExecutorFacade, StatisticsProvider {
	}

}
