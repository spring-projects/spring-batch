/*
 * Copyright 2013-2020 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Michael Minella
 * @author Parikshit Dutta
 */
public class MongoItemReaderTests {

	private MongoItemReader<String> reader;

	@Mock
	private MongoOperations template;

	@Mock
	private CloseableIterator<String> emptyIterator;

	private Map<String, Sort.Direction> sortOptions;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
		when(emptyIterator.hasNext()).thenReturn(false);
		reader = new MongoItemReader<>();

		sortOptions = new HashMap<>();
		sortOptions.put("name", Sort.Direction.DESC);

		reader.setTemplate(template);
		reader.setTargetType(String.class);
		reader.setQuery("{ }");
		reader.setSort(sortOptions);
		reader.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSetForQueryString() throws Exception{
		reader = new MongoItemReader<>();

		try {
			reader.afterPropertiesSet();
			fail("Template was not set but exception was not thrown.");
		} catch (IllegalStateException iae) {
			assertEquals("An implementation of MongoOperations is required.", iae.getMessage());
		} catch (Throwable t) {
			fail("Wrong exception was thrown.");
		}

		reader.setTemplate(template);

		try {
			reader.afterPropertiesSet();
			fail("type was not set but exception was not thrown.");
		} catch (IllegalStateException iae) {
			assertEquals("A type to convert the input into is required.", iae.getMessage());
		} catch (Throwable t) {
			fail("Wrong exception was thrown.");
		}

		reader.setTargetType(String.class);

		try {
			reader.afterPropertiesSet();
			fail("Query was not set but exception was not thrown.");
		} catch (IllegalStateException iae) {
			assertEquals("A query is required.", iae.getMessage());
		} catch (Throwable t) {
			fail("Wrong exception was thrown.");
		}

		reader.setQuery("");

		try {
			reader.afterPropertiesSet();
			fail("Sort was not set but exception was not thrown.");
		} catch (IllegalStateException iae) {
			assertEquals("A sort is required.", iae.getMessage());
		} catch (Throwable t) {
			fail("Wrong exception was thrown.");
		}

		reader.setSort(sortOptions);

		reader.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSetForQueryObject() throws Exception{
		reader = new MongoItemReader<>();

		reader.setTemplate(template);
		reader.setTargetType(String.class);

		Query query1 = new Query().with(Sort.by(new Order(Sort.Direction.ASC, "_id")));
		reader.setQuery(query1);

		reader.afterPropertiesSet();
	}

	@Test
	public void testBasicQuery() throws Exception {
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(emptyIterator);

		reader.doOpen();

		assertNull(reader.doRead());

		Query query = queryContainer.getValue();
		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
	}

	@Test
	public void testQueryWithFields() throws Exception {
		reader.setFields("{name : 1, age : 1, _id: 0}");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(emptyIterator);

		reader.doOpen();

		assertNull(reader.doRead());

		Query query = queryContainer.getValue();

		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals(1, query.getFieldsObject().get("name"));
		assertEquals(1, query.getFieldsObject().get("age"));
		assertEquals(0, query.getFieldsObject().get("_id"));
	}

	@Test
	public void testQueryWithHint() throws Exception {
		reader.setHint("{ $natural : 1}");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(emptyIterator);

		reader.doOpen();

		assertNull(reader.doRead());

		Query query = queryContainer.getValue();

		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals("{ $natural : 1}", query.getHint());
	}

	@SuppressWarnings("serial")
	@Test
	public void testQueryWithParameters() throws Exception {
		reader.setParameterValues(new ArrayList<Object>(){{
			add("foo");
		}});

		reader.setQuery("{ name : ?0 }");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(emptyIterator);

		reader.doOpen();

		assertNull(reader.doRead());

		Query query = queryContainer.getValue();

		assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
	}

	@SuppressWarnings("serial")
	@Test
	public void testQueryWithCollection() throws Exception {
		reader.setParameterValues(new ArrayList<Object>(){{
			add("foo");
		}});

		reader.setQuery("{ name : ?0 }");
		reader.setCollection("collection");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<String> collectionContainer = ArgumentCaptor.forClass(String.class);

		when(template.stream(queryContainer.capture(), eq(String.class), collectionContainer.capture()))
				.thenReturn(emptyIterator);

		reader.doOpen();

		assertNull(reader.doRead());

		Query query = queryContainer.getValue();

		assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals("collection", collectionContainer.getValue());
	}

	@Test
	public void testQueryObject() throws Exception {
		reader = new MongoItemReader<>();
		reader.setTemplate(template);

		Query query = new Query()
				.with(Sort.by(new Order(Sort.Direction.ASC, "_id")));
		reader.setQuery(query);
		reader.setTargetType(String.class);

		reader.afterPropertiesSet();

		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(emptyIterator);

		reader.doOpen();

		assertNull(reader.doRead());

		Query actualQuery = queryContainer.getValue();

		assertEquals("{}",actualQuery.getQueryObject().toJson());

	}



	@Test
	public void testQueryObjectWithCollection() throws Exception {
		reader = new MongoItemReader<>();
		reader.setTemplate(template);

		Query query = new Query()
				.with(Sort.by(new Order(Sort.Direction.ASC, "_id")));
		reader.setQuery(query);
		reader.setTargetType(String.class);
		reader.setCollection("collection");

		reader.afterPropertiesSet();

		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<String> stringContainer = ArgumentCaptor.forClass(String.class);
		when(template.stream(queryContainer.capture(), eq(String.class), stringContainer.capture()))
				.thenReturn(emptyIterator);

		reader.doOpen();

		assertNull(reader.doRead());

		Query actualQuery = queryContainer.getValue();
		assertEquals("{}",actualQuery.getQueryObject().toJson());
		assertEquals("collection", stringContainer.getValue());
	}
}
