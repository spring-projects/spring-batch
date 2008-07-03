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

package org.springframework.batch.item.jms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.NewItemIdentifier;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.support.AbstractItemReader;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.Assert;

/**
 * An {@link ItemReader} for JMS using a {@link JmsTemplate}. The template
 * should have a default destination, which will be used to provide items in
 * {@link #read()}. If a recovery step is needed, set the error destination and
 * the item will be sent there if processing fails in a stateful retry.
 * 
 * The implementation is thread safe after its properties are set (normal
 * singleton behaviour).
 * 
 * @author Dave Syer
 * 
 */
public class JmsItemReader extends AbstractItemReader implements ItemRecoverer, ItemKeyGenerator, NewItemIdentifier {

	protected Log logger = LogFactory.getLog(getClass());

	private JmsOperations jmsTemplate;

	private Class itemType;

	private String errorDestinationName;

	private Destination errorDestination;

	/**
	 * Set the error destination. Should not be the same as the default
	 * destination of the jms template.
	 * @param errorDestination a JMS Destination
	 */
	public void setErrorDestination(Destination errorDestination) {
		this.errorDestination = errorDestination;
	}

	/**
	 * Set the error destination by name. Will be resolved by the destination
	 * resolver in the jms template.
	 * 
	 * @param errorDestinationName the name of a JMS Destination
	 */
	public void setErrorDestinationName(String errorDestinationName) {
		this.errorDestinationName = errorDestinationName;
	}

	/**
	 * Setter for jms template.
	 * 
	 * @param jmsTemplate a {@link JmsOperations} instance
	 */
	public void setJmsTemplate(JmsOperations jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	/**
	 * Set the expected type of incoming message payloads. Set this to
	 * {@link Message} to receive the raw underlying message.
	 * 
	 * @param itemType the java class of the items to be delivered.
	 * 
	 * @throws IllegalStateException if the message payload is of the wrong
	 * type.
	 */
	public void setItemType(Class itemType) {
		this.itemType = itemType;
	}

	public Object read() {
		if (itemType != null && itemType.isAssignableFrom(Message.class)) {
			return jmsTemplate.receive();
		}
		Object result = jmsTemplate.receiveAndConvert();
		if (itemType != null && result != null) {
			Assert.state(itemType.isAssignableFrom(result.getClass()),
					"Received message payload of wrong type: expected [" + itemType + "]");
		}
		return result;
	}

	/**
	 * Send the message back to the provider using the specified error
	 * destination property of this reader. If the recovery is successful the
	 * item itself is returned, otherwise null.
	 * 
	 * @see org.springframework.batch.item.ItemRecoverer#recover(Object,
	 * Throwable)
	 */
	public Object recover(Object item, Throwable cause) {
		try {
			if (errorDestination != null) {
				jmsTemplate.convertAndSend(errorDestination, item);
			}
			else if (errorDestinationName != null) {
				jmsTemplate.convertAndSend(errorDestinationName, item);
			}
			else {
				// do nothing - it doesn't make sense to send the message back
				// to the destination it came from
				return null;
			}
			return item;
		}
		catch (JmsException e) {
			logger.error("Could not recover because of JmsException.", e);
			throw e;
		}
	}

	/**
	 * If the message is a {@link Message} then returns the JMS message ID.
	 * Otherwise just delegate to parent class.
	 * 
	 * @see org.springframework.batch.item.ItemKeyGenerator#getKey(java.lang.Object)
	 * 
	 * @throws UnexpectedInputException if the JMS id cannot be determined from
	 * a JMS Message
	 */
	public Object getKey(Object item) {
		if (itemType != null && itemType.isAssignableFrom(Message.class)) {
			try {
				return ((Message) item).getJMSMessageID();
			}
			catch (JMSException e) {
				throw new UnexpectedInputException("Could not extract message ID", e);
			}
		}
		return item;
	}

	/**
	 * If the item is a message, check the JMS re-delivered flag, otherwise
	 * return false to be on the safe side.
	 * 
	 * @see org.springframework.batch.item.NewItemIdentifier#isNew(java.lang.Object)
	 */
	public boolean isNew(Object item) {
		if (itemType != null && itemType.isAssignableFrom(Message.class)) {
			try {
				return !((Message) item).getJMSRedelivered();
			}
			catch (JMSException e) {
				throw new UnexpectedInputException("Could not extract message ID", e);
			}
		}
		return false;
	}

}
