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

import java.util.Properties;

import javax.management.Notification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.execution.facade.JobExecutorFacade;
import org.springframework.batch.execution.facade.NoSuchJobExecutionException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.interceptor.RepeatOperationsApplicationEvent;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;
import org.springframework.util.Assert;

/**
 * Bootstrapping mechanism for running job executions concurrently with a
 * {@link JobExecutorFacade}.
 * 
 * <p>
 * This implementation can run jobs asynchronously. Jobs are stopped by calling
 * the stop method in the {@link JobExecutorFacade}, which is a graceful shutdown.
 * </p>
 * 
 * @see JobExecutorFacade
 * @author Dave Syer
 * @since 2.1
 */
public class TaskExecutorJobLauncher extends AbstractJobLauncher implements
		ApplicationListener, NotificationPublisherAware,
		ApplicationEventPublisherAware {

	private static final Log logger = LogFactory
			.getLog(TaskExecutorJobLauncher.class);

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	private NotificationPublisher notificationPublisher;

	private int notificationCount = 0;

	private ApplicationEventPublisher applicationEventPublisher;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher)
	 */
	public void setApplicationEventPublisher(
			ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * Setter for the {@link TaskExecutor}. Defaults to a
	 * {@link SyncTaskExecutor}.
	 * 
	 * @param taskExecutor
	 *            the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Injection setter.
	 * 
	 * @see org.springframework.jmx.export.notification.NotificationPublisherAware#setNotificationPublisher(org.springframework.jmx.export.notification.NotificationPublisher)
	 */
	public void setNotificationPublisher(
			NotificationPublisher notificationPublisher) {
		this.notificationPublisher = notificationPublisher;
	}

	/**
	 * Start the job using the task executor provided.
	 * 
	 * @throws NoSuchJobConfigurationException
	 *             if a job configuration cannot be located.
	 */
	protected ExitStatus doRun(final JobIdentifier jobIdentifier,
			final Runnable exitCallback) {

		Assert.state(taskExecutor != null, "TaskExecutor must be provided");

		taskExecutor.execute(new Runnable() {
			public void run() {
				try {
					jobExecutorFacade.start(jobIdentifier);
				} catch (NoSuchJobConfigurationException e) {
					applicationEventPublisher
							.publishEvent(new RepeatOperationsApplicationEvent(
									jobIdentifier, "No such job",
									RepeatOperationsApplicationEvent.ERROR));
					logger
							.error(
									"JobConfiguration could not be located inside Runnable for identifier: ["
											+ jobIdentifier + "]", e);
				} finally {
					exitCallback.run();
				}
			}
		});

		return ExitStatus.RUNNING;

	}

	/**
	 * Delegates to the underlying {@link JobExecutorFacade}. Does not wait for
	 * the jobs to stop (therefore returns immediately by default).
	 * 
	 * @throws NoSuchJobExecutionException
	 * 
	 * @see org.springframework.context.Lifecycle#stop()
	 */
	protected void doStop(JobIdentifier jobIdentifier)
			throws NoSuchJobExecutionException {
		jobExecutorFacade.stop(jobIdentifier);
		// TODO: wait for the jobs to stop?
	}

	/**
	 * If the event is a {@link RepeatOperationsApplicationEvent} for open and
	 * close we log the event at INFO level and send a JMX notification if we
	 * are also an MBean.
	 * 
	 * @see org.springframework.batch.execution.bootstrap.AbstractJobLauncher#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		super.onApplicationEvent(applicationEvent);
		if (applicationEvent instanceof RepeatOperationsApplicationEvent) {
			RepeatOperationsApplicationEvent event = (RepeatOperationsApplicationEvent) applicationEvent;
			int type = event.getType();
			if (type == RepeatOperationsApplicationEvent.OPEN
					|| type == RepeatOperationsApplicationEvent.CLOSE
					|| type == RepeatOperationsApplicationEvent.ERROR) {
				String message = event.getMessage() + "; source="
						+ event.getSource();
				logger.info(message);
				publish(message);
			}
			return;
		}
	}

	/**
	 * Accessor for the job executions passed back in response to a call to
	 * {@link #requestContextNotification()}. Because the request is
	 * potentially fulfilled asynchronously, and only on demand, the data might
	 * be out of date by the time this method is called, so it should be used
	 * for information purposes only.
	 * 
	 * @return Properties representing the last {@link JobExecutionContext}
	 *         objects passed up from the underlying execution. If there are no
	 *         jobs running it will be empty.
	 */
	public Properties getStatistics() {
		if (jobExecutorFacade instanceof StatisticsProvider) {
			return ((StatisticsProvider) jobExecutorFacade).getStatistics();
		} else {
			return new Properties();
		}
	}

	/**
	 * Publish the provided message to an external listener if there is one.
	 * 
	 * @param message the message to publish
	 */
	private void publish(String message) {
		if (notificationPublisher != null) {
			Notification notification = new Notification(
					"RepeatOperationsApplicationEvent", this,
					notificationCount++, message);
			/*
			 * We can't create a notification with a null source, but we can set
			 * it to null after creation(!). We want it to be null so that
			 * Spring will replace it automatically with the ObjectName (in
			 * ModelMBeanNotificationPublisher).
			 */
			notification.setSource(null);
			notificationPublisher.sendNotification(notification);
		}
	}

}
