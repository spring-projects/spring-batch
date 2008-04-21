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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.AbstractItemWriter;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.jms.ExternalRetryInBatchTests;
import org.springframework.batch.retry.callback.ItemWriterRetryCallback;
import org.springframework.batch.retry.policy.ItemWriterRetryPolicy;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 * 
 */
public class BatchMessageListenerContainerIntegrationTests extends AbstractDependencyInjectionSpringContextTests {
	
	private final Log logger = LogFactory.getLog(getClass());

	private JmsTemplate jmsTemplate;

	private BatchMessageListenerContainer container;

	private int recovered;

	private int count;

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
				throw new RuntimeException("planned failure: " + msg);
			}
		});
		container.start();
		jmsTemplate.convertAndSend("queue", "foo");
		int waiting = 0;
		while (count < 2 && waiting++ < 10) {
			Thread.sleep(100L);
		}
		if (count < 2) {
			fail("Expected message to be processed twice.");
		}
	}

	public void testFailureAndRecovery() throws Exception {
		final RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new ItemWriterRetryPolicy(new NeverRetryPolicy()));
		container.setMessageListener(new MessageListener() {
			public void onMessage(final Message msg) {
				try {
					ItemWriterRetryCallback callback = new ItemWriterRetryCallback(msg, new AbstractItemWriter() {
						public void write(Object item) throws Exception {
							logger.debug("Message: "+item);
							count++;
							throw new RuntimeException("planned failure: " + msg);
						}
					});
					callback.setKeyGenerator(new ItemKeyGenerator() {
						public Object getKey(Object item) {
							String text;
							try {
								text = ((TextMessage)item).getJMSMessageID();
							}
							catch (JMSException e) {
								text = ""+item;
							}
							logger.debug("Key for message: "+text);
							return text;
						}
					});
					callback.setRecoverer(new ItemRecoverer() {
						public boolean recover(Object data, Throwable cause) {
							recovered++;
							logger.debug("Recovered: " + data);
							return true;
						}
					});
					retryTemplate.execute(callback);
				}
				catch (Exception e) {
					throw (RuntimeException) e;
				}
			}
		});
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
