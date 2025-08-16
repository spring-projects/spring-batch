/*
 * Copyright 2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.batch.item.redis.builder;

import org.springframework.batch.item.redis.RedisItemReader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

/**
 * Builder for {@link RedisItemReader}.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.1
 * @param <K> type of keys
 * @param <V> type of values
 */
public class RedisItemReaderBuilder<K, V> {

	private RedisTemplate<K, V> redisTemplate;

	private ScanOptions scanOptions;

	private int batchSize = 1;

	/**
	 * Set the {@link RedisTemplate} to use in the reader.
	 * @param redisTemplate the template to use
	 * @return the current builder instance for fluent chaining
	 */
	public RedisItemReaderBuilder<K, V> redisTemplate(RedisTemplate<K, V> redisTemplate) {
		this.redisTemplate = redisTemplate;
		return this;
	}

	/**
	 * Set the {@link ScanOptions} to select the key set.
	 * @param scanOptions the scan option to use
	 * @return the current builder instance for fluent chaining
	 */
	public RedisItemReaderBuilder<K, V> scanOptions(ScanOptions scanOptions) {
		this.scanOptions = scanOptions;
		return this;
	}

	/**
	 * Set the batch size for fetching values from Redis. When set to a value greater than
	 * 1, the reader will use Redis MGET operations to fetch multiple values in a single
	 * network round-trip, significantly improving performance.
	 * @param batchSize the number of keys to fetch in each batch (must be between 1 and
	 * 1000)
	 * @return the current builder instance for fluent chaining
	 */
	public RedisItemReaderBuilder<K, V> batchSize(int batchSize) {
		this.batchSize = batchSize;
		return this;
	}

	/**
	 * Build a new {@link RedisItemReader}.
	 * @return a new item reader
	 */
	public RedisItemReader<K, V> build() {
		return new RedisItemReader<>(this.redisTemplate, this.scanOptions, this.batchSize);
	}

}
