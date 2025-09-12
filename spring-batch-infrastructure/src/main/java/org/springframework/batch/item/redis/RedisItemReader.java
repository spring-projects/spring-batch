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
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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
 * The implementation is not thread-safe and not restartable.
 * </p>
 *
 * @author Mahmoud Ben Hassine
 * @since 5.1
 * @param <K> type of keys
 * @param <V> type of values
 */
public class RedisItemReader<K, V> implements ItemStreamReader<V> {

	private static final int DEFAULT_BATCH_SIZE = 1;

	private final RedisTemplate<K, V> redisTemplate;

	private final ScanOptions scanOptions;

	private final int batchSize;

	private Cursor<K> cursor;

	private Queue<V> valueBuffer;

	private boolean bufferInitialized = false;

	public RedisItemReader(RedisTemplate<K, V> redisTemplate, ScanOptions scanOptions) {
		this(redisTemplate, scanOptions, DEFAULT_BATCH_SIZE);
	}

	public RedisItemReader(RedisTemplate<K, V> redisTemplate, ScanOptions scanOptions, int batchSize) {
		Assert.notNull(redisTemplate, "redisTemplate must not be null");
		Assert.notNull(scanOptions, "scanOptions must no be null");
		Assert.isTrue(batchSize > 0, "batchSize must be greater than 0");

		this.redisTemplate = redisTemplate;
		this.scanOptions = scanOptions;
		this.batchSize = batchSize;
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		this.cursor = this.redisTemplate.scan(this.scanOptions);

		if (batchSize > 1) {
			this.valueBuffer = new ConcurrentLinkedQueue<>();
			this.bufferInitialized = true;
		}
	}

	@Override
	public V read() throws Exception {
		if (batchSize == 1) {
			return readSingle();
		}
		else {
			return readBatched();
		}
	}

	private V readSingle() throws Exception {
		if (this.cursor.hasNext()) {
			K nextKey = this.cursor.next();
			return this.redisTemplate.opsForValue().get(nextKey);
		} else {
			return null;
		}
	}

	private V readBatched() throws Exception {
		if (valueBuffer.isEmpty()) {
			fillBuffer();
		}
		return valueBuffer.poll();
	}

	private void fillBuffer() {
		if (!cursor.hasNext()) {
			return;
		}

		List<K> keyBatch = new ArrayList<>(batchSize);
		for (int i = 0; i < batchSize && cursor.hasNext(); i++) {
			keyBatch.add(cursor.next());
		}

		if (!keyBatch.isEmpty()) {
			List<V> values = redisTemplate.opsForValue().multiGet(keyBatch);

			if (values != null) {
				values.stream()
					.filter(Objects::nonNull)
					.forEach(valueBuffer::offer);
			}
		}
	}

	@Override
	public void close() throws ItemStreamException {
		if (this.cursor != null) {
			this.cursor.close();
		}

		if (bufferInitialized && valueBuffer != null) {
			valueBuffer.clear();
		}
	}

}
