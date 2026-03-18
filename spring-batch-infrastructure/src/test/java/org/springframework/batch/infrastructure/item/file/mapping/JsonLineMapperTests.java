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

import com.fasterxml.jackson.databind.ObjectMapper;
import tools.jackson.core.exc.UnexpectedEndOfInputException;
import org.junit.jupiter.api.Test;
import org.springframework.core.OverridingClassLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonLineMapperTests {

	private final JsonLineMapper mapper = new JsonLineMapper();

	@Test
	void testMapLine() throws Exception {
		Map<String, Object> map = mapper.mapLine("{\"foo\": 1}", 1);
		assertEquals(1, map.get("foo"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testMapNested() throws Exception {
		Map<String, Object> map = mapper.mapLine("{\"foo\": 1, \"bar\" : {\"foo\": 2}}", 1);
		assertEquals(1, map.get("foo"));
		assertEquals(2, ((Map<String, Object>) map.get("bar")).get("foo"));
	}

	@Test
	void testMappingError() {
		assertThrows(UnexpectedEndOfInputException.class, () -> mapper.mapLine("{\"foo\": 1", 1));
	}

	@Test
	void testMapLineWithJackson2ObjectMapper() throws Exception {
		JsonLineMapper mapper = new JsonLineMapper(new ObjectMapper());
		Map<String, Object> map = mapper.mapLine("{\"foo\": 1}", 1);
		assertEquals(1, map.get("foo"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testMapLineWithDefaultConstructorAndJackson2Only() throws Exception {
		ClassLoader classLoader = new OverridingClassLoader(JsonLineMapper.class.getClassLoader()) {
			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				if (name.startsWith("tools.jackson")) {
					throw new ClassNotFoundException(name);
				}
				return super.loadClass(name);
			}

			@Override
			protected boolean isEligibleForOverriding(String className) {
				return className.startsWith(JsonLineMapper.class.getName())
						|| JsonLineMapperJackson2OnlyRunner.class.getName().equals(className);
			}
		};
		Class<?> runnerClass = classLoader.loadClass(JsonLineMapperJackson2OnlyRunner.class.getName());
		Map<String, Object> map = (Map<String, Object>) runnerClass.getMethod("mapLine", String.class)
			.invoke(null, "{\"foo\": 1}");
		assertEquals(1, map.get("foo"));
	}

}
