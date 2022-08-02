/*
 * Copyright 2013-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Michael Minella
 * @author Parikshit Dutta
 */
@ExtendWith(MockitoExtension.class)
class MongoItemReaderTests {

	private MongoItemReader<String> reader;

	@Mock
	private MongoOperations template;

	private Map<String, Sort.Direction> sortOptions;

	@BeforeEach
	void setUp() throws Exception {
		reader = new MongoItemReader<>();

		sortOptions = new HashMap<>();
		sortOptions.put("name", Sort.Direction.DESC);

		reader.setTemplate(template);
		reader.setTargetType(String.class);
		reader.setQuery("{ }");
		reader.setSort(sortOptions);
		reader.afterPropertiesSet();
		reader.setPageSize(50);
	}

	@Test
	void testAfterPropertiesSetForQueryString() throws Exception {
		reader = new MongoItemReader<>();
		Exception exception = assertThrows(IllegalStateException.class, reader::afterPropertiesSet);
		assertEquals("An implementation of MongoOperations is required.", exception.getMessage());

		reader.setTemplate(template);

		exception = assertThrows(IllegalStateException.class, reader::afterPropertiesSet);
		assertEquals("A type to convert the input into is required.", exception.getMessage());

		reader.setTargetType(String.class);

		exception = assertThrows(IllegalStateException.class, reader::afterPropertiesSet);
		assertEquals("A query is required.", exception.getMessage());

		reader.setQuery("");

		exception = assertThrows(IllegalStateException.class, reader::afterPropertiesSet);
		assertEquals("A sort is required.", exception.getMessage());

		reader.setSort(sortOptions);
		reader.afterPropertiesSet();
	}

	@Test
	void testAfterPropertiesSetForQueryObject() throws Exception {
		reader = new MongoItemReader<>();

		reader.setTemplate(template);
		reader.setTargetType(String.class);

		Query query1 = new Query().with(Sort.by(new Order(Sort.Direction.ASC, "_id")));
		reader.setQuery(query1);

		reader.afterPropertiesSet();
	}

	@Test
	void testBasicQueryFirstPage() {
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		Query query = queryContainer.getValue();
		assertEquals(50, query.getLimit());
		assertEquals(0, query.getSkip());
		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
	}

	@Test
	void testBasicQuerySecondPage() {
		reader.page = 2;
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		Query query = queryContainer.getValue();

		assertEquals(50, query.getLimit());
		assertEquals(100, query.getSkip());
		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertTrue(query.getFieldsObject().isEmpty());
	}

	@Test
	void testQueryWithFields() {
		reader.setFields("{name : 1, age : 1, _id: 0}");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		Query query = queryContainer.getValue();
		assertEquals(50, query.getLimit());
		assertEquals(0, query.getSkip());
		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals(1, query.getFieldsObject().get("name"));
		assertEquals(1, query.getFieldsObject().get("age"));
		assertEquals(0, query.getFieldsObject().get("_id"));
	}

	@Test
	void testQueryWithHint() {
		reader.setHint("{ $natural : 1}");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		Query query = queryContainer.getValue();
		assertEquals(50, query.getLimit());
		assertEquals(0, query.getSkip());
		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals("{ $natural : 1}", query.getHint());
	}

	@Test
	void testQueryWithParameters() {
		reader.setParameterValues(Collections.singletonList("foo"));

		reader.setQuery("{ name : ?0 }");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		Query query = queryContainer.getValue();
		assertEquals(50, query.getLimit());
		assertEquals(0, query.getSkip());
		assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
	}

	@Test
	void testQueryWithCollection() {
		reader.setParameterValues(Collections.singletonList("foo"));

		reader.setQuery("{ name : ?0 }");
		reader.setCollection("collection");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<String> collectionContainer = ArgumentCaptor.forClass(String.class);

		when(template.find(queryContainer.capture(), eq(String.class), collectionContainer.capture()))
				.thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		Query query = queryContainer.getValue();
		assertEquals(50, query.getLimit());
		assertEquals(0, query.getSkip());
		assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals("collection", collectionContainer.getValue());
	}

