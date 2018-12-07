package org.springframework.batch.item.kafka.support;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;

public class TimestampOffsetsProvider implements OffsetsProvider {

	private Consumer<?, ?> consumer;

	private Long timestampToSearch;

	@Override
	public Map<TopicPartition, Long> get() {
		return consumer
				.offsetsForTimes(consumer.assignment().stream()
						.collect(Collectors.toMap(Function.identity(), topicPartition -> timestampToSearch)))
				.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().offset()));
	}

	@Override
	public void setConsumer(Consumer<?, ?> consumer) {
		this.consumer = consumer;
	}

	public void setTimestampToSearch(Long timestampToSearch) {
		this.timestampToSearch = timestampToSearch;
	}
}
