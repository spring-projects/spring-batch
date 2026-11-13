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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.kafka.KafkaItemReader;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Mathieu Ouellet
 * @author Mahmoud Ben Hassine
 * @author François Martin
 * @author Patrick Baumgartner
 * @author Hyunwoo Jung
 * @author Cobi Eun
 */
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(SpringExtension.class)
class KafkaItemReaderIntegrationTests {

	private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:4.0.0");

	@Container
	public static KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

	private KafkaItemReader<String, String> reader;

	private KafkaTemplate<String, String> template;

	private Properties consumerProperties;

	@BeforeAll
	static void setUpTopics() {
		Properties properties = new Properties();
		properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		try (AdminClient adminClient = AdminClient.create(properties)) {
			adminClient.createTopics(List.of(new NewTopic("topic1", 1, (short) 1), new NewTopic("topic2", 2, (short) 1),
					new NewTopic("topic3", 1, (short) 1), new NewTopic("topic4", 2, (short) 1),
					new NewTopic("topic5", 1, (short) 1), new NewTopic("topic6", 1, (short) 1),
					new NewTopic("topic7", 1, (short) 1), new NewTopic("topic8", 1, (short) 1),
					new NewTopic("topic9", 1, (short) 1), new NewTopic("topic10", 1, (short) 1),
					new NewTopic("topic11", 2, (short) 1), new NewTopic("topic12", 1, (short) 1),
					new NewTopic("topic13", 1, (short) 1), new NewTopic("topic14", 1, (short) 1),
					new NewTopic("topic15", 1, (short) 1), new NewTopic("topic16", 1, (short) 1),
					new NewTopic("topic17", 1, (short) 1)));
		}
	}

