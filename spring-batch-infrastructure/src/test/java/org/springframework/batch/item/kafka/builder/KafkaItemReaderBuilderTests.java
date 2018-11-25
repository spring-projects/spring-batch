package org.springframework.batch.item.kafka.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.kafka.KafkaItemReader;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Mathieu Ouellet
 */
public class KafkaItemReaderBuilderTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private ConsumerFactory<Object, Object> consumerFactory;

	private List<TopicPartition> topicPartitions = Collections.singletonList(new TopicPartition("topic", 0));

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		Map<String, Object> config = new HashMap<>();
		config.put("max.poll.records", 2);
		config.put("enable.auto.commit", false);
		when(consumerFactory.getConfigurationProperties()).thenReturn(config);
	}

	@Test
	public void testNullConsumerFactory() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("'consumerFactory' must not be null.");

		new KafkaItemReaderBuilder<>()
				.name("kafkaItemReader")
				.topicPartitions(topicPartitions)
				.build();
	}

	@Test
	public void testNullTopicsAndTopicPartitions() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Either 'topicPartitions' or 'topics' must be provided.");

		new KafkaItemReaderBuilder<>()
				.name("kafkaItemReader")
				.consumerFactory(consumerFactory)
				.build();
	}

	@Test
	public void testPollTimeoutNegative() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("pollTimeout must not be negative.");

		new KafkaItemReaderBuilder<>()
				.name("kafkaItemReader")
				.consumerFactory(consumerFactory)
				.topicPartitions(topicPartitions)
				.pollTimeout(-1)
				.build();
	}

	@Test
	public void testKafkaItemReaderBuild() {
		// given
		boolean saveState = false;
		long pollTimeout = 100;
		int maxItemCount = 100;
		String name = "kafkaItemReader";

		// when
		KafkaItemReader<Object, Object> reader = new KafkaItemReaderBuilder<>()
				.consumerFactory(consumerFactory)
				.topicPartitions(topicPartitions)
				.pollTimeout(pollTimeout)
				.saveState(saveState)
				.name(name)
				.maxItemCount(maxItemCount)
				.build();

		// then
		assertEquals(consumerFactory, ReflectionTestUtils.getField(reader, "consumerFactory"));
		assertEquals(topicPartitions, ReflectionTestUtils.getField(reader, "topicPartitions"));
		assertEquals(Duration.ofMillis(pollTimeout), ReflectionTestUtils.getField(reader, "pollTimeout"));
		assertEquals(saveState, ReflectionTestUtils.getField(reader, "saveState"));
		assertEquals(maxItemCount, ReflectionTestUtils.getField(reader, "maxItemCount"));
		assertEquals(name, ReflectionTestUtils.getField(reader, "name"));
	}
}