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
package org.springframework.batch.infrastructure.item.data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.bson.Document;
import org.bson.codecs.DecoderContext;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.util.json.ParameterBindingDocumentCodec;
import org.springframework.data.mongodb.util.json.ParameterBindingJsonReader;
import org.springframework.data.util.CloseableIterator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Cursor-based {@link ItemReader} implementation for MongoDB.
 *
 * @author LEE Juchan
 * @author Mahmoud Ben Hassine
 * @author Jimmy Praet
 * @since 5.1
 */
public class MongoCursorItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> implements InitializingBean {

	private MongoOperations template;

	private Class<? extends T> targetType;

	private @Nullable String collection;

	private @Nullable Query query;

	private @Nullable String queryString;

	private List<Object> parameterValues = new ArrayList<>();

	private @Nullable String fields;

	private @Nullable Sort sort;

	private @Nullable String hint;

	private int batchSize;

	private int limit;

	private @Nullable Duration maxTime;

	private @Nullable CloseableIterator<? extends T> cursor;

	/**
	 * Create a new {@link MongoCursorItemReader}.
	 * @param template the {@link MongoOperations} to use
	 * @param targetType the target type
	 * @since 6.0
	 */
	public MongoCursorItemReader(MongoOperations template, Class<? extends T> targetType) {
		Assert.notNull(template, "MongoOperations must not be null");
		Assert.notNull(targetType, "Target type must not be null");
		this.template = template;
		this.targetType = targetType;
	}

	/**
	 * Used to perform operations against the MongoDB instance. Also handles the mapping
	 * of documents to objects.
	 * @param template the MongoOperations instance to use
	 * @see MongoOperations
	 */
	public void setTemplate(MongoOperations template) {
		this.template = template;
	}

	/**
	 * The targetType of object to be returned for each {@link #read()} call.
	 * @param targetType the targetType of object to return
	 */
	public void setTargetType(Class<? extends T> targetType) {
		this.targetType = targetType;
	}

	/**
	 * @param collection Mongo collection to be queried.
	 */
	public void setCollection(String collection) {
		this.collection = collection;
	}

	/**
	 * A Mongo Query to be used.
	 * @param query Mongo Query to be used.
	 */
	public void setQuery(Query query) {
		this.query = query;
	}

	/**
	 * A JSON formatted MongoDB query. Parameterization of the provided query is allowed
	 * via ?&lt;index&gt; placeholders where the &lt;index&gt; indicates the index of the
	 * parameterValue to substitute.
	 * @param queryString JSON formatted Mongo query
	 */
	public void setQuery(String queryString) {
		this.queryString = queryString;
	}

	/**
	 * {@link List} of values to be substituted in for each of the parameters in the
	 * query.
	 * @param parameterValues values
	 */
	public void setParameterValues(List<Object> parameterValues) {
		Assert.notNull(parameterValues, "Parameter values must not be null");
		this.parameterValues = parameterValues;
	}

	/**
	 * JSON defining the fields to be returned from the matching documents by MongoDB.
	 * @param fields JSON string that identifies the fields to sort by.
	 */
	public void setFields(String fields) {
		this.fields = fields;
	}

	/**
	 * {@link Map} of property
	 * names/{@link org.springframework.data.domain.Sort.Direction} values to sort the
	 * input by.
	 * @param sorts map of properties and direction to sort each.
	 */
	public void setSort(Map<String, Sort.Direction> sorts) {
		Assert.notNull(sorts, "Sorts must not be null");
		this.sort = convertToSort(sorts);
	}

	/**
	 * JSON String telling MongoDB what index to use.
	 * @param hint string indicating what index to use.
	 */
	public void setHint(String hint) {
		this.hint = hint;
	}

	/**
	 * The size of batches to use when iterating over results.
	 * @param batchSize size the batch size to apply to the cursor
	 * @see Query#cursorBatchSize(int)
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * The query limit.
	 * @param limit The limit
	 * @see Query#limit(int)
	 */
	public void setLimit(int limit) {
		this.limit = limit;
	}

	/**
	 * The maximum execution time for the query
	 * @param maxTime The max time
	 * @see Query#maxTime(Duration)
	 */
	public void setMaxTime(Duration maxTime) {
		Assert.notNull(maxTime, "maxTime must not be null.");
		this.maxTime = maxTime;
	}

	/**
	 * Checks mandatory properties
	 *
	 * @see InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {
		Assert.state(queryString != null || query != null, "A query is required.");

		if (queryString != null) {
			Assert.state(sort != null, "A sort is required.");
		}
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	protected void doOpen() throws Exception {
		Query mongoQuery = queryString != null ? createQuery() : query;

		Stream<? extends T> stream;
		if (StringUtils.hasText(collection)) {
			stream = template.stream(mongoQuery, targetType, collection);
		}
		else {
			stream = template.stream(mongoQuery, targetType);
		}

		this.cursor = streamToIterator(stream);
	}

	private Query createQuery() {
		@SuppressWarnings("DataFlowIssue")
		String populatedQuery = replacePlaceholders(queryString, parameterValues);

		Query mongoQuery;
		if (StringUtils.hasText(fields)) {
			mongoQuery = new BasicQuery(populatedQuery, fields);
		}
		else {
			mongoQuery = new BasicQuery(populatedQuery);
		}

		if (sort != null) {
			mongoQuery.with(sort);
		}
		if (StringUtils.hasText(hint)) {
			mongoQuery.withHint(hint);
		}
		mongoQuery.cursorBatchSize(batchSize);
		mongoQuery.limit(limit);
		if (maxTime != null) {
			mongoQuery.maxTime(maxTime);
		}
		else {
			mongoQuery.noCursorTimeout();
		}

		return mongoQuery;
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	protected T doRead() throws Exception {
		return cursor.hasNext() ? cursor.next() : null;
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	protected void doClose() throws Exception {
		this.cursor.close();
	}

	private Sort convertToSort(Map<String, Sort.Direction> sorts) {
		List<Sort.Order> sortValues = new ArrayList<>(sorts.size());

		for (Map.Entry<String, Sort.Direction> curSort : sorts.entrySet()) {
			sortValues.add(new Sort.Order(curSort.getValue(), curSort.getKey()));
		}

		return Sort.by(sortValues);
	}

	private String replacePlaceholders(String input, List<Object> values) {
		ParameterBindingJsonReader reader = new ParameterBindingJsonReader(input, values.toArray());
		DecoderContext decoderContext = DecoderContext.builder().build();
		Document document = new ParameterBindingDocumentCodec().decode(reader, decoderContext);
		return document.toJson();
	}

	private CloseableIterator<? extends T> streamToIterator(Stream<? extends T> stream) {
		return new CloseableIterator<>() {
			final private Iterator<? extends T> delegate = stream.iterator();

			@Override
			public boolean hasNext() {
				return delegate.hasNext();
			}

			@Override
			public T next() {
				return delegate.next();
			}

			@Override
			public void close() {
				stream.close();
			}
		};
	}

}
