/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.template.Neo4jOperations;

public class Neo4jItemReaderTests {

	private Neo4jItemReader<String> reader;
	@Mock
	private Neo4jOperations template;
	@Mock
	private Result<Map<String, Object>> result;
	@Mock
	private Result<String> endResult;

	@Before
	public void setUp() throws Exception {
		reader = new Neo4jItemReader<String>();

		MockitoAnnotations.initMocks(this);

		reader.setTemplate(template);
		reader.setTargetType(String.class);
		reader.setStartStatement("n=node(*)");
		reader.setReturnStatement("*");
		reader.setOrderByStatement("n.age");
		reader.setPageSize(50);
		reader.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		reader = new Neo4jItemReader<String>();

		try {
			reader.afterPropertiesSet();
			fail("Template was not set but exception was not thrown.");
		} catch (IllegalStateException iae) {
			assertEquals("A Neo4JOperations implementation is required", iae.getMessage());
		} catch (Throwable t) {
			fail("Wrong exception was thrown:" + t);
		}

		reader.setTemplate(template);

		try {
			reader.afterPropertiesSet();
			fail("type was not set but exception was not thrown.");
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
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNullResults() {
		ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);

		when(template.query(query.capture(), (Map<String, Object>) isNull())).thenReturn(null);

		assertFalse(reader.doPageRead().hasNext());
		assertEquals("START n=node(*) RETURN * ORDER BY n.age SKIP 0 LIMIT 50", query.getValue());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNoResults() {
		ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);

		when(template.query(query.capture(), (Map<String, Object>) isNull())).thenReturn(result);
		when(result.to(String.class)).thenReturn(endResult);
		when(endResult.iterator()).thenReturn(new ArrayList<String>().iterator());

		assertFalse(reader.doPageRead().hasNext());
		assertEquals("START n=node(*) RETURN * ORDER BY n.age SKIP 0 LIMIT 50", query.getValue());
	}

	@SuppressWarnings({ "unchecked", "serial" })
	@Test
	public void testResultsWithConverter() {
		ResultConverter<Map<String, Object>, String> converter = new DefaultConverter<Map<String, Object>, String>();

		reader.setResultConverter(converter);
		ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);

		when(template.query(query.capture(), (Map<String, Object>) isNull())).thenReturn(result);
		when(result.to(String.class, converter)).thenReturn(endResult);
		when(endResult.iterator()).thenReturn(new ArrayList<String>(){{
			add(new String());
		}}.iterator());

		assertTrue(reader.doPageRead().hasNext());
		assertEquals("START n=node(*) RETURN * ORDER BY n.age SKIP 0 LIMIT 50", query.getValue());
	}

	@SuppressWarnings("serial")
	@Test
	public void testResultsWithMatchAndWhere() throws Exception {
		reader.setMatchStatement("n -- m");
		reader.setWhereStatement("has(n.name)");
		reader.setReturnStatement("m");
		reader.afterPropertiesSet();
		when(template.query("START n=node(*) MATCH n -- m WHERE has(n.name) RETURN m ORDER BY n.age SKIP 0 LIMIT 50", null)).thenReturn(result);
		when(result.to(String.class)).thenReturn(endResult);
		when(endResult.iterator()).thenReturn(new ArrayList<String>() {{
			add(new String());
		}}.iterator());

		assertTrue(reader.doPageRead().hasNext());
	}

	@SuppressWarnings("serial")
	@Test
	public void testResultsWithMatchAndWhereWithParameters() throws Exception {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("foo", "bar");
		reader.setParameterValues(params);
		reader.setMatchStatement("n -- m");
		reader.setWhereStatement("has(n.name)");
		reader.setReturnStatement("m");
		reader.afterPropertiesSet();
		when(template.query("START n=node(*) MATCH n -- m WHERE has(n.name) RETURN m ORDER BY n.age SKIP 0 LIMIT 50", params)).thenReturn(result);
		when(result.to(String.class)).thenReturn(endResult);
		when(endResult.iterator()).thenReturn(new ArrayList<String>(){{
			add(new String());
		}}.iterator());

		assertTrue(reader.doPageRead().hasNext());
	}
}
