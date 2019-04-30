package org.springframework.batch.item.kafka;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;

/**
 * A convenient strategy for providing initial externally stored offsets for the {@link KafkaItemReader} to
 * seeks to.
 *
 * @author Mathieu Ouellet
 * @since 4.2
 */
public interface OffsetsProvider {

	/**
	 * <p>
	 * Map of assigned topic-partitions and the offset, or read position, at which the {@link KafkaItemReader} should
	 * start.
	 * </p>
	 *
	 * @param topicPartitions list of assigned topic-partitions to get offset for
	 * @return map of offset by topic-partition
	 */
	Map<TopicPartition, Long> get(List<TopicPartition> topicPartitions);

	/**
	 * <p>
	 * Inject a {@link Consumer} that can be used to fetch internally stored offsets and/or topic-partition assignment.
	 * </p>
	 *
	 * @param consumer the {@link Consumer} to set
	 */
	void setConsumer(Consumer<?, ?> consumer);
}
