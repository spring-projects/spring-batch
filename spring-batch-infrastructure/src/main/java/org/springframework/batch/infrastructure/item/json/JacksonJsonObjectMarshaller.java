/*
 * Copyright 2018-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.infrastructure.item.json;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.batch.infrastructure.item.ItemStreamException;

/**
 * A json object marshaller that uses Jackson 3 to marshal an object into a json
 * representation.
 *
 * @param <T> type of objects to marshal
 * @author Mahmoud Ben Hassine
 * @since 4.1
 */
public class JacksonJsonObjectMarshaller<T> implements JsonObjectMarshaller<T> {

	private JsonMapper jsonMapper;

	public JacksonJsonObjectMarshaller() {
		this(new JsonMapper());
	}

	public JacksonJsonObjectMarshaller(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	/**
	 * Set the {@link JsonMapper} to use.
	 * @param jsonMapper to use
	 * @see #JacksonJsonObjectMarshaller(JsonMapper)
	 */
	public void setJsonMapper(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public String marshal(T item) {
		try {
			return this.jsonMapper.writeValueAsString(item);
		}
		catch (JacksonException e) {
			throw new ItemStreamException("Unable to marshal object " + item + " to Json", e);
		}
	}

}
