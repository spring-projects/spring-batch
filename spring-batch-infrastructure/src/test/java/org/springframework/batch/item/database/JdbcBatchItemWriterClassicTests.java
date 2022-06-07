/*
 * Copyright 2006-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Dave Syer
 * @author Thomas Risberg
 * @author Will Schipp
 */
public class JdbcBatchItemWriterClassicTests {

	private JdbcBatchItemWriter<String> writer = new JdbcBatchItemWriter<>();

	private JdbcTemplate jdbcTemplate;

	protected List<Object> list = new ArrayList<>();

	private PreparedStatement ps;

	@BeforeEach
	public void setUp() throws Exception {
		ps = mock(PreparedStatement.class);
		jdbcTemplate = new JdbcTemplate() {
			@Override
			public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
				list.add(sql);
				try {
					return action.doInPreparedStatement(ps);
				}
				catch (SQLException e) {
					throw new UncategorizedSQLException("doInPreparedStatement", sql, e);
				}
			}
		};
		writer.setSql("SQL");
		writer.setJdbcTemplate(new NamedParameterJdbcTemplate(jdbcTemplate));
		writer.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<String>() {
			@Override
			public void setValues(String item, PreparedStatement ps) throws SQLException {
				list.add(item);
			}
		});
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
		writer = new JdbcBatchItemWriter<>();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage();
			assertTrue(message.contains("NamedParameterJdbcTemplate"),
					"Message does not contain ' NamedParameterJdbcTemplate'.");
		}
		writer.setJdbcTemplate(new NamedParameterJdbcTemplate(jdbcTemplate));
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage().toLowerCase();
			assertTrue(message.contains("sql"), "Message does not contain 'sql'.");
		}
		writer.setSql("select * from foo where id = ?");
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage();
			assertTrue(message.contains("ItemPreparedStatementSetter"),
					"Message does not contain 'ItemPreparedStatementSetter'.");
		}
		writer.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<String>() {
			@Override
			public void setValues(String item, PreparedStatement ps) throws SQLException {
			}

		});
		writer.afterPropertiesSet();
	}

	@Test
	public void testWriteAndFlush() throws Exception {
		ps.addBatch();
		when(ps.executeBatch()).thenReturn(new int[] { 123 });
		writer.write(Collections.singletonList("bar"));
		assertEquals(2, list.size());
		assertTrue(list.contains("SQL"));
	}

	@Test
	public void testWriteAndFlushWithEmptyUpdate() throws Exception {
		ps.addBatch();
		when(ps.executeBatch()).thenReturn(new int[] { 0 });
		try {
			writer.write(Collections.singletonList("bar"));
			fail("Expected EmptyResultDataAccessException");
		}
		catch (EmptyResultDataAccessException e) {
			// expected
			String message = e.getMessage();
			assertTrue(message.contains("did not update"), "Wrong message: " + message);
		}
		assertEquals(2, list.size());
		assertTrue(list.contains("SQL"));
	}

	@Test
	public void testWriteAndFlushWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("bar");
		writer.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<String>() {
			@Override
			public void setValues(String item, PreparedStatement ps) throws SQLException {
				list.add(item);
				throw ex;
			}
		});
		ps.addBatch();
		when(ps.executeBatch()).thenReturn(new int[] { 123 });
		try {
			writer.write(Collections.singletonList("foo"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("bar", e.getMessage());
		}
		assertEquals(2, list.size());
		writer.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<String>() {
			@Override
			public void setValues(String item, PreparedStatement ps) throws SQLException {
				list.add(item);
			}
		});
		writer.write(Collections.singletonList("foo"));
		assertEquals(4, list.size());
		assertTrue(list.contains("SQL"));
		assertTrue(list.contains("foo"));
	}

}
