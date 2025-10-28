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

import java.io.InputStream;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ParseException;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Implementation of {@link JsonObjectReader} based on Jackson 3.
 *
 * @param <T> type of the target object
 * @author Mahmoud Ben Hassine
 * @author Jimmy Praet
 * @since 4.1
 */
public class JacksonJsonObjectReader<T> implements JsonObjectReader<T> {

	private final Class<? extends T> itemType;

	private JsonMapper mapper;

	private @Nullable JsonParser jsonParser;

	private @Nullable InputStream inputStream;

	/**
	 * Create a new {@link JacksonJsonObjectReader} instance. This will initialize the
	 * reader with a default {@link JsonMapper} having
	 * {@link DeserializationFeature#FAIL_ON_TRAILING_TOKENS} and
	 * {@link DeserializationFeature#FAIL_ON_NULL_FOR_PRIMITIVES} disabled by default. If
	 * you want to customize the mapper, use
	 * {@link #JacksonJsonObjectReader(JsonMapper, Class)}.
	 * @param itemType the target item type
	 */
	public JacksonJsonObjectReader(Class<? extends T> itemType) {
		this(JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
			.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
			.build(), itemType);
	}

	/**
	 * Create a new {@link JacksonJsonObjectReader} instance.
	 * @param mapper the json mapper to use
	 * @param itemType the target item type
	 */
	public JacksonJsonObjectReader(JsonMapper mapper, Class<? extends T> itemType) {
		this.mapper = mapper;
		this.itemType = itemType;
	}

	/**
	 * Set the json mapper to use to map Json objects to domain objects.
	 * @param mapper the object mapper to use
	 * @see #JacksonJsonObjectReader(JsonMapper, Class)
	 */
	public void setMapper(JsonMapper mapper) {
		Assert.notNull(mapper, "The mapper must not be null");
		this.mapper = mapper;
	}

	@Override
	public void open(Resource resource) throws Exception {
		Assert.notNull(resource, "The resource must not be null");
		this.inputStream = resource.getInputStream();
		this.jsonParser = this.mapper.createParser(this.inputStream);
		Assert.state(this.jsonParser.nextToken() == JsonToken.START_ARRAY,
				"The Json input stream must start with an array of Json objects");
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public @Nullable T read() throws Exception {
		try {
			if (this.jsonParser.nextToken() == JsonToken.START_OBJECT) {
				return this.mapper.readValue(this.jsonParser, this.itemType);
			}
		}
		catch (JacksonException e) {
			throw new ParseException("Unable to read next JSON object", e);
		}
		return null;
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void close() throws Exception {
		this.inputStream.close();
		this.jsonParser.close();
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void jumpToItem(int itemIndex) throws Exception {
		for (int i = 0; i < itemIndex; i++) {
			if (this.jsonParser.nextToken() == JsonToken.START_OBJECT) {
				this.jsonParser.skipChildren();
			}
		}
	}

}
