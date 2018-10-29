package org.springframework.batch.item.kafka;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

/**
 * @author Mathieu Ouellet
 */
public class KafkaItemReaderTests {

	@Mock
	private ConsumerFactory<String, String> consumerFactory;

	@Mock
	private Consumer<String, String> consumer;

	private TopicPartition topicPartition;

	private KafkaItemReader<String, String> reader;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		Map<String, Object> config = new HashMap<>();
		config.put("max.poll.records", 2);
		when(consumerFactory.getConfigurationProperties()).thenReturn(config);
		when(consumerFactory.createConsumer()).thenReturn(consumer);
		topicPartition = new TopicPartition("topic", 0);
		reader = new KafkaItemReader<>();
		reader.setConsumerFactory(consumerFactory);
		reader.setTopicPartitions(Collections.singletonList(topicPartition));
		reader.setSaveState(true);
		reader.setPollTimeout(50L);
		reader.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		reader = new KafkaItemReader<>();

		try {
			reader.afterPropertiesSet();
			fail("Expected exception was not thrown");
		}
		catch (IllegalStateException ignore) {
		}

		reader.setTopicPartitions(Collections.singletonList(new TopicPartition("topic", 0)));
		try {
			reader.afterPropertiesSet();
			fail("Expected exception was not thrown");
		}
		catch (IllegalArgumentException ignore) {
		}

		Map<String, Object> config = new HashMap<>();
		config.put("max.poll.records", 0);
		reader.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config));
		try {
			reader.afterPropertiesSet();
			fail("Expected exception was not thrown");
		}
		catch (IllegalStateException | IllegalArgumentException ignore) {
		}

		config = new HashMap<>();
		config.put("max.poll.records", "0");
		reader.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config));
		try {
			reader.afterPropertiesSet();
			fail("Expected exception was not thrown");
		}
		catch (IllegalStateException | IllegalArgumentException ignore) {
		}

		config = new HashMap<>();
		config.put("max.poll.records", 10);
		config.put("enable.auto.commit", true);
		reader.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config));
		try {
			reader.afterPropertiesSet();
			fail("Expected exception was not thrown");
		}
		catch (IllegalStateException | IllegalArgumentException ignore) {
		}

		config = new HashMap<>();
		config.put("max.poll.records", 10);
		config.put("enable.auto.commit", "true");
		reader.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config));
		try {
			reader.afterPropertiesSet();
			fail("Expected exception was not thrown");
		}
		catch (IllegalStateException | IllegalArgumentException ignore) {
		}

		config = new HashMap<>();
		config.put("max.poll.records", 10);
		reader.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config));
		reader.afterPropertiesSet();
	}

	@Test
	public void shouldAssignTopicPartitions() {
		reader.open(new ExecutionContext());
		verify(consumer).assign(Collections.singletonList(topicPartition));
	}

	@Test
	public void shouldSeekToBeginning() {
		reader.setSaveState(false);
		reader.open(new ExecutionContext());
		verify(consumer).seekToBeginning(Collections.singletonList(topicPartition));
	}

	@Test
	public void shouldSeekOnSavedState() {
		long offset = 100L;
		Map<TopicPartition, Long> offsets = new HashMap<>();
		offsets.put(topicPartition, offset);
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("topic.partition.offset", offsets);
		reader.open(executionContext);
		verify(consumer).seek(topicPartition, offset + 1);
	}

	@Test
	public void testRead() throws Exception {
		Map<TopicPartition, List<ConsumerRecord<String, String>>> records = new HashMap<>();
		records.put(topicPartition, Collections.singletonList(
				new ConsumerRecord<>(topicPartition.topic(), topicPartition.partition(), 0L, "key0", "val0")));
		when(consumer.poll(50L)).thenReturn(new ConsumerRecords<>(records));

		reader.open(new ExecutionContext());
		String read = reader.read();
		assertThat(read, is("val0"));
	}

	@Test
	public void testPollRecords() throws Exception {
		Map<TopicPartition, List<ConsumerRecord<String, String>>> firstPoll = new HashMap<>();
		firstPoll.put(topicPartition,
				Arrays.asList(
						new ConsumerRecord<>(topicPartition.topic(), topicPartition.partition(), 0L, "key0", "val0"),
						new ConsumerRecord<>(topicPartition.topic(), topicPartition.partition(), 1L, "key1", "val1")));

		Map<TopicPartition, List<ConsumerRecord<String, String>>> secondPoll = new HashMap<>();
		secondPoll.put(topicPartition, Collections.singletonList(
				new ConsumerRecord<>(topicPartition.topic(), topicPartition.partition(), 2L, "key2", "val2")));
		when(consumer.poll(50L)).thenReturn(new ConsumerRecords<>(firstPoll), new ConsumerRecords<>(secondPoll));

		reader.open(new ExecutionContext());

		String read = reader.read();
		assertThat(read, is("val0"));

		read = reader.read();
		assertThat(read, is("val1"));

		read = reader.read();
		assertThat(read, is("val2"));
	}

}