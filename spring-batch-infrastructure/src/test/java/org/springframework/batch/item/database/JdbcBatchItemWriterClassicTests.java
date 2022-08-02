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
class JdbcBatchItemWriterClassicTests {

	private JdbcBatchItemWriter<String> writer = new JdbcBatchItemWriter<>();

	private JdbcTemplate jdbcTemplate;

	protected List<Object> list = new ArrayList<>();

	private PreparedStatement ps;

	@BeforeEach
	void setUp() {
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
	 */
	@Test
	void testAfterPropertiesSet() {
		writer = new JdbcBatchItemWriter<>();
		Exception exception = assertThrows(IllegalArgumentException.class, writer::afterPropertiesSet);
		assertTrue(exception.getMessage().contains("NamedParameterJdbcTemplate"),
				"Message does not contain ' NamedParameterJdbcTemplate'.");

		writer.setJdbcTemplate(new NamedParameterJdbcTemplate(jdbcTemplate));
		exception = assertThrows(IllegalArgumentException.class, writer::afterPropertiesSet);
		String message = exception.getMessage();
		assertTrue(message.toLowerCase().contains("sql"), "Message does not contain 'sql'.");

		writer.setSql("select * from foo where id = ?");
		exception = assertThrows(IllegalArgumentException.class, writer::afterPropertiesSet);
		assertTrue(exception.getMessage().contains("ItemPreparedStatementSetter"),
				"Message does not contain 'ItemPreparedStatementSetter'.");

		writer.setItemPreparedStatementSetter((item, ps) -> {
		});
		writer.afterPropertiesSet();
	}

	@Test
	void testWriteAndFlush() throws Exception {
		ps.addBatch();
		when(ps.executeBatch()).thenReturn(new int[] { 123 });
		writer.write(Collections.singletonList("bar"));
		assertEquals(2, list.size());
		assertTrue(list.contains("SQL"));
	}

	@Test
	void testWriteAndFlushWithEmptyUpdate() throws Exception {
		ps.addBatch();
		when(ps.executeBatch()).thenReturn(new int[] { 0 });
		Exception exception = assertThrows(EmptyResultDataAccessException.class, () -> writer.write(List.of("bar")));
		String message = exception.getMessage();
		assertTrue(message.contains("did not update"), "Wrong message: " + message);
		assertEquals(2, list.size());
		assertTrue(list.contains("SQL"));
	}

	@Test
	void testWriteAndFlushWithFailure() throws Exception {
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
		Exception exception = assertThrows(RuntimeException.class, () -> writer.write(List.of("foo")));
		assertEquals("bar", exception.getMessage());
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
