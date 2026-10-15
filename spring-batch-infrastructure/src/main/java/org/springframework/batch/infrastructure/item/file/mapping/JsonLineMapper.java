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

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.json.JacksonJsonObjectUnmarshaller;
import org.springframework.batch.infrastructure.item.json.JsonObjectUnmarshaller;
import org.springframework.util.Assert;

/**
 * Interpret a line as a JSON object and unmarshal it to the target type, using a
 * {@link JsonObjectUnmarshaller}. This makes the mapper agnostic to the JSON library:
 * Jackson and Gson implementations of {@link JsonObjectUnmarshaller} are provided. The
 * line should be a standard JSON object, starting with "{" and ending with "}" and
 * composed of <code>name:value</code> pairs separated by commas. Whitespace is ignored,
 * e.g.
 *
 * <pre>
 * { "foo" : "bar", "value" : 123 }
 * </pre>
 *
 * The values can also be JSON objects:
 *
 * <pre>
 * { "foo": "bar", "map": { "one": 1, "two": 2}}
 * </pre>
 *
 * This is the reader-side counterpart of
 * {@link org.springframework.batch.infrastructure.item.file.transform.LineAggregator}
 * implementations that write JSON lines. For example, to map each line to a domain type
 * with Jackson:
 *
 * <pre>
 * new JsonLineMapper&lt;&gt;(new JacksonJsonObjectUnmarshaller&lt;&gt;(Trade.class));
 * </pre>
 *
 * @param <T> type of the target object
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 *
 */
public class JsonLineMapper<T> implements LineMapper<T> {

	private final JsonObjectUnmarshaller<T> jsonObjectUnmarshaller;

	/**
	 * Create a new {@link JsonLineMapper} with a default {@link JsonMapper} and
	 * {@code Map<String, Object>} target type.
	 * @deprecated since 6.1 in favor of {@link #JsonLineMapper(JsonObjectUnmarshaller)},
	 * for example {@code new JsonLineMapper<>(new JacksonJsonObjectUnmarshaller<>(new
	 * TypeReference<Map<String, Object>>() {}))}. Scheduled for removal in 7.0.
	 */
	@Deprecated(since = "6.1", forRemoval = true)
	public JsonLineMapper() {
		this(new JsonMapper());
	}

	/**
	 * Create a new {@link JsonLineMapper} with the provided {@link JsonMapper} and
	 * {@code Map<String, Object>} target type.
	 * @param jsonMapper the json mapper to use
	 * @since 6.0
	 * @deprecated since 6.1 in favor of {@link #JsonLineMapper(JsonObjectUnmarshaller)},
	 * for example {@code new JsonLineMapper<>(new
	 * JacksonJsonObjectUnmarshaller<>(jsonMapper, new TypeReference<Map<String,
	 * Object>>() {}))}. Scheduled for removal in 7.0.
	 */
	@Deprecated(since = "6.1", forRemoval = true)
	@SuppressWarnings("unchecked")
	public JsonLineMapper(JsonMapper jsonMapper) {
		this((JsonObjectUnmarshaller<T>) new JacksonJsonObjectUnmarshaller<>(jsonMapper,
				new TypeReference<Map<String, Object>>() {
				}));
	}

	/**
	 * Create a new {@link JsonLineMapper} with the provided
	 * {@link JsonObjectUnmarshaller}.
	 * @param jsonObjectUnmarshaller the unmarshaller used to convert a line to the target
	 * type
	 * @since 6.1
	 */
	public JsonLineMapper(JsonObjectUnmarshaller<T> jsonObjectUnmarshaller) {
		Assert.notNull(jsonObjectUnmarshaller, "The json object unmarshaller must not be null");
		this.jsonObjectUnmarshaller = jsonObjectUnmarshaller;
	}

	/**
	 * Interpret the line as a Json object and unmarshal it to the target type.
	 *
	 * @see LineMapper#mapLine(String, int)
	 */
	@Override
	public T mapLine(String line, int lineNumber) {
		return this.jsonObjectUnmarshaller.unmarshal(line);
	}

}
