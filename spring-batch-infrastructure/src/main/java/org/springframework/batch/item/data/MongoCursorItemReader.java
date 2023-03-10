/*
 * Copyright 2002-2023 the original author or authors.
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
package org.springframework.batch.item.data;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.bson.Document;
import org.bson.codecs.DecoderContext;

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.util.json.ParameterBindingDocumentCodec;
import org.springframework.data.mongodb.util.json.ParameterBindingJsonReader;
import org.springframework.data.util.CloseableIterator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author LEE Juchan
 * @since 5.0
 */
public class MongoCursorItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> implements InitializingBean {

    private MongoOperations template;

    private Class<? extends T> targetType;

    private String collection;

    private Query query;

    private String queryString;

    private List<Object> parameterValues = new ArrayList<>();

    private String fields;

    private Sort sort;

    private String hint;

    private Integer batchSize;

    private Integer limit;

    private Integer maxTimeMs;

    private CloseableIterator<? extends T> cursor;

    public MongoCursorItemReader() {
        super();
        setName(ClassUtils.getShortName(MongoCursorItemReader.class));
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
     */
    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * The query limit
     * @param limit The limit
     */
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    /**
     * The maximum execution time for the aggregation command
     * @param maxTimeMs The max time
     */
    public void setMaxTimeMs(Integer maxTimeMs) {
        this.maxTimeMs = maxTimeMs;
    }

    /**
     * Checks mandatory properties
     *
     * @see InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() {
        Assert.state(template != null, "An implementation of MongoOperations is required.");
        Assert.state(targetType != null, "A targetType to convert the input into is required.");
        Assert.state(queryString != null || query != null, "A query is required.");

        if (queryString != null) {
            Assert.state(sort != null, "A sort is required.");
        }
    }

    @Override
    protected void doOpen() throws Exception {
        Query mongoQuery;
        if (queryString != null) {
            mongoQuery = createQuery();
        } else {
            mongoQuery = query;
        }

        Stream<? extends T> stream;
        if (StringUtils.hasText(collection)) {
            stream = template.stream(mongoQuery, targetType, collection);
        } else {
            stream = template.stream(mongoQuery, targetType);
        }

        this.cursor = streamToIterator(stream);
    }

    @Override
    protected T doRead() throws Exception {
        return cursor.hasNext() ? cursor.next() : null;
    }

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

    private Query createQuery() {
        String populatedQuery = replacePlaceholders(queryString, parameterValues);

        Query mongoQuery;
        if (StringUtils.hasText(fields)) {
            mongoQuery = new BasicQuery(populatedQuery, fields);
        } else {
            mongoQuery = new BasicQuery(populatedQuery);
        }

        if (sort != null) {
            mongoQuery.with(sort);
        }
        if (StringUtils.hasText(hint)) {
            mongoQuery.withHint(hint);
        }
        if (batchSize != null) {
            mongoQuery.cursorBatchSize(batchSize);
        }
        if (limit != null) {
            mongoQuery.limit(limit);
        }
        if (maxTimeMs != null) {
            mongoQuery.maxTime(Duration.of(maxTimeMs, ChronoUnit.MILLIS));
        } else {
            mongoQuery.noCursorTimeout();
        }

        return mongoQuery;
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
