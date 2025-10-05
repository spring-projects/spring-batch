/*
 * Copyright 2017-2025 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.infrastructure.item.jms.builder;

import jakarta.jms.Message;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.jms.JmsItemReader;
import org.springframework.jms.core.JmsOperations;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified JmsItemReader.
 *
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @since 4.0
 */
public class JmsItemReaderBuilder<T> {

	protected @Nullable Class<? extends T> itemType;

	protected @Nullable JmsOperations jmsTemplate;

	/**
	 * Establish the JMS template that will be used by the JmsItemReader.
	 * @param jmsTemplate a {@link JmsOperations} instance
	 * @return this instance for method chaining.
	 * @see JmsItemReader#setJmsTemplate(JmsOperations)
	 */
	public JmsItemReaderBuilder<T> jmsTemplate(JmsOperations jmsTemplate) {
		this.jmsTemplate = jmsTemplate;

		return this;
	}

	/**
	 * Set the expected type of incoming message payloads. Set this to {@link Message} to
	 * receive the raw underlying message.
	 * @param itemType the java class of the items to be delivered. Typically the same as
	 * the class parameter
	 * @return this instance for method chaining.
	 * @throws IllegalStateException if the message payload is of the wrong type.
	 * @see JmsItemReader#setItemType(Class)
	 */
	public JmsItemReaderBuilder<T> itemType(Class<? extends T> itemType) {
		this.itemType = itemType;

		return this;
	}

	/**
	 * Returns a fully constructed {@link JmsItemReader}.
	 * @return a new {@link JmsItemReader}
	 */
	public JmsItemReader<T> build() {
		Assert.notNull(this.jmsTemplate, "jmsTemplate is required.");
		JmsItemReader<T> jmsItemReader = new JmsItemReader<>(this.jmsTemplate);

		if (this.itemType != null) {
			jmsItemReader.setItemType(this.itemType);
		}
		return jmsItemReader;
	}

}
