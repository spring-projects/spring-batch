/*
 * Copyright 2019-2020 the original author or authors.
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

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.batch.item.kafka.KafkaItemReader;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Ouellet
 * @author Mahmoud Ben Hassine
 */
public class KafkaItemReaderBuilderTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Properties consumerProperties;

	@Before
	public void setUp() throws Exception {
		this.consumerProperties = new Properties();
		this.consumerProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		this.consumerProperties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "1");
		this.consumerProperties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				StringDeserializer.class.getName());
		this.consumerProperties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				StringDeserializer.class.getName());
	}

	@Test
	public void testNullConsumerProperties() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Consumer properties must not be null");

		new KafkaItemReaderBuilder<>()
				.name("kafkaItemReader")
				.consumerProperties(null)
				.build();
	}

	@Test
	public void testConsumerPropertiesValidation() {
		try {
			new KafkaItemReaderBuilder<>()
					.name("kafkaItemReader")
					.consumerProperties(new Properties())
					.build();
			fail("Expected exception was not thrown");
		} catch (IllegalArgumentException exception) {
			assertEquals("bootstrap.servers property must be provided", exception.getMessage());
		}

		Properties consumerProperties = new Properties();
		consumerProperties.put("bootstrap.servers", "foo");
		try {
			new KafkaItemReaderBuilder<>()
					.name("kafkaItemReader")
					.consumerProperties(consumerProperties)
					.build();
			fail("Expected exception was not thrown");
		} catch (IllegalArgumentException exception) {
			assertEquals("group.id property must be provided", exception.getMessage());
		}

		consumerProperties.put("group.id", "1");
		try {
			new KafkaItemReaderBuilder<>()
					.name("kafkaItemReader")
					.consumerProperties(consumerProperties)
					.build();
			fail("Expected exception was not thrown");
		} catch (IllegalArgumentException exception) {
			assertEquals("key.deserializer property must be provided", exception.getMessage());
		}

		consumerProperties.put("key.deserializer", StringDeserializer.class.getName());
		try {
			new KafkaItemReaderBuilder<>()
					.name("kafkaItemReader")
					.consumerProperties(consumerProperties)
					.build();
			fail("Expected exception was not thrown");
		} catch (IllegalArgumentException exception) {
			assertEquals("value.deserializer property must be provided", exception.getMessage());
		}

		consumerProperties.put("value.deserializer", StringDeserializer.class.getName());
		try {
			new KafkaItemReaderBuilder<>()
					.name("kafkaItemReader")
					.consumerProperties(consumerProperties)
					.topic("test")
					.partitions(0, 1)
					.build();
		} catch (Exception exception) {
			fail("Must not throw an exception when configuration is valid");
		}
	}

	@Test
	public void testNullTopicName() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Topic name must not be null or empty");

		new KafkaItemReaderBuilder<>()
				.name("kafkaItemReader")
				.consumerProperties(this.consumerProperties)
				.topic(null)
				.build();
	}

	@Test
	public void testEmptyTopicName() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Topic name must not be null or empty");

		new KafkaItemReaderBuilder<>()
				.name("kafkaItemReader")
				.consumerProperties(this.consumerProperties)
				.topic("")
				.build();
	}

	@Test
	public void testNullPollTimeout() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("pollTimeout must not be null");

		new KafkaItemReaderBuilder<>()
				.name("kafkaItemReader")
				.consumerProperties(this.consumerProperties)
				.topic("test")
				.pollTimeout(null)
				.build();
	}

	@Test
	public void testNegativePollTimeout() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("pollTimeout must not be negative");

		new KafkaItemReaderBuilder<>()
				.name("kafkaItemReader")
				.consumerProperties(this.consumerProperties)
				.topic("test")
				.pollTimeout(Duration.ofSeconds(-1))
				.build();
	}

	@Test
	public void testZeroPollTimeout() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("pollTimeout must not be zero");

		new KafkaItemReaderBuilder<>()
				.name("kafkaItemReader")
				.consumerProperties(this.consumerProperties)
				.topic("test")
				.pollTimeout(Duration.ZERO)
				.build();
	}

	@Test
	public void testEmptyPartitions() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("At least one partition must be provided");

		new KafkaItemReaderBuilder<>()
				.name("kafkaItemReader")
				.consumerProperties(this.consumerProperties)
				.topic("test")
				.pollTimeout(Duration.ofSeconds(10))
				.build();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testKafkaItemReaderCreation() {
		// given
		boolean saveState = false;
		Duration pollTimeout = Duration.ofSeconds(100);
		String topic = "test";
		List<Integer> partitions = Arrays.asList(0, 1);
		Map<TopicPartition, Long> partitionOffsets = new HashMap<>();
		partitionOffsets.put(new TopicPartition(topic, partitions.get(0)), 10L);
		partitionOffsets.put(new TopicPartition(topic, partitions.get(1)), 15L);

		// when
		KafkaItemReader<String, String> reader = new KafkaItemReaderBuilder<String, String>()
				.name("kafkaItemReader")
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
		List<TopicPartition> topicPartitions = (List<TopicPartition>) ReflectionTestUtils.getField(reader, "topicPartitions");
		assertEquals(2, topicPartitions.size());
		assertEquals(topic, topicPartitions.get(0).topic());
		assertEquals(partitions.get(0).intValue(), topicPartitions.get(0).partition());
		assertEquals(topic, topicPartitions.get(1).topic());
		assertEquals(partitions.get(1).intValue(), topicPartitions.get(1).partition());
		Map<TopicPartition, Long> partitionOffsetsMap = (Map<TopicPartition, Long>) ReflectionTestUtils.getField(reader, "partitionOffsets");
		assertEquals(2, partitionOffsetsMap.size());
		assertEquals(new Long(10), partitionOffsetsMap.get(new TopicPartition(topic, partitions.get(0))));
		assertEquals(new Long(15), partitionOffsetsMap.get(new TopicPartition(topic, partitions.get(1))));
	}
}
