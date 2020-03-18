/*
 * Copyright 2018-2019 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import org.springframework.batch.item.ParseException;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link JsonObjectReader} based on
 * <a href="https://github.com/google/gson">Google Gson</a>.
 *
 * @param <T> type of the target object
 *
 * @author Mahmoud Ben Hassine
 * @since 4.1
 */
public class GsonJsonObjectReader<T> implements JsonObjectReader<T> {

	private Class<? extends T> itemType;

	private JsonReader jsonReader;

	private Gson mapper = new Gson();

	private InputStream inputStream;

	/**
	 * Create a new {@link GsonJsonObjectReader} instance.
	 * @param itemType the target item type
	 */
	public GsonJsonObjectReader(Class<? extends T> itemType) {
		this.itemType = itemType;
	}

	/**
	 * Set the object mapper to use to map Json objects to domain objects.
	 * @param mapper the object mapper to use
	 */
	public void setMapper(Gson mapper) {
		Assert.notNull(mapper, "The mapper must not be null");
		this.mapper = mapper;
	}

	@Override
	public void open(Resource resource) throws Exception {
		Assert.notNull(resource, "The resource must not be null");
		this.inputStream = resource.getInputStream();
		this.jsonReader = this.mapper.newJsonReader(new InputStreamReader(this.inputStream));
		Assert.state(this.jsonReader.peek() == JsonToken.BEGIN_ARRAY,
				"The Json input stream must start with an array of Json objects");
		this.jsonReader.beginArray();
	}

	@Nullable
	@Override
	public T read() throws Exception {
		try {
			if (this.jsonReader.hasNext()) {
				return this.mapper.fromJson(this.jsonReader, this.itemType);
			}
		} catch (IOException |JsonIOException | JsonSyntaxException e) {
			throw new ParseException("Unable to read next JSON object", e);
		}
		return null;
	}

	@Override
	public void close() throws Exception {
		this.inputStream.close();
		this.jsonReader.close();
	}

}
