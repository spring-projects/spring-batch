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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Interpret a line as a JSON object and parse it up to a Map. The line should be a
 * standard JSON object, starting with "{" and ending with "}" and composed of
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
 *
 */
public class JsonLineMapper implements LineMapper<Map<String, Object>> {

	private final JsonObjectMapper jsonMapper;

	/**
	 * Create a new {@link JsonLineMapper} with a default Jackson 3 or Jackson 2 object
	 * mapper.
	 */
	public JsonLineMapper() {
		this(createDefaultJsonObjectMapper());
	}

	/**
	 * Create a new {@link JsonLineMapper} with the provided {@link JsonMapper}.
	 * @param jsonMapper the json mapper to use
	 * @since 6.0
	 */
	public JsonLineMapper(JsonMapper jsonMapper) {
		this((line) -> jsonMapper.readValue(line, Map.class));
	}

	/**
	 * Create a new {@link JsonLineMapper} with the provided Jackson 2
	 * {@link ObjectMapper}.
	 * @param jsonMapper the json mapper to use
	 * @since 6.0
	 */
	public JsonLineMapper(ObjectMapper jsonMapper) {
		this((line) -> jsonMapper.readValue(line, Map.class));
	}

	private JsonLineMapper(JsonObjectMapper jsonMapper) {
		Assert.notNull(jsonMapper, "jsonMapper must not be null");
		this.jsonMapper = jsonMapper;
	}

	/**
	 * Interpret the line as a Json object and create a Map from it.
	 *
	 * @see LineMapper#mapLine(String, int)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> mapLine(String line, int lineNumber) throws Exception {
		return this.jsonMapper.readValue(line);
	}

	private static JsonObjectMapper createDefaultJsonObjectMapper() {
		ClassLoader classLoader = JsonLineMapper.class.getClassLoader();
		Object jsonMapper = instantiateJsonMapper("tools.jackson.databind.json.JsonMapper", classLoader);
		if (jsonMapper == null) {
			jsonMapper = instantiateJsonMapper("com.fasterxml.jackson.databind.ObjectMapper", classLoader);
		}
		if (jsonMapper == null) {
			throw new IllegalStateException("Either Jackson 3 or Jackson 2 is required to use JsonLineMapper");
		}
		Method readValueMethod = findReadValueMethod(jsonMapper.getClass());
		return new ReflectionJsonObjectMapper(jsonMapper, readValueMethod);
	}

	private static @Nullable Object instantiateJsonMapper(String className, ClassLoader classLoader) {
		try {
			Class<?> mapperClass = ClassUtils.forName(className, classLoader);
			return mapperClass.getConstructor().newInstance();
		}
		catch (ClassNotFoundException ex) {
			return null;
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
			throw new IllegalStateException("Failed to create JSON mapper of type " + className, ex);
		}
	}

	private static Method findReadValueMethod(Class<?> mapperClass) {
		try {
			return mapperClass.getMethod("readValue", String.class, Class.class);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(
					"The JSON mapper of type " + mapperClass.getName() + " does not expose readValue(String, Class)",
					ex);
		}
	}

	private interface JsonObjectMapper {

		Map<String, Object> readValue(String line) throws Exception;

	}

	private static final class ReflectionJsonObjectMapper implements JsonObjectMapper {

		private final Object jsonMapper;

		private final Method readValueMethod;

		private ReflectionJsonObjectMapper(Object jsonMapper, Method readValueMethod) {
			this.jsonMapper = jsonMapper;
			this.readValueMethod = readValueMethod;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Map<String, Object> readValue(String line) throws Exception {
			try {
				return (Map<String, Object>) this.readValueMethod.invoke(this.jsonMapper, line, Map.class);
			}
			catch (InvocationTargetException ex) {
				Throwable targetException = ex.getTargetException();
				if (targetException instanceof Exception exception) {
					throw exception;
				}
				throw new IllegalStateException(targetException);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Failed to invoke JSON mapper", ex);
			}
		}

	}

}
