package org.springframework.batch.item.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.template.Neo4jOperations;

public class Neo4jItemReaderTests {

	private Neo4jItemReader reader;
	@Mock
	private Neo4jOperations template;
	@Mock
	private Result result;
	@Mock
	private EndResult endResult;

	@Before
	public void setUp() throws Exception {
		reader = new Neo4jItemReader();

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
		reader = new Neo4jItemReader();

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

	@Test
	public void testNullResults() {
		ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);

		when(template.query(query.capture(), (Map<String, Object>) isNull())).thenReturn(null);

		assertFalse(reader.doPageRead().hasNext());
		assertEquals("START n=node(*) RETURN * ORDER BY n.age SKIP 0 LIMIT 50", query.getValue());
	}

	@Test
	public void testNoResults() {
		ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);

		when(template.query(query.capture(), (Map<String, Object>) isNull())).thenReturn(result);
		when(result.to(String.class)).thenReturn(endResult);
		when(endResult.iterator()).thenReturn(new ArrayList().iterator());

		assertFalse(reader.doPageRead().hasNext());
		assertEquals("START n=node(*) RETURN * ORDER BY n.age SKIP 0 LIMIT 50", query.getValue());
	}

	@Test
	public void testResultsWithConverter() {
		ResultConverter converter = new DefaultConverter();

		reader.setResultConverter(converter);
		ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);

		when(template.query(query.capture(), (Map<String, Object>) isNull())).thenReturn(result);
		when(result.to(String.class, converter)).thenReturn(endResult);
		when(endResult.iterator()).thenReturn(new ArrayList(){{
			add(new String());
		}}.iterator());

		assertTrue(reader.doPageRead().hasNext());
		assertEquals("START n=node(*) RETURN * ORDER BY n.age SKIP 0 LIMIT 50", query.getValue());
	}

	@Test
	public void testResultsWithMatchAndWhere() throws Exception {
		reader.setMatchStatement("n -- m");
		reader.setWhereStatement("has(n.name)");
		reader.setReturnStatement("m");
		reader.afterPropertiesSet();
		when(template.query("START n=node(*) MATCH n -- m WHERE has(n.name) RETURN m ORDER BY n.age SKIP 0 LIMIT 50", null)).thenReturn(result);
		when(result.to(String.class)).thenReturn(endResult);
		when(endResult.iterator()).thenReturn(new ArrayList(){{
			add(new String());
		}}.iterator());

		assertTrue(reader.doPageRead().hasNext());
	}
}
