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

import static org.junit.Assert.*;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.callback.RecoveryRetryCallback;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.policy.RecoveryCallbackRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

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

	private int recovered;

	private volatile int count;

	@Before
	public void onSetUp() throws Exception {
		while(jmsTemplate.receiveAndConvert("queue")!=null) {
			// do nothing
		}
	}

	@After
	public void onTearDown() throws Exception {
		container.stop();
		while(jmsTemplate.receiveAndConvert("queue")!=null) {
			// do nothing
		}
	}

	@Test
	public void testConfiguration() throws Exception {
		assertNotNull(container);
	}

	@Test
	public void testSendAndReceive() throws Exception {
		container.setMessageListener(new MessageListener() {
			public void onMessage(Message msg) {
				count++;
			}
		});
		container.initializeProxy();
		container.start();
		jmsTemplate.convertAndSend("queue", "foo");
		jmsTemplate.convertAndSend("queue", "bar");
		int waiting = 0;
		while (count < 2 && waiting++ < 10) {
			Thread.sleep(100L);
		}
		if (count < 2) {
			fail("Expected message to be processed.");
		}
	}

	@Test
	public void testFailureAndRepresent() throws Exception {
		container.setMessageListener(new MessageListener() {
			public void onMessage(Message msg) {
				count++;
				throw new RuntimeException("planned failure for represent: " + msg);
			}
		});
		container.initializeProxy();
		container.start();
		jmsTemplate.convertAndSend("queue", "foo");
		int waiting = 0;
		while (count < 2 && waiting++ < 20) {
			Thread.sleep(100L);
		}
		if (count < 2) {
			fail("Expected message to be processed twice.");
		}
	}

	@Test
	public void testFailureAndRecovery() throws Exception {
		final RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new RecoveryCallbackRetryPolicy(new NeverRetryPolicy()));
		container.setMessageListener(new MessageListener() {
			public void onMessage(final Message msg) {
				try {
					RecoveryRetryCallback callback = new RecoveryRetryCallback(msg, new RetryCallback() {
						public Object doWithRetry(RetryContext context) throws Throwable {
							count++;
							throw new RuntimeException("planned failure: " + msg);
						}
					}, msg.getJMSMessageID());
					callback.setRecoveryCallback(new RecoveryCallback() {
						public Object recover(RetryContext context) {
							recovered++;
							return msg;
						}
					});
					retryTemplate.execute(callback);
				}
				catch (Exception e) {
					throw (RuntimeException) e;
				}
			}
		});
		container.initializeProxy();
		container.start();
		jmsTemplate.convertAndSend("queue", "foo");
		int waiting = 0;
		while ((count < 1 || recovered < 1) && waiting++ < 1000) {
			Thread.sleep(100L);
		}
		assertEquals(1, count);
		assertEquals(1, recovered);
	}

}
