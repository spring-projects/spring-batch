/*
 * Copyright 2006-2013 the original author or authors.
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
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.util.Assert;

/**
 * <p>{@link ItemWriter} that uses the batching features from
 * {@link NamedParameterJdbcTemplate} to execute a batch of statements for all items
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

	private NamedParameterJdbcOperations namedParameterJdbcTemplate;

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
		if (namedParameterJdbcTemplate == null) {
			this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
		}
	}

	/**
	 * Public setter for the {@link NamedParameterJdbcOperations}.
	 * @param namedParameterJdbcTemplate the {@link NamedParameterJdbcOperations} to set
	 */
	public void setJdbcTemplate(NamedParameterJdbcOperations namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	/**
	 * Check mandatory properties - there must be a SimpleJdbcTemplate and an SQL statement plus a
	 * parameter source.
	 */
	@Override
	public void afterPropertiesSet() {
		Assert.notNull(namedParameterJdbcTemplate, "A DataSource or a NamedParameterJdbcTemplate is required.");
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
	@Override
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
				updateCounts = namedParameterJdbcTemplate.batchUpdate(sql, batchArgs);
			}
			else {
				updateCounts = (int[]) namedParameterJdbcTemplate.getJdbcOperations().execute(sql, new PreparedStatementCallback() {
					@Override
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
