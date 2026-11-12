/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.infrastructure.item.file.transform;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.mapping.JsonLineMapper;
import org.springframework.batch.infrastructure.item.json.GsonJsonObjectMarshaller;
import org.springframework.batch.infrastructure.item.json.GsonJsonObjectUnmarshaller;
import org.springframework.batch.infrastructure.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.infrastructure.item.json.JacksonJsonObjectUnmarshaller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Yanming Zhou
 * @author Mahmoud Ben Hassine
 */
class JsonLineAggregatorTests {

	private static final User USER = new User("foo", "bar@example.com", """
			I'm
			powerful
			man""");

	private static final String USER_LINE = """
			{"name":"foo","email":"bar@example.com","introduction":"I'm\\npowerful\\nman"}""";

	@Test
	void testAggregateWithJackson() {
		JsonLineAggregator<User> aggregator = new JsonLineAggregator<>(new JacksonJsonObjectMarshaller<>());
		assertEquals(USER_LINE, aggregator.aggregate(USER));
	}

	@Test
	void testAggregateWithGson() {
		JsonLineAggregator<User> aggregator = new JsonLineAggregator<>(new GsonJsonObjectMarshaller<>());
		// Gson escapes the apostrophe by default
		assertEquals(USER_LINE.replace("'", "\\u0027"), aggregator.aggregate(USER));
	}

	@Test
	void testAggregateWithCustomMarshaller() {
		JsonLineAggregator<User> aggregator = new JsonLineAggregator<>(user -> "{\"name\":\"" + user.name() + "\"}");
		assertEquals("{\"name\":\"foo\"}", aggregator.aggregate(USER));
	}

	@Test
	void testAggregatedLineHasNoLineBreak() {
		JsonLineAggregator<User> aggregator = new JsonLineAggregator<>(new JacksonJsonObjectMarshaller<>());
		String line = aggregator.aggregate(USER);
		assertEquals(-1, line.indexOf('\n'));
		assertEquals(-1, line.indexOf('\r'));
	}

	@Test
	void testRoundTripWithJsonLineMapper() {
		JsonLineAggregator<User> aggregator = new JsonLineAggregator<>(new JacksonJsonObjectMarshaller<>());
		JsonLineMapper<User> jacksonMapper = new JsonLineMapper<>(new JacksonJsonObjectUnmarshaller<>(User.class));
		JsonLineMapper<User> gsonMapper = new JsonLineMapper<>(new GsonJsonObjectUnmarshaller<>(User.class));

		String line = aggregator.aggregate(USER);

		assertEquals(USER, jacksonMapper.mapLine(line, 1));
		assertEquals(USER, gsonMapper.mapLine(line, 1));
	}

	@Test
	void testNullMarshaller() {
		assertThrows(IllegalArgumentException.class, () -> new JsonLineAggregator<User>(null));
	}

	record User(String name, String email, String introduction) {

	}

}
