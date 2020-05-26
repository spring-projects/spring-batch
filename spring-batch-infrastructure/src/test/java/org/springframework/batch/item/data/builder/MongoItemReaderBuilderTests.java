/*
 * Copyright 2017-2020 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Drummond Dawson
 * @author Parikshit Dutta
 */
public class MongoItemReaderBuilderTests {
	@Mock
	private MongoOperations template;

	private Map<String, Sort.Direction> sortOptions;

	private ArgumentCaptor<Query> queryContainer;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.sortOptions = new HashMap<>();
		this.sortOptions.put("name", Sort.Direction.DESC);
		this.queryContainer = ArgumentCaptor.forClass(Query.class);
	}

	@Test
	public void testBasic() throws Exception {
		MongoItemReader<String> reader = getBasicBuilder().build();

		when(template.find(this.queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertNull("reader should not return result", reader.read());

		Query query = this.queryContainer.getValue();
		assertEquals(50, query.getLimit());
		assertEquals(0, query.getSkip());
		assertEquals("{}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
	}

	@Test
	public void testFields() throws Exception {
		MongoItemReader<String> reader = getBasicBuilder()
				.fields("{name : 1, age : 1, _id: 0}")
				.build();

		when(this.template.find(this.queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertNull("reader should not return result", reader.read());

		Query query = this.queryContainer.getValue();
		assertEquals(1, query.getFieldsObject().get("name"));
		assertEquals(1, query.getFieldsObject().get("age"));
		assertEquals(0, query.getFieldsObject().get("_id"));
	}

	@Test
	public void testHint() throws Exception {
		MongoItemReader<String> reader = getBasicBuilder()
				.hint("{ $natural : 1}")
				.build();

		when(this.template.find(this.queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertNull("reader should not return result", reader.read());

		Query query = this.queryContainer.getValue();
		assertEquals("{ $natural : 1}", query.getHint());
	}

	@Test
	public void testCollection() throws Exception {
		MongoItemReader<String> reader = getBasicBuilder()
				.parameterValues(Collections.singletonList("foo"))
				.jsonQuery("{ name : ?0 }")
				.collection("collection")
				.build();

		ArgumentCaptor<String> collectionContainer = ArgumentCaptor.forClass(String.class);

		when(this.template.find(this.queryContainer.capture(), eq(String.class), collectionContainer.capture()))
				.thenReturn(new ArrayList<>());

		assertNull("reader should not return result", reader.read());

		Query query = this.queryContainer.getValue();
		assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals("collection", collectionContainer.getValue());
	}

	@Test
	public void testVarargs() throws Exception {
		MongoItemReader<String> reader = getBasicBuilder()
				.parameterValues("foo")
				.jsonQuery("{ name : ?0 }")
				.collection("collection")
				.build();

		ArgumentCaptor<String> collectionContainer = ArgumentCaptor.forClass(String.class);

		when(this.template.find(this.queryContainer.capture(), eq(String.class), collectionContainer.capture()))
				.thenReturn(new ArrayList<>());

		assertNull("reader should not return result", reader.read());

		Query query = this.queryContainer.getValue();
		assertEquals("{\"name\": \"foo\"}", query.getQueryObject().toJson());
		assertEquals("{\"name\": -1}", query.getSortObject().toJson());
		assertEquals("collection", collectionContainer.getValue());
	}

	@Test
	public void testWithoutQueryLimit() throws Exception {
		MongoItemReader<String> reader = new MongoItemReaderBuilder<String>().template(this.template)
				.targetType(String.class)
				.query(new Query())
				.sorts(this.sortOptions)
				.name("mongoReaderTest")
				.pageSize(50)
				.build();

		when(template.find(this.queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertNull("reader should not return result", reader.read());

		Query query = this.queryContainer.getValue();
		assertEquals(50, query.getLimit());
	}

	@Test
	public void testWithoutQueryLimitAndPageSize() throws Exception {
		MongoItemReader<String> reader = new MongoItemReaderBuilder<String>().template(this.template)
				.targetType(String.class)
				.query(new Query())
				.sorts(this.sortOptions)
				.name("mongoReaderTest")
				.build();

		when(template.find(this.queryContainer.capture(), eq(String.class))).thenReturn(new ArrayList<>());

		assertNull("reader should not return result", reader.read());

		Query query = this.queryContainer.getValue();
		assertEquals(10, query.getLimit());
	}

	@Test
	public void testNullTemplate() {
		validateExceptionMessage(new MongoItemReaderBuilder<String>().targetType(String.class)
				.jsonQuery("{ }")
				.sorts(this.sortOptions)
				.name("mongoReaderTest")
				.pageSize(50), "template is required.");
	}

	@Test
	public void testNullTargetType() {
		validateExceptionMessage(new MongoItemReaderBuilder<String>().template(this.template)
				.jsonQuery("{ }")
				.sorts(this.sortOptions)
				.name("mongoReaderTest")
				.pageSize(50), "targetType is required.");
	}

	@Test
	public void testNullQuery() {
		validateExceptionMessage(new MongoItemReaderBuilder<String>().template(this.template)
				.targetType(String.class)
				.sorts(this.sortOptions)
				.name("mongoReaderTest")
				.pageSize(50), "A query is required");
	}

	@Test
	public void testNullSorts() {
		validateExceptionMessage(new MongoItemReaderBuilder<String>().template(this.template)
				.targetType(String.class)
				.jsonQuery("{ }")
				.name("mongoReaderTest")
				.pageSize(50), "sorts map is required.");
	}

	@Test
	public void testNullName() {
		validateExceptionMessage(new MongoItemReaderBuilder<String>().template(this.template)
				.targetType(String.class)
				.jsonQuery("{ }")
				.sorts(this.sortOptions)
				.pageSize(50), "A name is required when saveState is set to true");
	}

	private void validateExceptionMessage(MongoItemReaderBuilder<String> builder, String message) {
		try {
			builder.build();
			fail("Exception should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("IllegalArgumentException message did not match the expected result.", message,
					iae.getMessage());
		}
		catch (IllegalStateException ise) {
			assertEquals("IllegalStateException message did not match the expected result.", message,
					ise.getMessage());
		}
	}

	private MongoItemReaderBuilder<String> getBasicBuilder() {
		return new MongoItemReaderBuilder<String>().template(this.template)
				.targetType(String.class)
				.jsonQuery("{ }")
				.sorts(this.sortOptions)
				.name("mongoReaderTest")
				.pageSize(50);
	}
}
