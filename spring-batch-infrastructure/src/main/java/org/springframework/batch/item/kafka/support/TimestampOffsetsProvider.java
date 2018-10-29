package org.springframework.batch.item.kafka.support;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.batch.item.kafka.OffsetsProvider;

/**
 * <p>
 * Implementation of {@link OffsetsProvider} that returns offsets for the given topic-partitions by timestamp.
 * </p>
 *
 * @author Mathieu Ouellet
 * @see org.apache.kafka.clients.consumer.KafkaConsumer#offsetsForTimes(java.util.Map)
 * @since 4.2
 */
public class TimestampOffsetsProvider implements OffsetsProvider {

	private final Long timestampToSearch;

	private Consumer<?, ?> consumer;

	public TimestampOffsetsProvider(Long timestampToSearch) {
		this.timestampToSearch = timestampToSearch;
	}

	@Override
	public Map<TopicPartition, Long> get(List<TopicPartition> topicPartitions) {
		Map<TopicPartition, Long> timestampsToSearch = topicPartitions.stream()
				.collect(Collectors.toMap(Function.identity(), topicPartition -> timestampToSearch));
		return consumer.offsetsForTimes(timestampsToSearch).entrySet().stream()
				.filter(entry -> entry.getValue() != null)
				.collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().offset()));
	}

	@Override
	public void setConsumer(Consumer<?, ?> consumer) {
		this.consumer = consumer;
	}
}
