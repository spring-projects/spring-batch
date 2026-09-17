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

import java.lang.reflect.Type;
import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.batch.infrastructure.item.file.LineMapper;

/**
 * Interpret a line as a JSON object and parse it up to provided target type. The line
 * should be a standard JSON object, starting with "{" and ending with "}" and composed of
 * <code>name:value</code> pairs separated by commas. Whitespace is ignored, e.g.
 *
 * <pre>
 * { "foo" : "bar", "value" : 123 }
 * </pre>
 *
 * The values can also be JSON objects (which are converted to maps):
 *
 * <pre>
 * { "foo": "bar", "map": { "one": 1, "two": 2}}
 * </pre>
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 *
 */
public class JsonLineMapper<T> implements LineMapper<T> {

	private final JsonMapper jsonMapper;

	private final TypeReference<T> type;

	/**
	 * Create a new {@link JsonLineMapper} with a default {@link JsonMapper} and
	 * {@code Map<String, Object>} target type.
	 * @deprecated in favor of {@code new JsonLineMapper<Map<String, Object>>(new
	 * TypeReference<>() {})}
	 */
	@Deprecated(forRemoval = true)
	public JsonLineMapper() {
		this(new JsonMapper());
	}

	/**
	 * Create a new {@link JsonLineMapper} with the provided {@link JsonMapper} and
	 * {@code Map<String, Object>} target type.
	 * @param jsonMapper the json mapper to use
	 * @since 6.0
	 * @deprecated in favor of {@code new JsonLineMapper<Map<String, Object>>(jsonMapper,
	 * new TypeReference<>() {})}
	 */
	@Deprecated(forRemoval = true)
	@SuppressWarnings("unchecked")
	public JsonLineMapper(JsonMapper jsonMapper) {
		this(jsonMapper, (TypeReference<T>) new TypeReference<Map<String, Object>>() {
		});
	}

	/**
	 * Create a new {@link JsonLineMapper} with the provided {@link JsonMapper} and
	 * provided target type.
	 * @param type the target type
	 * @since 6.1
	 */
	public JsonLineMapper(Class<T> type) {
		this(new JsonMapper(), type);
	}

	/**
	 * Create a new {@link JsonLineMapper} with the provided {@link JsonMapper} and
	 * provided target type.
	 * @param type the target type
	 * @since 6.1
	 */
	public JsonLineMapper(TypeReference<T> type) {
		this(new JsonMapper(), type);
	}

	/**
	 * Create a new {@link JsonLineMapper} with the provided {@link JsonMapper} and
	 * provided target type.
	 * @param jsonMapper the json mapper to use
	 * @param type the target type
	 * @since 6.1
	 */
	public JsonLineMapper(JsonMapper jsonMapper, Class<T> type) {
		this(jsonMapper, new TypeReference<>() {
			@Override
			public Type getType() {
				return type;
			}
		});
	}

	/**
	 * Create a new {@link JsonLineMapper} with the provided {@link JsonMapper}.
	 * @param jsonMapper the json mapper to use
	 * @param type the target type
	 * @since 6.1
	 */
	public JsonLineMapper(JsonMapper jsonMapper, TypeReference<T> type) {
		this.jsonMapper = jsonMapper;
		this.type = type;
	}

	/**
	 * Interpret the line as a Json object and convert it to target type.
	 *
	 * @see LineMapper#mapLine(String, int)
	 */
	@Override
	public T mapLine(String line, int lineNumber) throws Exception {
		return this.jsonMapper.readValue(line, type);
	}

}
