package org.springframework.batch.item.kafka.support;

import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;

public class StoredOffsetsProvider implements OffsetsProvider {

	private Map<TopicPartition, Long> offsets;

	@Override
	public Map<TopicPartition, Long> get() {
		return offsets;
	}

	@Override
	public void setConsumer(Consumer<?, ?> consumer) {
	}

	public void setOffsets(Map<TopicPartition, Long> offsets) {
		this.offsets = offsets;
	}
}
