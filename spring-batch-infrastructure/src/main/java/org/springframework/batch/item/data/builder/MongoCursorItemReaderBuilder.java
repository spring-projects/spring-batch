/*
 * Copyright 2023-2025 the original author or authors.
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

package org.springframework.batch.item.data.builder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.item.data.MongoCursorItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author LEE Juchan
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @since 5.1
 * @see MongoCursorItemReader
 */
public class MongoCursorItemReaderBuilder<T> {

	private boolean saveState = true;

	private @Nullable String name;

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	private @Nullable MongoOperations template;

	private @Nullable Class<? extends T> targetType;

	private @Nullable String collection;

	private @Nullable Query query;

	private @Nullable String jsonQuery;

	private List<Object> parameterValues = new ArrayList<>();

	private @Nullable String fields;

	private @Nullable Map<String, Sort.Direction> sorts;

	private @Nullable String hint;

	private int batchSize;

	private int limit;

	private @Nullable Duration maxTime;

	/**
	 * Configure if the state of the
	 * {@link org.springframework.batch.item.ItemStreamSupport} should be persisted within
	 * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public MongoCursorItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

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
	public MongoCursorItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Configure the max number of items to be read.
	 * @param maxItemCount the max items to be read
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public MongoCursorItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;

		return this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 * @param currentItemCount current index
	 * @return this instance for method chaining
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public MongoCursorItemReaderBuilder<T> currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;

		return this;
	}

	/**
	 * Used to perform operations against the MongoDB instance. Also handles the mapping
	 * of documents to objects.
	 * @param template the MongoOperations instance to use
	 * @see MongoOperations
	 * @return The current instance of the builder
	 * @see MongoCursorItemReader#setTemplate(MongoOperations)
	 */
	public MongoCursorItemReaderBuilder<T> template(MongoOperations template) {
		this.template = template;

		return this;
	}

	/**
	 * The targetType of object to be returned for each
	 * {@link MongoCursorItemReader#read()} call.
	 * @param targetType the targetType of object to return
	 * @return The current instance of the builder
	 * @see MongoCursorItemReader#setTargetType(Class)
	 */
	public MongoCursorItemReaderBuilder<T> targetType(Class<? extends T> targetType) {
		this.targetType = targetType;

		return this;
	}

	/**
	 * Establish an optional collection that can be queried.
	 * @param collection Mongo collection to be queried.
	 * @return The current instance of the builder
	 * @see MongoCursorItemReader#setCollection(String)
	 */
	public MongoCursorItemReaderBuilder<T> collection(String collection) {
		this.collection = collection;

		return this;
	}

	/**
	 * Provide a Spring Data Mongo {@link Query}. This will take precedence over a JSON
	 * configured query.
	 * @param query Query to execute
	 * @return this instance for method chaining
	 * @see MongoCursorItemReader#setQuery(Query)
	 */
	public MongoCursorItemReaderBuilder<T> query(Query query) {
		this.query = query;

		return this;
	}

	/**
	 * A JSON formatted MongoDB jsonQuery. Parameterization of the provided jsonQuery is
	 * allowed via ?&lt;index&gt; placeholders where the &lt;index&gt; indicates the index
	 * of the parameterValue to substitute.
	 * @param query JSON formatted Mongo jsonQuery
	 * @return The current instance of the builder
	 * @see MongoCursorItemReader#setQuery(String)
	 */
	public MongoCursorItemReaderBuilder<T> jsonQuery(String query) {
		this.jsonQuery = query;

		return this;
	}

	/**
	 * Values to be substituted in for each of the parameters in the query.
	 * @param parameterValues values
	 * @return The current instance of the builder
	 * @see MongoCursorItemReader#setParameterValues(List)
	 */
	public MongoCursorItemReaderBuilder<T> parameterValues(List<Object> parameterValues) {
		this.parameterValues = parameterValues;

		return this;
	}

	/**
	 * JSON defining the fields to be returned from the matching documents by MongoDB.
	 * @param fields JSON string that identifies the fields to sort by.
	 * @return The current instance of the builder
	 * @see MongoCursorItemReader#setFields(String)
	 */
	public MongoCursorItemReaderBuilder<T> fields(String fields) {
		this.fields = fields;

		return this;
	}

	/**
	 * {@link Map} of property
	 * names/{@link org.springframework.data.domain.Sort.Direction} values to sort the
	 * input by.
	 * @param sorts map of properties and direction to sort each.
	 * @return The current instance of the builder
	 * @see MongoCursorItemReader#setSort(Map)
	 */
	public MongoCursorItemReaderBuilder<T> sorts(Map<String, Sort.Direction> sorts) {
		this.sorts = sorts;

		return this;
	}

	/**
	 * JSON String telling MongoDB what index to use.
	 * @param hint string indicating what index to use.
	 * @return The current instance of the builder
	 * @see MongoCursorItemReader#setHint(String)
	 */
	public MongoCursorItemReaderBuilder<T> hint(String hint) {
		this.hint = hint;

		return this;
	}

	/**
	 * The size of batches to use when iterating over results.
	 * @param batchSize string indicating what index to use.
	 * @return The current instance of the builder
	 * @see MongoCursorItemReader#setHint(String)
	 */
	public MongoCursorItemReaderBuilder<T> batchSize(int batchSize) {
		this.batchSize = batchSize;

		return this;
	}

	/**
	 * The query limit
	 * @param limit The limit
	 * @return The current instance of the builder
	 * @see MongoCursorItemReader#setLimit(int)
	 */
	public MongoCursorItemReaderBuilder<T> limit(int limit) {
		this.limit = limit;

		return this;
	}

	/**
	 * The maximum execution time for the query
	 * @param maxTime The max time
	 * @return The current instance of the builder
	 * @see MongoCursorItemReader#setMaxTime(Duration)
	 */
	public MongoCursorItemReaderBuilder<T> maxTime(Duration maxTime) {
		Assert.notNull(maxTime, "maxTime must not be null.");
		this.maxTime = maxTime;

		return this;
	}

	public MongoCursorItemReader<T> build() {
		Assert.notNull(this.template, "template is required.");
		if (this.saveState) {
			Assert.hasText(this.name, "A name is required when saveState is set to true");
		}
		Assert.notNull(this.targetType, "targetType is required.");
		Assert.state(StringUtils.hasText(this.jsonQuery) || this.query != null, "A query is required");
		Assert.notNull(this.sorts, "sorts map is required.");

		MongoCursorItemReader<T> reader = new MongoCursorItemReader<>(this.template, this.targetType);
		reader.setSaveState(this.saveState);
		if (this.name != null) {
			reader.setName(this.name);
		}
		reader.setCurrentItemCount(this.currentItemCount);
		reader.setMaxItemCount(this.maxItemCount);

		reader.setTemplate(this.template);
		reader.setTargetType(this.targetType);
		if (this.collection != null) {
			reader.setCollection(this.collection);
		}
		if (this.query != null) {
			reader.setQuery(this.query);
		}
		if (StringUtils.hasText(this.jsonQuery)) {
			reader.setQuery(this.jsonQuery);
		}
		reader.setParameterValues(this.parameterValues);
		if (this.fields != null) {
			reader.setFields(this.fields);
		}
		reader.setSort(this.sorts);
		if (this.hint != null) {
			reader.setHint(this.hint);
		}
		reader.setBatchSize(this.batchSize);
		reader.setLimit(this.limit);
		if (this.maxTime != null) {
			reader.setMaxTime(this.maxTime);
		}

		return reader;
	}

}
