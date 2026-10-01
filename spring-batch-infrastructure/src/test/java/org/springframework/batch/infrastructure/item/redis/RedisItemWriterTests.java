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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisItemWriterTests {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private RedisTemplate<String, String> redisTemplate;

	private RedisItemWriter<String, String> redisItemWriter;

	@BeforeEach
	void setup() {
		this.redisItemWriter = new RedisItemWriter<>(new RedisItemKeyMapper(), this.redisTemplate);
		when(this.redisTemplate.executePipelined(any(SessionCallback.class))).thenAnswer(invocation -> {
			SessionCallback<?> sessionCallback = invocation.getArgument(0);
			sessionCallback.execute(this.redisTemplate);
			return null;
		});
	}

	@Test
	void shouldWriteAllItemsToRedis() throws Exception {
		Chunk<String> items = new Chunk<>("val1", "val2");
		this.redisItemWriter.write(items);
		verify(this.redisTemplate.opsForValue()).set(items.getItems().get(0), items.getItems().get(0));
		verify(this.redisTemplate.opsForValue()).set(items.getItems().get(1), items.getItems().get(1));
	}

	@Test
	void shouldDeleteAllItemsToRedis() throws Exception {
		this.redisItemWriter.setDelete(true);
		Chunk<String> items = new Chunk<>("val1", "val2");
		this.redisItemWriter.write(items);
		verify(this.redisTemplate).delete(items.getItems().get(0));
		verify(this.redisTemplate).delete(items.getItems().get(0));
	}

	static class RedisItemKeyMapper implements Converter<String, String> {

		@Override
		public String convert(String source) {
			return source;
		}

	}

}
