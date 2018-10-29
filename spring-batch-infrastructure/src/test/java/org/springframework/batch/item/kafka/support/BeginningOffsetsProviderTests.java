package org.springframework.batch.item.kafka.support;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static java.util.Collections.singletonList;

import static org.mockito.Mockito.verify;

public class BeginningOffsetsProviderTests {

	private static final TopicPartition TOPIC_PARTITION = new TopicPartition("topic", 0);

	@Mock
	private Consumer<?, ?> consumer;

	private BeginningOffsetsProvider offsetsProvider;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		offsetsProvider = new BeginningOffsetsProvider();
		offsetsProvider.setConsumer(consumer);
	}

	@Test
	public void testGetBeginningOffsets() {
		offsetsProvider.get(singletonList(TOPIC_PARTITION));

		verify(consumer).beginningOffsets(singletonList(TOPIC_PARTITION));
	}
}