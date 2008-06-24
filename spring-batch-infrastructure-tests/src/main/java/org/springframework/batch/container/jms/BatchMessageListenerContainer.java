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
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.interceptor.RepeatOperationsInterceptor;
import org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Message listener container adapted for intercepting the message reception
 * with advice provided through configuration.<br/>
 * 
 * To enable batching of messages in a single transaction, use the
 * {@link TransactionInterceptor} and the {@link RepeatOperationsInterceptor} in
 * the advice chain (with or without a transaction manager set in the base
 * class). Instead of receiving a single message and processing it, the
 * container will then use a {@link RepeatOperations} to receive multiple
 * messages in the same thread. Use with a {@link RepeatOperations} and a
 * transaction interceptor. If the transaction interceptor uses XA then use an
 * XA connection factory, or else the
 * {@link TransactionAwareConnectionFactoryProxy} to synchronize the JMS session
 * with the ongoing transaction (opening up the possibility of duplicate
 * messages after a failure). In the latter case you will not need to provide a
 * transaction manager in the base class - it only gets on the way and prevents
 * the JMS session from synchronizing with the database transaction.
 * 
 * @author Dave Syer
 * 
 */
public class BatchMessageListenerContainer extends DefaultMessageListenerContainer {

	/**
	 * @author Dave Syer
	 * 
	 */
	public static interface ContainerDelegate {
		boolean receiveAndExecute(Object invoker, Session session, MessageConsumer consumer) throws JMSException;
	}

	private Advice[] advices = new Advice[0];

	private ContainerDelegate delegate = new ContainerDelegate() {
		public boolean receiveAndExecute(Object invoker, Session session, MessageConsumer consumer) throws JMSException {
			return BatchMessageListenerContainer.super.receiveAndExecute(invoker, session, consumer);
		}
	};

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
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		initializeProxy();
	}

	/**
	 * Override base class to prevent exceptions from being swallowed. Should be
	 * an injectable strategy (see SPR-4733).
	 * 
	 * @see org.springframework.jms.listener.AbstractMessageListenerContainer#handleListenerException(java.lang.Throwable)
	 */
	protected void handleListenerException(Throwable ex) {
		if (!isSessionTransacted()) {
			// Log the exceptions in base class if not transactional anyway
			super.handleListenerException(ex);
			return;
		}
		logger.debug("Re-throwing exception in container.");
		if (ex instanceof RuntimeException) {
			// We need to re-throw so that an enclosing non-JMS transaction can
			// rollback...
			throw (RuntimeException) ex;
		}
		else if (ex instanceof Error) {
			// Just re-throw Error instances because otherwise unit tests just
			// swallow exceptions from EasyMock and JUnit.
			throw (Error) ex;
		}
	}

	/**
	 * Override base class method to wrap call in advice if provided.
	 * @see org.springframework.jms.listener.AbstractPollingMessageListenerContainer#receiveAndExecute(Object,
	 * javax.jms.Session, javax.jms.MessageConsumer)
	 */
	protected boolean receiveAndExecute(final Object invoker, final Session session, final MessageConsumer consumer)
			throws JMSException {
		return proxy.receiveAndExecute(invoker, session, consumer);
	}

	/**
	 * 
	 */
	public void initializeProxy() {
		ProxyFactory factory = new ProxyFactory();
		for (int i = 0; i < advices.length; i++) {
			DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(advices[i]);
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
