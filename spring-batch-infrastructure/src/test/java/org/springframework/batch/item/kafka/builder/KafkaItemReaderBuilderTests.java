package org.springframework.batch.item.kafka.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

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
	}

	@Test
	public void testNullConsumerFactory() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("consumerFactory is required.");

		new KafkaItemReaderBuilder<>().topicPartitions(topicPartitions).build();
	}

	@Test
	public void testNullTopicPartitions() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("topicPartitions is required.");

		new KafkaItemReaderBuilder<>().consumerFactory(consumerFactory).build();
	}

	@Test
	public void testPollTimeoutNotGreaterThanZero() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("pollTimeout must be greater than zero");

		new KafkaItemReaderBuilder<>().consumerFactory(consumerFactory).topicPartitions(topicPartitions).pollTimeout(0)
				.build();
	}

	@Test
	public void testKafkaItemReaderBuild() {
		// given
		boolean saveState = false;
		long pollTimeout = 100;

		// when
		KafkaItemReader<Object, Object> reader = new KafkaItemReaderBuilder<>().consumerFactory(consumerFactory)
				.topicPartitions(topicPartitions).pollTimeout(pollTimeout).saveState(saveState).build();

		// then
		assertFalse((Boolean) ReflectionTestUtils.getField(reader, "saveState"));
		assertEquals(consumerFactory, ReflectionTestUtils.getField(reader, "consumerFactory"));
		assertEquals(topicPartitions, ReflectionTestUtils.getField(reader, "topicPartitions"));
		assertEquals(Duration.ofMillis(pollTimeout), ReflectionTestUtils.getField(reader, "pollTimeout"));
	}
}