/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.DefaultRetryState;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
@SpringJUnitConfig(locations = "/org/springframework/batch/infrastructure/jms/jms-context.xml")
@DirtiesContext
class BatchMessageListenerContainerIntegrationTests {

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private BatchMessageListenerContainer container;

	private final BlockingQueue<String> recovered = new LinkedBlockingQueue<>();

	private final BlockingQueue<String> processed = new LinkedBlockingQueue<>();

	@AfterEach
	@BeforeEach
	void drainQueue() {
		container.stop();
		while (jmsTemplate.receiveAndConvert("queue") != null) {
			// do nothing
		}
		processed.clear();
	}

	@AfterAll
	static void giveContainerTimeToStop() throws Exception {
		Thread.sleep(1000);
	}

	@Test
	void testConfiguration() {
		assertNotNull(container);
	}

	@Test
	void testSendAndReceive() throws Exception {
		container.setMessageListener((MessageListener) msg -> {
			try {
				processed.add(((TextMessage) msg).getText());
			}
			catch (JMSException e) {
				throw new IllegalStateException(e);
			}
		});
		container.initializeProxy();
		container.start();
		jmsTemplate.convertAndSend("queue", "foo");
		jmsTemplate.convertAndSend("queue", "bar");
		SortedSet<String> result = new TreeSet<>();
		for (int i = 0; i < 2; i++) {
			result.add(processed.poll(5, TimeUnit.SECONDS));
		}
		assertEquals("[bar, foo]", result.toString());
	}

	@Test
	void testFailureAndRepresent() throws Exception {
		container.setMessageListener((MessageListener) msg -> {
			try {
				processed.add(((TextMessage) msg).getText());
			}
			catch (JMSException e) {
				throw new IllegalStateException(e);
			}
			throw new RuntimeException("planned failure for represent: " + msg);
		});
		container.initializeProxy();
		container.start();
		jmsTemplate.convertAndSend("queue", "foo");
		for (int i = 0; i < 2; i++) {
			assertEquals("foo", processed.poll(5, TimeUnit.SECONDS));
		}
	}

	@Test
	void testFailureAndRecovery() throws Exception {
		final RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new NeverRetryPolicy());
		container.setMessageListener((MessageListener) msg -> {
			try {
				RetryCallback<Message, Exception> callback = context -> {
					try {
						processed.add(((TextMessage) msg).getText());
					}
					catch (JMSException e) {
						throw new IllegalStateException(e);
					}
					throw new RuntimeException("planned failure: " + msg);
				};
				RecoveryCallback<Message> recoveryCallback = context -> {
					try {
						recovered.add(((TextMessage) msg).getText());
					}
					catch (JMSException e) {
						throw new IllegalStateException(e);
					}
					return msg;
				};
				retryTemplate.execute(callback, recoveryCallback, new DefaultRetryState(msg.getJMSMessageID()));
			}
			catch (Exception e) {
				throw (RuntimeException) e;
			}
		});
		container.initializeProxy();
		container.start();
		jmsTemplate.convertAndSend("queue", "foo");
		assertEquals("foo", processed.poll(5, TimeUnit.SECONDS));
		assertEquals("foo", recovered.poll(5, TimeUnit.SECONDS));
	}

}