	@Test
	void testQueryObject() throws Exception {
		reader = new MongoItemReader<>();
		reader.setTemplate(template);

		Query query = new Query().with(Sort.by(new Order(Sort.Direction.ASC, "_id")));
		reader.setQuery(query);
		reader.setTargetType(String.class);

		reader.afterPropertiesSet();

		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		Query actualQuery = queryContainer.getValue();
		assertFalse(reader.doPageRead().hasNext());
		assertEquals(10, actualQuery.getLimit());
		assertEquals(0, actualQuery.getSkip());
	}

	@Test
	void testQueryObjectWithIgnoredPageSize() throws Exception {
		reader = new MongoItemReader<>();
		reader.setTemplate(template);

		Query query = new Query().with(Sort.by(new Order(Sort.Direction.ASC, "_id"))).with(PageRequest.of(0, 50));
		reader.setQuery(query);
		reader.setTargetType(String.class);

		reader.afterPropertiesSet();

		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		Query actualQuery = queryContainer.getValue();
		assertFalse(reader.doPageRead().hasNext());
		assertEquals(10, actualQuery.getLimit());
		assertEquals(0, actualQuery.getSkip());
	}

	@Test
	void testQueryObjectWithPageSize() throws Exception {
		reader = new MongoItemReader<>();
		reader.setTemplate(template);

		Query query = new Query().with(Sort.by(new Order(Sort.Direction.ASC, "_id"))).with(PageRequest.of(30, 50));
		reader.setQuery(query);
		reader.setTargetType(String.class);
		reader.setPageSize(100);

		reader.afterPropertiesSet();

		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		Query actualQuery = queryContainer.getValue();
		assertFalse(reader.doPageRead().hasNext());
		assertEquals(100, actualQuery.getLimit());
		assertEquals(0, actualQuery.getSkip());
	}

	@Test
	void testQueryObjectWithoutLimit() throws Exception {
		reader = new MongoItemReader<>();
		reader.setTemplate(template);

		reader.setQuery(new Query());
		reader.setTargetType(String.class);
		reader.setPageSize(100);

		reader.afterPropertiesSet();

		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		Query actualQuery = queryContainer.getValue();
		assertEquals(100, actualQuery.getLimit());
	}

	@Test
	void testQueryObjectWithoutLimitAndPageSize() throws Exception {
		reader = new MongoItemReader<>();
		reader.setTemplate(template);

		reader.setQuery(new Query());
		reader.setTargetType(String.class);

		reader.afterPropertiesSet();

		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		Query actualQuery = queryContainer.getValue();
		assertEquals(10, actualQuery.getLimit());
	}

	@Test
	void testQueryObjectWithCollection() throws Exception {
		reader = new MongoItemReader<>();
		reader.setTemplate(template);

		Query query = new Query().with(Sort.by(new Order(Sort.Direction.ASC, "_id")));
		reader.setQuery(query);
		reader.setTargetType(String.class);
		reader.setCollection("collection");

		reader.afterPropertiesSet();

		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<String> stringContainer = ArgumentCaptor.forClass(String.class);
		when(template.find(queryContainer.capture(), eq(String.class), stringContainer.capture()))
				.thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		Query actualQuery = queryContainer.getValue();
		assertFalse(reader.doPageRead().hasNext());
		assertEquals(10, actualQuery.getLimit());
		assertEquals(0, actualQuery.getSkip());
		assertEquals("collection", stringContainer.getValue());
	}

	@Test
	void testSortThrowsExceptionWhenInvokedWithNull() {
		// given
		reader = new MongoItemReader<>();

		// when + then
		assertThatIllegalArgumentException().isThrownBy(() -> reader.setSort(null))
				.withMessage("Sorts must not be null");
	}

}
