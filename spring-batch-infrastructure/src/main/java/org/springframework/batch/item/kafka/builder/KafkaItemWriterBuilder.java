/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.batch.item.kafka.builder;

import org.springframework.batch.item.kafka.KafkaItemWriter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link KafkaItemWriter}
 *
 * @author Mathieu Ouellet
 * @since 4.2
 */
public class KafkaItemWriterBuilder<K, V> {

	private KafkaTemplate<K, V> kafkaTemplate;

	private Converter<V, K> itemKeyMapper;

	private boolean delete;

	/**
	 * Establish the KafkaTemplate to be used by the KafkaItemWriter.
	 * @param kafkaTemplate the template to be used
	 * @return this instance for method chaining
	 * @see KafkaItemWriter#setKafkaTemplate(KafkaTemplate)
	 */
	public KafkaItemWriterBuilder<K, V> kafkaTemplate(KafkaTemplate<K, V> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
		return this;
	}

	/**
	 * Set the {@link Converter} to use to derive the key from the item.
	 * @param itemKeyMapper the Converter to use.
	 * @return The current instance of the builder.
	 * @see KafkaItemWriter#setItemKeyMapper(Converter)
	 */
	public KafkaItemWriterBuilder<K, V> itemKeyMapper(Converter<V, K> itemKeyMapper) {
		this.itemKeyMapper = itemKeyMapper;
		return this;
	}

	/**
	 * Indicate if the items being passed to the writer are all to be sent as delete events to the topic. A delete
	 * event is made of a key with a null value. If set to false (default), the items will be sent with provided value
	 * and key converter by the itemKeyMapper. If set to true, the items will be sent with the key converter from the
	 * value by the itemKeyMapper and a null value.
	 * @param delete removal indicator.
	 * @return The current instance of the builder.
	 * @see KafkaItemWriter#setDelete(boolean)
	 */
	public KafkaItemWriterBuilder<K, V> delete(boolean delete) {
		this.delete = delete;
		return this;
	}

	/**
	 * Validates and builds a {@link KafkaItemWriter}.
	 * @return a {@link KafkaItemWriter}
	 */
	public KafkaItemWriter<K, V> build() {
		Assert.notNull(this.kafkaTemplate, "kafkaTemplate is required.");
		Assert.notNull(this.itemKeyMapper, "itemKeyMapper is required.");

		KafkaItemWriter<K, V> writer = new KafkaItemWriter<>();
		writer.setKafkaTemplate(this.kafkaTemplate);
		writer.setItemKeyMapper(this.itemKeyMapper);
		writer.setDelete(this.delete);
		return writer;
	}
}
