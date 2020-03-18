/*
 * Copyright 2013-2017 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

public class Neo4jItemReaderTests {

	@Mock
	private Iterable<String> result;
	@Mock
	private SessionFactory sessionFactory;
	@Mock
	private Session session;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

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
	public void testAfterPropertiesSet() throws Exception {

		Neo4jItemReader<String> reader = new Neo4jItemReader<>();

		try {
			reader.afterPropertiesSet();
			fail("SessionFactory was not set but exception was not thrown.");
		} catch (IllegalStateException iae) {
			assertEquals("A SessionFactory is required", iae.getMessage());
		} catch (Throwable t) {
			fail("Wrong exception was thrown:" + t);
		}

		reader.setSessionFactory(this.sessionFactory);

		try {
			reader.afterPropertiesSet();
			fail("Target Type was not set but exception was not thrown.");
		} catch (IllegalStateException iae) {
			assertEquals("The type to be returned is required", iae.getMessage());
		} catch (Throwable t) {
			fail("Wrong exception was thrown:" + t);
		}

		reader.setTargetType(String.class);

		try {
			reader.afterPropertiesSet();
			fail("START was not set but exception was not thrown.");
		} catch (IllegalStateException iae) {
			assertEquals("A START statement is required", iae.getMessage());
		} catch (Throwable t) {
			fail("Wrong exception was thrown:" + t);
		}

		reader.setStartStatement("n=node(*)");

		try {
			reader.afterPropertiesSet();
			fail("RETURN was not set but exception was not thrown.");
		} catch (IllegalStateException iae) {
			assertEquals("A RETURN statement is required", iae.getMessage());
		} catch (Throwable t) {
			fail("Wrong exception was thrown:" + t);
		}

		reader.setReturnStatement("n.name, n.phone");

		try {
			reader.afterPropertiesSet();
			fail("ORDER BY was not set but exception was not thrown.");
		} catch (IllegalStateException iae) {
			assertEquals("A ORDER BY statement is required", iae.getMessage());
		} catch (Throwable t) {
			fail("Wrong exception was thrown:" + t);
		}

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

	@SuppressWarnings("unchecked")
	@Test
	public void testNullResultsWithSession() throws Exception {

		Neo4jItemReader<String> itemReader = buildSessionBasedReader();

		ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		when(this.session.query(eq(String.class), query.capture(), isNull())).thenReturn(null);

		assertFalse(itemReader.doPageRead().hasNext());
		assertEquals("START n=node(*) RETURN * ORDER BY n.age SKIP 0 LIMIT 50", query.getValue());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNoResultsWithSession() throws Exception {
		Neo4jItemReader<String> itemReader = buildSessionBasedReader();
		ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		when(this.session.query(eq(String.class), query.capture(), isNull())).thenReturn(result);
		when(result.iterator()).thenReturn(Collections.emptyIterator());

		assertFalse(itemReader.doPageRead().hasNext());
		assertEquals("START n=node(*) RETURN * ORDER BY n.age SKIP 0 LIMIT 50", query.getValue());
	}

	@SuppressWarnings("serial")
	@Test
	public void testResultsWithMatchAndWhereWithSession() throws Exception {
		Neo4jItemReader<String> itemReader = buildSessionBasedReader();
		itemReader.setMatchStatement("n -- m");
		itemReader.setWhereStatement("has(n.name)");
		itemReader.setReturnStatement("m");
		itemReader.afterPropertiesSet();

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		when(this.session.query(String.class, "START n=node(*) MATCH n -- m WHERE has(n.name) RETURN m ORDER BY n.age SKIP 0 LIMIT 50", null)).thenReturn(result);
		when(result.iterator()).thenReturn(Arrays.asList("foo", "bar", "baz").iterator());

		assertTrue(itemReader.doPageRead().hasNext());
	}

	@SuppressWarnings("serial")
	@Test
	public void testResultsWithMatchAndWhereWithParametersWithSession() throws Exception {
		Neo4jItemReader<String> itemReader = buildSessionBasedReader();
		Map<String, Object> params = new HashMap<>();
		params.put("foo", "bar");
		itemReader.setParameterValues(params);
		itemReader.setMatchStatement("n -- m");
		itemReader.setWhereStatement("has(n.name)");
		itemReader.setReturnStatement("m");
		itemReader.afterPropertiesSet();

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		when(this.session.query(String.class, "START n=node(*) MATCH n -- m WHERE has(n.name) RETURN m ORDER BY n.age SKIP 0 LIMIT 50", params)).thenReturn(result);
		when(result.iterator()).thenReturn(Arrays.asList("foo", "bar", "baz").iterator());

		assertTrue(itemReader.doPageRead().hasNext());
	}
}
