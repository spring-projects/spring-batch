/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.batch.item.json.builder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.JsonObjectReader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A builder for {@link JsonItemReader}.
 *
 * @param <T> type of the target item
 *
 * @author Mahmoud Ben Hassine
 * @since 4.1
 */
public class JsonItemReaderBuilder<T> {

	protected Log logger = LogFactory.getLog(getClass());

	private JsonObjectReader<T> jsonObjectReader;

	private Resource resource;

	private String name;

	private boolean strict = true;

	private boolean saveState = true;

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	/**
	 * Set the {@link JsonObjectReader} to use to read and map Json objects to domain objects.
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
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}. Required if
	 * {@link #saveState(boolean)} is set to true.
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public JsonItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Setting this value to true indicates that it is an error if the input
	 * does not exist and an exception will be thrown. Defaults to true.
	 * @param strict indicates the input resource must exist
	 * @return The current instance of the builder.
	 * @see JsonItemReader#setStrict(boolean)
	 */
	public JsonItemReaderBuilder<T> strict(boolean strict) {
		this.strict = strict;

		return this;
	}

	/**
	 * Configure if the state of the {@link org.springframework.batch.item.ItemStreamSupport}
	 * should be persisted within the {@link org.springframework.batch.item.ExecutionContext}
	 * for restart purposes.
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
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public JsonItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;

		return this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 * @param currentItemCount current index
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
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
		if (this.saveState) {
			Assert.state(StringUtils.hasText(this.name), "A name is required when saveState is set to true.");
		}

		if (this.resource == null) {
			logger.debug("The resource is null. This is only a valid scenario when " +
					"injecting it later as in when using the MultiResourceItemReader");
		}

		JsonItemReader<T> reader = new JsonItemReader<>();
		reader.setResource(this.resource);
		reader.setJsonObjectReader(this.jsonObjectReader);
		reader.setName(this.name);
		reader.setStrict(this.strict);
		reader.setSaveState(this.saveState);
		reader.setMaxItemCount(this.maxItemCount);
		reader.setCurrentItemCount(this.currentItemCount);

		return reader;
	}
}
