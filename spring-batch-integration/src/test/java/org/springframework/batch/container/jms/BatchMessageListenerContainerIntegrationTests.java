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

import javax.jms.Message;
import javax.jms.MessageListener;

import org.springframework.batch.jms.ExternalRetryInBatchTests;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.callback.RecoveryRetryCallback;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.policy.RecoveryCallbackRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 * 
 */
public class BatchMessageListenerContainerIntegrationTests extends AbstractDependencyInjectionSpringContextTests {
	
	private JmsTemplate jmsTemplate;

	private BatchMessageListenerContainer container;

	private int recovered;

	private volatile int count;

	/**
	 * Public setter for the {@link BatchMessageListenerContainer}.
	 * @param container the container to set
	 */
	public void setContainer(BatchMessageListenerContainer container) {
		this.container = container;
	}

	/**
	 * Public setter for the JmsTemplate.
	 * @param jmsTemplate the jmsTemplate to set
	 */
	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.AbstractSingleSpringContextTests#getConfigLocations()
	 */
	protected String[] getConfigLocations() {
		// Share config with other test so that ActiveMQ only starts up once.
		return new String[] { ClassUtils.addResourcePathToPackagePath(ExternalRetryInBatchTests.class, "jms-context.xml") };
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.test.AbstractSingleSpringContextTests#onSetUp()
	 */
	protected void onSetUp() throws Exception {
		while(jmsTemplate.receiveAndConvert("queue")!=null) {
			// do nothing
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.AbstractSingleSpringContextTests#onTearDown()
	 */
	protected void onTearDown() throws Exception {
		container.stop();
		while(jmsTemplate.receiveAndConvert("queue")!=null) {
			// do nothing
		}
	}

	public void testConfiguration() throws Exception {
		assertNotNull(container);
	}

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

	public void testFailureAndRepresent() throws Exception {
		container.setMessageListener(new MessageListener() {
			public void onMessage(Message msg) {
				logger.debug("Message: "+msg);
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
			logger.debug("Count: "+count);
			fail("Expected message to be processed twice.");
		}
	}

	public void testFailureAndRecovery() throws Exception {
		final RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new RecoveryCallbackRetryPolicy(new NeverRetryPolicy()));
		container.setMessageListener(new MessageListener() {
			public void onMessage(final Message msg) {
				try {
					RecoveryRetryCallback callback = new RecoveryRetryCallback(msg, new RetryCallback() {
						public Object doWithRetry(RetryContext context) throws Throwable {
							logger.debug("Message: "+msg);
							count++;
							throw new RuntimeException("planned failure: " + msg);
						}
					}, msg.getJMSMessageID());
					callback.setRecoveryCallback(new RecoveryCallback() {
						public Object recover(Throwable throwable) {
							recovered++;
							logger.debug("Recovered: " + msg);
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
