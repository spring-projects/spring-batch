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
import org.springframework.retry.interceptor.NewMethodArgumentsIdentifier;

/**
 * A {@link NewMethodArgumentsIdentifier} for JMS that looks for a message in the
 * arguments and checks its delivery status.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class JmsNewMethodArgumentsIdentifier<T> implements NewMethodArgumentsIdentifier {

	/**
	 * If any of the arguments is a message, check the JMS re-delivered flag and return
	 * it, otherwise return false to be on the safe side.
	 *
	 * @see org.springframework.retry.interceptor.NewMethodArgumentsIdentifier#isNew(java.lang.Object[])
	 */
	@Override
	public boolean isNew(Object[] args) {

		for (Object item : args) {
			if (item instanceof Message message) {
				try {
					return !message.getJMSRedelivered();
				}
				catch (JMSException e) {
					throw new UnexpectedInputException("Could not extract message ID", e);
				}
			}
		}
		return false;
	}

}
