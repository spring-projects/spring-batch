/*
 * Copyright 2019-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.kafka;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.KeyValueItemWriter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * An {@link ItemWriter} implementation for Apache Kafka using a
 * {@link KafkaTemplate} with default topic configured.
 * </p>
 *
 * @author Mathieu Ouellet
 * @author Mahmoud Ben Hassine
 * @since 4.2
 *
 */
public class KafkaItemWriter<K, T> extends KeyValueItemWriter<K, T> {

	protected KafkaTemplate<K, T> kafkaTemplate;
	private final List<ListenableFuture<SendResult<K, T>>> listenableFutures = new ArrayList<>();
	private long timeout = -1;

	@Override
	protected void writeKeyValue(K key, T value) {
		if (this.delete) {
			this.listenableFutures.add(this.kafkaTemplate.sendDefault(key, null));
		}
		else {
			this.listenableFutures.add(this.kafkaTemplate.sendDefault(key, value));
		}
	}

	@Override
	protected void flush() throws Exception{
		this.kafkaTemplate.flush();
		for(ListenableFuture<SendResult<K,T>> future: this.listenableFutures){
			if (this.timeout >= 0) {
				future.get(this.timeout, TimeUnit.MILLISECONDS);
			}
			else {
				future.get();
			}
		}
		this.listenableFutures.clear();
	}

	@Override
	protected void init() {
		Assert.notNull(this.kafkaTemplate, "KafkaTemplate must not be null.");
		Assert.notNull(this.kafkaTemplate.getDefaultTopic(), "KafkaTemplate must have the default topic set.");
	}

	/**
	 * Set the {@link KafkaTemplate} to use.
	 * @param kafkaTemplate to use
	 */
	public void setKafkaTemplate(KafkaTemplate<K, T> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	/**
	 * The time limit to wait when flushing items to Kafka.
	 *
	 * @param timeout milliseconds to wait, defaults to -1 (no timeout).
	 * @since 4.3.2
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

}
