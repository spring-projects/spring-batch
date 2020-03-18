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
import org.springframework.batch.item.amqp.AmqpItemWriter;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link AmqpItemWriter}
 * @author Glenn Renfro
 * @since 4.0
 * @see AmqpItemWriter
 */
public class AmqpItemWriterBuilder<T> {

	private AmqpTemplate amqpTemplate;

	/**
	 * Establish the amqpTemplate to be used by the AmqpItemWriter.
	 * @param amqpTemplate the template to be used.
	 * @return this instance for method chaining
	 * @see AmqpItemWriter#AmqpItemWriter(AmqpTemplate)
	 */
	public AmqpItemWriterBuilder<T> amqpTemplate(AmqpTemplate amqpTemplate) {
		this.amqpTemplate = amqpTemplate;

		return this;
	}

	/**
	 * Validates and builds a {@link AmqpItemWriter}.
	 *
	 * @return a {@link AmqpItemWriter}
	 */
	public AmqpItemWriter<T> build() {
		Assert.notNull(this.amqpTemplate, "amqpTemplate is required.");

		AmqpItemWriter<T> writer = new AmqpItemWriter<>(this.amqpTemplate);

		return writer;
	}

}
