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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mahmoud Ben Hassine
 */
class GsonJsonObjectUnmarshallerTests {

	@Test
	void testUnmarshalToDomainType() {
		// given
		GsonJsonObjectUnmarshaller<Foo> unmarshaller = new GsonJsonObjectUnmarshaller<>(Foo.class);

		// when
		Foo foo = unmarshaller.unmarshal("{\"id\":1,\"name\":\"foo\"}");

		// then
		assertEquals(new Foo(1, "foo"), foo);
	}

	@Test
	void testUnmarshalToParameterizedType() {
		// given
		GsonJsonObjectUnmarshaller<Map<String, Integer>> unmarshaller = new GsonJsonObjectUnmarshaller<>(
				new TypeToken<>() {
				});

		// when
		Map<String, Integer> map = unmarshaller.unmarshal("{\"foo\": 1, \"bar\": 2}");

		// then
		assertEquals(Map.of("foo", 1, "bar", 2), map);
	}

	@Test
	void testUnmarshalToGenericDomainType() {
		// given
		GsonJsonObjectUnmarshaller<Wrapper<Foo>> unmarshaller = new GsonJsonObjectUnmarshaller<>(new TypeToken<>() {
		});

		// when
		Wrapper<Foo> wrapper = unmarshaller.unmarshal("{\"items\":[{\"id\":1,\"name\":\"foo\"}]}");

		// then
		assertEquals(new Foo(1, "foo"), wrapper.items().get(0));
	}

	@Test
	void testUnmarshalWithCustomGson() {
		// given
		Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
		GsonJsonObjectUnmarshaller<Foo> unmarshaller = new GsonJsonObjectUnmarshaller<>(gson, Foo.class);

		// when
		Foo foo = unmarshaller.unmarshal("{\"Id\":1,\"Name\":\"foo\"}");

		// then
		assertEquals(new Foo(1, "foo"), foo);
	}

	@Test
	void testUnmarshalWithCustomGsonAndTypeToken() {
		// given
		Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
		GsonJsonObjectUnmarshaller<Wrapper<Foo>> unmarshaller = new GsonJsonObjectUnmarshaller<>(gson,
				new TypeToken<>() {
				});

		// when
		Wrapper<Foo> wrapper = unmarshaller.unmarshal("{\"Items\":[{\"Id\":1,\"Name\":\"foo\"}]}");

		// then
		assertEquals(new Foo(1, "foo"), wrapper.items().get(0));
	}

	@Test
	void testUnmarshalInvalidJson() {
		// given
		GsonJsonObjectUnmarshaller<Foo> unmarshaller = new GsonJsonObjectUnmarshaller<>(Foo.class);

		// when
		ParseException exception = assertThrows(ParseException.class, () -> unmarshaller.unmarshal("{\"id\":1"));

		// then
		assertInstanceOf(JsonParseException.class, exception.getCause());
	}

	record Foo(int id, String name) {
	}

	record Wrapper<T>(List<T> items) {
	}

}
