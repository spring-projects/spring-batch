package org.springframework.batch.item.kafka.support;

import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;

public class BeginningOffsetsProvider implements OffsetsProvider {

	private Consumer<?, ?> consumer;

	@Override
	public Map<TopicPartition, Long> get() {
		return this.consumer.beginningOffsets(this.consumer.assignment());
	}

	@Override
	public void setConsumer(Consumer<?, ?> consumer) {
		this.consumer = consumer;
	}
}
