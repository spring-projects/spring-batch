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

package org.springframework.batch.infrastructure.item.json.builder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.batch.infrastructure.item.json.JsonItemReader;
import org.springframework.batch.infrastructure.item.json.JsonObjectReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A builder for {@link JsonItemReader}.
 *
 * @param <T> type of the target item
 * @author Mahmoud Ben Hassine
 * @author Andrey Litvitski
 * @since 4.1
 */
public class JsonItemReaderBuilder<T> {

	protected Log logger = LogFactory.getLog(getClass());

	private @Nullable JsonObjectReader<T> jsonObjectReader;

	private @Nullable Resource resource;

	private @Nullable String name;

	private boolean strict = true;

	private boolean saveState = true;

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	/**
	 * Set the {@link JsonObjectReader} to use to read and map Json objects to domain
	 * objects.
	 * @param jsonObjectReader to use
	 * @return The current instance of the builder.
	 * @see JsonItemReader#setJsonObjectReader(JsonObjectReader)
	 */
	public JsonItemReaderBuilder<T> jsonObjectReader(JsonObjectReader<T> jsonObjectReader) {
		this.jsonObjectReader = jsonObjectReader;

		return this;
	}

	/**
	 * The {@link Resource} to be used as input.
	 * @param resource the input to the reader.
	 * @return The current instance of the builder.
	 * @see JsonItemReader#setResource(Resource)
	 */
	public JsonItemReaderBuilder<T> resource(Resource resource) {
		this.resource = resource;

		return this;
	}

	/**
	 * The name used to calculate the key within the {@link ExecutionContext}. Required if
	 * {@link #saveState(boolean)} is set to true.
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see ItemStreamSupport#setName(String)
	 */
	public JsonItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Setting this value to true indicates that it is an error if the input does not
	 * exist and an exception will be thrown. Defaults to true.
	 * @param strict indicates the input resource must exist
	 * @return The current instance of the builder.
	 * @see JsonItemReader#setStrict(boolean)
	 */
	public JsonItemReaderBuilder<T> strict(boolean strict) {
		this.strict = strict;

		return this;
	}

	/**
	 * Configure if the state of the {@link ItemStreamSupport} should be persisted within
	 * the {@link ExecutionContext} for restart purposes.
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public JsonItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * Configure the max number of items to be read.
	 * @param maxItemCount the max items to be read
	 * @return The current instance of the builder.
	 * @see AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public JsonItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;

		return this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 * @param currentItemCount current index
	 * @return The current instance of the builder.
	 * @see AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public JsonItemReaderBuilder<T> currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;

		return this;
	}

	/**
	 * Validate the configuration and build a new {@link JsonItemReader}.
	 * @return a new instance of the {@link JsonItemReader}
	 */
	public JsonItemReader<T> build() {
		Assert.notNull(this.jsonObjectReader, "A json object reader is required.");

		if (this.resource == null) {
			logger.debug("The resource is null. This is only a valid scenario when "
					+ "injecting it later as in when using the MultiResourceItemReader");
			// TODO check if this is feasible
			this.resource = new ByteArrayResource(new byte[0]);
		}
		JsonItemReader<T> reader = new JsonItemReader<>(this.resource, this.jsonObjectReader);
		reader.setJsonObjectReader(this.jsonObjectReader);
		if (this.name != null) {
			reader.setName(this.name);
		}
		reader.setStrict(this.strict);
		reader.setSaveState(this.saveState);
		reader.setMaxItemCount(this.maxItemCount);
		reader.setCurrentItemCount(this.currentItemCount);

		return reader;
	}

}
