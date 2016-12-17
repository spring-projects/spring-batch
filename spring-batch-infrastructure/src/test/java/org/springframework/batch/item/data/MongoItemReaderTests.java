/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class MongoItemReaderTests {

	private MongoItemReader<String> reader;
	@Mock
	private MongoOperations template;
	private Map<String, Sort.Direction> sortOptions;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		reader = new MongoItemReader<String>();

		sortOptions = new HashMap<String, Sort.Direction>();
		sortOptions.put("name", Sort.Direction.DESC);

		reader.setTemplate(template);
		reader.setTargetType(String.class);
		reader.setQuery("{ }");
		reader.setSort(sortOptions);
		reader.afterPropertiesSet();
		reader.setPageSize(50);
	}

	@Test
	public void testAfterPropertiesSet() throws Exception{
		reader = new MongoItemReader<String>();

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
	public void testBasicQueryFirstPage() {
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<String>());

		assertFalse(reader.doPageRead().hasNext());

		Query query = queryContainer.getValue();
		assertEquals(50, query.getLimit());
		assertEquals(0, query.getSkip());
		assertEquals("{ }", query.getQueryObject().toJson());
		assertEquals("{ \"name\" : -1 }", query.getSortObject().toJson());
	}

	@Test
	public void testBasicQuerySecondPage() {
		reader.page = 2;
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<String>());

		assertFalse(reader.doPageRead().hasNext());

		Query query = queryContainer.getValue();

		assertEquals(50, query.getLimit());
		assertEquals(100, query.getSkip());
		assertEquals("{ }", query.getQueryObject().toJson());
		assertEquals("{ \"name\" : -1 }", query.getSortObject().toJson());
		assertNull(query.getFieldsObject());
	}

	@Test
	public void testQueryWithFields() {
		reader.setFields("{name : 1, age : 1, _id: 0}");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<String>());

		assertFalse(reader.doPageRead().hasNext());

		Query query = queryContainer.getValue();
		assertEquals(50, query.getLimit());
		assertEquals(0, query.getSkip());
		assertEquals("{ }", query.getQueryObject().toJson());
		assertEquals("{ \"name\" : -1 }", query.getSortObject().toJson());
		assertEquals(1, query.getFieldsObject().get("name"));
		assertEquals(1, query.getFieldsObject().get("age"));
		assertEquals(0, query.getFieldsObject().get("_id"));
	}

	@Test
	public void testQueryWithHint() {
		reader.setHint("{ $natural : 1}");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<String>());

		assertFalse(reader.doPageRead().hasNext());

		Query query = queryContainer.getValue();
		assertEquals(50, query.getLimit());
		assertEquals(0, query.getSkip());
		assertEquals("{ }", query.getQueryObject().toJson());
		assertEquals("{ \"name\" : -1 }", query.getSortObject().toJson());
		assertEquals("{ $natural : 1}", query.getHint());
	}

	@SuppressWarnings("serial")
	@Test
	public void testQueryWithParameters() {
		reader.setParameterValues(new ArrayList<Object>(){{
			add("foo");
		}});

		reader.setQuery("{ name : ?0 }");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.find(queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<String>());

		assertFalse(reader.doPageRead().hasNext());

		Query query = queryContainer.getValue();
		assertEquals(50, query.getLimit());
		assertEquals(0, query.getSkip());
		assertEquals("{ \"name\" : \"foo\" }", query.getQueryObject().toJson());
		assertEquals("{ \"name\" : -1 }", query.getSortObject().toJson());
	}

	@SuppressWarnings("serial")
	@Test
	public void testQueryWithCollection() {
		reader.setParameterValues(new ArrayList<Object>(){{
			add("foo");
		}});

		reader.setQuery("{ name : ?0 }");
		reader.setCollection("collection");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<String> collectionContainer = ArgumentCaptor.forClass(String.class);

		when(template.find(queryContainer.capture(), eq(String.class), collectionContainer.capture())).thenReturn(new ArrayList<String>());

		assertFalse(reader.doPageRead().hasNext());

		Query query = queryContainer.getValue();
		assertEquals(50, query.getLimit());
		assertEquals(0, query.getSkip());
		assertEquals("{ \"name\" : \"foo\" }", query.getQueryObject().toJson());
		assertEquals("{ \"name\" : -1 }", query.getSortObject().toJson());
		assertEquals("collection", collectionContainer.getValue());
	}
}
