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
package org.springframework.batch.infrastructure.item.redis.builder;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.redis.RedisItemWriter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;

/**
 * Builder for {@link RedisItemWriter}.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.1
 */
public class RedisItemWriterBuilder<K, V> {

	private @Nullable RedisTemplate<K, V> redisTemplate;

	private @Nullable Converter<V, K> itemKeyMapper;

	private boolean delete;

	/**
	 * Set the {@link RedisTemplate} to use to write items to Redis.
	 * @param redisTemplate the template to use.
	 * @return The current instance of the builder.
	 * @see RedisItemWriter#setRedisTemplate(RedisTemplate)
	 */
	public RedisItemWriterBuilder<K, V> redisTemplate(RedisTemplate<K, V> redisTemplate) {
		this.redisTemplate = redisTemplate;
		return this;
	}

	/**
	 * Set the {@link Converter} to use to derive the key from the item.
	 * @param itemKeyMapper the Converter to use.
	 * @return The current instance of the builder.
	 * @see RedisItemWriter#setItemKeyMapper(Converter)
	 */
	public RedisItemWriterBuilder<K, V> itemKeyMapper(Converter<V, K> itemKeyMapper) {
		this.itemKeyMapper = itemKeyMapper;
		return this;
	}

	/**
	 * Indicate if the items being passed to the writer should be deleted.
	 * @param delete removal indicator.
	 * @return The current instance of the builder.
	 * @see RedisItemWriter#setDelete(boolean)
	 */
	public RedisItemWriterBuilder<K, V> delete(boolean delete) {
		this.delete = delete;
		return this;
	}

	/**
	 * Validates and builds a {@link RedisItemWriter}.
	 * @return a {@link RedisItemWriter}
	 */
	public RedisItemWriter<K, V> build() {
		Assert.notNull(this.redisTemplate, "RedisTemplate is required.");
		Assert.notNull(this.itemKeyMapper, "itemKeyMapper is required.");

		RedisItemWriter<K, V> writer = new RedisItemWriter<>(this.itemKeyMapper, this.redisTemplate);
		writer.setDelete(this.delete);
		return writer;
	}

}
