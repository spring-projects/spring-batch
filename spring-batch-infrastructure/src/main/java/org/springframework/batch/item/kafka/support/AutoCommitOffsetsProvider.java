package org.springframework.batch.item.kafka.support;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.batch.item.kafka.OffsetsProvider;

/**
 * Noop implementation of {@link OffsetsProvider} to use with automatic offset committing. This is the only
 * implementation allowed when the KafkaConsumer has 'enable.auto.commit' set to true. See 'auto.offset.reset' config
 * for initial offset strategy options.
 *
 * @author Mathieu Ouellet
 * @since 4.2
 */
public class AutoCommitOffsetsProvider implements OffsetsProvider {

	/**
	 * @return null, preventing a call to
	 * {@link org.apache.kafka.clients.consumer.Consumer#seek(org.apache.kafka.common.TopicPartition, long)}
	 */
	@Override
	public Map<TopicPartition, Long> get(List<TopicPartition> topicPartitions) {
		return null;
	}

	@Override
	public void setConsumer(Consumer<?, ?> consumer) {
	}
}
