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


import javax.management.Notification;

import org.springframework.batch.repeat.interceptor.RepeatOperationsApplicationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;

/**
 * JMX notification broadcaster
 * 
 * @author Dave Syer
 * @since 2.1
 */
public class JobExecutionNotificationPublisher implements
		ApplicationListener, NotificationPublisherAware {

	private NotificationPublisher notificationPublisher;

	private int notificationCount = 0;

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
	 * If the event is a {@link RepeatOperationsApplicationEvent} for open and
	 * close we log the event at INFO level and send a JMX notification if we
	 * are also an MBean.
	 * 
	 * @see org.springframework.batch.execution.bootstrap.SimpleJobLauncher#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if (applicationEvent instanceof RepeatOperationsApplicationEvent) {
			RepeatOperationsApplicationEvent event = (RepeatOperationsApplicationEvent) applicationEvent;
			int type = event.getType();
			if (type == RepeatOperationsApplicationEvent.OPEN
					|| type == RepeatOperationsApplicationEvent.CLOSE
					|| type == RepeatOperationsApplicationEvent.ERROR) {
				String message = event.getMessage() + "; source="
						+ event.getSource();
				SimpleJobLauncher.logger.info(message);
				publish(message);
			}
			return;
		}
	}

	/**
	 * Publish the provided message to an external listener if there is one.
	 * 
	 * @param message
	 *            the message to publish
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
