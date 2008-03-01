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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.container.jms.BatchMessageListenerContainer;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.util.ReflectionUtils;

public class BatchMessageListenerContainerTests extends TestCase {

	BatchMessageListenerContainer container;

	int count = 0;

	public void testReceiveAndExecuteWithNoCallback() throws Exception {
		RepeatTemplate template = new RepeatTemplate() {
			public ExitStatus iterate(RepeatCallback callback) {
				count++;
				return ExitStatus.CONTINUABLE; // means we can continue to operate, but no message is received
			}
		};
		container = getContainer(template);
		boolean received = doExecute(null, null);
		assertEquals(1, count);
		assertFalse("Message received", received);
	}

	private BatchMessageListenerContainer getContainer(RepeatTemplate template) {
		MockControl connectionFactoryControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		BatchMessageListenerContainer container = new BatchMessageListenerContainer(template);
		container.setConnectionFactory(connectionFactory);
		return container;
	}

	public void testReceiveAndExecuteWithCallback() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);

		MockControl sessionControl = MockControl.createNiceControl(Session.class);
		MockControl consumerControl = MockControl.createControl(MessageConsumer.class);
		MockControl messageControl = MockControl.createControl(Message.class);

		Session session = (Session) sessionControl.getMock();
		MessageConsumer consumer = (MessageConsumer) consumerControl.getMock();
		Message message = (Message) messageControl.getMock();

		// Expect two calls to consumer (chunk size)...
		consumerControl.expectAndReturn(consumer.receive(1000), message);
		consumerControl.expectAndReturn(consumer.receive(1000), message);

		sessionControl.replay();
		consumerControl.replay();
		messageControl.replay();

		boolean received = doExecute(session, consumer);
		assertTrue("Message not received", received);

		sessionControl.verify();
		consumerControl.verify();
		messageControl.verify();

	}

	public void testReceiveAndExecuteWithCallbackReturningNull() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);

		MockControl sessionControl = MockControl.createNiceControl(Session.class);
		MockControl consumerControl = MockControl.createControl(MessageConsumer.class);

		Session session = (Session) sessionControl.getMock();
		MessageConsumer consumer = (MessageConsumer) consumerControl.getMock();
		Message message = null;

		// Expect one call to consumer (chunk size is 2 but terminates on
		// first)...
		consumerControl.expectAndReturn(consumer.receive(1000), message);

		sessionControl.replay();
		consumerControl.replay();

		boolean received = doExecute(session, consumer);
		assertFalse("Message not received", received);

		sessionControl.verify();
		consumerControl.verify();

	}

	public void testTransactionalReceiveAndExecuteWithCallbackThrowingException() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);
		container.setSessionTransacted(true);
		boolean received = doTestWithException(new IllegalStateException("No way!"), true, 2);
		assertFalse("Message received", received);
	}

	public void testNonTransactionalReceiveAndExecuteWithCallbackThrowingException() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);
		container.setSessionTransacted(false);
		boolean received = doTestWithException(new IllegalStateException("No way!"), false, 2);
		assertTrue("Message not received", received);
	}

	public void testNonTransactionalReceiveAndExecuteWithCallbackThrowingError() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);
		container.setSessionTransacted(false);
		try {
			boolean received = doTestWithException(new RuntimeException("No way!"), false, 2);
			assertTrue("Message not received", received);
		}
		catch (RuntimeException e) {
			assertEquals("No way!", e.getMessage());
			fail("Unexpected Error - should be swallowed");
		}
	}

	private boolean doTestWithException(final Throwable t, boolean expectRollback, int expectGetTransactionCount)
			throws JMSException, IllegalAccessException {
		container.setAcceptMessagesWhileStopping(true);
		container.setMessageListener(new MessageListener() {
			public void onMessage(Message arg0) {
				if (t instanceof RuntimeException)
					throw (RuntimeException) t;
				else
					throw (Error) t;
			}
		});

		MockControl sessionControl = MockControl.createNiceControl(Session.class);
		MockControl consumerControl = MockControl.createNiceControl(MessageConsumer.class);
		MockControl messageControl = MockControl.createNiceControl(Message.class);

		Session session = (Session) sessionControl.getMock();
		MessageConsumer consumer = (MessageConsumer) consumerControl.getMock();
		Message message = (Message) messageControl.getMock();

		sessionControl.expectAndReturn(session.getTransacted(), true, expectGetTransactionCount);

		// Expect only one call to consumer (chunk size is 2, but first one
		// rolls back terminating batch)...
		consumerControl.expectAndReturn(consumer.receive(1000), message);
		if (expectRollback) {
			session.rollback();
			sessionControl.setVoidCallable();
		}

		sessionControl.replay();
		consumerControl.replay();
		messageControl.replay();

		boolean received = doExecute(session, consumer);

		sessionControl.verify();
		consumerControl.verify();
		messageControl.verify();
		return received;
	}

	private boolean doExecute(Session session, MessageConsumer consumer) throws IllegalAccessException {
		Method method = ReflectionUtils.findMethod(container.getClass(), "receiveAndExecute", new Class[] {
				Session.class, MessageConsumer.class });
		method.setAccessible(true);
		boolean received;
		try {
			received = ((Boolean) method.invoke(container, new Object[] { session, consumer })).booleanValue();
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			} else {
				throw (Error) e.getCause();
			}
		}
		return received;
	}

}
