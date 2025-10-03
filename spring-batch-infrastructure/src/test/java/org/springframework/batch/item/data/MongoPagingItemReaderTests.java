/*
 * Copyright 2013-2024 the original author or authors.
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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Michael Minella
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(MockitoExtension.class)
class MongoPagingItemReaderTests {

	private MongoPagingItemReader<String> reader;

	@Mock
	private MongoOperations template;

	private Map<String, Sort.Direction> sortOptions;

	@BeforeEach
	void setUp() throws Exception {
		reader = new MongoPagingItemReader<>(template, String.class);

		sortOptions = new HashMap<>();
		sortOptions.put("name", Sort.Direction.DESC);

		reader.setQuery("{ }");
		reader.setSort(sortOptions);
		reader.afterPropertiesSet();
		reader.setPageSize(50);
	}

	@Test
	void testAfterPropertiesSetForQueryObject() throws Exception {
		reader = new MongoPagingItemReader<>(template, String.class);
		;

		Query query1 = new Query().with(Sort.by(new Order(Sort.Direction.ASC, "_id")));
		reader.setQuery(query1);

		reader.afterPropertiesSet();
	}

	@Test
	void testBasicQueryFirstPage() {
		when(template.find(any(), any())).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		verify(template).find(assertArg(query -> {
			assertEquals(50, query.getLimit());
			assertEquals(0, query.getSkip());
			assertEquals("{}", query.getQueryObject().toJson());
			assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		}), eq(String.class));
	}

	@Test
	void testBasicQuerySecondPage() {
		reader.page = 2;

		when(template.find(any(), any())).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		verify(template).find(assertArg(query -> {
			assertEquals(50, query.getLimit());
			assertEquals(100, query.getSkip());
			assertEquals("{}", query.getQueryObject().toJson());
			assertEquals("{\"name\": -1}", query.getSortObject().toJson());
			assertTrue(query.getFieldsObject().isEmpty());
		}), eq(String.class));
	}

	@Test
	void testQueryWithFields() {
		reader.setFields("{name : 1, age : 1, _id: 0}");

		when(template.find(any(), any())).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		verify(template).find(assertArg(query -> {
			assertEquals(50, query.getLimit());
			assertEquals(0, query.getSkip());
			assertEquals("{}", query.getQueryObject().toJson());
			assertEquals("{\"name\": -1}", query.getSortObject().toJson());
			assertEquals(1, query.getFieldsObject().get("name"));
			assertEquals(1, query.getFieldsObject().get("age"));
			assertEquals(0, query.getFieldsObject().get("_id"));
		}), eq(String.class));
	}

	@Test
	void testQueryWithHint() {
		reader.setHint("{ $natural : 1}");

		when(template.find(any(), any())).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		verify(template).find(assertArg(query -> {
			assertEquals(50, query.getLimit());
			assertEquals(0, query.getSkip());
			assertEquals("{}", query.getQueryObject().toJson());
			assertEquals("{\"name\": -1}", query.getSortObject().toJson());
			assertEquals("{ $natural : 1}", query.getHint());
		}), eq(String.class));
	}

	@Test
	void testQueryWithParameters() {
		reader.setParameterValues(Collections.singletonList("foo"));

		reader.setQuery("{ name : ?0 }");

		when(template.find(any(), any())).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		verify(template).find(assertArg(query -> {
			assertEquals(50, query.getLimit());
			assertEquals(0, query.getSkip());
			assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
			assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		}), eq(String.class));
	}

	@Test
	void testQueryWithCollection() {
		reader.setParameterValues(Collections.singletonList("foo"));

		reader.setQuery("{ name : ?0 }");
		reader.setCollection("collection");

		when(template.find(any(), any(), anyString())).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		verify(template).find(assertArg(query -> {
			assertEquals(50, query.getLimit());
			assertEquals(0, query.getSkip());
			assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
			assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		}), eq(String.class), eq("collection"));
	}

	@Test
	void testQueryObject() throws Exception {
		reader = new MongoPagingItemReader<>(template, String.class);

		Query query = new Query().with(Sort.by(new Order(Sort.Direction.ASC, "_id")));
		reader.setQuery(query);

		reader.afterPropertiesSet();

		when(template.find(any(), any())).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		verify(template).find(assertArg(actualQuery -> {
			assertEquals(10, actualQuery.getLimit());
			assertEquals(0, actualQuery.getSkip());
		}), eq(String.class));
	}

	@Test
	void testQueryObjectWithIgnoredPageSize() throws Exception {
		reader = new MongoPagingItemReader<>(template, String.class);

		Query query = new Query().with(Sort.by(new Order(Sort.Direction.ASC, "_id"))).with(PageRequest.of(0, 50));
		reader.setQuery(query);

		reader.afterPropertiesSet();

		when(template.find(any(), any())).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		verify(template).find(assertArg(actualQuery -> {
			assertEquals(10, actualQuery.getLimit());
			assertEquals(0, actualQuery.getSkip());
		}), eq(String.class));
	}

	@Test
	void testQueryObjectWithPageSize() throws Exception {
		reader = new MongoPagingItemReader<>(template, String.class);

		Query query = new Query().with(Sort.by(new Order(Sort.Direction.ASC, "_id"))).with(PageRequest.of(30, 50));
		reader.setQuery(query);
		reader.setPageSize(100);

		reader.afterPropertiesSet();

		when(template.find(any(), any())).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		verify(template).find(assertArg(actualQuery -> {
			assertEquals(100, actualQuery.getLimit());
			assertEquals(0, actualQuery.getSkip());
		}), eq(String.class));
	}

	@Test
	void testQueryObjectWithoutLimit() throws Exception {
		reader = new MongoPagingItemReader<>(template, String.class);

		reader.setQuery(new Query());
		reader.setPageSize(100);

		reader.afterPropertiesSet();

		when(template.find(any(), any())).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		verify(template).find(assertArg(actualQuery -> assertEquals(100, actualQuery.getLimit())), eq(String.class));
	}

	@Test
	void testQueryObjectWithoutLimitAndPageSize() throws Exception {
		reader = new MongoPagingItemReader<>(template, String.class);

		reader.setQuery(new Query());

		reader.afterPropertiesSet();

		when(template.find(any(), any())).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		verify(template).find(assertArg(actualQuery -> assertEquals(10, actualQuery.getLimit())), eq(String.class));
	}

	@Test
	void testQueryObjectWithCollection() throws Exception {
		reader = new MongoPagingItemReader<>(template, String.class);

		Query query = new Query().with(Sort.by(new Order(Sort.Direction.ASC, "_id")));
		reader.setQuery(query);
		reader.setCollection("collection");

		reader.afterPropertiesSet();

		when(template.find(any(), any(), anyString())).thenReturn(new ArrayList<>());

		assertFalse(reader.doPageRead().hasNext());

		verify(template).find(assertArg(actualQuery -> {
			assertFalse(reader.doPageRead().hasNext());
			assertEquals(10, actualQuery.getLimit());
			assertEquals(0, actualQuery.getSkip());
		}), eq(String.class), eq("collection"));
	}

	@Test
	void testSortThrowsExceptionWhenInvokedWithNull() {
		// given
		reader = new MongoPagingItemReader<>(template, String.class);

		// when + then
		assertThatIllegalArgumentException().isThrownBy(() -> reader.setSort(null))
			.withMessage("Sorts must not be null");
	}

	@Test
	void testClose() throws Exception {
		// given
		when(template.find(any(), any())).thenReturn(List.of("string"));
		reader.read();

		// when
		reader.close();

		// then
		assertEquals(0, reader.page);
		assertNull(reader.results);
	}

}
