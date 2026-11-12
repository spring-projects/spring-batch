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
package org.springframework.batch.infrastructure.item.json;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import tools.jackson.core.exc.UnexpectedEndOfInputException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.batch.infrastructure.item.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mahmoud Ben Hassine
 */
class JacksonJsonObjectUnmarshallerTests {

	@Test
	void testUnmarshalToDomainType() {
		// given
		JacksonJsonObjectUnmarshaller<Foo> unmarshaller = new JacksonJsonObjectUnmarshaller<>(Foo.class);

		// when
		Foo foo = unmarshaller.unmarshal("{\"id\":1,\"name\":\"foo\"}");

		// then
		assertEquals(new Foo(1, "foo"), foo);
	}

	@Test
	void testUnmarshalToParameterizedType() {
		// given
		JacksonJsonObjectUnmarshaller<Map<String, Object>> unmarshaller = new JacksonJsonObjectUnmarshaller<>(
				new TypeReference<>() {
				});

		// when
		Map<String, Object> map = unmarshaller.unmarshal("{\"foo\": 1, \"bar\": {\"foo\": 2}}");

		// then
		assertEquals(Map.of("foo", 1, "bar", Map.of("foo", 2)), map);
	}

	@Test
	void testUnmarshalToGenericDomainType() {
		// given
		JacksonJsonObjectUnmarshaller<Wrapper<Foo>> unmarshaller = new JacksonJsonObjectUnmarshaller<>(
				new TypeReference<>() {
				});

		// when
		Wrapper<Foo> wrapper = unmarshaller.unmarshal("{\"items\":[{\"id\":1,\"name\":\"foo\"}]}");

		// then
		assertEquals(new Foo(1, "foo"), wrapper.items().get(0));
	}

	@Test
	void testUnmarshalWithCustomJsonMapper() {
		// given
		JsonMapper jsonMapper = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
		JacksonJsonObjectUnmarshaller<Foo> unmarshaller = new JacksonJsonObjectUnmarshaller<>(jsonMapper, Foo.class);

		// when
		Foo foo = unmarshaller.unmarshal("{\"id\":1,\"name\":\"foo\",\"unknown\":true}");

		// then
		assertEquals(new Foo(1, "foo"), foo);
	}

	@Test
	void testUnmarshalWithCustomJsonMapperAndTypeReference() {
		// given
		JsonMapper jsonMapper = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
		JacksonJsonObjectUnmarshaller<Wrapper<Foo>> unmarshaller = new JacksonJsonObjectUnmarshaller<>(jsonMapper,
				new TypeReference<>() {
				});

		// when
		Wrapper<Foo> wrapper = unmarshaller.unmarshal("{\"items\":[{\"id\":1,\"name\":\"foo\",\"unknown\":true}]}");

		// then
		assertEquals(new Foo(1, "foo"), wrapper.items().get(0));
	}

	@Test
	void testUnmarshalInvalidJson() {
		// given
		JacksonJsonObjectUnmarshaller<Foo> unmarshaller = new JacksonJsonObjectUnmarshaller<>(Foo.class);

		// when
		ParseException exception = assertThrows(ParseException.class, () -> unmarshaller.unmarshal("{\"id\":1"));

		// then
		assertInstanceOf(UnexpectedEndOfInputException.class, exception.getCause());
	}

	record Foo(int id, String name) {
	}

	record Wrapper<T>(List<T> items) {
	}

}
