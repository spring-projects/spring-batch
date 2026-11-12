/*
 * Copyright 2009-present the original author or authors.
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
package org.springframework.batch.infrastructure.item.file.mapping;

import java.util.Map;

import org.junit.jupiter.api.Test;
import tools.jackson.core.exc.UnexpectedEndOfInputException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.batch.infrastructure.item.ParseException;
import org.springframework.batch.infrastructure.item.json.GsonJsonObjectUnmarshaller;
import org.springframework.batch.infrastructure.item.json.JacksonJsonObjectUnmarshaller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonLineMapperTests {

	private static final String USER_LINE = """
			{"name":"foo","email":"bar@example.com","introduction":"I'm\\npowerful\\nman"}""";

	private final JsonLineMapper<Map<String, Object>> mapper = new JsonLineMapper<>(
			new JacksonJsonObjectUnmarshaller<>(new TypeReference<>() {
			}));

	@Test
	void testMapLine() {
		Map<String, Object> map = mapper.mapLine("{\"foo\": 1}", 1);
		assertEquals(1, map.get("foo"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testMapNested() {
		Map<String, Object> map = mapper.mapLine("{\"foo\": 1, \"bar\" : {\"foo\": 2}}", 1);
		assertEquals(1, map.get("foo"));
		assertEquals(2, ((Map<String, Object>) map.get("bar")).get("foo"));
	}

	@Test
	void testMappingError() {
		ParseException exception = assertThrows(ParseException.class, () -> mapper.mapLine("{\"foo\": 1", 1));
		assertInstanceOf(UnexpectedEndOfInputException.class, exception.getCause());
	}

	@Test
	void testMapLineToDomainTypeWithJackson() {
		JsonLineMapper<User> userMapper = new JsonLineMapper<>(new JacksonJsonObjectUnmarshaller<>(User.class));
		assertUser(userMapper.mapLine(USER_LINE, 1));
	}

	@Test
	void testMapLineToDomainTypeWithGson() {
		JsonLineMapper<User> userMapper = new JsonLineMapper<>(new GsonJsonObjectUnmarshaller<>(User.class));
		assertUser(userMapper.mapLine(USER_LINE, 1));
	}

	@Test
	void testMapLineWithCustomUnmarshaller() {
		JsonLineMapper<Integer> lengthMapper = new JsonLineMapper<>(String::length);
		assertEquals(10, lengthMapper.mapLine("0123456789", 1));
	}

	@SuppressWarnings("removal")
	@Test
	void testDeprecatedConstructorsMapToMap() {
		JsonLineMapper<Map<String, Object>> defaultMapper = new JsonLineMapper<>();
		JsonLineMapper<Map<String, Object>> customMapper = new JsonLineMapper<>(new JsonMapper());

		assertEquals(1, defaultMapper.mapLine("{\"foo\": 1}", 1).get("foo"));
		assertEquals(1, customMapper.mapLine("{\"foo\": 1}", 1).get("foo"));
	}

	private void assertUser(User user) {
		assertEquals("foo", user.name());
		assertEquals("bar@example.com", user.email());
		assertEquals("""
				I'm
				powerful
				man""", user.introduction());
	}

	record User(String name, String email, String introduction) {

	}

}
