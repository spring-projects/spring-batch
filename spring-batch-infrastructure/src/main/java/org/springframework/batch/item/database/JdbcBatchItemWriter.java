/*
 * Copyright 2006-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.util.Assert;

/**
 * <p>
 * {@link ItemWriter} that uses the batching features from
 * {@link NamedParameterJdbcTemplate} to execute a batch of statements for all items
 * provided.
 * </p>
 *
 * The user must provide an SQL query and a special callback for either of
 * {@link ItemPreparedStatementSetter} or {@link ItemSqlParameterSourceProvider}. You can
 * use either named parameters or the traditional '?' placeholders. If you use the named
 * parameter support then you should provide a {@link ItemSqlParameterSourceProvider},
 * otherwise you should provide a {@link ItemPreparedStatementSetter}. This callback would
 * be responsible for mapping the item to the parameters needed to execute the SQL
 * statement.<br>
 *
 * It is expected that {@link #write(Chunk)} is called inside a transaction.<br>
 *
 * The writer is thread-safe after its properties are set (normal singleton behavior), so
 * it can be used to write in multiple concurrent transactions.
 *
 * @author Dave Syer
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @since 2.0
 */
public class JdbcBatchItemWriter<T> implements ItemWriter<T>, InitializingBean {

	protected static final Log logger = LogFactory.getLog(JdbcBatchItemWriter.class);

	protected @Nullable NamedParameterJdbcOperations namedParameterJdbcTemplate;

	protected @Nullable ItemPreparedStatementSetter<T> itemPreparedStatementSetter;

	protected @Nullable ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider;

	protected @Nullable String sql;

	protected boolean assertUpdates = true;

	protected int parameterCount;

	protected boolean usingNamedParameters;

	/**
	 * Public setter for the flag that determines whether an assertion is made that all
	 * items cause at least one row to be updated.
	 * @param assertUpdates the flag to set. Defaults to true;
	 */
	public void setAssertUpdates(boolean assertUpdates) {
		this.assertUpdates = assertUpdates;
	}

	/**
	 * Public setter for the query string to execute on write. The parameters should
	 * correspond to those known to the {@link ItemPreparedStatementSetter}.
	 * @param sql the query to set
	 */
	public void setSql(String sql) {
		this.sql = sql;
	}

	/**
	 * Public setter for the {@link ItemPreparedStatementSetter}.
	 * @param preparedStatementSetter the {@link ItemPreparedStatementSetter} to set. This
	 * is required when using traditional '?' placeholders for the SQL statement.
	 */
	public void setItemPreparedStatementSetter(ItemPreparedStatementSetter<T> preparedStatementSetter) {
		this.itemPreparedStatementSetter = preparedStatementSetter;
	}

	/**
	 * Public setter for the {@link ItemSqlParameterSourceProvider}.
	 * @param itemSqlParameterSourceProvider the {@link ItemSqlParameterSourceProvider} to
	 * set. This is required when using named parameters for the SQL statement and the
	 * type to be written does not implement {@link Map}.
	 */
	public void setItemSqlParameterSourceProvider(ItemSqlParameterSourceProvider<T> itemSqlParameterSourceProvider) {
		this.itemSqlParameterSourceProvider = itemSqlParameterSourceProvider;
	}

	/**
	 * Public setter for the data source for injection purposes.
	 * @param dataSource {@link javax.sql.DataSource} to use for querying against
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
	 * Check mandatory properties - there must be a NamedParameterJdbcOperations and an
	 * SQL statement plus a parameter source.
	 */
	@Override
	public void afterPropertiesSet() {
		Assert.state(namedParameterJdbcTemplate != null, "A DataSource or a NamedParameterJdbcTemplate is required.");
		Assert.state(sql != null, "An SQL statement is required.");
		List<String> namedParameters = new ArrayList<>();
		parameterCount = JdbcParameterUtils.countParameterPlaceholders(sql, namedParameters);
		if (!namedParameters.isEmpty()) {
			if (parameterCount != namedParameters.size()) {
				throw new InvalidDataAccessApiUsageException(
						"You can't use both named parameters and classic \"?\" placeholders: " + sql);
			}
			usingNamedParameters = true;
		}
		if (!usingNamedParameters) {
			Assert.state(itemPreparedStatementSetter != null,
					"Using SQL statement with '?' placeholders requires an ItemPreparedStatementSetter");
		}
	}

	@SuppressWarnings({ "unchecked", "DataFlowIssue" })
	@Override
	public void write(Chunk<? extends T> chunk) throws Exception {

		if (!chunk.isEmpty()) {

			if (logger.isDebugEnabled()) {
				logger.debug("Executing batch with " + chunk.size() + " items.");
			}

			int[] updateCounts;

			if (usingNamedParameters) {
				if (chunk.getItems().get(0) instanceof Map && this.itemSqlParameterSourceProvider == null) {
					updateCounts = namedParameterJdbcTemplate.batchUpdate(sql,
							chunk.getItems().toArray(new Map[chunk.size()]));
				}
				else {
					SqlParameterSource[] batchArgs = new SqlParameterSource[chunk.size()];
					int i = 0;
					for (T item : chunk) {
						batchArgs[i++] = itemSqlParameterSourceProvider.createSqlParameterSource(item);
					}
					updateCounts = namedParameterJdbcTemplate.batchUpdate(sql, batchArgs);
				}
			}
			else {
				updateCounts = namedParameterJdbcTemplate.getJdbcOperations()
					.execute(sql, (PreparedStatementCallback<int[]>) ps -> {
						for (T item : chunk) {
							itemPreparedStatementSetter.setValues(item, ps);
							ps.addBatch();
						}
						return ps.executeBatch();
					});
			}

			if (assertUpdates) {
				for (int i = 0; i < updateCounts.length; i++) {
					int value = updateCounts[i];
					if (value == 0) {
						throw new EmptyResultDataAccessException("Item " + i + " of " + updateCounts.length
								+ " did not update any rows: [" + chunk.getItems().get(i) + "]", 1);
					}
				}
			}

			processUpdateCounts(updateCounts);
		}
	}

	/**
	 * Extension point to post process the update counts for each item.
	 * @param updateCounts the array of update counts for each item
	 * @since 5.1
	 */
	protected void processUpdateCounts(int[] updateCounts) {
		// No Op
	}

}
