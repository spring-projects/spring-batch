/*
 * Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.kafka.builder;

import java.util.List;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.batch.item.kafka.KafkaItemReader;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link KafkaItemReader}
 *
 * @author Mathieu Ouellet
 * @since 4.2
 */
public class KafkaItemReaderBuilder<K, V> {

	private ConsumerFactory<K, V> consumerFactory;

	private List<TopicPartition> topicPartitions;

	private long pollTimeout = 50L;

	private boolean saveState = true;

	/**
	 * The {@link ConsumerFactory} implementation to produce a new {@link Consumer} instance for the reader.
	 *
	 * @param consumerFactory
	 * @return The current instance of the builder.
	 * @see KafkaItemReader#setConsumerFactory(ConsumerFactory)
	 */
	public KafkaItemReaderBuilder<K, V> consumerFactory(ConsumerFactory<K, V> consumerFactory) {
		this.consumerFactory = consumerFactory;
		return this;
	}

	/**
	 * A list of {@link TopicPartition}s to manually assign to the consumer.
	 * 
	 * @param topicPartitions list of partitions to assign to the consumer
	 * @return The current instance of the builder.
	 * @see KafkaItemReader#setTopicPartitions(List)
	 */
	public KafkaItemReaderBuilder<K, V> topicPartitions(List<TopicPartition> topicPartitions) {
		this.topicPartitions = topicPartitions;
		return this;
	}

	/**
	 * Set the pollTimeout for the poll() operations.
	 *
	 * @param pollTimeout default to 50ms
	 * @return The current instance of the builder.
	 * @see KafkaItemReader#setPollTimeout(long)
	 */
	public KafkaItemReaderBuilder<K, V> pollTimeout(long pollTimeout) {
		this.pollTimeout = pollTimeout;
		return this;
	}

	/**
	 * Configure if the state of the {@link org.springframework.batch.item.ItemStreamSupport} should be persisted within
	 * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
	 *
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 * @see KafkaItemReader#setSaveState(boolean)
	 */
	public KafkaItemReaderBuilder<K, V> saveState(boolean saveState) {
		this.saveState = saveState;
		return this;
	}

	public KafkaItemReader<K, V> build() {
		Assert.notNull(this.consumerFactory, "consumerFactory is required.");
		Assert.notNull(this.topicPartitions, "topicPartitions is required.");
		Assert.isTrue(this.pollTimeout >= 0, "pollTimeout must not be negative.");

		KafkaItemReader<K, V> reader = new KafkaItemReader<>();
		reader.setConsumerFactory(this.consumerFactory);
		reader.setTopicPartitions(this.topicPartitions);
		reader.setPollTimeout(this.pollTimeout);
		reader.setSaveState(this.saveState);
		return reader;
	}
}
