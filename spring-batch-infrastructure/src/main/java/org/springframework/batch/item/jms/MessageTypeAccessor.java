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

import javax.jms.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jms.core.JmsOperations;

/**
 * Base class for JMS concerns.
 * 
 * @author Dave Syer
 * 
 */
class MessageTypeAccessor<T> {

	protected Log logger = LogFactory.getLog(getClass());

	protected Class<? extends T> itemType;

	protected JmsOperations jmsTemplate;

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
	 * @param itemType the java class of the items to be delivered. Typically
	 * the same as the class parameter
	 * 
	 * @throws IllegalStateException if the message payload is of the wrong
	 * type.
	 */
	public void setItemType(Class<? extends T> itemType) {
		this.itemType = itemType;
	}
	
	public  boolean isMessageType() {
		return itemType != null && itemType.isAssignableFrom(Message.class);
	}



}
