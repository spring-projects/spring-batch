/*
 * Copyright 2006-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.database;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import java.util.Collections;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * @author Thomas Risberg
 */
public class JdbcBatchItemWriterNamedParameterTests {

	private JdbcBatchItemWriter<Foo> writer = new JdbcBatchItemWriter<Foo>();

    private NamedParameterJdbcOperations namedParameterJdbcOperations;
	
	private String sql = "update foo set bar = :bar where id = :id";

	@SuppressWarnings("unused")
	private class Foo {
		private Long id;
		private String bar;

		public Foo(String bar) {
			this.id = 1L;
			this.bar = bar;
		}
		
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
		
	}

	@Before
	public void setUp() throws Exception {
        namedParameterJdbcOperations = createMock(NamedParameterJdbcOperations.class);
		writer.setSql(sql);
		writer.setJdbcTemplate(namedParameterJdbcOperations);
		writer.setItemSqlParameterSourceProvider(
				new BeanPropertyItemSqlParameterSourceProvider<Foo>());
		writer.afterPropertiesSet();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.JdbcBatchItemWriter#afterPropertiesSet()}
	 * .
	 * @throws Exception
	 */
	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new JdbcBatchItemWriter<Foo>();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Message does not contain 'NamedParameterJdbcTemplate'.", message.indexOf("NamedParameterJdbcTemplate") >= 0);
		}
		writer.setJdbcTemplate(namedParameterJdbcOperations);
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage().toLowerCase();
			assertTrue("Message does not contain 'sql'.", message.indexOf("sql") >= 0);
		}
		writer.setSql("select * from foo where id = :id");
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Message does not contain 'ItemSqlParameterSourceProvider'.", message.indexOf("ItemSqlParameterSourceProvider") >= 0);
		}
		writer.setItemSqlParameterSourceProvider(
				new BeanPropertyItemSqlParameterSourceProvider<Foo>());
		writer.afterPropertiesSet();
	}

	@Test
	public void testWriteAndFlush() throws Exception {
		expect(namedParameterJdbcOperations.batchUpdate(eq(sql),
				eqSqlParameterSourceArray(new SqlParameterSource[] {new BeanPropertySqlParameterSource(new Foo("bar"))})))
				.andReturn(new int[] {1});
		replay(namedParameterJdbcOperations);
		writer.write(Collections.singletonList(new Foo("bar")));
		verify(namedParameterJdbcOperations);
	}

	@Test
	public void testWriteAndFlushWithEmptyUpdate() throws Exception {
		expect(namedParameterJdbcOperations.batchUpdate(eq(sql),
				eqSqlParameterSourceArray(new SqlParameterSource[] {new BeanPropertySqlParameterSource(new Foo("bar"))})))
				.andReturn(new int[] {0});
		replay(namedParameterJdbcOperations);
		try {
			writer.write(Collections.singletonList(new Foo("bar")));
			fail("Expected EmptyResultDataAccessException");
		}
		catch (EmptyResultDataAccessException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.indexOf("did not update") >= 0);
		}
		verify(namedParameterJdbcOperations);
	}

	@Test
	public void testWriteAndFlushWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("ERROR");
		expect(namedParameterJdbcOperations.batchUpdate(eq(sql),
				eqSqlParameterSourceArray(new SqlParameterSource[] {new BeanPropertySqlParameterSource(new Foo("bar"))})))
				.andThrow(ex);
		replay(namedParameterJdbcOperations);
		try {
			writer.write(Collections.singletonList(new Foo("bar")));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("ERROR", e.getMessage());
		}
		verify(namedParameterJdbcOperations);
	}

	public static SqlParameterSource[] eqSqlParameterSourceArray(SqlParameterSource[] in) {
	    EasyMock.reportMatcher(new SqlParameterSourceArrayEquals(in));
	    return null;
	}

	public static class SqlParameterSourceArrayEquals implements IArgumentMatcher {
	    private SqlParameterSource[] expected;

	    public SqlParameterSourceArrayEquals(SqlParameterSource[] expected) {
	        this.expected = expected;
	    }

	    public boolean matches(Object actual) {
	        if (!(actual instanceof SqlParameterSource[])) {
	            return false;
	        }
	        SqlParameterSource[] actualArray = (SqlParameterSource[])actual;
	        if (expected.length != actualArray.length) {
	        	return false;
	        }
	        for (int i = 0; i < expected.length; i++) {
	        	if (!expected[i].getClass().equals(actualArray[i].getClass())) {
		        	return false;
	        	}
	        }
	        return true;
	    }

	    public void appendTo(StringBuffer buffer) {
	        buffer.append("eqSqlParameterSourceArray(");
	        buffer.append(expected.getClass().getName());
	        buffer.append(" with length \"");
	        buffer.append(expected.length);
	        buffer.append("\")");

	    }
	}
}
