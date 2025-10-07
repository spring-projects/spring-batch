/*
 * Copyright 2023 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.batch.infrastructure.item.redis.RedisItemWriter;
import org.springframework.batch.infrastructure.item.redis.builder.RedisItemWriterBuilder;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class RedisItemWriterBuilderTests {

	@Mock
	private RedisTemplate<String, String> redisTemplate;

	private RedisItemKeyMapper itemKeyMapper;

	@BeforeEach
	void setUp() {
		this.itemKeyMapper = new RedisItemKeyMapper();
	}

	@Test
	void testNullRedisTemplate() {
		// given
		final RedisItemWriterBuilder<String, String> builder = new RedisItemWriterBuilder<String, String>()
			.itemKeyMapper(this.itemKeyMapper);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("RedisTemplate is required.");
	}

	@Test
	void testNullItemKeyMapper() {
		// given
		final RedisItemWriterBuilder<String, String> builder = new RedisItemWriterBuilder<String, String>()
			.redisTemplate(this.redisTemplate);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("itemKeyMapper is required.");
	}

	@Test
	void testRedisItemWriterBuild() {
		// given
		boolean delete = true;

		// when
		RedisItemWriter<String, String> writer = new RedisItemWriterBuilder<String, String>()
			.redisTemplate(this.redisTemplate)
			.itemKeyMapper(this.itemKeyMapper)
			.delete(delete)
			.build();

		// then
		assertTrue((Boolean) ReflectionTestUtils.getField(writer, "delete"));
		assertEquals(this.itemKeyMapper, ReflectionTestUtils.getField(writer, "itemKeyMapper"));
		assertEquals(this.redisTemplate, ReflectionTestUtils.getField(writer, "redisTemplate"));
	}

	static class RedisItemKeyMapper implements Converter<String, String> {

		@Override
		public String convert(String source) {
			return source;
		}

	}

}
