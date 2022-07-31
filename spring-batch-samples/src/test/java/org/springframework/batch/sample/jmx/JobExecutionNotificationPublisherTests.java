/*
 * Copyright 2006-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.sample.jmx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.management.Notification;

import org.junit.jupiter.api.Test;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.UnableToSendNotificationException;

/**
 * @author Dave Syer
 * @author Thomas Risberg
 *
 */
class JobExecutionNotificationPublisherTests {

	private final JobExecutionNotificationPublisher publisher = new JobExecutionNotificationPublisher();

	@Test
	void testRepeatOperationsOpenUsed() {
		final List<Notification> list = new ArrayList<>();

		publisher.setNotificationPublisher(new NotificationPublisher() {
			@Override
			public void sendNotification(Notification notification) throws UnableToSendNotificationException {
				list.add(notification);
			}
		});

		publisher.onApplicationEvent(new SimpleMessageApplicationEvent(this, "foo"));
		assertEquals(1, list.size());
		String message = list.get(0).getMessage();
		assertTrue(message.contains("foo"), "Message does not contain 'foo': ");
	}

}
