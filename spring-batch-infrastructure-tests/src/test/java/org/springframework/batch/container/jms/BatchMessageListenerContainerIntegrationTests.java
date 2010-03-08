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
package org.springframework.batch.container.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.support.DefaultRetryState;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/jms/jms-context.xml")
public class BatchMessageListenerContainerIntegrationTests {

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private BatchMessageListenerContainer container;

	private volatile BlockingQueue<String> recovered = new LinkedBlockingQueue<String>();

	private volatile BlockingQueue<String> processed = new LinkedBlockingQueue<String>();

	@After
	@Before
	public void drainQueue() throws Exception {
		container.stop();
		while (jmsTemplate.receiveAndConvert("queue") != null) {
			// do nothing
		}
		processed.clear();
	}

	@AfterClass
	public static void giveContainerTimeToStop() throws Exception {
		Thread.sleep(1000);
	}

	@Test
	public void testConfiguration() throws Exception {
		assertNotNull(container);
	}

	@Test
	public void testSendAndReceive() throws Exception {
		container.setMessageListener(new MessageListener() {
			public void onMessage(Message msg) {
				try {
					processed.add(((TextMessage) msg).getText());
				}
				catch (JMSException e) {
					throw new IllegalStateException(e);
				}
			}
		});
		container.initializeProxy();
		container.start();
		jmsTemplate.convertAndSend("queue", "foo");
		jmsTemplate.convertAndSend("queue", "bar");
		SortedSet<String> result = new TreeSet<String>();
		for (int i = 0; i < 2; i++) {
			result.add(processed.poll(5, TimeUnit.SECONDS));
		}
		assertEquals("[bar, foo]", result.toString());
	}

	@Test
	public void testFailureAndRepresent() throws Exception {
		container.setMessageListener(new MessageListener() {
			public void onMessage(Message msg) {
				try {
					processed.add(((TextMessage) msg).getText());
				}
				catch (JMSException e) {
					throw new IllegalStateException(e);
				}
				throw new RuntimeException("planned failure for represent: " + msg);
			}
		});
		container.initializeProxy();
		container.start();
		jmsTemplate.convertAndSend("queue", "foo");
		for (int i = 0; i < 2; i++) {
			assertEquals("foo", processed.poll(5, TimeUnit.SECONDS));
		}
	}

	@Test
	public void testFailureAndRecovery() throws Exception {
		final RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new NeverRetryPolicy());
		container.setMessageListener(new MessageListener() {
			public void onMessage(final Message msg) {
				try {
					RetryCallback<Message> callback = new RetryCallback<Message>() {
						public Message doWithRetry(RetryContext context) throws Exception {
							try {
								processed.add(((TextMessage) msg).getText());
							}
							catch (JMSException e) {
								throw new IllegalStateException(e);
							}
							throw new RuntimeException("planned failure: " + msg);
						}
					};
					RecoveryCallback<Message> recoveryCallback = new RecoveryCallback<Message>() {
						public Message recover(RetryContext context) {
							try {
								recovered.add(((TextMessage) msg).getText());
							}
							catch (JMSException e) {
								throw new IllegalStateException(e);
							}
							return msg;
						}
					};
					retryTemplate.execute(callback, recoveryCallback, new DefaultRetryState(msg.getJMSMessageID()));
				}
				catch (Exception e) {
					throw (RuntimeException) e;
				}
			}
		});
		container.initializeProxy();
		container.start();
		jmsTemplate.convertAndSend("queue", "foo");
		assertEquals("foo", processed.poll(5, TimeUnit.SECONDS));
		assertEquals("foo", recovered.poll(5, TimeUnit.SECONDS));
	}

}
