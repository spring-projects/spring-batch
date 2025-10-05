/*
 * Copyright 2017-2023 the original author or authors.
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

package org.springframework.batch.infrastructure.item.data.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.data.MongoPagingItemReader;
import org.springframework.batch.infrastructure.item.data.builder.MongoPagingItemReaderBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
class MongoPagingItemReaderBuilderTests {

	@Mock
	private MongoOperations template;

	private Map<String, Sort.Direction> sortOptions;

	@BeforeEach
	void setUp() {
		this.sortOptions = new HashMap<>();
		this.sortOptions.put("name", Sort.Direction.DESC);
	}

	@Test
	void testBasic() throws Exception {
		MongoPagingItemReader<String> reader = getBasicBuilder().build();

		when(this.template.find(any(), any())).thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		verify(this.template).find(assertArg(query -> {
			assertEquals(50, query.getLimit());
			assertEquals(0, query.getSkip());
			assertEquals("{}", query.getQueryObject().toJson());
			assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		}), eq(String.class));
	}

	@Test
	void testFields() throws Exception {
		MongoPagingItemReader<String> reader = getBasicBuilder().fields("{name : 1, age : 1, _id: 0}").build();

		when(this.template.find(any(), any())).thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		verify(this.template).find(assertArg(query -> {
			assertEquals(1, query.getFieldsObject().get("name"));
			assertEquals(1, query.getFieldsObject().get("age"));
			assertEquals(0, query.getFieldsObject().get("_id"));
		}), eq(String.class));
	}

	@Test
	void testHint() throws Exception {
		MongoPagingItemReader<String> reader = getBasicBuilder().hint("{ $natural : 1}").build();

		when(this.template.find(any(), any())).thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		verify(this.template).find(assertArg(query -> assertEquals("{ $natural : 1}", query.getHint())),
				eq(String.class));
	}

	@Test
	void testCollection() throws Exception {
		MongoPagingItemReader<String> reader = getBasicBuilder().parameterValues(Collections.singletonList("foo"))
			.jsonQuery("{ name : ?0 }")
			.collection("collection")
			.build();

		when(this.template.find(any(), any(), anyString())).thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		verify(this.template).find(assertArg(query -> {
			assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
			assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		}), eq(String.class), eq("collection"));
	}

	@Test
	void testVarargs() throws Exception {
		MongoPagingItemReader<String> reader = getBasicBuilder().parameterValues("foo")
			.jsonQuery("{ name : ?0 }")
			.collection("collection")
			.build();

		when(this.template.find(any(), any(), anyString())).thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		verify(this.template).find(assertArg(query -> {
			assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
			assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		}), eq(String.class), eq("collection"));
	}

	@Test
	void testWithoutQueryLimit() throws Exception {
		MongoPagingItemReader<String> reader = new MongoPagingItemReaderBuilder<String>().template(this.template)
			.targetType(String.class)
			.query(new Query())
			.sorts(this.sortOptions)
			.name("mongoReaderTest")
			.pageSize(50)
			.build();

		when(template.find(any(), any())).thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		verify(this.template).find(assertArg(query -> assertEquals(50, query.getLimit())), eq(String.class));
	}

	@Test
	void testWithoutQueryLimitAndPageSize() throws Exception {
		MongoPagingItemReader<String> reader = new MongoPagingItemReaderBuilder<String>().template(this.template)
			.targetType(String.class)
			.query(new Query())
			.sorts(this.sortOptions)
			.name("mongoReaderTest")
			.build();

		when(template.find(any(), any())).thenReturn(new ArrayList<>());

		assertNull(reader.read(), "reader should not return result");

		verify(this.template).find(assertArg(query -> assertEquals(10, query.getLimit())), eq(String.class));
	}

	@Test
	void testNullTemplate() {
		validateExceptionMessage(new MongoPagingItemReaderBuilder<String>().targetType(String.class)
			.jsonQuery("{ }")
			.sorts(this.sortOptions)
			.name("mongoReaderTest")
			.pageSize(50), "template is required.");
	}

	@Test
	void testNullTargetType() {
		validateExceptionMessage(new MongoPagingItemReaderBuilder<String>().template(this.template)
			.jsonQuery("{ }")
			.sorts(this.sortOptions)
			.name("mongoReaderTest")
			.pageSize(50), "targetType is required.");
	}

	@Test
	void testNullQuery() {
		validateExceptionMessage(new MongoPagingItemReaderBuilder<String>().template(this.template)
			.targetType(String.class)
			.sorts(this.sortOptions)
			.name("mongoReaderTest")
			.pageSize(50), "A query is required");
	}

	@Test
	void testNullSortsWithQueryString() {
		validateExceptionMessage(new MongoPagingItemReaderBuilder<String>().template(this.template)
			.targetType(String.class)
			.jsonQuery("{ }")
			.name("mongoReaderTest")
			.pageSize(50), "sorts map is required.");
	}

	@Test
	void testNullSortsWithQuery() {
		validateExceptionMessage(new MongoPagingItemReaderBuilder<String>().template(this.template)
			.targetType(String.class)
			.query(query(where("_id").is("10")))
			.name("mongoReaderTest")
			.pageSize(50), "sorts map is required.");
	}

	@Test
	void testNullName() {
		validateExceptionMessage(new MongoPagingItemReaderBuilder<String>().template(this.template)
			.targetType(String.class)
			.jsonQuery("{ }")
			.sorts(this.sortOptions)
			.pageSize(50), "A name is required when saveState is set to true");
	}

	private void validateExceptionMessage(MongoPagingItemReaderBuilder<String> builder, String message) {
		Exception exception = assertThrows(RuntimeException.class, builder::build);
		assertTrue(exception instanceof IllegalArgumentException || exception instanceof IllegalStateException);
		assertEquals(message, exception.getMessage());
	}

	private MongoPagingItemReaderBuilder<String> getBasicBuilder() {
		return new MongoPagingItemReaderBuilder<String>().template(this.template)
			.targetType(String.class)
			.jsonQuery("{ }")
			.sorts(this.sortOptions)
			.name("mongoReaderTest")
			.pageSize(50);
	}

}
