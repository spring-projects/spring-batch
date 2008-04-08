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

import org.easymock.MockControl;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Dave Syer
 * 
 */
public class BatchSqlUpdateItemWriterTests extends TestCase {

	private BatchSqlUpdateItemWriter writer = new BatchSqlUpdateItemWriter();

	private JdbcTemplate jdbcTemplate;

	protected List list = new ArrayList();

	private RepeatContext context = new RepeatContextSupport(null);

	private PreparedStatement ps;

	private MockControl control = MockControl.createControl(PreparedStatement.class);

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		ps = (PreparedStatement) control.getMock();
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
		writer.setItemPreparedStatementSetter(new ItemPreparedStatementSetter() {
			public void setValues(Object item, PreparedStatement ps) throws SQLException {
				list.add(item);
			}
		});
		TransactionSynchronizationManager.bindResource(BatchSqlUpdateItemWriter.ITEMS_PROCESSED, new HashSet(
				Collections.singleton("spam")));
		RepeatSynchronizationManager.register(context);
	}

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		if (TransactionSynchronizationManager.hasResource(BatchSqlUpdateItemWriter.ITEMS_PROCESSED)) {
			TransactionSynchronizationManager.unbindResource(BatchSqlUpdateItemWriter.ITEMS_PROCESSED);
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
	 * {@link org.springframework.batch.item.database.BatchSqlUpdateItemWriter#write(java.lang.Object)}.
	 * @throws Exception
	 */
	public void testWrite() throws Exception {
		writer.setSql("foo");
		writer.write("bar");
		// Nothing happens till we flush
		assertEquals(0, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.BatchSqlUpdateItemWriter#clear()}.
	 */
	public void testClear() {
		assertTrue(TransactionSynchronizationManager.hasResource(BatchSqlUpdateItemWriter.ITEMS_PROCESSED));
		writer.clear();
		assertFalse(TransactionSynchronizationManager.hasResource(BatchSqlUpdateItemWriter.ITEMS_PROCESSED));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.BatchSqlUpdateItemWriter#flush()}.
	 */
	public void testFlush() {
		assertTrue(TransactionSynchronizationManager.hasResource(BatchSqlUpdateItemWriter.ITEMS_PROCESSED));
		writer.flush();
		assertFalse(TransactionSynchronizationManager.hasResource(BatchSqlUpdateItemWriter.ITEMS_PROCESSED));
		assertEquals(2, list.size());
		assertTrue(list.contains("SQL"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.BatchSqlUpdateItemWriter#flush()}.
	 * @throws Exception
	 */
	public void testWriteAndFlush() throws Exception {
		assertTrue(TransactionSynchronizationManager.hasResource(BatchSqlUpdateItemWriter.ITEMS_PROCESSED));
		writer.write("bar");
		writer.flush();
		assertFalse(TransactionSynchronizationManager.hasResource(BatchSqlUpdateItemWriter.ITEMS_PROCESSED));
		assertEquals(3, list.size());
		assertTrue(list.contains("SQL"));
	}

	public void testWriteAndFlushWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("bar");
		writer.setItemPreparedStatementSetter(new ItemPreparedStatementSetter() {
			public void setValues(Object item, PreparedStatement ps) throws SQLException {
				list.add(item);
				throw ex;
			}
		});
		ps.addBatch();
		control.setVoidCallable();
		control.expectAndReturn(ps.executeBatch(), new int[] { 123 });
		control.replay();
		writer.write("foo");
		try {
			writer.flush();
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("bar", e.getMessage());
		}
		assertFalse(TransactionSynchronizationManager.hasResource(BatchSqlUpdateItemWriter.ITEMS_PROCESSED));
		assertEquals(2, list.size());
		writer.setItemPreparedStatementSetter(new ItemPreparedStatementSetter() {
			public void setValues(Object item, PreparedStatement ps) throws SQLException {
				list.add(item);
			}
		});
		writer.write("foo");
		writer.flush();
		control.verify();
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
		TransactionSynchronizationManager.unbindResource(BatchSqlUpdateItemWriter.ITEMS_PROCESSED);
		writer.flush();
	}

}
