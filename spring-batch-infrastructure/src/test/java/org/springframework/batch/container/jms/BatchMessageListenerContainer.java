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

package org.springframework.batch.container.jms;

import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.interceptor.RepeatOperationsInterceptor;
import org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;

/**
 * Message listener container adapted for intercepting the message reception with advice
 * provided through configuration.<br>
 *
 * To enable batching of messages in a single transaction, use the
 * {@link TransactionInterceptor} and the {@link RepeatOperationsInterceptor} in the
 * advice chain (with or without a transaction manager set in the base class). Instead of
 * receiving a single message and processing it, the container will then use a
 * {@link RepeatOperations} to receive multiple messages in the same thread. Use with a
 * {@link RepeatOperations} and a transaction interceptor. If the transaction interceptor
 * uses XA then use an XA connection factory, or else the
 * {@link TransactionAwareConnectionFactoryProxy} to synchronize the JMS session with the
 * ongoing transaction (opening up the possibility of duplicate messages after a failure).
 * In the latter case you will not need to provide a transaction manager in the base class
 * - it only gets on the way and prevents the JMS session from synchronizing with the
 * database transaction.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class BatchMessageListenerContainer extends DefaultMessageListenerContainer {

	/**
	 * @author Dave Syer
	 *
	 */
	public interface ContainerDelegate {

		boolean receiveAndExecute(Object invoker, Session session, MessageConsumer consumer) throws JMSException;

	}

	private Advice[] advices = new Advice[0];

	private final ContainerDelegate delegate = BatchMessageListenerContainer.super::receiveAndExecute;

	private ContainerDelegate proxy = delegate;

	/**
	 * Public setter for the {@link Advice}.
	 * @param advices the advice to set
	 */
	public void setAdviceChain(Advice[] advices) {
		this.advices = advices;
	}

	/**
	 * Set up interceptor with provided advice on the
	 * {@link #receiveAndExecute(Object, Session, MessageConsumer)} method.
	 *
	 * @see org.springframework.jms.listener.AbstractJmsListeningContainer#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		initializeProxy();
	}

	/**
	 * Override base class to prevent exceptions from being swallowed. Should be an
	 * injectable strategy (see SPR-4733).
	 *
	 * @see org.springframework.jms.listener.AbstractMessageListenerContainer#handleListenerException(java.lang.Throwable)
	 */
	@Override
	protected void handleListenerException(Throwable ex) {
		if (!isSessionTransacted()) {
			// Log the exceptions in base class if not transactional anyway
			super.handleListenerException(ex);
			return;
		}
		logger.debug("Re-throwing exception in container.");
		if (ex instanceof RuntimeException runtimeException) {
			// We need to re-throw so that an enclosing non-JMS transaction can
			// rollback...
			throw runtimeException;
		}
		else if (ex instanceof Error error) {
			// Just re-throw Error instances because otherwise unit tests just swallow
			// exceptions from EasyMock and JUnit.
			throw error;
		}
	}

	/**
	 * Override base class method to wrap call in advice if provided.
	 * @see org.springframework.jms.listener.AbstractPollingMessageListenerContainer#receiveAndExecute(Object,
	 * jakarta.jms.Session, jakarta.jms.MessageConsumer)
	 */
	@Override
	protected boolean receiveAndExecute(Object invoker, final Session session, final MessageConsumer consumer)
			throws JMSException {
		return proxy.receiveAndExecute(invoker, session, consumer);
	}

	/**
	 *
	 */
	public void initializeProxy() {
		ProxyFactory factory = new ProxyFactory();
		for (Advice advice : advices) {
			DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(advice);
			NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
			pointcut.addMethodName("receiveAndExecute");
			advisor.setPointcut(pointcut);
			factory.addAdvisor(advisor);
		}
		factory.setProxyTargetClass(false);
		factory.addInterface(ContainerDelegate.class);
		factory.setTarget(delegate);
		proxy = (ContainerDelegate) factory.getProxy();
	}

}
