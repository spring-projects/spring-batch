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

import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.retry.interceptor.MethodArgumentsKeyGenerator;

/**
 * A {@link MethodArgumentsKeyGenerator} for JMS 
 * 
 * @author Dave Syer
 * 
 */
public class JmsMethodArgumentsKeyGenerator implements MethodArgumentsKeyGenerator {

	/**
	 * If the message is a {@link Message} then returns the JMS message ID.
	 * Otherwise just return the first argument.
	 * 
	 * @see org.springframework.batch.retry.interceptor.MethodArgumentsKeyGenerator#getKey(Object[])
	 * 
	 * @throws UnexpectedInputException if the JMS id cannot be determined from
	 * a JMS Message
	 * @throws IllegalArgumentException if the arguments are empty
	 */
	public Object getKey(Object[] items) {
		for (Object item : items) {
			if (item instanceof Message) {
				try {
					return ((Message) item).getJMSMessageID();
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
