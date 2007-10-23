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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.management.Notification;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.executor.JobExecutionListener;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.core.runtime.SimpleJobIdentifierFactory;
import org.springframework.batch.execution.facade.JobExecutorFacade;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.interceptor.RepeatOperationsApplicationEvent;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.UnableToSendNotificationException;

public class TaskExecutorJobLauncherTests extends TestCase {

	private TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();

	protected void setUp() throws Exception {
		super.setUp();
		launcher
				.setJobIdentifierFactory(new SimpleJobIdentifierFactory());
	}

	public void testStopContainer() throws Exception {

		// Important (otherwise start() does not return!)
		launcher.setTaskExecutor(new SimpleAsyncTaskExecutor());

		InterruptibleContainer container = new InterruptibleContainer();
		launcher.setJobExecutorFacade(container);
		launcher.setJobConfigurationName(new JobConfiguration("foo").getName());

		launcher.run();
		// give the thread some time to start up:
		Thread.sleep(100);
		assertTrue(launcher.isRunning());
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

	public void testRepeatOperationsBeforeNotUsed() throws Exception {
		final List list = new ArrayList();
		launcher.setNotificationPublisher(new NotificationPublisher() {
			public void sendNotification(Notification notification)
					throws UnableToSendNotificationException {
				list.add(notification);
			}
		});
		launcher.onApplicationEvent(new RepeatOperationsApplicationEvent(this,
				"foo", RepeatOperationsApplicationEvent.BEFORE) {
		});
		assertEquals(0, list.size());
	}

	public void testRepeatOperationsOpenUsed() throws Exception {
		final List list = new ArrayList();
		launcher.setNotificationPublisher(new NotificationPublisher() {
			public void sendNotification(Notification notification)
					throws UnableToSendNotificationException {
				list.add(notification);
			}
		});
		launcher.onApplicationEvent(new RepeatOperationsApplicationEvent(this,
				"foo", RepeatOperationsApplicationEvent.OPEN));
		assertEquals(1, list.size());
		assertEquals("foo", ((Notification) list.get(0)).getMessage()
				.substring(0, 3));
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

	private class InterruptibleContainer implements JobExecutorFacade {
		private volatile boolean running = true;

		public void start() {
			while (running) {
				try {
					// 1 seconds should be long enough to allow the thread to be
					// started and
					// for interrupt to be called;
					Thread.sleep(300);
				} catch (InterruptedException ex) {
					// thread intterrupted, allow to exit normally
				}
			}
		}
		
		public ExitStatus start(JobIdentifier jobIdentifier,
				JobExecutionListener listener)
				throws NoSuchJobConfigurationException {
			throw new UnsupportedOperationException("Not implemented");
		}

		public ExitStatus start(JobIdentifier runtimeInformation) {
			start();
			return ExitStatus.FAILED;
		}
		
		public void stop(JobIdentifier runtimeInformation) {
			running = false;
		}

		public boolean isRunning() {
			// not needed
			return false;
		}
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
		JobExecutorFacade batchContainer = (JobExecutorFacade) control
				.getMock();
		launcher.setJobExecutorFacade(batchContainer);
		SimpleJobIdentifier jobRuntimeInformation = new SimpleJobIdentifier(
				"spam");
		batchContainer.start(jobRuntimeInformation);
		control.setThrowable(new NoSuchJobConfigurationException("SPAM"));

		control.replay();
		launcher.run(jobRuntimeInformation);
		assertEquals(1, list.size());
		control.verify();
	}

	private interface JobExecutorFacadeWithStatistics extends
			JobExecutorFacade, StatisticsProvider {
	}

}
