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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.batch.item.ExecutionContext;
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

	@Test
	void testReadWithBatchSize() throws Exception {
		// given
		int batchSize = 2;
		Mockito.when(this.redisTemplate.scan(this.scanOptions)).thenReturn(this.cursor);
		Mockito.when(this.cursor.hasNext()).thenReturn(true, true, true, true, false);
		Mockito.when(this.cursor.next()).thenReturn("person:1", "person:2", "person:3", "person:4");

		// Setup multiGet - batch of 2 keys each time
		Mockito.when(this.redisTemplate.opsForValue().multiGet(Mockito.anyList())).thenAnswer(invocation -> {
			List<String> keys = invocation.getArgument(0);
			if (keys.size() == 2) {
				if (keys.get(0).equals("person:1") && keys.get(1).equals("person:2")) {
					return Arrays.asList("foo", "bar");
				}
				if (keys.get(0).equals("person:3") && keys.get(1).equals("person:4")) {
					return Arrays.asList("baz", "qux");
				}
			}
			return Collections.emptyList();
		});

		RedisItemReader<String, String> redisItemReader = new RedisItemReader<>(this.redisTemplate, this.scanOptions,
				batchSize);
		redisItemReader.open(new ExecutionContext());

		// when
		String item1 = redisItemReader.read();
		String item2 = redisItemReader.read();
		String item3 = redisItemReader.read();
		String item4 = redisItemReader.read();
		String item5 = redisItemReader.read();

		// then
		Assertions.assertEquals("foo", item1);
		Assertions.assertEquals("bar", item2);
		Assertions.assertEquals("baz", item3);
		Assertions.assertEquals("qux", item4);
		Assertions.assertNull(item5);

		// Verify multiGet was called instead of individual get
		Mockito.verify(this.redisTemplate.opsForValue(), Mockito.times(2)).multiGet(Mockito.any());
		Mockito.verify(this.redisTemplate.opsForValue(), Mockito.never()).get(Mockito.any());
	}

	@Test
	void testReadWithBatchSizeHandlesNullValues() throws Exception {
		// given
		int batchSize = 3;
		Mockito.when(this.redisTemplate.scan(this.scanOptions)).thenReturn(this.cursor);
		Mockito.when(this.cursor.hasNext()).thenReturn(true, true, true, false);
		Mockito.when(this.cursor.next()).thenReturn("person:1", "person:2", "person:3");

		// multiGet returns some null values
		List<String> keys = Arrays.asList("person:1", "person:2", "person:3");
		List<String> values = Arrays.asList("foo", null, "baz"); // person:2 is null
		Mockito.when(this.redisTemplate.opsForValue().multiGet(keys)).thenReturn(values);

		RedisItemReader<String, String> redisItemReader = new RedisItemReader<>(this.redisTemplate, this.scanOptions,
				batchSize);
		redisItemReader.open(new ExecutionContext());

		// when
		String item1 = redisItemReader.read();
		String item2 = redisItemReader.read();
		String item3 = redisItemReader.read();

		// then - null values should be filtered out
		Assertions.assertEquals("foo", item1);
		Assertions.assertEquals("baz", item2);
		Assertions.assertNull(item3);
	}

	@Test
	void testBackwardCompatibilityWithDefaultBatchSize() throws Exception {
		// given - using constructor without batchSize (defaults to 1)
		Mockito.when(this.redisTemplate.scan(this.scanOptions)).thenReturn(this.cursor);
		Mockito.when(this.cursor.hasNext()).thenReturn(true, true, false);
		Mockito.when(this.cursor.next()).thenReturn("person:1", "person:2");
		Mockito.when(this.redisTemplate.opsForValue().get("person:1")).thenReturn("foo");
		Mockito.when(this.redisTemplate.opsForValue().get("person:2")).thenReturn("bar");

		// Using old constructor
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

		// Verify individual get was called, not multiGet (backward compatibility)
		Mockito.verify(this.redisTemplate.opsForValue(), Mockito.times(2)).get(Mockito.any());
		Mockito.verify(this.redisTemplate.opsForValue(), Mockito.never()).multiGet(Mockito.any());
	}

}
