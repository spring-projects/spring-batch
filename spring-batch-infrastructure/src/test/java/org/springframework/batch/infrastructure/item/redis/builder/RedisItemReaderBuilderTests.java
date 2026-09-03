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

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.redis.RedisItemReader;
import org.springframework.batch.infrastructure.item.redis.builder.RedisItemReaderBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Test class for {@link RedisItemReaderBuilder}.
 *
 * @author Mahmoud Ben Hassine
 * @author Hyunwoo Jung
 */
class RedisItemReaderBuilderTests {

	@Test
	void testRedisItemReaderCreation() {
		// given
		RedisTemplate<String, String> redisTemplate = mock();
		ScanOptions scanOptions = mock();

		// when
		RedisItemReader<String, String> reader = new RedisItemReaderBuilder<String, String>()
			.redisTemplate(redisTemplate)
			.scanOptions(scanOptions)
			.fetchSize(10)
			.build();

		// then
		assertNotNull(reader);
		assertEquals(redisTemplate, ReflectionTestUtils.getField(reader, "redisTemplate"));
		assertEquals(scanOptions, ReflectionTestUtils.getField(reader, "scanOptions"));
		assertEquals(10, ReflectionTestUtils.getField(reader, "fetchSize"));
	}

}
