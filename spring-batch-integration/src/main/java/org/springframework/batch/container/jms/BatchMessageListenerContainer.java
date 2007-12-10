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
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

/**
 * Message listener container adapted for batching the message processing.
 * Instead of receiving a single message and processing it, we use a
 * {@link RepeatOperations} to receive multiple messages in the same thread. Use
 * with a transactional {@link RepeatOperations} and either an XA connection
 * factory, or the {@link TransactionAwareConnectionFactoryProxy} to synchronize
 * the JMS session with an ongoing transaction.
 * 
 * @author Dave Syer
 * 
 */
public class BatchMessageListenerContainer extends DefaultMessageListenerContainer {

	private RepeatOperations template;

	private ThreadLocal messageHolder = new ThreadLocal();

	/**
	 * Create a new {@link BatchMessageListenerContainer}. The container is set
	 * with auto startup = false (not the default of the parent container).
	 * 
	 * @param template a {@link RepeatOperations}. It is advisable to set the
	 * {@link RepeatOperations} with a sensible termination policy, like a small
	 * fixed chunk size.
	 */
	public BatchMessageListenerContainer(RepeatOperations template) {
		super();
		this.template = template;
		setAutoStartup(false);
		// Avoid error on startup...
		// http://opensource.atlassian.com/projects/spring/browse/SPR-3154
		setMessageListener(new MessageListenerAdapter());
	}

	/**
	 * Override base class method to store message in a thread local for later
	 * use.
	 * 
	 * @see org.springframework.jms.listener.AbstractPollingMessageListenerContainer#receiveMessage(javax.jms.MessageConsumer)
	 */
	protected Message receiveMessage(MessageConsumer consumer) throws JMSException {
		Message message = super.receiveMessage(consumer);
		if (message!=null) {
			messageHolder.set(message);
		}
		return message;
	}

	/**
	 * Override base class method to enable the message holder to be reset,
	 * signalling that a rollback has occurred.
	 * 
	 * @see org.springframework.jms.listener.AbstractMessageListenerContainer#rollbackOnExceptionIfNecessary(javax.jms.Session,
	 * java.lang.Throwable)
	 */
	protected void rollbackOnExceptionIfNecessary(Session session, Throwable ex) throws JMSException {
		super.rollbackOnExceptionIfNecessary(session, ex);
		if (session.getTransacted() && isSessionTransacted()) {
			messageHolder.set(null);
		}
	}

	/**
	 * Override base class to allow extra processing in the case of exception,
	 * with knowledge of the message.
	 * 
	 * @see org.springframework.jms.listener.AbstractMessageListenerContainer#doExecuteListener(javax.jms.Session,
	 * javax.jms.Message)
	 */
	protected void doExecuteListener(Session session, Message message) throws JMSException {
		try {
			super.doExecuteListener(session, message);
		}
		catch (Throwable ex) {
			handleListenerException(session, message, ex);
		}
	}

	/**
	 * Extension point for subclasses. Do anything necessary to recover from the
	 * exception, which was raised when the message was being processed.
	 * @param session the current JMS session.
	 * @param message the message just receieved and failed to process.
	 * @param ex the exception thrown during message processing.
	 */
	protected void recover(Session session, Message message, Throwable ex) throws JMSException {
		// do nothing...
	}

	/**
	 * Used to provide a recovery path - delegates to
	 * {@link #recover(Session, Message, Throwable)}. TODO: Could be merged
	 * into base class?
	 * @param session the JMS session
	 * @param message the last message
	 * @param ex the exception thrown by listener
	 * @see #doExecuteListener(Session, Message)
	 * @see #recover(Session, Message, Throwable)
	 */
	protected final void handleListenerException(Session session, Message message, Throwable ex) throws JMSException {
		// Call out to recovery path...
		recover(session, message, ex);
		if (ex instanceof RuntimeException) {
			// We need to rethrow so that an enclosing non-JMS transaction can
			// rollback...
			throw (RuntimeException) ex;
		}
		else if (ex instanceof Error) {
			// Just re-throw Error instances because otherwise unit tests just
			// swallow expections from EasyMock and JUnit.
			throw (Error) ex;
		}
	}

	/**
	 * Override base class method to wrap call in a batch.
	 * @see org.springframework.jms.listener.AbstractPollingMessageListenerContainer#receiveAndExecute(javax.jms.Session,
	 * javax.jms.MessageConsumer)
	 */
	protected boolean receiveAndExecute(final Session session, final MessageConsumer consumer) throws JMSException {

		template.iterate(new RepeatCallback() {
			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				return doBatchCallBack(session, consumer);
			}
		});
		
		if (messageHolder.get()==null) {
			return false;
		}

		messageHolder.set(null);
		return true;
	}

	/**
	 * Wraps a call to {@link #receiveAndExecute(Session, MessageConsumer)},
	 * retrieving the message from thread local.
	 * 
	 * @param session
	 * @param consumer
	 * @return
	 * @throws JMSException
	 * 
	 * @see {@link #receiveMessage(MessageConsumer)}
	 */
	protected ExitStatus doBatchCallBack(Session session, MessageConsumer consumer) throws JMSException {
		/*
		 * The base class receiveAndExecute is transactional (if configured). We
		 * could extend the tx boundary to the whole batch by making the
		 * template.execute transactional, and either switch off the tx manager
		 * in this object, or live with its default propagation=REQUIRED
		 * behaviour.
		 * 
		 * But if the super class transaction manager is a
		 * JmsTransactionManager, which is the normal choice for a message
		 * listener container (see
		 * http://opensource.atlassian.com/projects/spring/browse/SPR-3156),
		 * then it will not behave as expected. In particular since the
		 * JmsTransactionManager is not aware of the batch template (execute or
		 * callback) transactions, it will commit message sessions that should
		 * be rolled back when a batch fails.
		 */
		if (super.receiveAndExecute(session, consumer)) {
			Object message = messageHolder.get();
			return new ExitStatus(message!=null);
		}
		return ExitStatus.FINISHED;
	}

}
