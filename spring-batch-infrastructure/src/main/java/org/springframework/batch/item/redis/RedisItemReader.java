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
package org.springframework.batch.item.redis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.util.Assert;

/**
 * Item reader for Redis based on Spring Data Redis. Uses a {@link RedisTemplate} to query
 * data. The user should provide a {@link ScanOptions} to specify the set of keys to
 * query.
 *
 * <p>
 * The reader supports batch fetching using Redis MGET operations when batchSize is
 * greater than 1, which significantly improves performance by reducing network
 * round-trips.
 * </p>
 *
 * <p>
 * The implementation is not thread-safe and not restartable.
 * </p>
 *
 * @author Mahmoud Ben Hassine
 * @since 5.1
 * @param <K> type of keys
 * @param <V> type of values
 */
public class RedisItemReader<K, V> implements ItemStreamReader<V> {

	private final RedisTemplate<K, V> redisTemplate;

	private final ScanOptions scanOptions;

	private final int batchSize;

	private final Queue<V> valueBuffer;

	private Cursor<K> cursor;

	public RedisItemReader(RedisTemplate<K, V> redisTemplate, ScanOptions scanOptions) {
		this(redisTemplate, scanOptions, 1);
	}

	public RedisItemReader(RedisTemplate<K, V> redisTemplate, ScanOptions scanOptions, int batchSize) {
		Assert.notNull(redisTemplate, "redisTemplate must not be null");
		Assert.notNull(scanOptions, "scanOptions must no be null");
		Assert.isTrue(batchSize > 0 && batchSize <= 1000, "batchSize must be between 1 and 1000");
		this.redisTemplate = redisTemplate;
		this.scanOptions = scanOptions;
		this.batchSize = batchSize;
		this.valueBuffer = new LinkedList<>();
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		this.cursor = this.redisTemplate.scan(this.scanOptions);
	}

	@Override
	public V read() throws Exception {
		// If buffer has values, return from buffer first
		if (!this.valueBuffer.isEmpty()) {
			return this.valueBuffer.poll();
		}

		// If batch size is 1, use single GET operation (backward compatibility)
		if (this.batchSize == 1) {
			if (this.cursor.hasNext()) {
				K nextKey = this.cursor.next();
				return this.redisTemplate.opsForValue().get(nextKey);
			}
			else {
				return null;
			}
		}

		// Batch mode: collect keys and use MGET
		List<K> keysToFetch = new ArrayList<>();
		while (this.cursor.hasNext() && keysToFetch.size() < this.batchSize) {
			keysToFetch.add(this.cursor.next());
		}

		if (keysToFetch.isEmpty()) {
			return null;
		}

		// Use multiGet for batch fetching
		List<V> values = this.redisTemplate.opsForValue().multiGet(keysToFetch);

		if (values != null) {
			// Filter out null values and add to buffer
			for (V value : values) {
				if (value != null) {
					this.valueBuffer.offer(value);
				}
			}
		}

		// Return first value from buffer or null if empty
		return this.valueBuffer.poll();
	}

	@Override
	public void close() throws ItemStreamException {
		this.cursor.close();
	}

}
