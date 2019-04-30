package org.springframework.batch.item.kafka.support;

import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TimestampOffsetsProviderTests {

	private static final TopicPartition TOPIC_PARTITION = new TopicPartition("topic", 0);

	@Mock
	private Consumer<?, ?> consumer;

	private Long timestampToSearch = 0L;

	private TimestampOffsetsProvider offsetsProvider;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		offsetsProvider = new TimestampOffsetsProvider(timestampToSearch);
		offsetsProvider.setConsumer(consumer);
	}

	@Test
	public void testFilterNullOffsetsForTimestamp() {
		// given
		when(consumer.offsetsForTimes(singletonMap(TOPIC_PARTITION, timestampToSearch)))
				.thenReturn(singletonMap(TOPIC_PARTITION, null));

		// when
		Map<TopicPartition, Long> offsets = offsetsProvider.get(singletonList(TOPIC_PARTITION));

		// then
		assertTrue(offsets.isEmpty());
	}

	@Test
	public void testGetOffsetsForTimes() {
		offsetsProvider.get(singletonList(TOPIC_PARTITION));

		verify(consumer).offsetsForTimes(singletonMap(TOPIC_PARTITION, timestampToSearch));
	}
}