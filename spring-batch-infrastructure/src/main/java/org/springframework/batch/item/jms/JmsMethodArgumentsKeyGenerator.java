/*
 * Copyright 2006-2021 the original author or authors.
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

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.retry.interceptor.MethodArgumentsKeyGenerator;

/**
 * A {@link MethodArgumentsKeyGenerator} for JMS
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class JmsMethodArgumentsKeyGenerator implements MethodArgumentsKeyGenerator {

	/**
	 * If the message is a {@link Message} then returns the JMS message ID. Otherwise just
	 * return the first argument.
	 *
	 * @see org.springframework.retry.interceptor.MethodArgumentsKeyGenerator#getKey(Object[])
	 * @throws UnexpectedInputException if the JMS id cannot be determined from a JMS
	 * Message
	 * @throws IllegalArgumentException if the arguments are empty
	 */
	@Override
	public Object getKey(Object[] items) {
		for (Object item : items) {
			if (item instanceof Message message) {
				try {
					return message.getJMSMessageID();
				}
				catch (JMSException e) {
					throw new UnexpectedInputException("Could not extract message ID", e);
				}
			}
		}
		if (items.length == 0) {
			throw new IllegalArgumentException(
					"Method parameters are empty.  The key generator cannot determine a unique key.");
		}
		return items[0];
	}

}
