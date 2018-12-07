package org.springframework.batch.item.kafka.support;

import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;

public interface OffsetsProvider {

	Map<TopicPartition, Long> get();

	void setConsumer(Consumer<?, ?> consumer);
}
