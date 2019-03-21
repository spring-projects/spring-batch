/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.batch.item.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.batch.item.ItemStreamException;

/**
 * A json object marshaller that uses <a href="https://github.com/FasterXML/jackson">Jackson</a>
 * to marshal an object into a json representation.
 *
 * @param <T> type of objects to marshal
 * @author Mahmoud Ben Hassine
 * @since 4.1
 */
public class JacksonJsonObjectMarshaller<T> implements JsonObjectMarshaller<T> {

	private ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Set the {@link ObjectMapper} to use.
	 * @param objectMapper to use
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String marshal(T item)  {
		try {
			return objectMapper.writeValueAsString(item);
		} catch (JsonProcessingException e) {
			throw new ItemStreamException("Unable to marshal object " + item + " to Json", e);
		}
	}
}
