/*
 * Copyright 2006-2018 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.retry.interceptor.MethodInvocationRecoverer;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsOperations;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * 
 */
public class JmsMethodInvocationRecoverer<T> implements MethodInvocationRecoverer<T> {

	protected Log logger = LogFactory.getLog(getClass());

	private JmsOperations jmsTemplate;

	/**
	 * Setter for jms template.
	 * 
	 * @param jmsTemplate a {@link JmsOperations} instance
	 */
	public void setJmsTemplate(JmsOperations jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	/**
	 * Send one message per item in the arguments list using the default destination of
	 * the jms template. If the recovery is successful {@code null} is returned.
	 * 
	 * @see org.springframework.retry.interceptor.MethodInvocationRecoverer#recover(Object[],
	 * Throwable)
	 */
    @Override
	@Nullable
	public T recover(Object[] items, Throwable cause) {
		try {
			for (Object item : items) {
				jmsTemplate.convertAndSend(item);
			}
			return null;
		}
		catch (JmsException e) {
			logger.error("Could not recover because of JmsException.", e);
			throw e;
		}
	}

}
