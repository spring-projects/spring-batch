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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import static org.easymock.EasyMock.*;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Dave Syer
 * 
 */
public class BatchSqlUpdateItemWriterTests extends TestCase {

	private BatchSqlUpdateItemWriter<String> writer = new BatchSqlUpdateItemWriter<String>();

	private JdbcTemplate jdbcTemplate;

	protected List<Object> list = new ArrayList<Object>();

	private RepeatContext context = new RepeatContextSupport(null);

	private PreparedStatement ps;

	/*
	 * (non-Javadoc)
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
		TransactionSynchronizationManager.bindResource(writer.getResourceKey(), new HashSet<Object>(
				Collections.singleton("spam")));
		RepeatSynchronizationManager.register(context);
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		if (TransactionSynchronizationManager.hasResource(writer.getResourceKey())) {
			TransactionSynchronizationManager.unbindResource(writer.getResourceKey());
		}
		RepeatSynchronizationManager.clear();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.BatchSqlUpdateItemWriter#afterPropertiesSet()}.
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

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.BatchSqlUpdateItemWriter#write(List)}.
	 * @throws Exception
	 */
	public void testWrite() throws Exception {
		writer.setSql("foo");
		writer.write(Collections.singletonList("bar"));
		// Nothing happens till we flush
		assertEquals(0, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.BatchSqlUpdateItemWriter#clear()}.
	 */
	public void testClear() {
		assertTrue(TransactionSynchronizationManager.hasResource(writer.getResourceKey()));
		writer.clear();
		assertFalse(TransactionSynchronizationManager.hasResource(writer.getResourceKey()));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.BatchSqlUpdateItemWriter#flush()}.
	 * @throws SQLException 
	 */
	public void testFlush() throws SQLException {
		assertTrue(TransactionSynchronizationManager.hasResource(writer.getResourceKey()));
		ps.addBatch(); // there is one item in the buffer to start
		expectLastCall().times(1);
		expect(ps.executeBatch()).andReturn(new int[0]);
		replay(ps);
		writer.flush();
		assertFalse(TransactionSynchronizationManager.hasResource(writer.getResourceKey()));
		assertEquals(2, list.size());
		assertTrue(list.contains("SQL"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.BatchSqlUpdateItemWriter#flush()}.
	 * @throws Exception
	 */
	public void testWriteAndFlush() throws Exception {
		assertTrue(TransactionSynchronizationManager.hasResource(writer.getResourceKey()));
		ps.addBatch();
		expectLastCall().times(2);
		expect(ps.executeBatch()).andReturn(new int[] { 123 });
		replay(ps);
		writer.write(Collections.singletonList("bar"));
		writer.flush();
		assertFalse(TransactionSynchronizationManager.hasResource(writer.getResourceKey()));
		assertEquals(3, list.size());
		assertTrue(list.contains("SQL"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.BatchSqlUpdateItemWriter#flush()}.
	 * @throws Exception
	 */
	public void testWriteAndFlushWithEmptyUpdate() throws Exception {
		assertTrue(TransactionSynchronizationManager.hasResource(writer.getResourceKey()));
		ps.addBatch();
		expectLastCall().times(2);
		expect(ps.executeBatch()).andReturn(new int[] {0});
		replay(ps);
		writer.write(Collections.singletonList("bar"));
		try {
			writer.flush();
			fail("Expected EmptyResultDataAccessException");
		} catch (EmptyResultDataAccessException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Wrong message: "+message, message.indexOf("did not update")>=0);
		}
		assertFalse(TransactionSynchronizationManager.hasResource(writer.getResourceKey()));
		assertEquals(3, list.size());
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
		expect(ps.executeBatch()).andReturn(new int[] {123});
		replay(ps);
		writer.write(Collections.singletonList("foo"));
		try {
			writer.flush();
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("bar", e.getMessage());
		}
		assertFalse(TransactionSynchronizationManager.hasResource(writer.getResourceKey()));
		assertEquals(2, list.size());
		writer.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<String>() {
			public void setValues(String item, PreparedStatement ps) throws SQLException {
				list.add(item);
			}
		});
		writer.write(Collections.singletonList("foo"));
		writer.flush();
		verify(ps);
		assertEquals(4, list.size());
		assertTrue(list.contains("SQL"));
		assertTrue(list.contains("foo"));
		assertTrue(context.isCompleteOnly());
	}

	/**
	 * Flushing without writing items previously should be handled gracefully.
	 */
	public void testEmptyFlush() {
		// items are bound on write, so we unbind them first
		TransactionSynchronizationManager.unbindResource(writer.getResourceKey());
		writer.flush();
	}

}
