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

import org.springframework.batch.item.ItemReader;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.Assert;

/**
 * An {@link ItemReader} for JMS using a {@link JmsTemplate}. The template
 * should have a default destination, which will be used to provide items in
 * {@link #read()}.<br/><br/>
 * 
 * The implementation is thread safe after its properties are set (normal
 * singleton behaviour).
 * 
 * @author Dave Syer
 * 
 */
public class JmsItemReader<T> extends MessageTypeAccessor<T> implements ItemReader<T> {

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

}
