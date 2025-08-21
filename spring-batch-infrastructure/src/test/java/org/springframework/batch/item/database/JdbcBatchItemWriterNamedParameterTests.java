/*
 * Copyright 2006-2025 the original author or authors.
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

import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.batch.item.Chunk;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * @author Thomas Risberg
 * @author Will Schipp
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
public class JdbcBatchItemWriterNamedParameterTests {

	private JdbcBatchItemWriter<Foo> writer = new JdbcBatchItemWriter<>();

	private NamedParameterJdbcOperations namedParameterJdbcOperations;

	private final String sql = "update foo set bar = :bar where id = :id";

	@SuppressWarnings("unused")
	private static class Foo {

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

	@BeforeEach
	void setUp() {
		namedParameterJdbcOperations = mock();
		writer.setSql(sql);
		writer.setJdbcTemplate(namedParameterJdbcOperations);
		writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
		writer.afterPropertiesSet();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.JdbcBatchItemWriter#afterPropertiesSet()}
	 * .
	 */
	@Test
	void testAfterPropertiesSet() {
		writer = new JdbcBatchItemWriter<>();
		Exception exception = assertThrows(IllegalStateException.class, writer::afterPropertiesSet);
		String message = exception.getMessage();
		assertTrue(message.contains("NamedParameterJdbcTemplate"),
				"Message does not contain 'NamedParameterJdbcTemplate'.");

		writer.setJdbcTemplate(namedParameterJdbcOperations);
		exception = assertThrows(IllegalStateException.class, writer::afterPropertiesSet);
		message = exception.getMessage().toLowerCase();
		assertTrue(message.contains("sql"), "Message does not contain 'sql'.");

		writer.setSql("select * from foo where id = :id");
		writer.afterPropertiesSet();
	}

	@Test
	void testWriteAndFlush() throws Exception {
		when(namedParameterJdbcOperations.batchUpdate(eq(sql),
				eqSqlParameterSourceArray(
						new SqlParameterSource[] { new BeanPropertySqlParameterSource(new Foo("bar")) })))
			.thenReturn(new int[] { 1 });
		writer.write(Chunk.of(new Foo("bar")));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testWriteAndFlushMap() throws Exception {
		JdbcBatchItemWriter<Map<String, Object>> mapWriter = new JdbcBatchItemWriter<>();

		mapWriter.setSql(sql);
		mapWriter.setJdbcTemplate(namedParameterJdbcOperations);
		mapWriter.afterPropertiesSet();

		ArgumentCaptor<Map[]> captor = ArgumentCaptor.forClass(Map[].class);

		when(namedParameterJdbcOperations.batchUpdate(eq(sql), captor.capture())).thenReturn(new int[] { 1 });
		mapWriter.write(Chunk.of(Map.of("foo", "bar")));

		assertEquals(1, captor.getValue().length);
		Map<String, Object> results = captor.getValue()[0];
		assertEquals("bar", results.get("foo"));
	}

	@Test
	void testWriteAndFlushMapWithItemSqlParameterSourceProvider() throws Exception {
		JdbcBatchItemWriter<Map<String, Object>> mapWriter = new JdbcBatchItemWriter<>();

		mapWriter.setSql(sql);
		mapWriter.setJdbcTemplate(namedParameterJdbcOperations);
		mapWriter.setItemSqlParameterSourceProvider(MapSqlParameterSource::new);
		mapWriter.afterPropertiesSet();

		ArgumentCaptor<SqlParameterSource[]> captor = ArgumentCaptor.forClass(SqlParameterSource[].class);

		when(namedParameterJdbcOperations.batchUpdate(any(String.class), captor.capture())).thenReturn(new int[] { 1 });
		mapWriter.write(Chunk.of(Map.of("foo", "bar")));

		assertEquals(1, captor.getValue().length);
		SqlParameterSource results = captor.getValue()[0];
		assertEquals("bar", results.getValue("foo"));
	}

	@Test
	void testWriteAndFlushWithEmptyUpdate() {
		when(namedParameterJdbcOperations.batchUpdate(eq(sql),
				eqSqlParameterSourceArray(
						new SqlParameterSource[] { new BeanPropertySqlParameterSource(new Foo("bar")) })))
			.thenReturn(new int[] { 0 });
		Exception exception = assertThrows(EmptyResultDataAccessException.class,
				() -> writer.write(Chunk.of(new Foo("bar"))));
		String message = exception.getMessage();
		assertTrue(message.contains("did not update"), "Wrong message: " + message);
	}

	@Test
	void testWriteAndFlushWithFailure() {
		final RuntimeException ex = new RuntimeException("ERROR");
		when(namedParameterJdbcOperations.batchUpdate(eq(sql),
				eqSqlParameterSourceArray(
						new SqlParameterSource[] { new BeanPropertySqlParameterSource(new Foo("bar")) })))
			.thenThrow(ex);
		Exception exception = assertThrows(RuntimeException.class, () -> writer.write(Chunk.of(new Foo("bar"))));
		assertEquals("ERROR", exception.getMessage());
	}

	public static @Nullable SqlParameterSource[] eqSqlParameterSourceArray(SqlParameterSource[] in) {
		argThat(new SqlParameterSourceArrayEquals(in));
		return null;
	}

	public static class SqlParameterSourceArrayEquals extends BaseMatcher<SqlParameterSource[]> {

		private final SqlParameterSource[] expected;

		public SqlParameterSourceArrayEquals(SqlParameterSource[] expected) {
			this.expected = expected;
		}

		@Override
		public boolean matches(Object actual) {
			if (!(actual instanceof SqlParameterSource[] actualArray)) {
				return false;
			}
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
