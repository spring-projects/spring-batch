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

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.batch.infrastructure.item.ParseException;
import org.springframework.util.Assert;

/**
 * A json object unmarshaller that uses Jackson 3 to unmarshal a json representation into
 * an object.
 *
 * @param <T> type of objects to unmarshal
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 * @since 6.1
 */
public class JacksonJsonObjectUnmarshaller<T> implements JsonObjectUnmarshaller<T> {

	private final JsonMapper jsonMapper;

	private final JavaType type;

	/**
	 * Create a new {@link JacksonJsonObjectUnmarshaller} with a default
	 * {@link JsonMapper}.
	 * @param type the target type
	 */
	public JacksonJsonObjectUnmarshaller(Class<T> type) {
		this(new JsonMapper(), type);
	}

	/**
	 * Create a new {@link JacksonJsonObjectUnmarshaller} with a default
	 * {@link JsonMapper}. Use this constructor for parameterized target types, for
	 * example {@code new TypeReference<Map<String, Object>>() {}}.
	 * @param type the target type
	 */
	public JacksonJsonObjectUnmarshaller(TypeReference<T> type) {
		this(new JsonMapper(), type);
	}

	/**
	 * Create a new {@link JacksonJsonObjectUnmarshaller} with the provided
	 * {@link JsonMapper}.
	 * @param jsonMapper the json mapper to use
	 * @param type the target type
	 */
	public JacksonJsonObjectUnmarshaller(JsonMapper jsonMapper, Class<T> type) {
		Assert.notNull(jsonMapper, "The json mapper must not be null");
		Assert.notNull(type, "The target type must not be null");
		this.jsonMapper = jsonMapper;
		this.type = jsonMapper.constructType(type);
	}

	/**
	 * Create a new {@link JacksonJsonObjectUnmarshaller} with the provided
	 * {@link JsonMapper}. Use this constructor for parameterized target types, for
	 * example {@code new TypeReference<Map<String, Object>>() {}}.
	 * @param jsonMapper the json mapper to use
	 * @param type the target type
	 */
	public JacksonJsonObjectUnmarshaller(JsonMapper jsonMapper, TypeReference<T> type) {
		Assert.notNull(jsonMapper, "The json mapper must not be null");
		Assert.notNull(type, "The target type must not be null");
		this.jsonMapper = jsonMapper;
		this.type = jsonMapper.constructType(type);
	}

	/**
	 * Unmarshal the provided json into an object of the target type.
	 * @param json the json representation of the object
	 * @return the unmarshalled object
	 * @throws ParseException if the json cannot be unmarshalled
	 */
	@Override
	public T unmarshal(String json) {
		try {
			return this.jsonMapper.readValue(json, this.type);
		}
		catch (JacksonException e) {
			throw new ParseException("Unable to unmarshal json to " + this.type, e);
		}
	}

}
