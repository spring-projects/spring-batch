/*
 * Copyright 2017-2022 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.data.builder;

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
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * @author Glenn Renfro
 * @author Drummond Dawson
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(MockitoExtension.class)
class MongoItemReaderBuilderTests {

	@Mock
	private MongoOperations template;

	private Map<String, Sort.Direction> sortOptions;

	private ArgumentCaptor<Query> queryContainer;

	@BeforeEach
	void setUp() {
		this.sortOptions = new HashMap<>();
		this.sortOptions.put("name", Sort.Direction.DESC);
		this.queryContainer = ArgumentCaptor.forClass(Query.class);
	}

	@Test
	void testBasic() throws Exception {
		MongoItemReader<String> reader = getBasicBuilder().build();

		when(template.find(this.queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		Query query = this.queryContainer.getValue();
		assertEquals(50, query.getLimit());
		assertEquals(0, query.getSkip());
		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
	}

	@Test
	void testFields() throws Exception {
		MongoItemReader<String> reader = getBasicBuilder().fields("{name : 1, age : 1, _id: 0}").build();

		when(this.template.find(this.queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		Query query = this.queryContainer.getValue();
		assertEquals(1, query.getFieldsObject().get("name"));
		assertEquals(1, query.getFieldsObject().get("age"));
		assertEquals(0, query.getFieldsObject().get("_id"));
	}

	@Test
	void testHint() throws Exception {
		MongoItemReader<String> reader = getBasicBuilder().hint("{ $natural : 1}").build();

		when(this.template.find(this.queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		Query query = this.queryContainer.getValue();
		assertEquals("{ $natural : 1}", query.getHint());
	}

	@Test
	void testCollection() throws Exception {
		MongoItemReader<String> reader = getBasicBuilder().parameterValues(Collections.singletonList("foo"))
				.jsonQuery("{ name : ?0 }").collection("collection").build();

		ArgumentCaptor<String> collectionContainer = ArgumentCaptor.forClass(String.class);

		when(this.template.find(this.queryContainer.capture(), eq(String.class), collectionContainer.capture()))
				.thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		Query query = this.queryContainer.getValue();
		assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals("collection", collectionContainer.getValue());
	}

	@Test
	void testVarargs() throws Exception {
		MongoItemReader<String> reader = getBasicBuilder().parameterValues("foo").jsonQuery("{ name : ?0 }")
				.collection("collection").build();

		ArgumentCaptor<String> collectionContainer = ArgumentCaptor.forClass(String.class);

		when(this.template.find(this.queryContainer.capture(), eq(String.class), collectionContainer.capture()))
				.thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		Query query = this.queryContainer.getValue();
		assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals("collection", collectionContainer.getValue());
	}

	@Test
	void testWithoutQueryLimit() throws Exception {
		MongoItemReader<String> reader = new MongoItemReaderBuilder<String>().template(this.template)
				.targetType(String.class).query(new Query()).sorts(this.sortOptions).name("mongoReaderTest")
				.pageSize(50).build();

		when(template.find(this.queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		Query query = this.queryContainer.getValue();
		assertEquals(50, query.getLimit());
	}

	@Test
	void testWithoutQueryLimitAndPageSize() throws Exception {
		MongoItemReader<String> reader = new MongoItemReaderBuilder<String>().template(this.template)
				.targetType(String.class).query(new Query()).sorts(this.sortOptions).name("mongoReaderTest").build();

		when(template.find(this.queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		Query query = this.queryContainer.getValue();
		assertEquals(10, query.getLimit());
	}

	@Test
	void testNullTemplate() {
		validateExceptionMessage(new MongoItemReaderBuilder<String>().targetType(String.class).jsonQuery("{ }")
				.sorts(this.sortOptions).name("mongoReaderTest").pageSize(50), "template is required.");
	}

	@Test
	void testNullTargetType() {
		validateExceptionMessage(new MongoItemReaderBuilder<String>().template(this.template).jsonQuery("{ }")
				.sorts(this.sortOptions).name("mongoReaderTest").pageSize(50), "targetType is required.");
	}

	@Test
	void testNullQuery() {
		validateExceptionMessage(new MongoItemReaderBuilder<String>().template(this.template).targetType(String.class)
				.sorts(this.sortOptions).name("mongoReaderTest").pageSize(50), "A query is required");
	}

	@Test
	void testNullSortsWithQueryString() {
		validateExceptionMessage(new MongoItemReaderBuilder<String>().template(this.template).targetType(String.class)
				.jsonQuery("{ }").name("mongoReaderTest").pageSize(50), "sorts map is required.");
	}

	@Test
	void testNullSortsWithQuery() {
		validateExceptionMessage(
				new MongoItemReaderBuilder<String>().template(this.template).targetType(String.class)
						.query(query(where("_id").is("10"))).name("mongoReaderTest").pageSize(50),
				"sorts map is required.");
	}

	@Test
	void testNullName() {
		validateExceptionMessage(new MongoItemReaderBuilder<String>().template(this.template).targetType(String.class)
				.jsonQuery("{ }").sorts(this.sortOptions).pageSize(50),
				"A name is required when saveState is set to true");
	}

	private void validateExceptionMessage(MongoItemReaderBuilder<String> builder, String message) {
		Exception exception = assertThrows(RuntimeException.class, builder::build);
		assertTrue(exception instanceof IllegalArgumentException || exception instanceof IllegalStateException);
		assertEquals(message, exception.getMessage());
	}

	private MongoItemReaderBuilder<String> getBasicBuilder() {
		return new MongoItemReaderBuilder<String>().template(this.template).targetType(String.class).jsonQuery("{ }")
				.sorts(this.sortOptions).name("mongoReaderTest").pageSize(50);
	}

}
