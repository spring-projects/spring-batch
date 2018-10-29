package org.springframework.batch.item.kafka.support;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.batch.item.kafka.OffsetsProvider;

/**
 * <p>
 * Implementation of {@link OffsetsProvider} that returns the earliest offsets for the given topic-partitions.
 * Equivalent of using 'auto.offset.reset' set to 'earliest' with 'enable.auto.commit' set to false.
 * </p>
 *
 * @author Mathieu Ouellet
 * @see org.apache.kafka.clients.consumer.KafkaConsumer#beginningOffsets(Collection)
 * @since 4.2
 */
public class BeginningOffsetsProvider implements OffsetsProvider {

	private Consumer<?, ?> consumer;

	@Override
	public Map<TopicPartition, Long> get(List<TopicPartition> topicPartitions) {
		return this.consumer.beginningOffsets(topicPartitions);
	}

	@Override
	public void setConsumer(Consumer<?, ?> consumer) {
		this.consumer = consumer;
	}
}
