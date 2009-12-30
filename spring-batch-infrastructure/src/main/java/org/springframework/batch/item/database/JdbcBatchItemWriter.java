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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

/**
 * <p>{@link ItemWriter} that uses the batching features from 
 * {@link SimpleJdbcTemplate} to execute a batch of statements for all items 
 * provided.</p>
 * 
 * The user must provide an SQL query and a special callback in the for of either 
 * {@link ItemPreparedStatementSetter}, or a {@link ItemSqlParameterSourceProvider}.
 * You can use either named parameters or the traditional '?' placeholders. If you use the 
 * named parameter support then you should provide a {@link ItemSqlParameterSourceProvider}, 
 * otherwise you should provide a  {@link ItemPreparedStatementSetter}.
 * This callback would be responsible for mapping the item to the parameters needed to 
 * execute the SQL statement.<br/>
 * 
 * It is expected that {@link #write(List)} is called inside a transaction.<br/>
 * 
 * The writer is thread safe after its properties are set (normal singleton
 * behavior), so it can be used to write in multiple concurrent transactions.
 * 
 * @author Dave Syer
 * @author Thomas Risberg
 * @since 2.0
 */
public class JdbcBatchItemWriter<T> implements ItemWriter<T>, InitializingBean {

	protected static final Log logger = LogFactory.getLog(JdbcBatchItemWriter.class);

	private SimpleJdbcOperations simpleJdbcTemplate;

	private ItemPreparedStatementSetter<T> itemPreparedStatementSetter;

	private ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider;

	private String sql;

	private boolean assertUpdates = true;

	private int parameterCount;

	private boolean usingNamedParameters;

	/**
	 * Public setter for the flag that determines whether an assertion is made
	 * that all items cause at least one row to be updated.
	 * @param assertUpdates the flag to set. Defaults to true;
	 */
	public void setAssertUpdates(boolean assertUpdates) {
		this.assertUpdates = assertUpdates;
	}

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
	 * set. This is required when using traditional '?' placeholders for the SQL statement.
	 */
	public void setItemPreparedStatementSetter(ItemPreparedStatementSetter<T> preparedStatementSetter) {
		this.itemPreparedStatementSetter = preparedStatementSetter;
	}

	/**
	 * Public setter for the {@link ItemSqlParameterSourceProvider}.
	 * @param itemSqlParameterSourceProvider the {@link ItemSqlParameterSourceProvider} to
	 * set. This is required when using named parameters for the SQL statement.
	 */
	public void setItemSqlParameterSourceProvider(ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider) {
		this.itemSqlParameterSourceProvider = itemSqlParameterSourceProvider;
	}

	/**
	 * Public setter for the data source for injection purposes.
	 * 
	 * @param dataSource
	 */
	public void setDataSource(DataSource dataSource) {
		if (simpleJdbcTemplate == null) {
			this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
		}
	}

	/**
	 * Public setter for the {@link JdbcOperations}.
	 * @param simpleJdbcTemplate the {@link JdbcOperations} to set
	 */
	public void setSimpleJdbcTemplate(SimpleJdbcOperations simpleJdbcTemplate) {
		this.simpleJdbcTemplate = simpleJdbcTemplate;
	}

	/**
	 * Check mandatory properties - there must be a SimpleJdbcTemplate and an SQL statement plus a 
	 * parameter source.
	 */
	public void afterPropertiesSet() {
		Assert.notNull(simpleJdbcTemplate, "A DataSource or a SimpleJdbcTemplate is required.");
		Assert.notNull(sql, "An SQL statement is required.");
		List<String> namedParameters = new ArrayList<String>();
		parameterCount = JdbcParameterUtils.countParameterPlaceholders(sql, namedParameters);
		if (namedParameters.size() > 0) {
			if (parameterCount != namedParameters.size()) {
				throw new InvalidDataAccessApiUsageException("You can't use both named parameters and classic \"?\" placeholders: " + sql);
			}
			usingNamedParameters = true;
		}
		if (usingNamedParameters) {
			Assert.notNull(itemSqlParameterSourceProvider, "Using SQL statement with named parameters requires an ItemSqlParameterSourceProvider");
		}
		else {
			Assert.notNull(itemPreparedStatementSetter, "Using SQL statement with '?' placeholders requires an ItemPreparedStatementSetter");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	public void write(final List<? extends T> items) throws Exception {

		if (!items.isEmpty()) {

			if (logger.isDebugEnabled()) {
				logger.debug("Executing batch with " + items.size() + " items.");
			}

			int[] updateCounts = null;
			
			if (usingNamedParameters) {
				SqlParameterSource[] batchArgs = new SqlParameterSource[items.size()];
				int i = 0;
				for (T item : items) {
					batchArgs[i++] = itemSqlParameterSourceProvider.createSqlParameterSource(item);
				}
				updateCounts = simpleJdbcTemplate.batchUpdate(sql, batchArgs);
			}
			else {
				updateCounts = (int[]) simpleJdbcTemplate.getJdbcOperations().execute(sql, new PreparedStatementCallback() {
					public Object doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
						for (T item : items) {
							itemPreparedStatementSetter.setValues(item, ps);
							ps.addBatch();
						}
						return ps.executeBatch();
					}
				});
			}

			if (assertUpdates) {
				for (int i = 0; i < updateCounts.length; i++) {
					int value = updateCounts[i];
					if (value == 0) {
						throw new EmptyResultDataAccessException("Item " + i + " of " + updateCounts.length
								+ " did not update any rows: [" + items.get(i) + "]", 1);
					}
				}
			}

		}

	}

}
