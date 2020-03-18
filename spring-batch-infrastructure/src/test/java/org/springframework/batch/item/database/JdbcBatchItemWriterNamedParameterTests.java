/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.item.database;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * @author Thomas Risberg
 * @author Will Schipp
 * @author Michael Minella
 */
public class JdbcBatchItemWriterNamedParameterTests {

	private JdbcBatchItemWriter<Foo> writer = new JdbcBatchItemWriter<>();

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
		namedParameterJdbcOperations = mock(NamedParameterJdbcOperations.class);
		writer.setSql(sql);
		writer.setJdbcTemplate(namedParameterJdbcOperations);
		writer.setItemSqlParameterSourceProvider(
				new BeanPropertyItemSqlParameterSourceProvider<>());
		writer.afterPropertiesSet();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.JdbcBatchItemWriter#afterPropertiesSet()}
	 * .
	 */
	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new JdbcBatchItemWriter<>();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Message does not contain 'NamedParameterJdbcTemplate'.", message.contains("NamedParameterJdbcTemplate"));
		}
		writer.setJdbcTemplate(namedParameterJdbcOperations);
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage().toLowerCase();
			assertTrue("Message does not contain 'sql'.", message.contains("sql"));
		}
		writer.setSql("select * from foo where id = :id");

		writer.afterPropertiesSet();
	}

	@Test
	public void testWriteAndFlush() throws Exception {
		when(namedParameterJdbcOperations.batchUpdate(eq(sql),
				eqSqlParameterSourceArray(new SqlParameterSource[] {new BeanPropertySqlParameterSource(new Foo("bar"))})))
				.thenReturn(new int[] {1});
		writer.write(Collections.singletonList(new Foo("bar")));
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void testWriteAndFlushMap() throws Exception {
		JdbcBatchItemWriter<Map<String, Object>> mapWriter = new JdbcBatchItemWriter<>();
		
		mapWriter.setSql(sql);
		mapWriter.setJdbcTemplate(namedParameterJdbcOperations);
		mapWriter.afterPropertiesSet();
		
		ArgumentCaptor<Map []> captor = ArgumentCaptor.forClass(Map[].class);

		when(namedParameterJdbcOperations.batchUpdate(eq(sql),
				captor.capture()))
				.thenReturn(new int[] {1});
		mapWriter.write(Collections.singletonList(new HashMap<String, Object>() {{put("foo", "bar");}}));

		assertEquals(1, captor.getValue().length);
		Map<String, Object> results = captor.getValue()[0];
		assertEquals("bar", results.get("foo"));
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void testWriteAndFlushMapWithItemSqlParameterSourceProvider() throws Exception {
		JdbcBatchItemWriter<Map<String, Object>> mapWriter = new JdbcBatchItemWriter<>();

		mapWriter.setSql(sql);
		mapWriter.setJdbcTemplate(namedParameterJdbcOperations);
		mapWriter.setItemSqlParameterSourceProvider(new ItemSqlParameterSourceProvider<Map<String, Object>>() {
			@Override
			public SqlParameterSource createSqlParameterSource(Map<String, Object> item) {
				return new MapSqlParameterSource(item);
			}
		});
		mapWriter.afterPropertiesSet();

		ArgumentCaptor<SqlParameterSource []> captor = ArgumentCaptor.forClass(SqlParameterSource[].class);

		when(namedParameterJdbcOperations.batchUpdate(any(String.class),
				captor.capture()))
				.thenReturn(new int[] {1});
		mapWriter.write(Collections.singletonList(new HashMap<String, Object>() {{put("foo", "bar");}}));

		assertEquals(1, captor.getValue().length);
		SqlParameterSource results = captor.getValue()[0];
		assertEquals("bar", results.getValue("foo"));
	}

	@Test
	public void testWriteAndFlushWithEmptyUpdate() throws Exception {
		when(namedParameterJdbcOperations.batchUpdate(eq(sql),
				eqSqlParameterSourceArray(new SqlParameterSource[] {new BeanPropertySqlParameterSource(new Foo("bar"))})))
				.thenReturn(new int[] {0});
		try {
			writer.write(Collections.singletonList(new Foo("bar")));
			fail("Expected EmptyResultDataAccessException");
		}
		catch (EmptyResultDataAccessException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.contains("did not update"));
		}
	}

	@Test
	public void testWriteAndFlushWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("ERROR");
		when(namedParameterJdbcOperations.batchUpdate(eq(sql),
				eqSqlParameterSourceArray(new SqlParameterSource[] {new BeanPropertySqlParameterSource(new Foo("bar"))})))
				.thenThrow(ex);
		try {
			writer.write(Collections.singletonList(new Foo("bar")));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("ERROR", e.getMessage());
		}
	}

	public static SqlParameterSource[] eqSqlParameterSourceArray(SqlParameterSource[] in) {
		argThat(new SqlParameterSourceArrayEquals(in));
		return null;
	}

	public static class SqlParameterSourceArrayEquals extends BaseMatcher<SqlParameterSource[]> {
		private SqlParameterSource[] expected;

		public SqlParameterSourceArrayEquals(SqlParameterSource[] expected) {
			this.expected = expected;
		}

		@Override
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


		@Override
		public void describeTo(Description description) {
			description.appendText("eqSqlParameterSourceArray(");
			description.appendText(expected.getClass().getName());
			description.appendText(" with length \"");
			description.appendValue(expected.length);
			description.appendText("\")");
		}
	}

}
