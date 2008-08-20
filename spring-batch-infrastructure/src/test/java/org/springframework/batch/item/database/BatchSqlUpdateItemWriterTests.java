/*
 * Copyright 2006-2007 the original author or authors.
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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;

/**
 * @author Dave Syer
 * 
 */
public class BatchSqlUpdateItemWriterTests extends TestCase {

	private BatchSqlUpdateItemWriter<String> writer = new BatchSqlUpdateItemWriter<String>();

	private JdbcTemplate jdbcTemplate;

	protected List<Object> list = new ArrayList<Object>();

	private PreparedStatement ps;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
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
		writer.setJdbcTemplate(jdbcTemplate);
		writer.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<String>() {
			public void setValues(String item, PreparedStatement ps) throws SQLException {
				list.add(item);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		RepeatSynchronizationManager.clear();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.BatchSqlUpdateItemWriter#afterPropertiesSet()}
	 * .
	 * @throws Exception
	 */
	public void testAfterPropertiesSet() throws Exception {
		try {
			writer.afterPropertiesSet();
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage().toLowerCase();
			assertTrue("Message does not contain 'query'.", message.indexOf("query") >= 0);
		}
	}

	public void testWriteAndFlush() throws Exception {
		ps.addBatch();
		expectLastCall();
		expect(ps.executeBatch()).andReturn(new int[] { 123 });
		replay(ps);
		writer.write(Collections.singletonList("bar"));
		assertEquals(2, list.size());
		assertTrue(list.contains("SQL"));
	}

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
