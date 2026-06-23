/*
 * Copyright 2023-2025 the original author or authors.
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

package org.springframework.batch.infrastructure.item.redis;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.KeyValueItemWriter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.util.Pair;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * An {@link ItemWriter} implementation for Redis using a {@link RedisTemplate} .
 * </p>
 *
 * @author Santiago Molano
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @author Hyunwoo Jung
 * @since 5.1
 */
public class RedisItemWriter<K, T> extends KeyValueItemWriter<K, T> {

	private RedisTemplate<K, T> redisTemplate;

	private final List<Pair<K, T>> buffer = new ArrayList<>();

	/**
	 * Create a new {@link RedisItemWriter}.
	 * @param itemKeyMapper the {@link Converter} used to derive a key from an item.
	 * @param redisTemplate the {@link RedisTemplate} to use to interact with Redis.
	 * @since 6.0
	 */
	public RedisItemWriter(Converter<T, K> itemKeyMapper, RedisTemplate<K, T> redisTemplate) {
		super(itemKeyMapper);
		Assert.notNull(redisTemplate, "RedisTemplate must not be null");
		this.redisTemplate = redisTemplate;
	}

	@Override
	protected void writeKeyValue(K key, T value) {
		this.buffer.add(Pair.of(key, value));
	}

	@Override
	protected void init() {
		Assert.notNull(this.redisTemplate, "RedisTemplate must not be null");
	}

	@Override
	protected void flush() throws Exception {
		if (this.buffer.isEmpty()) {
			return;
		}

		this.redisTemplate.executePipelined(sessionCallback());

		this.buffer.clear();
	}

	/**
	 * Set the {@link RedisTemplate} to use.
	 * @param redisTemplate the template to use
	 */
	public void setRedisTemplate(RedisTemplate<K, T> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	private SessionCallback<Object> sessionCallback() {
		return new SessionCallback<>() {

			@SuppressWarnings({ "unchecked", "NullAway" })
			@Override
			public @Nullable Object execute(RedisOperations operations) throws DataAccessException {
				if (RedisItemWriter.this.delete) {
					executeDeleteOperations(operations);
				}
				else {
					executeSetOperations(operations);
				}
				return null;
			}
		};
	}

	private void executeDeleteOperations(RedisOperations<K, T> operations) {
		for (Pair<K, T> item : this.buffer) {
			operations.delete(item.getFirst());
		}
	}

	private void executeSetOperations(RedisOperations<K, T> operations) {
		for (Pair<K, T> item : this.buffer) {
			operations.opsForValue().set(item.getFirst(), item.getSecond());
		}
	}

}
