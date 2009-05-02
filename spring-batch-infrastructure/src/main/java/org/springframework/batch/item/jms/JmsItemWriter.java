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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.Assert;

/**
 * An {@link ItemWriter} for JMS using a {@link JmsTemplate}. The template
 * should have a default destination, which will be used to send items in
 * {@link #write(List)}.<br/>
 * <br/>
 * 
 * The implementation is thread safe after its properties are set (normal
 * singleton behavior).
 * 
 * @author Dave Syer
 * 
 */
public class JmsItemWriter<T> implements ItemWriter<T> {

	protected Log logger = LogFactory.getLog(getClass());

	private JmsOperations jmsTemplate;

	/**
	 * Setter for JMS template.
	 * 
	 * @param jmsTemplate
	 *            a {@link JmsOperations} instance
	 */
	public void setJmsTemplate(JmsOperations jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
		if (jmsTemplate instanceof JmsTemplate) {
			JmsTemplate template = (JmsTemplate) jmsTemplate;
			Assert
					.isTrue(template.getDefaultDestination() != null
							|| template.getDefaultDestinationName() != null,
							"JmsTemplate must have a defaultDestination or defaultDestinationName!");
		}
	}

	/**
	 * Send the items one-by-one to the default destination of the jms template.
	 * 
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	public void write(List<? extends T> items) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("Writing to JMS with " + items.size() + " items.");
		}

		for (T item : items) {
			jmsTemplate.convertAndSend(item);
		}

	}

}
