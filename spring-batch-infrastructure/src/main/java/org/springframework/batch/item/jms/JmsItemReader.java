/*
 * Copyright 2006-2019 the original author or authors.
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

package org.springframework.batch.item.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.jms.Message;

/**
 * An {@link ItemReader} for JMS using a {@link JmsTemplate}. The template
 * should have a default destination, which will be used to provide items in
 * {@link #read()}.<br>
 * <br>
 * 
 * The implementation is thread-safe after its properties are set (normal
 * singleton behavior).
 * 
 * @author Dave Syer
 * 
 */
public class JmsItemReader<T> implements ItemReader<T>, InitializingBean {

	protected Log logger = LogFactory.getLog(getClass());

	protected Class<? extends T> itemType;

	protected JmsOperations jmsTemplate;

	/**
	 * Setter for JMS template.
	 * 
	 * @param jmsTemplate a {@link JmsOperations} instance
	 */
	public void setJmsTemplate(JmsOperations jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
		if (jmsTemplate instanceof JmsTemplate) {
			JmsTemplate template = (JmsTemplate) jmsTemplate;
			Assert.isTrue(template.getReceiveTimeout() != JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT,
					"JmsTemplate must have a receive timeout!");
			Assert.isTrue(template.getDefaultDestination() != null || template.getDefaultDestinationName() != null,
					"JmsTemplate must have a defaultDestination or defaultDestinationName!");
		}
	}

	/**
	 * Set the expected type of incoming message payloads. Set this to
	 * {@link Message} to receive the raw underlying message.
	 * 
	 * @param itemType the java class of the items to be delivered. Typically
	 * the same as the class parameter
	 * 
	 * @throws IllegalStateException if the message payload is of the wrong
	 * type.
	 */
	public void setItemType(Class<? extends T> itemType) {
		this.itemType = itemType;
	}

    @Nullable
	@Override
	@SuppressWarnings("unchecked")
	public T read() {
		if (itemType != null && itemType.isAssignableFrom(Message.class)) {
			return (T) jmsTemplate.receive();
		}
		Object result = jmsTemplate.receiveAndConvert();
		if (itemType != null && result != null) {
			Assert.state(itemType.isAssignableFrom(result.getClass()),
					"Received message payload of wrong type: expected [" + itemType + "]");
		}
		return (T) result;
	}

    @Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.jmsTemplate, "The 'jmsTemplate' is required.");
	}
}
