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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.batch.item.data.Neo4jItemReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 */
@SuppressWarnings("deprecation")
@ExtendWith(MockitoExtension.class)
class Neo4jItemReaderBuilderTests {

	@Mock
	private Iterable<String> result;

	@Mock
	private SessionFactory sessionFactory;

	@Mock
	private Session session;

	@Test
	void testFullyQualifiedItemReader() throws Exception {
		Neo4jItemReader<String> itemReader = new Neo4jItemReaderBuilder<String>().sessionFactory(this.sessionFactory)
				.targetType(String.class).startStatement("n=node(*)").orderByStatement("n.age").pageSize(50).name("bar")
				.matchStatement("n -- m").whereStatement("has(n.name)").returnStatement("m").build();

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		when(this.session.query(String.class,
				"START n=node(*) MATCH n -- m WHERE has(n.name) RETURN m ORDER BY n.age SKIP 0 LIMIT 50", null))
				.thenReturn(result);
		when(result.iterator()).thenReturn(Arrays.asList("foo", "bar", "baz").iterator());

		assertEquals("foo", itemReader.read(), "The expected value was not returned by reader.");
		assertEquals("bar", itemReader.read(), "The expected value was not returned by reader.");
		assertEquals("baz", itemReader.read(), "The expected value was not returned by reader.");
	}

	@Test
	void testCurrentSize() throws Exception {
		Neo4jItemReader<String> itemReader = new Neo4jItemReaderBuilder<String>().sessionFactory(this.sessionFactory)
				.targetType(String.class).startStatement("n=node(*)").orderByStatement("n.age").pageSize(50).name("bar")
				.returnStatement("m").currentItemCount(0).maxItemCount(1).build();

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		when(this.session.query(String.class, "START n=node(*) RETURN m ORDER BY n.age SKIP 0 LIMIT 50", null))
				.thenReturn(result);
		when(result.iterator()).thenReturn(Arrays.asList("foo", "bar", "baz").iterator());

		assertEquals("foo", itemReader.read(), "The expected value was not returned by reader.");
		assertNull(itemReader.read(), "The expected value was not should be null.");
	}

	@Test
	void testResultsWithMatchAndWhereWithParametersWithSession() throws Exception {
		Map<String, Object> params = new HashMap<>();
		params.put("foo", "bar");
		Neo4jItemReader<String> itemReader = new Neo4jItemReaderBuilder<String>().sessionFactory(this.sessionFactory)
				.targetType(String.class).startStatement("n=node(*)").returnStatement("*").orderByStatement("n.age")
				.pageSize(50).name("foo").parameterValues(params).matchStatement("n -- m").whereStatement("has(n.name)")
				.returnStatement("m").build();

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		when(this.session.query(String.class,
				"START n=node(*) MATCH n -- m WHERE has(n.name) RETURN m ORDER BY n.age SKIP 0 LIMIT 50", params))
				.thenReturn(result);
		when(result.iterator()).thenReturn(Arrays.asList("foo", "bar", "baz").iterator());

		assertEquals("foo", itemReader.read(), "The expected value was not returned by reader.");
	}

	@Test
	void testNoSessionFactory() {
		var builder = new Neo4jItemReaderBuilder<String>().targetType(String.class).startStatement("n=node(*)")
				.returnStatement("*").orderByStatement("n.age").pageSize(50).name("bar");
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("sessionFactory is required.", exception.getMessage());
	}

	@Test
	void testZeroPageSize() {
		validateExceptionMessage(
				new Neo4jItemReaderBuilder<String>().sessionFactory(this.sessionFactory).targetType(String.class)
						.startStatement("n=node(*)").returnStatement("*").orderByStatement("n.age").pageSize(0)
						.name("foo").matchStatement("n -- m").whereStatement("has(n.name)").returnStatement("m"),
				"pageSize must be greater than zero");
	}

	@Test
	void testZeroMaxItemCount() {
		validateExceptionMessage(new Neo4jItemReaderBuilder<String>().sessionFactory(this.sessionFactory)
				.targetType(String.class).startStatement("n=node(*)").returnStatement("*").orderByStatement("n.age")
				.pageSize(5).maxItemCount(0).name("foo").matchStatement("n -- m").whereStatement("has(n.name)")
				.returnStatement("m"), "maxItemCount must be greater than zero");
	}

	@Test
	void testCurrentItemCountGreaterThanMaxItemCount() {
		validateExceptionMessage(
				new Neo4jItemReaderBuilder<String>().sessionFactory(this.sessionFactory).targetType(String.class)
						.startStatement("n=node(*)").returnStatement("*").orderByStatement("n.age").pageSize(5)
						.maxItemCount(5).currentItemCount(6).name("foo").matchStatement("n -- m")
						.whereStatement("has(n.name)").returnStatement("m"),
				"maxItemCount must be greater than currentItemCount");
	}

	@Test
	void testNullName() {
		validateExceptionMessage(
				new Neo4jItemReaderBuilder<String>().sessionFactory(this.sessionFactory).targetType(String.class)
						.startStatement("n=node(*)").returnStatement("*").orderByStatement("n.age").pageSize(50),
				"A name is required when saveState is set to true");

		// tests that name is not required if saveState is set to false.
		new Neo4jItemReaderBuilder<String>().sessionFactory(this.sessionFactory).targetType(String.class)
				.startStatement("n=node(*)").returnStatement("*").orderByStatement("n.age").saveState(false)
				.pageSize(50).build();
	}

	@Test
	void testNullTargetType() {
		validateExceptionMessage(
				new Neo4jItemReaderBuilder<String>().sessionFactory(this.sessionFactory).startStatement("n=node(*)")
						.returnStatement("*").orderByStatement("n.age").pageSize(50).name("bar")
						.matchStatement("n -- m").whereStatement("has(n.name)").returnStatement("m"),
				"targetType is required.");
	}

	@Test
	void testNullStartStatement() {
		validateExceptionMessage(
				new Neo4jItemReaderBuilder<String>().sessionFactory(this.sessionFactory).targetType(String.class)
						.returnStatement("*").orderByStatement("n.age").pageSize(50).name("bar")
						.matchStatement("n -- m").whereStatement("has(n.name)").returnStatement("m"),
				"startStatement is required.");
	}

	@Test
	void testNullReturnStatement() {
		validateExceptionMessage(new Neo4jItemReaderBuilder<String>().sessionFactory(this.sessionFactory)
				.targetType(String.class).startStatement("n=node(*)").orderByStatement("n.age").pageSize(50).name("bar")
				.matchStatement("n -- m").whereStatement("has(n.name)"), "returnStatement is required.");
	}

	@Test
	void testNullOrderByStatement() {
		validateExceptionMessage(
				new Neo4jItemReaderBuilder<String>().sessionFactory(this.sessionFactory).targetType(String.class)
						.startStatement("n=node(*)").returnStatement("*").pageSize(50).name("bar")
						.matchStatement("n -- m").whereStatement("has(n.name)").returnStatement("m"),
				"orderByStatement is required.");
	}

	private void validateExceptionMessage(Neo4jItemReaderBuilder<?> builder, String message) {
		Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals(message, exception.getMessage());
	}

}
