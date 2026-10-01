/*
 * Copyright 2023-2025 the original author or authors.
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
package org.springframework.batch.infrastructure.item.redis.builder;

import org.springframework.batch.infrastructure.item.redis.RedisItemReader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

/**
 * Builder for {@link RedisItemReader}.
 *
 * @author Mahmoud Ben Hassine
 * @author Hyunwoo Jung
 * @since 5.1
 * @param <K> type of keys
 * @param <V> type of values
 */
public class RedisItemReaderBuilder<K, V> {

	private RedisTemplate<K, V> redisTemplate;

	private ScanOptions scanOptions;

	private int fetchSize;

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
	 * Set the fetchSize to how many items from Redis in a single round-trip.
	 * @param fetchSize the number of items to fetch per pipeline execution
	 * @return the current builder instance for fluent chaining
	 */
	public RedisItemReaderBuilder<K, V> fetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
		return this;
	}

	/**
	 * Build a new {@link RedisItemReader}.
	 * @return a new item reader
	 */
	public RedisItemReader<K, V> build() {
		return new RedisItemReader<>(this.redisTemplate, this.scanOptions, this.fetchSize);
	}

}
