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

import org.aopalliance.aop.Advice;
import org.easymock.MockControl;
import org.springframework.batch.repeat.interceptor.RepeatOperationsInterceptor;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.util.ReflectionUtils;

public class BatchMessageListenerContainerTests extends TestCase {

	BatchMessageListenerContainer container;

	int count = 0;

	public void testReceiveAndExecuteWithCallback() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);

		MockControl<Session> sessionControl = MockControl.createNiceControl(Session.class);
		MockControl<MessageConsumer> consumerControl = MockControl.createControl(MessageConsumer.class);
		MockControl<Message> messageControl = MockControl.createControl(Message.class);

		Session session = sessionControl.getMock();
		MessageConsumer consumer = consumerControl.getMock();
		Message message = messageControl.getMock();

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

		MockControl<Session> sessionControl = MockControl.createNiceControl(Session.class);
		MockControl<MessageConsumer> consumerControl = MockControl.createControl(MessageConsumer.class);

		Session session = sessionControl.getMock();
		MessageConsumer consumer = consumerControl.getMock();
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
		try {
			boolean received = doTestWithException(new IllegalStateException("No way!"), true, 2);
			assertFalse("Message received", received);
			fail("Expected IllegalStateException");
		} catch (IllegalStateException e) {
			assertEquals("No way!", e.getMessage());
		}
	}

	public void testNonTransactionalReceiveAndExecuteWithCallbackThrowingException() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);
		container.setSessionTransacted(false);
		boolean received = doTestWithException(new IllegalStateException("No way!"), false, 1);
		assertTrue("Message not received but listener not transactional so this should be true", received);
	}

	public void testNonTransactionalReceiveAndExecuteWithCallbackThrowingError() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);
		container.setSessionTransacted(false);
		try {
			boolean received = doTestWithException(new RuntimeException("No way!"), false, 1);
			assertTrue("Message not received but listener not transactional so this should be true", received);
		}
		catch (RuntimeException e) {
			assertEquals("No way!", e.getMessage());
			fail("Unexpected Error - should be swallowed");
		}
	}

	private BatchMessageListenerContainer getContainer(RepeatTemplate template) {
		MockControl<ConnectionFactory> connectionFactoryControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = connectionFactoryControl.getMock();
		// Yuck: we need to turn these method in base class to no-ops because the invoker is a private class
		// we can't create for test purposes...
		BatchMessageListenerContainer container = new BatchMessageListenerContainer() {
			protected void messageReceived(Object invoker, Session session) {
			}
			protected void noMessageReceived(Object invoker, Session session) {
			}
		};
		RepeatOperationsInterceptor interceptor = new RepeatOperationsInterceptor();
		interceptor.setRepeatOperations(template);
		container.setAdviceChain(new Advice[] {interceptor});
		container.setConnectionFactory(connectionFactory);
		container.setDestinationName("queue");
		container.afterPropertiesSet();
		return container;
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

		MockControl<Session> sessionControl = MockControl.createNiceControl(Session.class);
		MockControl<MessageConsumer> consumerControl = MockControl.createNiceControl(MessageConsumer.class);
		MockControl<Message> messageControl = MockControl.createNiceControl(Message.class);

		Session session = sessionControl.getMock();
		MessageConsumer consumer = consumerControl.getMock();
		Message message = messageControl.getMock();

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
				Object.class, Session.class, MessageConsumer.class });
		method.setAccessible(true);
		boolean received;
		try {
			// A null invoker is not normal, but we don't care about the invoker for a unit test
			received = ((Boolean) method.invoke(container, new Object[] { null, session, consumer })).booleanValue();
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
