package org.springframework.batch.item.kafka.support;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.batch.item.kafka.OffsetsProvider;

/**
 * <p>
 * Implementation of {@link OffsetsProvider} that returns default or provided offsets for the given topic-partitions.
 * </p>
 *
 * @author Mathieu Ouellet
 * @since 4.2
 */
public class SimpleOffsetsProvider implements OffsetsProvider {

	private Map<TopicPartition, Long> offsets;

	@Override
	public Map<TopicPartition, Long> get(List<TopicPartition> topicPartitions) {
		return this.offsets;
	}

	@Override
	public void setConsumer(Consumer<?, ?> consumer) {
	}

	public void setOffsets(Map<TopicPartition, Long> offsets) {
		this.offsets = offsets;
	}
}
