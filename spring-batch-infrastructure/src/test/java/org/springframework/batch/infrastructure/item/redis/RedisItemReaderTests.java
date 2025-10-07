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
package org.springframework.batch.infrastructure.item.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.redis.RedisItemReader;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

@ExtendWith(MockitoExtension.class)
public class RedisItemReaderTests {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private ScanOptions scanOptions;

	@Mock
	private Cursor<String> cursor;

	@Test
	void testRead() throws Exception {
		// given
		Mockito.when(this.redisTemplate.scan(this.scanOptions)).thenReturn(this.cursor);
		Mockito.when(this.cursor.hasNext()).thenReturn(true, true, false);
		Mockito.when(this.cursor.next()).thenReturn("person:1", "person:2");
		Mockito.when(this.redisTemplate.opsForValue().get("person:1")).thenReturn("foo");
		Mockito.when(this.redisTemplate.opsForValue().get("person:2")).thenReturn("bar");
		RedisItemReader<String, String> redisItemReader = new RedisItemReader<>(this.redisTemplate, this.scanOptions);
		redisItemReader.open(new ExecutionContext());

		// when
		String item1 = redisItemReader.read();
		String item2 = redisItemReader.read();
		String item3 = redisItemReader.read();

		// then
		Assertions.assertEquals("foo", item1);
		Assertions.assertEquals("bar", item2);
		Assertions.assertNull(item3);
	}

}
