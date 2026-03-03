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
package org.springframework.batch.infrastructure.item.redis;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.util.Assert;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Item reader for Redis based on Spring Data Redis. Uses a {@link RedisTemplate} to query
 * data. The user should provide a {@link ScanOptions} to specify the set of keys to
 * query. The {@code fetchSize} property controls how many items are fetched from Redis in
 * a single pipeline round-trip for efficiency.
 *
 * <p>
 * The implementation is not thread-safe and not restartable.
 * </p>
 *
 * @author Mahmoud Ben Hassine
 * @author Hyunwoo Jung
 * @since 5.1
 * @param <K> type of keys
 * @param <V> type of values
 */
public class RedisItemReader<K, V> implements ItemStreamReader<V> {

	private final RedisTemplate<K, V> redisTemplate;

	private final ScanOptions scanOptions;

	private @Nullable Cursor<K> cursor;

	private final int fetchSize;

	private final Deque<V> buffer;

	public RedisItemReader(RedisTemplate<K, V> redisTemplate, ScanOptions scanOptions, int fetchSize) {
		Assert.notNull(redisTemplate, "redisTemplate must not be null");
		Assert.notNull(scanOptions, "scanOptions must no be null");
		Assert.isTrue(fetchSize > 0, "fetchSize must be greater than 0");
		this.redisTemplate = redisTemplate;
		this.scanOptions = scanOptions;
		this.fetchSize = fetchSize;
		this.buffer = new ArrayDeque<>();
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		this.cursor = this.redisTemplate.scan(this.scanOptions);
	}

	@Override
	public @Nullable V read() throws Exception {
		if (this.buffer.isEmpty()) {
			fetchNext();
		}

		return this.buffer.pollFirst();
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void close() throws ItemStreamException {
		this.cursor.close();
	}

	@SuppressWarnings("DataFlowIssue")
	private void fetchNext() {
		List<K> keys = new ArrayList<>();
		while (this.cursor.hasNext() && keys.size() < this.fetchSize) {
			keys.add(this.cursor.next());
		}

		if (keys.isEmpty()) {
			return;
		}

		@SuppressWarnings("unchecked")
		List<V> items = (List<V>) this.redisTemplate.executePipelined(sessionCallback(keys));

		this.buffer.addAll(items);
	}

	private SessionCallback<Object> sessionCallback(List<K> keys) {
		return new SessionCallback<>() {

			@SuppressWarnings("NullAway")
			@Override
			public @Nullable Object execute(RedisOperations operations) throws DataAccessException {
				for (K key : keys) {
					operations.opsForValue().get(key);
				}

				return null;
			}
		};
	}

}
