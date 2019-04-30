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
import org.springframework.batch.item.kafka.OffsetsProvider;
import org.springframework.batch.item.kafka.support.AutoCommitOffsetsProvider;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link KafkaItemReader}
 *
 * @author Mathieu Ouellet
 * @since 4.2
 * @see KafkaItemReader
 */
public class KafkaItemReaderBuilder<K, V> {

	private ConsumerFactory<K, V> consumerFactory;

	private List<TopicPartition> topicPartitions;

	private List<String> topics;

	private OffsetsProvider offsetsProvider = new AutoCommitOffsetsProvider();

	private long pollTimeout = 50L;

	private boolean saveState = true;

	private String name;

	private int maxItemCount = Integer.MAX_VALUE;

	/**
	 * Configure if the state of the {@link org.springframework.batch.item.ItemStreamSupport} should be persisted within
	 * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
	 *
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public KafkaItemReaderBuilder<K, V> saveState(boolean saveState) {
		this.saveState = saveState;
		return this;
	}

	/**
	 * The name used to calculate the key within the {@link org.springframework.batch.item.ExecutionContext}. Required
	 * if {@link #saveState(boolean)} is set to true.
	 *
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public KafkaItemReaderBuilder<K, V> name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Configure the max number of items to be read.
	 *
	 * @param maxItemCount the max items to be read
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public KafkaItemReaderBuilder<K, V> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;
		return this;
	}

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
	 * A list of {@link TopicPartition}s to manually assign the consumer.
	 *
	 * @param topicPartitions list of partitions to assign the consumer
	 * @return The current instance of the builder.
	 * @see KafkaItemReader#setTopicPartitions(List)
	 */
	public KafkaItemReaderBuilder<K, V> topicPartitions(List<TopicPartition> topicPartitions) {
		this.topicPartitions = topicPartitions;
		return this;
	}

	/**
	 * A list of topics to manually assign the consumer.
	 *
	 * @param topics list of topics to assign the consumer
	 * @return The current instance of the builder.
	 * @see KafkaItemReader#setTopics(List)
	 */
	public KafkaItemReaderBuilder<K, V> topics(List<String> topics) {
		this.topics = topics;
		return this;
	}

	/**
	 * The {@link OffsetsProvider} implementation to provide initial offsets.
	 *
	 * @param offsetsProvider
	 * @return The current instance of the builder.
	 * @see KafkaItemReader#setOffsetsProvider(OffsetsProvider)
	 */
	public KafkaItemReaderBuilder<K,V> offsetsProvider(OffsetsProvider offsetsProvider)  {
		this.offsetsProvider = offsetsProvider;
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

	public KafkaItemReader<K, V> build() {
		if (this.saveState) {
			Assert.hasText(this.name, "A name is required when saveState is set to true");
		}
		Assert.state(this.topicPartitions != null || this.topics != null, "Either 'topicPartitions' or 'topics' must be provided.");
		Assert.state(this.topicPartitions == null || this.topics == null, "Both 'topicPartitions' and 'topics' cannot be specified together.");
		Assert.isTrue(this.pollTimeout >= 0, "pollTimeout must not be negative.");
		Assert.notNull(this.consumerFactory, "'consumerFactory' must not be null.");
		Assert.notNull(this.offsetsProvider, "'offsetsProvider' must not be null.");
		if  (this.consumerFactory.isAutoCommit()) {
			Assert.state(this.offsetsProvider instanceof AutoCommitOffsetsProvider, "'AutoCommitOffsetsProvider' must be used if 'consumerFactory' is set to auto commit.");
		}

		KafkaItemReader<K, V> reader = new KafkaItemReader<>();
		reader.setConsumerFactory(this.consumerFactory);
		reader.setTopicPartitions(this.topicPartitions);
		reader.setTopics(this.topics);
		reader.setOffsetsProvider(this.offsetsProvider);
		reader.setPollTimeout(this.pollTimeout);
		reader.setSaveState(this.saveState);
		reader.setName(this.name);
		reader.setMaxItemCount(this.maxItemCount);
		return reader;
	}
}
