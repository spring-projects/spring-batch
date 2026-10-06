/*
 * Copyright 2019-2026 the original author or authors.
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

package org.springframework.batch.infrastructure.item.kafka;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.springframework.batch.infrastructure.item.ItemReader;

/**
 * <p>
 * An {@link ItemReader} implementation for Apache Kafka. Uses a {@link KafkaConsumer} to
 * read data from a given topic. Multiple partitions within the same topic can be assigned
 * to this reader.
 * </p>
 *
 * <p>
 * Since {@link KafkaConsumer} is not thread-safe, this reader is not thread-safe.
 * </p>
 *
 * @author djechelon@github.com
 * @since 6.0
 */
public class KafkaConsumerRecordItemReader<K, V> extends AbstractKafkaItemReader<K, V, ConsumerRecord<K, V>> {

	/**
	 * Create a new {@link KafkaConsumerRecordItemReader}.
	 * <p>
	 * <strong>{@code consumerProperties} must contain the following keys:
	 * 'bootstrap.servers', 'group.id', 'key.deserializer' and 'value.deserializer'
	 * </strong>
	 * </p>
	 * .
	 * @param consumerProperties properties of the consumer
	 * @param topicName name of the topic to read data from
	 * @param partitions list of partitions to read data from
	 */
	public KafkaConsumerRecordItemReader(Properties consumerProperties, String topicName, Integer... partitions) {
		this(consumerProperties, topicName, Arrays.asList(partitions));
	}

	/**
	 * Create a new {@link KafkaConsumerRecordItemReader}.
	 * <p>
	 * <strong>{@code consumerProperties} must contain the following keys:
	 * 'bootstrap.servers', 'group.id', 'key.deserializer' and 'value.deserializer'
	 * </strong>
	 * </p>
	 * .
	 * @param consumerProperties properties of the consumer
	 * @param topicName name of the topic to read data from
	 * @param partitions list of partitions to read data from
	 */
	public KafkaConsumerRecordItemReader(Properties consumerProperties, String topicName, List<Integer> partitions) {
		super(consumerProperties, topicName, partitions);
	}

	@Override
	protected ConsumerRecord<K, V> mapRecord(ConsumerRecord<K, V> record) {
		return record;
	}

}
