/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.batch.item.amqp.builder;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.batch.item.amqp.AmqpItemReader;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link AmqpItemReader}
 *
 * @author Glenn Renfro
 * @since 4.0
 * @see AmqpItemReader
 */
public class AmqpItemReaderBuilder<T> {

	private AmqpTemplate amqpTemplate;

	private Class<? extends T> itemType;

	/**
	 * Establish the amqpTemplate to be used by the AmqpItemReader.
	 * @param amqpTemplate the template to be used.
	 * @return this instance for method chaining
	 * @see AmqpItemReader#AmqpItemReader(AmqpTemplate)
	 */
	public AmqpItemReaderBuilder<T> amqpTemplate(AmqpTemplate amqpTemplate) {
		this.amqpTemplate = amqpTemplate;

		return this;
	}

	/**
	 * Establish the itemType for the reader.
	 * @param itemType class type that will be returned by the reader.
	 * @return this instance for method chaining.
	 * @see AmqpItemReader#setItemType(Class)
	 */
	public AmqpItemReaderBuilder<T> itemType(Class<? extends T> itemType) {
		this.itemType = itemType;

		return this;
	}

	/**
	 * Validates and builds a {@link AmqpItemReader}.
	 *
	 * @return a {@link AmqpItemReader}
	 */
	public AmqpItemReader<T> build() {
		Assert.notNull(this.amqpTemplate, "amqpTemplate is required.");

		AmqpItemReader<T> reader = new AmqpItemReader<>(this.amqpTemplate);
		if(this.itemType != null) {
			reader.setItemType(this.itemType);
		}

		return reader;
	}
}
