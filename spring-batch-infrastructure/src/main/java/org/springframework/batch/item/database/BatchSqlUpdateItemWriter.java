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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * {@link ItemWriter} that uses the batching features from
 * {@link PreparedStatement} if available and can take some rudimentary steps to
 * locate a failure during a flush, and identify the items that failed. When one
 * of those items is encountered again the batch is flushed aggressively so that
 * the bad item is eventually identified and can be dealt with in isolation.<br/>
 * 
 * The user must provide an SQL query and a special callback
 * {@link ItemPreparedStatementSetter}, which is responsible for mapping the
 * item to a PreparedStatement.
 * 
 * @author Dave Syer
 * 
 */
public class BatchSqlUpdateItemWriter implements ItemWriter, InitializingBean {

	/**
	 * Key for items processed in the current transaction {@link RepeatContext}.
	 */
	protected static final String ITEMS_PROCESSED = BatchSqlUpdateItemWriter.class.getName() + ".ITEMS_PROCESSED";

	private Set failed = new HashSet();

	private JdbcOperations jdbcTemplate;

	private ItemPreparedStatementSetter preparedStatementSetter;

	private String sql;

	/**
	 * Public setter for the query string to execute on write. The parameters
	 * should correspond to those known to the
	 * {@link ItemPreparedStatementSetter}.
	 * @param sql the query to set
	 */
	public void setSql(String sql) {
		this.sql = sql;
	}

	/**
	 * Public setter for the {@link ItemPreparedStatementSetter}.
	 * @param preparedStatementSetter the {@link ItemPreparedStatementSetter} to
	 * set
	 */
	public void setItemPreparedStatementSetter(ItemPreparedStatementSetter preparedStatementSetter) {
		this.preparedStatementSetter = preparedStatementSetter;
	}

	/**
	 * Public setter for the {@link JdbcOperations}.
	 * @param jdbcTemplate the {@link JdbcOperations} to set
	 */
	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Check mandatory properties - there must be a delegate.
	 * 
	 * @see org.springframework.dao.support.DaoSupport#initDao()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate, "BatchSqlUpdateItemWriter requires an data source.");
		Assert.notNull(preparedStatementSetter, "BatchSqlUpdateItemWriter requires a ItemPreparedStatementSetter");
	}

	/**
	 * Buffer the item in a transaction resource, but flush aggressively if the
	 * item was previously part of a failed chunk.
	 * 
	 * @throws Exception
	 * 
	 * @see org.springframework.batch.io.OutputSource#write(java.lang.Object)
	 */
	public void write(Object output) throws Exception {
		bindTransactionResources();
		getProcessed().add(output);
		flushIfNecessary(output);
	}

	/**
	 * Accessor for the list of processed items in this transaction.
	 * 
	 * @return the processed
	 */
	private Set getProcessed() {
		Set processed = (Set) TransactionSynchronizationManager.getResource(ITEMS_PROCESSED);
		if (processed == null) {
			processed = Collections.EMPTY_SET;
		}
		return processed;
	}

	/**
	 * Set up the {@link RepeatContext} as a transaction resource.
	 * 
	 * @param context the context to set
	 */
	private void bindTransactionResources() {
		if (TransactionSynchronizationManager.hasResource(ITEMS_PROCESSED)) {
			return;
		}
		TransactionSynchronizationManager.bindResource(ITEMS_PROCESSED, new HashSet());
	}

	/**
	 * Remove the transaction resource associated with this context.
	 */
	private void unbindTransactionResources() {
		if (!TransactionSynchronizationManager.hasResource(ITEMS_PROCESSED)) {
			return;
		}
		TransactionSynchronizationManager.unbindResource(ITEMS_PROCESSED);
	}

	/**
	 * Accessor for the context property.
	 * 
	 * @param output
	 * 
	 * @return the context
	 */
	private void flushIfNecessary(Object output) throws Exception {
		boolean flush;
		synchronized (failed) {
			flush = failed.contains(output);
		}
		if (flush) {
			RepeatContext context = RepeatSynchronizationManager.getContext();
			// Force early completion to commit aggressively if we encounter a
			// failed item (from a failed chunk but we don't know which one was
			// the problem).
			context.setCompleteOnly();
			// Flush now, so that if there is a failure this record can be
			// skipped.
			doFlush();
		}
	}

	/**
	 * Flush the hibernate session from within a repeat context.
	 */
	private void doFlush() {
		final Set processed = getProcessed();
		try {
			if (!processed.isEmpty()) {
				jdbcTemplate.execute(sql, new PreparedStatementCallback() {
					public Object doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
						for (Iterator iterator = processed.iterator(); iterator.hasNext();) {
							Object item = (Object) iterator.next();
							preparedStatementSetter.setValues(item, ps);
							ps.addBatch();
						}
						return ps.executeBatch();
					}
				});
			}
		}
		catch (RuntimeException e) {
			synchronized (failed) {
				failed.addAll(processed);
			}
			throw e;
		}
		finally {
			getProcessed().clear();
		}
	}

	/**
	 * Unbind transaction resources, effectively clearing the item buffer.
	 * 
	 * @see org.springframework.batch.item.ItemWriter#clear()
	 */
	public void clear() throws ClearFailedException {
		unbindTransactionResources();
	}

	/**
	 * Flush the internal item buffer and record failures if there are any.
	 * 
	 * @see org.springframework.batch.item.ItemWriter#flush()
	 */
	public void flush() throws FlushFailedException {
		try {
			doFlush();
		}
		finally {
			unbindTransactionResources();
		}
	}

}
