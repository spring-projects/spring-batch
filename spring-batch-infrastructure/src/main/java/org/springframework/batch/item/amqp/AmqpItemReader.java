/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.batch.item.amqp;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.batch.item.ItemReader;
import org.springframework.util.Assert;

/**
 * <p>
 * AMQP {@link ItemReader} implementation using an {@link AmqpTemplate} to receive and/or
 * convert messages.
 * </p>
 *
 * <p>
 * This reader is thread-safe as long as the delegate <code>AmqpTemplate</code>
 * implementation is thread-safe.
 * </p>
 *
 * @author Chris Schaefer
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 */
public class AmqpItemReader<T> implements ItemReader<T> {

	private final AmqpTemplate amqpTemplate;

	private @Nullable Class<? extends T> itemType;

	/**
	 * Initialize the AmqpItemReader.
	 * @param amqpTemplate the template to be used. Must not be null.
	 */
	public AmqpItemReader(AmqpTemplate amqpTemplate) {
		Assert.notNull(amqpTemplate, "AmqpTemplate must not be null");

		this.amqpTemplate = amqpTemplate;
	}

	@Override
	@SuppressWarnings("unchecked")
	public @Nullable T read() {
		if (itemType != null && itemType.isAssignableFrom(Message.class)) {
			return (T) amqpTemplate.receive();
		}

		Object result = amqpTemplate.receiveAndConvert();

		if (itemType != null && result != null) {
			Assert.state(itemType.isAssignableFrom(result.getClass()),
					"Received message payload of wrong type: expected [" + itemType + "]");
		}

		return (T) result;
	}

	/**
	 * Establish the itemType for the reader.
	 * @param itemType class type that will be returned by the reader.
	 */
	public void setItemType(Class<? extends T> itemType) {
		Assert.notNull(itemType, "Item type cannot be null");
		this.itemType = itemType;
	}

}