	@BeforeEach
	void setUp() {
		Map<String, Object> producerProperties = KafkaTestUtils.producerProps(kafka.getBootstrapServers());
		ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(producerProperties);
		this.template = new KafkaTemplate<>(producerFactory);

		this.consumerProperties = new Properties();
		this.consumerProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		this.consumerProperties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "1");
		this.consumerProperties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				StringDeserializer.class.getName());
		this.consumerProperties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				StringDeserializer.class.getName());
	}

	@Test
	void testUpdateDoesNotCommitOffsetRestoredFromExecutionContext() throws Exception {
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("topic.partition.offsets", Map.of("0", 1L));
		this.reader = new KafkaItemReader<>(noAutoCommitProperties(), "topic9", 0);
		this.reader.open(executionContext);
		this.reader.update(executionContext);
		this.reader.close();
		assertNull(KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "1", "topic9", 0));
	}

	@Test
	void testUpdateDoesNotCommitCustomStartOffset() throws Exception {
		this.reader = new KafkaItemReader<>(noAutoCommitProperties(), "topic10", 0);
		this.reader.setPartitionOffsets(new HashMap<>(Map.of(new TopicPartition("topic10", 0), 2L)));
		this.reader.open(new ExecutionContext());
		this.reader.update(new ExecutionContext());
		this.reader.close();
		assertNull(KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "1", "topic10", 0));
	}

	@Test
	void testUpdateDoesNotCommitPartitionsWithoutReturnedRecords() throws Exception {
		for (int partition = 0; partition < 2; partition++) {
			for (int i = 0; i < 3; i++) {
				this.template.send("topic11", partition, null, "p" + partition + "-" + i).get();
			}
		}
		this.reader = new KafkaItemReader<>(noAutoCommitProperties(), "topic11", 0, 1);
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(new ExecutionContext());
		String item = this.reader.read();
		int partition = Integer.parseInt(item.substring(1, 2));
		this.reader.update(new ExecutionContext());
		this.reader.close();
		assertNull(KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "1", "topic11", 1 - partition));
		assertEquals(1,
				KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "1", "topic11", partition).offset());
	}

	@Test
	void testUpdateAfterFullyConsumingTopicCommitsLastOffsetPlusOne() throws Exception {
		for (int i = 0; i < 3; i++) {
			this.template.send("topic12", "val" + i).get();
		}
		this.reader = new KafkaItemReader<>(noAutoCommitProperties(), "topic12", 0);
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(new ExecutionContext());
		assertThat(this.reader.read(), is("val0"));
		assertThat(this.reader.read(), is("val1"));
		assertThat(this.reader.read(), is("val2"));
		assertNull(this.reader.read());
		this.reader.update(new ExecutionContext());
		this.reader.close();
		assertEquals(3, KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "1", "topic12", 0).offset());
	}

	@Test
	void testUpdateKeepsExecutionContextFormat() throws Exception {
		for (int i = 0; i < 5; i++) {
			this.template.send("topic13", "val" + i).get();
		}
		ExecutionContext executionContext = new ExecutionContext();
		this.reader = new KafkaItemReader<>(noAutoCommitProperties(), "topic13", 0);
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(executionContext);
		assertThat(this.reader.read(), is("val0"));
		assertThat(this.reader.read(), is("val1"));
		this.reader.update(executionContext);
		this.reader.close();
		assertEquals(Map.of("0", 1L), executionContext.get("topic.partition.offsets"));
	}

	@Test
	void testUpdateCommitsReturnedRecordsWhenSaveStateIsDisabled() throws Exception {
		for (int i = 0; i < 5; i++) {
			this.template.send("topic14", "val" + i).get();
		}
		ExecutionContext executionContext = new ExecutionContext();
		this.reader = new KafkaItemReader<>(noAutoCommitProperties(), "topic14", 0);
		this.reader.setSaveState(false);
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(executionContext);
		assertThat(this.reader.read(), is("val0"));
		assertThat(this.reader.read(), is("val1"));
		this.reader.update(executionContext);
		this.reader.close();
		assertFalse(executionContext.containsKey("topic.partition.offsets"));
		assertEquals(2, KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "1", "topic14", 0).offset());
	}

	@Test
	void testReopenResetsReturnedOffsets() throws Exception {
		for (int i = 0; i < 5; i++) {
			this.template.send("topic15", "val" + i).get();
		}
		Properties properties = noAutoCommitProperties();
		this.reader = new KafkaItemReader<>(properties, "topic15", 0);
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(new ExecutionContext());
		assertThat(this.reader.read(), is("val0"));
		assertThat(this.reader.read(), is("val1"));
		this.reader.update(new ExecutionContext());
		this.reader.close();
		assertEquals(2, KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "1", "topic15", 0).offset());

		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "2");
		this.reader.open(new ExecutionContext());
		this.reader.update(new ExecutionContext());
		this.reader.close();
		assertNull(KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "2", "topic15", 0));
		assertEquals(2, KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "1", "topic15", 0).offset());
	}

	@Test
	void testUpdatePreservesConsumerPositionAfterEmptyPoll() throws Exception {
		this.template.send("topic16", "initial").get();
		Properties properties = noAutoCommitProperties();
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		String lastItem = null;
		for (int execution = 0; execution < 3; execution++) {
			this.reader = new KafkaItemReader<>(properties, "topic16", 0);
			this.reader.setPartitionOffsets(new HashMap<>());
			this.reader.setPollTimeout(Duration.ofSeconds(1));
			this.reader.open(new ExecutionContext());
			String item;
			while ((item = this.reader.read()) != null) {
				lastItem = item;
			}
			this.reader.update(new ExecutionContext());
			this.reader.close();
			if (execution < 2) {
				this.template.send("topic16", "between-" + (execution + 1)).get();
			}
		}
		assertThat(lastItem, is("between-2"));
	}

	@Test
	void testUpdateDoesNotCommitBeforePollingAfterReopen() throws Exception {
		for (int i = 0; i < 3; i++) {
			this.template.send("topic17", "val" + i).get();
		}
		Properties properties = noAutoCommitProperties();
		this.reader = new KafkaItemReader<>(properties, "topic17", 0);
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(new ExecutionContext());
		assertThat(this.reader.read(), is("val0"));
		assertThat(this.reader.read(), is("val1"));
		assertThat(this.reader.read(), is("val2"));
		assertNull(this.reader.read());
		this.reader.update(new ExecutionContext());
		this.reader.close();
		assertEquals(3, KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "1", "topic17", 0).offset());

		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "2");
		this.reader.open(new ExecutionContext());
		this.reader.update(new ExecutionContext());
		this.reader.close();
		assertNull(KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "2", "topic17", 0));
	}

	private Properties noAutoCommitProperties() {
		Properties properties = new Properties();
		properties.putAll(this.consumerProperties);
		properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		return properties;
	}

	@Test
	void testUpdateWithoutReadDoesNotCommit() throws Exception {
		this.reader = new KafkaItemReader<>(noAutoCommitProperties(), "topic8", 0);
		this.reader.open(new ExecutionContext());
		this.reader.update(new ExecutionContext());
		this.reader.close();
		assertNull(KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "1", "topic8", 0));
	}

	@Test
	void testUpdateCommitsOnlyReturnedRecordsForPartialPoll() throws Exception {
		for (int i = 0; i < 5; i++) {
			this.template.send("topic7", "val" + i).get();
		}
		this.reader = new KafkaItemReader<>(noAutoCommitProperties(), "topic7", 0);
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(new ExecutionContext());
		assertThat(this.reader.read(), is("val0"));
		assertThat(this.reader.read(), is("val1"));
		this.reader.update(new ExecutionContext());
		this.reader.close();
		assertEquals(2, KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "1", "topic7", 0).offset());

		this.reader = new KafkaItemReader<>(noAutoCommitProperties(), "topic7", 0);
		this.reader.setPartitionOffsets(new HashMap<>());
		this.reader.setPollTimeout(Duration.ofSeconds(1));
		this.reader.open(new ExecutionContext());
		assertThat(this.reader.read(), is("val2"));
		assertThat(this.reader.read(), is("val3"));
		assertThat(this.reader.read(), is("val4"));
		assertNull(this.reader.read());
		this.reader.close();
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
		OffsetAndMetadata currentOffset = KafkaTestUtils.getCurrentOffset(kafka.getBootstrapServers(), "1", "topic6",
				0);
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
		Map<String, Long> offsets = new HashMap<>();
		offsets.put("0", 1L);
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
		Map<String, Long> offsets = new HashMap<>();
		offsets.put("0", 1L);
		offsets.put("1", 2L);
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
