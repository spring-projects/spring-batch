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

package org.springframework.batch.item.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mathieu Ouellet
 * @author Mahmoud Ben Hassine
 */
@EmbeddedKafka
@ExtendWith(SpringExtension.class)
class KafkaItemReaderTests {

	@Autowired
	private EmbeddedKafkaBroker embeddedKafka;

	private KafkaItemReader<String, String> reader;

	private KafkaTemplate<String, String> template;

	private Properties consumerProperties;

	@BeforeAll
	static void setUpTopics(@Autowired EmbeddedKafkaBroker embeddedKafka) {
		embeddedKafka.addTopics(new NewTopic("topic1", 1, (short) 1), new NewTopic("topic2", 2, (short) 1),
				new NewTopic("topic3", 1, (short) 1), new NewTopic("topic4", 2, (short) 1),
				new NewTopic("topic5", 1, (short) 1), new NewTopic("topic6", 1, (short) 1));
	}

	@BeforeEach
	void setUp() {
		Map<String, Object> producerProperties = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(producerProperties);
		this.template = new KafkaTemplate<>(producerFactory);

		this.consumerProperties = new Properties();
		this.consumerProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
				embeddedKafka.getBrokersAsString());
		this.consumerProperties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "1");
		this.consumerProperties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				StringDeserializer.class.getName());
		this.consumerProperties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				StringDeserializer.class.getName());
	}

	@Test
	void testValidation() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new KafkaItemReader<>(null, "topic", 0));
		assertEquals("Consumer properties must not be null", exception.getMessage());

		exception = assertThrows(IllegalArgumentException.class,
				() -> new KafkaItemReader<>(new Properties(), "topic", 0));
		assertEquals("bootstrap.servers property must be provided", exception.getMessage());

		Properties consumerProperties = new Properties();
		consumerProperties.put("bootstrap.servers", embeddedKafka);
		exception = assertThrows(IllegalArgumentException.class,
				() -> new KafkaItemReader<>(consumerProperties, "topic", 0));
		assertEquals("group.id property must be provided", exception.getMessage());

		consumerProperties.put("group.id", "1");
		exception = assertThrows(IllegalArgumentException.class,
				() -> new KafkaItemReader<>(consumerProperties, "topic", 0));
		assertEquals("key.deserializer property must be provided", exception.getMessage());

		consumerProperties.put("key.deserializer", StringDeserializer.class.getName());
		exception = assertThrows(IllegalArgumentException.class,
				() -> new KafkaItemReader<>(consumerProperties, "topic", 0));
		assertEquals("value.deserializer property must be provided", exception.getMessage());

		consumerProperties.put("value.deserializer", StringDeserializer.class.getName());
		exception = assertThrows(IllegalArgumentException.class,
				() -> new KafkaItemReader<>(consumerProperties, "", 0));
		assertEquals("Topic name must not be null or empty", exception.getMessage());

		exception = assertThrows(Exception.class, () -> new KafkaItemReader<>(consumerProperties, "topic"));
		assertEquals("At least one partition must be provided", exception.getMessage());

		this.reader = new KafkaItemReader<>(consumerProperties, "topic", 0);

		exception = assertThrows(IllegalArgumentException.class, () -> this.reader.setPollTimeout(null));
		assertEquals("pollTimeout must not be null", exception.getMessage());

		exception = assertThrows(IllegalArgumentException.class, () -> this.reader.setPollTimeout(Duration.ZERO));
		assertEquals("pollTimeout must not be zero", exception.getMessage());

		exception = assertThrows(IllegalArgumentException.class,
				() -> this.reader.setPollTimeout(Duration.ofSeconds(-1)));
		assertEquals("pollTimeout must not be negative", exception.getMessage());
	}

	@Test
	void testReadFromSinglePartition() throws ExecutionException, InterruptedException {
		this.template.setDefaultTopic("topic1");
		var futures = new ArrayList<CompletableFuture<?>>();
		futures.add(this.template.sendDefault("val0"));
		futures.add(this.template.sendDefault("val1"));
		futures.add(this.template.sendDefault("val2"));
		futures.add(this.template.sendDefault("val3"));
		for (var future : futures) {
			future.get();
		}

		this.reader = new KafkaItemReader<>(this.consumerProperties, "topic1", 0);
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(new ExecutionContext());

		String item = this.reader.read();
		assertThat(item, is("val0"));

		item = this.reader.read();
		assertThat(item, is("val1"));

		item = this.reader.read();
		assertThat(item, is("val2"));

		item = this.reader.read();
		assertThat(item, is("val3"));

		item = this.reader.read();
		assertNull(item);

		this.reader.close();
	}

	@Test
	void testReadFromSinglePartitionFromCustomOffset() throws ExecutionException, InterruptedException {
		this.template.setDefaultTopic("topic5");
		var futures = new ArrayList<CompletableFuture<?>>();
		futures.add(this.template.sendDefault("val0")); // <-- offset 0
		futures.add(this.template.sendDefault("val1")); // <-- offset 1
		futures.add(this.template.sendDefault("val2")); // <-- offset 2
		futures.add(this.template.sendDefault("val3")); // <-- offset 3
		for (var future : futures) {
			future.get();
		}

		this.reader = new KafkaItemReader<>(this.consumerProperties, "topic5", 0);

		// specify which offset to start from
		Map<TopicPartition, Long> partitionOffsets = new HashMap<>();
		partitionOffsets.put(new TopicPartition("topic5", 0), 2L);
		this.reader.setPartitionOffsets(partitionOffsets);

		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(new ExecutionContext());

		String item = this.reader.read();
		assertThat(item, is("val2"));

		item = this.reader.read();
		assertThat(item, is("val3"));

		item = this.reader.read();
		assertNull(item);

		this.reader.close();
	}

	@Test
	void testReadFromSinglePartitionFromTheOffsetStoredInKafka() throws Exception {
		// first run: read a topic from the beginning

		this.template.setDefaultTopic("topic6");
		var futures = new ArrayList<CompletableFuture<?>>();
		futures.add(this.template.sendDefault("val0")); // <-- offset 0
		futures.add(this.template.sendDefault("val1")); // <-- offset 1
		for (var future : futures) {
			future.get();
		}
		this.reader = new KafkaItemReader<>(this.consumerProperties, "topic6", 0);
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(new ExecutionContext());

		String item = this.reader.read();
		assertThat(item, is("val0"));

		item = this.reader.read();
		assertThat(item, is("val1"));

		item = this.reader.read();
		assertNull(item);

		this.reader.close();

		// The offset stored in Kafka should be equal to 2 at this point
		OffsetAndMetadata currentOffset = KafkaTestUtils.getCurrentOffset(embeddedKafka.getBrokersAsString(), "1",
				"topic6", 0);
		assertEquals(2, currentOffset.offset());

		// second run (with same consumer group ID): new messages arrived since the last
		// run.

		this.template.sendDefault("val2"); // <-- offset 2
		this.template.sendDefault("val3"); // <-- offset 3

		this.reader = new KafkaItemReader<>(this.consumerProperties, "topic6", 0);
		// Passing an empty map means the reader should start from the offset stored in
		// Kafka (offset 2 in this case)
		this.reader.setPartitionOffsets(new HashMap<>());
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(new ExecutionContext());

		item = this.reader.read();
		assertThat(item, is("val2"));

		item = this.reader.read();
		assertThat(item, is("val3"));

		item = this.reader.read();
		assertNull(item);

		this.reader.close();
	}

	@Test
	void testReadFromMultiplePartitions() throws ExecutionException, InterruptedException {
		this.template.setDefaultTopic("topic2");
		var futures = new ArrayList<CompletableFuture<?>>();
		futures.add(this.template.sendDefault("val0"));
		futures.add(this.template.sendDefault("val1"));
		futures.add(this.template.sendDefault("val2"));
		futures.add(this.template.sendDefault("val3"));
		for (var future : futures) {
			future.get();
		}

		this.reader = new KafkaItemReader<>(this.consumerProperties, "topic2", 0, 1);
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(new ExecutionContext());

		List<String> items = new ArrayList<>();
		items.add(this.reader.read());
		items.add(this.reader.read());
		items.add(this.reader.read());
		items.add(this.reader.read());
		assertThat(items, containsInAnyOrder("val0", "val1", "val2", "val3"));
		String item = this.reader.read();
		assertNull(item);

		this.reader.close();
	}

	@Test
	void testReadFromSinglePartitionAfterRestart() throws ExecutionException, InterruptedException {
		this.template.setDefaultTopic("topic3");
		var futures = new ArrayList<CompletableFuture<?>>();
		futures.add(this.template.sendDefault("val0"));
		futures.add(this.template.sendDefault("val1"));
		futures.add(this.template.sendDefault("val2"));
		futures.add(this.template.sendDefault("val3"));
		futures.add(this.template.sendDefault("val4"));
		for (var future : futures) {
			future.get();
		}
		ExecutionContext executionContext = new ExecutionContext();
		Map<TopicPartition, Long> offsets = new HashMap<>();
		offsets.put(new TopicPartition("topic3", 0), 1L);
		executionContext.put("topic.partition.offsets", offsets);

		// topic3-0: val0, val1, val2, val3, val4
		// ^
		// |
		// last committed offset = 1 (should restart from offset = 2)

		this.reader = new KafkaItemReader<>(this.consumerProperties, "topic3", 0);
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(executionContext);

		List<String> items = new ArrayList<>();
		items.add(this.reader.read());
		items.add(this.reader.read());
		items.add(this.reader.read());
		assertThat(items, containsInAnyOrder("val2", "val3", "val4"));
		String item = this.reader.read();
		assertNull(item);

		this.reader.close();
	}

	@Test
	void testReadFromMultiplePartitionsAfterRestart() throws ExecutionException, InterruptedException {
		var futures = new ArrayList<CompletableFuture<?>>();
		futures.add(this.template.send("topic4", 0, null, "val0"));
		futures.add(this.template.send("topic4", 0, null, "val2"));
		futures.add(this.template.send("topic4", 0, null, "val4"));
		futures.add(this.template.send("topic4", 0, null, "val6"));
		futures.add(this.template.send("topic4", 1, null, "val1"));
		futures.add(this.template.send("topic4", 1, null, "val3"));
		futures.add(this.template.send("topic4", 1, null, "val5"));
		futures.add(this.template.send("topic4", 1, null, "val7"));

		for (var future : futures) {
			future.get();
		}

		ExecutionContext executionContext = new ExecutionContext();
		Map<TopicPartition, Long> offsets = new HashMap<>();
		offsets.put(new TopicPartition("topic4", 0), 1L);
		offsets.put(new TopicPartition("topic4", 1), 2L);
		executionContext.put("topic.partition.offsets", offsets);

		// topic4-0: val0, val2, val4, val6
		// ^
		// |
		// last committed offset = 1 (should restart from offset = 2)
		// topic4-1: val1, val3, val5, val7
		// ^
		// |
		// last committed offset = 2 (should restart from offset = 3)

		this.reader = new KafkaItemReader<>(this.consumerProperties, "topic4", 0, 1);
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(executionContext);

		List<String> items = new ArrayList<>();
		items.add(this.reader.read());
		items.add(this.reader.read());
		items.add(this.reader.read());
		assertThat(items, containsInAnyOrder("val4", "val6", "val7"));
		String item = this.reader.read();
		assertNull(item);

		this.reader.close();
	}

}
