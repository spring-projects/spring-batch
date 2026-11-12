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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.springframework.batch.infrastructure.item.ParseException;
import org.springframework.util.Assert;

/**
 * A json object unmarshaller that uses <a href="https://github.com/google/gson">Google
 * Gson</a> to unmarshal a json representation into an object.
 *
 * @param <T> type of objects to unmarshal
 * @author Mahmoud Ben Hassine
 * @since 6.1
 */
public class GsonJsonObjectUnmarshaller<T> implements JsonObjectUnmarshaller<T> {

	private final Gson gson;

	private final TypeToken<T> type;

	/**
	 * Create a new {@link GsonJsonObjectUnmarshaller} with a default {@link Gson}.
	 * @param type the target type
	 */
	public GsonJsonObjectUnmarshaller(Class<T> type) {
		this(new Gson(), type);
	}

	/**
	 * Create a new {@link GsonJsonObjectUnmarshaller} with a default {@link Gson}. Use
	 * this constructor for parameterized target types, for example {@code new
	 * TypeToken<Map<String, Object>>() {}}.
	 * @param type the target type
	 */
	public GsonJsonObjectUnmarshaller(TypeToken<T> type) {
		this(new Gson(), type);
	}

	/**
	 * Create a new {@link GsonJsonObjectUnmarshaller} with the provided {@link Gson}.
	 * @param gson the gson object to use
	 * @param type the target type
	 */
	public GsonJsonObjectUnmarshaller(Gson gson, Class<T> type) {
		this(gson, TypeToken.get(type));
	}

	/**
	 * Create a new {@link GsonJsonObjectUnmarshaller} with the provided {@link Gson}. Use
	 * this constructor for parameterized target types, for example {@code new
	 * TypeToken<Map<String, Object>>() {}}.
	 * @param gson the gson object to use
	 * @param type the target type
	 */
	public GsonJsonObjectUnmarshaller(Gson gson, TypeToken<T> type) {
		Assert.notNull(gson, "The gson object must not be null");
		Assert.notNull(type, "The target type must not be null");
		this.gson = gson;
		this.type = type;
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
			return this.gson.fromJson(json, this.type);
		}
		catch (JsonParseException e) {
			throw new ParseException("Unable to unmarshal json to " + this.type, e);
		}
	}

}
