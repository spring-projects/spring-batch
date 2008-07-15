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
package org.springframework.batch.item.database.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.DrivingQueryItemReader;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.KeyCollector;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * <p>
 * JDBC implementation of the {@link KeyCollector} interface that works for
 * composite keys. (i.e. keys represented by multiple columns) A SQL query to be
 * used to return the keys and a {@link ItemPreparedStatementSetter} to map each
 * row in the result set to an Object must be set in order to work correctly.
 * </p>
 * 
 * The implementation is thread-safe as long as the
 * {@link #setPreparedStatementSetter(ItemPreparedStatementSetter)} and
 * {@link #setKeyMapper(RowMapper)} are thread-safe (true for default values).
 * 
 * @author Lucas Ward
 * 
 * @see DrivingQueryItemReader
 * @see ItemPreparedStatementSetter
 */
public class MultipleColumnJdbcKeyCollector extends ExecutionContextUserSupport implements KeyCollector {

	private static final String CURRENT_KEY = "current.key";

	private JdbcTemplate jdbcTemplate;

	private RowMapper keyMapper = new ColumnMapRowMapper();

	private ItemPreparedStatementSetter preparedStatementSetter = new ColumnMapItemPreparedStatementSetter();

	private String sql;

	private String restartSql;

	public MultipleColumnJdbcKeyCollector() {
		setName(ClassUtils.getShortName(MultipleColumnJdbcKeyCollector.class));
	}

	/**
	 * Construct a new ItemReader.
	 * 
	 * @param jdbcTemplate
	 * @param sql - SQL statement that returns all keys to process. object.
	 */
	public MultipleColumnJdbcKeyCollector(JdbcTemplate jdbcTemplate, String sql) {
		this();
		Assert.notNull(jdbcTemplate, "The JdbcTemplate must not be null.");
		Assert.hasText(sql, "The sql statement must not be null or empty.");
		this.jdbcTemplate = jdbcTemplate;
		this.sql = sql;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.io.sql.scratch.AbstractDrivingQueryItemReader
	 * #retrieveKeys()
	 */
	@SuppressWarnings("unchecked")
	public List<Object> retrieveKeys(ExecutionContext executionContext) {

		Assert.state(keyMapper != null, "KeyMapper must not be null.");
		Assert.state(StringUtils.hasText(restartSql), "The RestartQuery must not be null or empty"
				+ " in order to restart.");

		if (executionContext.size() > 0) {
			Object key = executionContext.get(getKey(CURRENT_KEY));
			return jdbcTemplate.query(restartSql, new PreparedStatementSetterKeyWrapper(key, preparedStatementSetter),
					keyMapper);
		}
		else {
			return jdbcTemplate.query(sql, keyMapper);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.io.driving.KeyGenerator#getKeyAsExecutionContext
	 * (java.lang.Object)
	 */
	public void updateContext(Object key, ExecutionContext executionContext) {
		Assert.notNull(key, "The key must not be null");
		Assert.notNull(executionContext, "The ExecutionContext must not be null");
		executionContext.put(getKey(CURRENT_KEY), key);
	}

	/**
	 * Set the query to use to retrieve keys in order to restore the previous
	 * state for restart.
	 * 
	 * @param restartQuery
	 */
	public void setRestartSql(String restartQuery) {
		this.restartSql = restartQuery;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate, "The JdbcTemplate must not be null.");
		Assert.hasText(sql, "The DrivingQuery must not be null or empty.");
		Assert.notNull(keyMapper, "The key RowMapper must not be null.");
	}

	/**
	 * Set the {@link RowMapper} to be used to map a result set to keys.
	 * 
	 * @param keyMapper
	 */
	public void setKeyMapper(RowMapper keyMapper) {
		this.keyMapper = keyMapper;
	}

	/**
	 * Set the sql statement used to generate the keys list.
	 * 
	 * @param sql
	 */
	public void setSql(String sql) {
		this.sql = sql;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setPreparedStatementSetter(ItemPreparedStatementSetter preparedStatementSetter) {
		this.preparedStatementSetter = preparedStatementSetter;
	}

	private static class PreparedStatementSetterKeyWrapper implements PreparedStatementSetter {

		private Object key;

		private ItemPreparedStatementSetter pss;

		public PreparedStatementSetterKeyWrapper(Object key, ItemPreparedStatementSetter pss) {
			this.key = key;
			this.pss = pss;
		}

		public void setValues(PreparedStatement ps) throws SQLException {
			pss.setValues(key, ps);
		}
	}
}
