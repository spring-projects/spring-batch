/*
 * Copyright 2019-2022 the original author or authors.
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

package org.springframework.batch.infrastructure.item.kafka.builder;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.kafka.KafkaItemReader;
import org.springframework.batch.infrastructure.item.kafka.builder.KafkaItemReaderBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mathieu Ouellet
 * @author Mahmoud Ben Hassine
 */
class KafkaItemReaderBuilderTests {

	private Properties consumerProperties;

	@BeforeEach
	void setUp() {
		this.consumerProperties = new Properties();
		this.consumerProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		this.consumerProperties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "1");
		this.consumerProperties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				StringDeserializer.class.getName());
		this.consumerProperties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				StringDeserializer.class.getName());
	}

	@Test
	void testNullConsumerProperties() {
		// given
		final KafkaItemReaderBuilder<Object, Object> builder = new KafkaItemReaderBuilder<>().name("kafkaItemReader")
			.consumerProperties(null);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("Consumer properties must not be null");
	}

	@Test
	void testConsumerPropertiesValidation() {
		var builder = new KafkaItemReaderBuilder<>().name("kafkaItemReader").consumerProperties(new Properties());
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("bootstrap.servers property must be provided", exception.getMessage());

		Properties consumerProperties = new Properties();
		consumerProperties.put("bootstrap.servers", "foo");
		builder = new KafkaItemReaderBuilder<>().name("kafkaItemReader").consumerProperties(consumerProperties);
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("group.id property must be provided", exception.getMessage());

		consumerProperties.put("group.id", "1");
		builder = new KafkaItemReaderBuilder<>().name("kafkaItemReader").consumerProperties(consumerProperties);
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("key.deserializer property must be provided", exception.getMessage());

		consumerProperties.put("key.deserializer", StringDeserializer.class.getName());
		builder = new KafkaItemReaderBuilder<>().name("kafkaItemReader").consumerProperties(consumerProperties);
		exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("value.deserializer property must be provided", exception.getMessage());

		consumerProperties.put("value.deserializer", StringDeserializer.class.getName());
		new KafkaItemReaderBuilder<>().name("kafkaItemReader")
			.consumerProperties(consumerProperties)
			.topic("test")
			.partitions(0, 1)
			.build();
	}

	@Test
	void testNullTopicName() {
		// given
		final KafkaItemReaderBuilder<Object, Object> builder = new KafkaItemReaderBuilder<>().name("kafkaItemReader")
			.consumerProperties(this.consumerProperties)
			.topic(null);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("Topic name must not be null or empty");
	}

	@Test
	void testEmptyTopicName() {
		// given
		final KafkaItemReaderBuilder<Object, Object> builder = new KafkaItemReaderBuilder<>().name("kafkaItemReader")
			.consumerProperties(this.consumerProperties)
			.topic("");

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("Topic name must not be null or empty");
	}

	@Test
	void testNullPollTimeout() {
		// given
		final KafkaItemReaderBuilder<Object, Object> builder = new KafkaItemReaderBuilder<>().name("kafkaItemReader")
			.consumerProperties(this.consumerProperties)
			.topic("test")
			.pollTimeout(null);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("pollTimeout must not be null");
	}

	@Test
	void testNegativePollTimeout() {
		// given
		final KafkaItemReaderBuilder<Object, Object> builder = new KafkaItemReaderBuilder<>().name("kafkaItemReader")
			.consumerProperties(this.consumerProperties)
			.topic("test")
			.pollTimeout(Duration.ofSeconds(-1));

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("pollTimeout must not be negative");
	}

	@Test
	void testZeroPollTimeout() {
		// given
		final KafkaItemReaderBuilder<Object, Object> builder = new KafkaItemReaderBuilder<>().name("kafkaItemReader")
			.consumerProperties(this.consumerProperties)
			.topic("test")
			.pollTimeout(Duration.ZERO);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("pollTimeout must not be zero");
	}

	@Test
	void testEmptyPartitions() {
		// given
		final KafkaItemReaderBuilder<Object, Object> builder = new KafkaItemReaderBuilder<>().name("kafkaItemReader")
			.consumerProperties(this.consumerProperties)
			.topic("test")
			.pollTimeout(Duration.ofSeconds(10));

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("At least one partition must be provided");
	}

	@Test
	@SuppressWarnings("unchecked")
	void testKafkaItemReaderCreation() {
		// given
		boolean saveState = false;
		Duration pollTimeout = Duration.ofSeconds(100);
		String topic = "test";
		List<Integer> partitions = Arrays.asList(0, 1);
		Map<TopicPartition, Long> partitionOffsets = new HashMap<>();
		partitionOffsets.put(new TopicPartition(topic, partitions.get(0)), 10L);
		partitionOffsets.put(new TopicPartition(topic, partitions.get(1)), 15L);

		// when
		KafkaItemReader<String, String> reader = new KafkaItemReaderBuilder<String, String>().name("kafkaItemReader")
			.consumerProperties(this.consumerProperties)
			.topic(topic)
			.partitions(partitions)
			.partitionOffsets(partitionOffsets)
			.pollTimeout(pollTimeout)
			.saveState(saveState)
			.build();

		// then
		assertNotNull(reader);
		assertFalse((Boolean) ReflectionTestUtils.getField(reader, "saveState"));
		assertEquals(pollTimeout, ReflectionTestUtils.getField(reader, "pollTimeout"));
		List<TopicPartition> topicPartitions = (List<TopicPartition>) ReflectionTestUtils.getField(reader,
				"topicPartitions");
		assertEquals(2, topicPartitions.size());
		assertEquals(topic, topicPartitions.get(0).topic());
		assertEquals(partitions.get(0).intValue(), topicPartitions.get(0).partition());
		assertEquals(topic, topicPartitions.get(1).topic());
		assertEquals(partitions.get(1).intValue(), topicPartitions.get(1).partition());
		Map<TopicPartition, Long> partitionOffsetsMap = (Map<TopicPartition, Long>) ReflectionTestUtils.getField(reader,
				"partitionOffsets");
		assertEquals(2, partitionOffsetsMap.size());
		assertEquals(Long.valueOf(10L), partitionOffsetsMap.get(new TopicPartition(topic, partitions.get(0))));
		assertEquals(Long.valueOf(15L), partitionOffsetsMap.get(new TopicPartition(topic, partitions.get(1))));
	}

}
