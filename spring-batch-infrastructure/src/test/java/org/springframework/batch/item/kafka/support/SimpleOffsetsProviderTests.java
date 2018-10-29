package org.springframework.batch.item.kafka.support;

import java.util.Map;

import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import static org.junit.Assert.assertEquals;

public class SimpleOffsetsProviderTests {

	private static final TopicPartition TOPIC_PARTITION = new TopicPartition("topic", 0);

	private Map<TopicPartition, Long> offsets;

	private SimpleOffsetsProvider offsetsProvider;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		offsets = singletonMap(TOPIC_PARTITION, 0L);
		offsetsProvider = new SimpleOffsetsProvider();
		offsetsProvider.setOffsets(offsets);
	}

	@Test
	public void testGetProvidedOffsets() {
		assertEquals(offsets, offsetsProvider.get(singletonList(TOPIC_PARTITION)));
	}

}