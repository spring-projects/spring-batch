/*
 * Copyright 2023 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link MongoCursorItemReader}.
 *
 * @author LEE Juchan
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(MockitoExtension.class)
class MongoCursorItemReaderTest {

	private MongoCursorItemReader<String> reader;

	@Mock
	private MongoTemplate template;

	private Map<String, Sort.Direction> sortOptions;

	@BeforeEach
	void setUp() {
		reader = new MongoCursorItemReader<>();

		sortOptions = new HashMap<>();
		sortOptions.put("name", Sort.Direction.DESC);

		reader.setTemplate(template);
		reader.setTargetType(String.class);
		reader.setQuery("{ }");
		reader.setSort(sortOptions);
		reader.afterPropertiesSet();
	}

	@Test
	void testAfterPropertiesSetForQueryString() {
		reader = new MongoCursorItemReader<>();
		Exception exception = assertThrows(IllegalStateException.class, reader::afterPropertiesSet);
		assertEquals("An implementation of MongoOperations is required.", exception.getMessage());

		reader.setTemplate(template);

		exception = assertThrows(IllegalStateException.class, reader::afterPropertiesSet);
		assertEquals("A targetType to convert the input into is required.", exception.getMessage());

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
	void testAfterPropertiesSetForQueryObject() {
		reader = new MongoCursorItemReader<>();

		reader.setTemplate(template);
		reader.setTargetType(String.class);

		Query query = new Query().with(Sort.by(new Sort.Order(Sort.Direction.ASC, "_id")));
		reader.setQuery(query);

		reader.afterPropertiesSet();
	}

	@Test
	void testBasicQuery() throws Exception {
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(Stream.of("hello world"));

		reader.doOpen();
		assertEquals(reader.doRead(), "hello world");

		Query query = queryContainer.getValue();
		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
	}

	@Test
	void testQueryWithFields() throws Exception {
		reader.setFields("{name : 1, age : 1, _id: 0}");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(Stream.of());

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
	void testQueryWithHint() throws Exception {
		reader.setHint("{ $natural : 1}");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(Stream.of());

		reader.doOpen();
		assertNull(reader.doRead());

		Query query = queryContainer.getValue();
		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals("{ $natural : 1}", query.getHint());
	}

	@Test
	void testQueryWithParameters() throws Exception {
		reader.setParameterValues(Collections.singletonList("foo"));

		reader.setQuery("{ name : ?0 }");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(Stream.of());

		reader.doOpen();
		assertNull(reader.doRead());

		Query query = queryContainer.getValue();
		assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
	}

	@Test
	void testQueryWithBatchSize() throws Exception {
		reader.setBatchSize(50);
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(Stream.of());

		reader.doOpen();
		assertNull(reader.doRead());

		Query query = queryContainer.getValue();
		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals(50, query.getMeta().getCursorBatchSize());
	}

	@Test
	void testQueryWithLimit() throws Exception {
		reader.setLimit(200);
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(Stream.of());

		reader.doOpen();
		assertNull(reader.doRead());

		Query query = queryContainer.getValue();
		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals(200, query.getLimit());
	}

	@Test
	void testQueryWithMaxTime() throws Exception {
		reader.setMaxTime(Duration.ofSeconds(3));
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);

		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(Stream.of());

		reader.doOpen();
		assertNull(reader.doRead());

		Query query = queryContainer.getValue();
		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals(3000, query.getMeta().getMaxTimeMsec());
	}

	@Test
	void testQueryWithCollection() throws Exception {
		reader.setParameterValues(Collections.singletonList("foo"));

		reader.setQuery("{ name : ?0 }");
		reader.setCollection("collection");
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<String> collectionContainer = ArgumentCaptor.forClass(String.class);

		when(template.stream(queryContainer.capture(), eq(String.class), collectionContainer.capture()))
			.thenReturn(Stream.of());

		reader.doOpen();
		assertNull(reader.doRead());

		Query query = queryContainer.getValue();
		assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals("collection", collectionContainer.getValue());
	}

	@Test
	void testQueryObject() throws Exception {
		reader = new MongoCursorItemReader<>();
		reader.setTemplate(template);

		Query query = new Query().with(Sort.by(new Sort.Order(Sort.Direction.ASC, "_id")));
		reader.setQuery(query);
		reader.setTargetType(String.class);

		reader.afterPropertiesSet();

		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		when(template.stream(queryContainer.capture(), eq(String.class))).thenReturn(Stream.of());

		reader.doOpen();
		assertNull(reader.doRead());

		Query actualQuery = queryContainer.getValue();
		assertEquals("{}", actualQuery.getQueryObject().toJson());
		assertEquals("{\"_id\": 1}", actualQuery.getSortObject().toJson());
	}

	@Test
	void testQueryObjectWithCollection() throws Exception {
		reader = new MongoCursorItemReader<>();
		reader.setTemplate(template);

		Query query = new Query().with(Sort.by(new Sort.Order(Sort.Direction.ASC, "_id")));
		reader.setQuery(query);
		reader.setTargetType(String.class);
		reader.setCollection("collection");

		reader.afterPropertiesSet();

		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		ArgumentCaptor<String> stringContainer = ArgumentCaptor.forClass(String.class);
		when(template.stream(queryContainer.capture(), eq(String.class), stringContainer.capture()))
			.thenReturn(Stream.of());

		reader.doOpen();
		assertNull(reader.doRead());

		Query actualQuery = queryContainer.getValue();
		assertEquals("{}", actualQuery.getQueryObject().toJson());
		assertEquals("{\"_id\": 1}", actualQuery.getSortObject().toJson());
		assertEquals("collection", stringContainer.getValue());
	}

	@Test
	void testSortThrowsExceptionWhenInvokedWithNull() {
		// given
		reader = new MongoCursorItemReader<>();

		// when + then
		assertThatIllegalArgumentException().isThrownBy(() -> reader.setSort(null))
			.withMessage("Sorts must not be null");
	}

	@Test
	void testCursorRead() throws Exception {
		ArgumentCaptor<Query> queryContainer = ArgumentCaptor.forClass(Query.class);
		when(template.stream(queryContainer.capture(), eq(String.class)))
			.thenReturn(Stream.of("first", "second", "third"));

		reader.doOpen();

		assertEquals("first", reader.doRead());
		assertEquals("second", reader.doRead());
		assertEquals("third", reader.doRead());
		assertNull(reader.doRead());
	}

}
