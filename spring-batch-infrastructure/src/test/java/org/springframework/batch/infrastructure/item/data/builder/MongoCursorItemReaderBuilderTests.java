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
package org.springframework.batch.infrastructure.item.data.builder;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.data.MongoCursorItemReader;
import org.springframework.batch.infrastructure.item.data.builder.MongoCursorItemReaderBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;

/**
 * Test class for {@link MongoCursorItemReaderBuilder}.
 *
 * @author Mahmoud Ben Hassine
 */
public class MongoCursorItemReaderBuilderTests {

	@Test
	void testBuild() {
		// given
		MongoTemplate template = mock();
		Class<String> targetType = String.class;
		Query query = mock();
		Map<String, Sort.Direction> sorts = mock();
		int batchSize = 100;
		int limit = 10000;
		Duration maxTime = Duration.ofSeconds(1);

		// when
		MongoCursorItemReader<String> reader = new MongoCursorItemReaderBuilder<String>().name("reader")
			.template(template)
			.targetType(targetType)
			.query(query)
			.sorts(sorts)
			.batchSize(batchSize)
			.limit(limit)
			.maxTime(maxTime)
			.build();

		// then
		Assertions.assertEquals(template, ReflectionTestUtils.getField(reader, "template"));
		Assertions.assertEquals(targetType, ReflectionTestUtils.getField(reader, "targetType"));
		Assertions.assertEquals(query, ReflectionTestUtils.getField(reader, "query"));
		Assertions.assertEquals(batchSize, ReflectionTestUtils.getField(reader, "batchSize"));
		Assertions.assertEquals(limit, ReflectionTestUtils.getField(reader, "limit"));
		Assertions.assertEquals(maxTime, ReflectionTestUtils.getField(reader, "maxTime"));
	}

}
