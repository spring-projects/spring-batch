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

package org.springframework.batch.container.jms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;

import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.Test;

import org.springframework.batch.repeat.interceptor.RepeatOperationsInterceptor;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.util.ReflectionUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BatchMessageListenerContainerTests {

	BatchMessageListenerContainer container;

	@Test
	void testReceiveAndExecuteWithCallback() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);

		container.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message arg0) {
			}
		});

		Session session = mock(Session.class);
		MessageConsumer consumer = mock(MessageConsumer.class);
		Message message = mock(Message.class);

		// Expect two calls to consumer (chunk size)...
		when(session.getTransacted()).thenReturn(true);
		when(session.getTransacted()).thenReturn(true);
		when(consumer.receive(1000)).thenReturn(message);

		boolean received = doExecute(session, consumer);
		assertTrue(received, "Message not received");

	}

	@Test
	void testReceiveAndExecuteWithCallbackReturningNull() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);

		Session session = mock(Session.class);
		MessageConsumer consumer = mock(MessageConsumer.class);
		Message message = null;

		// Expect one call to consumer (chunk size is 2 but terminates on
		// first)...
		when(consumer.receive(1000)).thenReturn(message);
		when(session.getTransacted()).thenReturn(false);

		boolean received = doExecute(session, consumer);
		assertFalse(received, "Message not received");

	}

	@Test
	void testTransactionalReceiveAndExecuteWithCallbackThrowingException() {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);
		container.setSessionTransacted(true);
		Exception exception = assertThrows(IllegalStateException.class,
				() -> doTestWithException(new IllegalStateException("No way!"), true, 2));
		assertEquals("No way!", exception.getMessage());
	}

	@Test
	void testNonTransactionalReceiveAndExecuteWithCallbackThrowingException() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);
		container.setSessionTransacted(false);
		boolean received = doTestWithException(new IllegalStateException("No way!"), false, 2);
		assertTrue(received, "Message not received but listener not transactional so this should be true");
	}

	@Test
	void testNonTransactionalReceiveAndExecuteWithCallbackThrowingError() throws Exception {
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(2));
		container = getContainer(template);
		container.setSessionTransacted(false);
		boolean received = doTestWithException(new RuntimeException("No way!"), false, 2);
		assertTrue(received, "Message not received but listener not transactional so this should be true");
	}

	private BatchMessageListenerContainer getContainer(RepeatTemplate template) {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		// Yuck: we need to turn these method in base class to no-ops because the invoker
		// is a private class
		// we can't create for test purposes...
		BatchMessageListenerContainer container = new BatchMessageListenerContainer() {
			@Override
			protected void messageReceived(Object invoker, Session session) {
			}

			@Override
			protected void noMessageReceived(Object invoker, Session session) {
			}
		};
		RepeatOperationsInterceptor interceptor = new RepeatOperationsInterceptor();
		interceptor.setRepeatOperations(template);
		container.setAdviceChain(new Advice[] { interceptor });
		container.setConnectionFactory(connectionFactory);
		container.setDestinationName("queue");
		container.afterPropertiesSet();
		return container;
	}

	private boolean doTestWithException(final Throwable t, boolean expectRollback, int expectGetTransactionCount)
			throws JMSException, IllegalAccessException {
		container.setAcceptMessagesWhileStopping(true);
		container.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message arg0) {
				if (t instanceof RuntimeException)
					throw (RuntimeException) t;
				else
					throw (Error) t;
			}
		});

		Session session = mock(Session.class);
		MessageConsumer consumer = mock(MessageConsumer.class);
		Message message = mock(Message.class);

		if (expectGetTransactionCount > 0) {
			when(session.getTransacted()).thenReturn(true);
		}

		// Expect only one call to consumer (chunk size is 2, but first one
		// rolls back terminating batch)...
		when(consumer.receive(1000)).thenReturn(message);
		if (expectRollback) {
			session.rollback();
		}

		boolean received = doExecute(session, consumer);

		return received;
	}

	private boolean doExecute(Session session, MessageConsumer consumer) throws IllegalAccessException {
		Method method = ReflectionUtils.findMethod(container.getClass(), "receiveAndExecute", Object.class,
				Session.class, MessageConsumer.class);
		method.setAccessible(true);
		boolean received;
		try {
			// A null invoker is not normal, but we don't care about the invoker for a
			// unit test
			received = (Boolean) method.invoke(container, null, session, consumer);
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}
			else {
				throw (Error) e.getCause();
			}
		}
		return received;
	}

}
