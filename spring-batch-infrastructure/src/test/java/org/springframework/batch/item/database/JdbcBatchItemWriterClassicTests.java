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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * @author Dave Syer
 * @author Thomas Risberg
 */
public class JdbcBatchItemWriterClassicTests {

	private JdbcBatchItemWriter<String> writer = new JdbcBatchItemWriter<String>();

	private JdbcTemplate jdbcTemplate;

	protected List<Object> list = new ArrayList<Object>();

	private PreparedStatement ps;

	@Before
	public void setUp() throws Exception {
		ps = createMock(PreparedStatement.class);
		jdbcTemplate = new JdbcTemplate() {
			public Object execute(String sql, PreparedStatementCallback action) throws DataAccessException {
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
		writer.setSimpleJdbcTemplate(new SimpleJdbcTemplate(jdbcTemplate));
		writer.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<String>() {
			public void setValues(String item, PreparedStatement ps) throws SQLException {
				list.add(item);
			}
		});
		writer.afterPropertiesSet();
	}

//	@After
//	public void tearDown() throws Exception {
//		RepeatSynchronizationManager.clear();
//	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.JdbcBatchItemWriter#afterPropertiesSet()}
	 * .
	 * @throws Exception
	 */
	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new JdbcBatchItemWriter<String>();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Message does not contain 'SimpleJdbcTemplate'.", message.indexOf("SimpleJdbcTemplate") >= 0);
		}
		writer.setSimpleJdbcTemplate(new SimpleJdbcTemplate(jdbcTemplate));
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage().toLowerCase();
			assertTrue("Message does not contain 'sql'.", message.indexOf("sql") >= 0);
		}
		writer.setSql("select * from foo where id = ?");
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Message does not contain 'ItemPreparedStatementSetter'.", message.indexOf("ItemPreparedStatementSetter") >= 0);
		}
		writer.setItemPreparedStatementSetter(
				new ItemPreparedStatementSetter<String>() {
					public void setValues(String item, PreparedStatement ps)
							throws SQLException {
					}
					
				});
		writer.afterPropertiesSet();
	}

	@Test
	public void testWriteAndFlush() throws Exception {
		ps.addBatch();
		expectLastCall();
		expect(ps.executeBatch()).andReturn(new int[] { 123 });
		replay(ps);
		writer.write(Collections.singletonList("bar"));
		assertEquals(2, list.size());
		assertTrue(list.contains("SQL"));
	}

	@Test
	public void testWriteAndFlushWithEmptyUpdate() throws Exception {
		ps.addBatch();
		expectLastCall();
		expect(ps.executeBatch()).andReturn(new int[] { 0 });
		replay(ps);
		try {
			writer.write(Collections.singletonList("bar"));
			fail("Expected EmptyResultDataAccessException");
		}
		catch (EmptyResultDataAccessException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.indexOf("did not update") >= 0);
		}
		assertEquals(2, list.size());
		assertTrue(list.contains("SQL"));
	}

	@Test
	public void testWriteAndFlushWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("bar");
		writer.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<String>() {
			public void setValues(String item, PreparedStatement ps) throws SQLException {
				list.add(item);
				throw ex;
			}
		});
		ps.addBatch();
		expectLastCall().times(1);
		expect(ps.executeBatch()).andReturn(new int[] { 123 });
		replay(ps);
		try {
			writer.write(Collections.singletonList("foo"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("bar", e.getMessage());
		}
		assertEquals(2, list.size());
		writer.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<String>() {
			public void setValues(String item, PreparedStatement ps) throws SQLException {
				list.add(item);
			}
		});
		writer.write(Collections.singletonList("foo"));
		verify(ps);
		assertEquals(4, list.size());
		assertTrue(list.contains("SQL"));
		assertTrue(list.contains("foo"));
	}

}
