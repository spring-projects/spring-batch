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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@ExtendWith(MockitoExtension.class)
class Neo4jItemReaderTests {

	@Mock
	private Iterable<String> result;

	@Mock
	private SessionFactory sessionFactory;

	@Mock
	private Session session;

	private Neo4jItemReader<String> buildSessionBasedReader() throws Exception {
		Neo4jItemReader<String> reader = new Neo4jItemReader<>();

		reader.setSessionFactory(this.sessionFactory);
		reader.setTargetType(String.class);
		reader.setStartStatement("n=node(*)");
		reader.setReturnStatement("*");
		reader.setOrderByStatement("n.age");
		reader.setPageSize(50);
		reader.afterPropertiesSet();

		return reader;
	}

	@Test
	void testAfterPropertiesSet() throws Exception {

		Neo4jItemReader<String> reader = new Neo4jItemReader<>();

		Exception exception = assertThrows(IllegalStateException.class, reader::afterPropertiesSet);
		assertEquals("A SessionFactory is required", exception.getMessage());

		reader.setSessionFactory(this.sessionFactory);

		exception = assertThrows(IllegalStateException.class, reader::afterPropertiesSet);
		assertEquals("The type to be returned is required", exception.getMessage());

		reader.setTargetType(String.class);

		exception = assertThrows(IllegalStateException.class, reader::afterPropertiesSet);
		assertEquals("A START statement is required", exception.getMessage());

		reader.setStartStatement("n=node(*)");

		exception = assertThrows(IllegalStateException.class, reader::afterPropertiesSet);
		assertEquals("A RETURN statement is required", exception.getMessage());

		reader.setReturnStatement("n.name, n.phone");

		exception = assertThrows(IllegalStateException.class, reader::afterPropertiesSet);
		assertEquals("A ORDER BY statement is required", exception.getMessage());

		reader.setOrderByStatement("n.age");
		reader.afterPropertiesSet();

		reader = new Neo4jItemReader<>();
		reader.setSessionFactory(this.sessionFactory);
		reader.setTargetType(String.class);
		reader.setStartStatement("n=node(*)");
		reader.setReturnStatement("n.name, n.phone");
		reader.setOrderByStatement("n.age");

		reader.afterPropertiesSet();
	}

	@Test
	void testNullResultsWithSession() throws Exception {

		Neo4jItemReader<String> itemReader = buildSessionBasedReader();

		ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		when(this.session.query(eq(String.class), query.capture(), isNull())).thenReturn(null);

		assertFalse(itemReader.doPageRead().hasNext());
		assertEquals("START n=node(*) RETURN * ORDER BY n.age SKIP 0 LIMIT 50", query.getValue());
	}

	@Test
	void testNoResultsWithSession() throws Exception {
		Neo4jItemReader<String> itemReader = buildSessionBasedReader();
		ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		when(this.session.query(eq(String.class), query.capture(), isNull())).thenReturn(result);
		when(result.iterator()).thenReturn(Collections.emptyIterator());

		assertFalse(itemReader.doPageRead().hasNext());
		assertEquals("START n=node(*) RETURN * ORDER BY n.age SKIP 0 LIMIT 50", query.getValue());
	}

	@Test
	void testResultsWithMatchAndWhereWithSession() throws Exception {
		Neo4jItemReader<String> itemReader = buildSessionBasedReader();
		itemReader.setMatchStatement("n -- m");
		itemReader.setWhereStatement("has(n.name)");
		itemReader.setReturnStatement("m");
		itemReader.afterPropertiesSet();

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		when(this.session.query(String.class,
				"START n=node(*) MATCH n -- m WHERE has(n.name) RETURN m ORDER BY n.age SKIP 0 LIMIT 50", null))
			.thenReturn(result);
		when(result.iterator()).thenReturn(Arrays.asList("foo", "bar", "baz").iterator());

		assertTrue(itemReader.doPageRead().hasNext());
	}

	@Test
	void testResultsWithMatchAndWhereWithParametersWithSession() throws Exception {
		Neo4jItemReader<String> itemReader = buildSessionBasedReader();
		Map<String, Object> params = new HashMap<>();
		params.put("foo", "bar");
		itemReader.setParameterValues(params);
		itemReader.setMatchStatement("n -- m");
		itemReader.setWhereStatement("has(n.name)");
		itemReader.setReturnStatement("m");
		itemReader.afterPropertiesSet();

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		when(this.session.query(String.class,
				"START n=node(*) MATCH n -- m WHERE has(n.name) RETURN m ORDER BY n.age SKIP 0 LIMIT 50", params))
			.thenReturn(result);
		when(result.iterator()).thenReturn(Arrays.asList("foo", "bar", "baz").iterator());

		assertTrue(itemReader.doPageRead().hasNext());
	}

}
