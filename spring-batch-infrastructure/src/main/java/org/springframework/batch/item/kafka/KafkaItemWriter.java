/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.batch.item.kafka;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.KeyValueItemWriter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.Assert;

/**
 * <p>
 * An {@link ItemWriter} implementation for Apache Kafka using a
 * {@link KafkaTemplate} with default topic configured.
 * </p>
 *
 * @author Mathieu Ouellet
 * @since 4.2
 *
 */
public class KafkaItemWriter<K, T> extends KeyValueItemWriter<K, T> {

	protected KafkaTemplate<K, T> kafkaTemplate;

	@Override
	protected void writeKeyValue(K key, T value) {
		if (this.delete) {
			this.kafkaTemplate.sendDefault(key, null);
		}
		else {
			this.kafkaTemplate.sendDefault(key, value);
		}
	}

	@Override
	protected void init() {
		Assert.notNull(this.kafkaTemplate, "KafkaTemplate must not be null.");
		Assert.notNull(this.kafkaTemplate.getDefaultTopic(), "KafkaTemplate must have the default topic set.");
	}

	/**
	 * Set the {@link KafkaTemplate} to use.
	 * @param kafkaTemplate to use
	 */
	public void setKafkaTemplate(KafkaTemplate<K, T> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}
}
