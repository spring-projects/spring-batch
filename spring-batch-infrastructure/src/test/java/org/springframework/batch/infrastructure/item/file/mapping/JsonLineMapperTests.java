/*
 * Copyright 2009-2025 the original author or authors.
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

import tools.jackson.core.exc.UnexpectedEndOfInputException;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonLineMapperTests {

	private final JsonLineMapper<Map<String, Object>> mapJsonLineMapper = new JsonLineMapper<>(new TypeReference<>() {
	});

	private final JsonLineMapper<User> userJsonLineMapper = new JsonLineMapper<>(User.class);

	@Test
	void testMapLine() throws Exception {
		Map<String, Object> map = mapJsonLineMapper.mapLine("{\"foo\": 1}", 1);
		assertEquals(1, map.get("foo"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testMapNested() throws Exception {
		Map<String, Object> map = mapJsonLineMapper.mapLine("{\"foo\": 1, \"bar\" : {\"foo\": 2}}", 1);
		assertEquals(1, map.get("foo"));
		assertEquals(2, ((Map<String, Object>) map.get("bar")).get("foo"));
	}

	@Test
	void testMappingError() {
		assertThrows(UnexpectedEndOfInputException.class, () -> mapJsonLineMapper.mapLine("{\"foo\": 1", 1));
	}

	@Test
	void testMapLineToDomainType() throws Exception {
		User user = userJsonLineMapper.mapLine("""
				{"name":"foo","email":"bar@example.com","introduction":"I'm\\npowerful\\nman"}""", 1);
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
