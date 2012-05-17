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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.DataSource;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * <p>
 * {@link org.springframework.batch.item.ItemReader} for reading database
 * records using JDBC in a paging fashion.
 * </p>
 * 
 * <p>
 * It executes the SQL built by the {@link PagingQueryProvider} to retrieve
 * requested data. The query is executed using paged requests of a size
 * specified in {@link #setPageSize(int)}. Additional pages are requested when
 * needed as {@link #read()} method is called, returning an object corresponding
 * to current position. On restart it uses the last sort key value to locate the
 * first page to read (so it doesn't matter if the successfully processed itmes
 * have been removed or modified).
 * </p>
 * 
 * <p>
 * The performance of the paging depends on the database specific features
 * available to limit the number of returned rows. Setting a fairly large page
 * size and using a commit interval that matches the page size should provide
 * better performance.
 * </p>
 * 
 * <p>
 * The implementation is thread-safe in between calls to
 * {@link #open(ExecutionContext)}, but remember to use
 * <code>saveState=false</code> if used in a multi-threaded client (no restart
 * available).
 * </p>
 * 
 * @author Thomas Risberg
 * @author Dave Syer
 * @since 2.0
 */
public class JdbcPagingItemReader<T> extends AbstractPagingItemReader<T> implements InitializingBean {

	/**
	 * 
	 */
	private static final String START_AFTER_VALUE = "start.after";

	public static final int VALUE_NOT_SET = -1;

	private DataSource dataSource;

	private PagingQueryProvider queryProvider;

	private Map<String, Object> parameterValues;

	private SimpleJdbcTemplate simpleJdbcTemplate;

	private RowMapper rowMapper;

	private String firstPageSql;

	private String remainingPagesSql;

	private Object startAfterValue;

	private int fetchSize = VALUE_NOT_SET;

	public JdbcPagingItemReader() {
		setName(ClassUtils.getShortName(JdbcPagingItemReader.class));
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Gives the JDBC driver a hint as to the number of rows that should be
	 * fetched from the database when more rows are needed for this
	 * <code>ResultSet</code> object. If the fetch size specified is zero, the
	 * JDBC driver ignores the value.
	 * 
	 * @param fetchSize the number of rows to fetch
	 * @see ResultSet#setFetchSize(int)
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * A {@link PagingQueryProvider}. Supplies all the platform dependent query
	 * generation capabilities needed by the reader.
	 * 
	 * @param queryProvider the {@link PagingQueryProvider} to use
	 */
	public void setQueryProvider(PagingQueryProvider queryProvider) {
		this.queryProvider = queryProvider;
	}

	/**
	 * The row mapper implementation to be used by this reader. The row mapper
	 * is used to convert result set rows into objects, which are then returned
	 * by the reader.
	 * 
	 * @param rowMapper a
	 * {@link org.springframework.jdbc.core.simple.ParameterizedRowMapper}
	 * implementation
	 */
	public void setRowMapper(RowMapper rowMapper) {
		this.rowMapper = rowMapper;
	}

	/**
	 * The parameter values to be used for the query execution. If you use named
	 * parameters then the key should be the name used in the query clause. If
	 * you use "?" placeholders then the key should be the relative index that
	 * the parameter appears in the query string built using the select, from
	 * and where clauses specified.
	 * 
	 * @param parameterValues the values keyed by the parameter named/index used
	 * in the query string.
	 */
	public void setParameterValues(Map<String, Object> parameterValues) {
		this.parameterValues = parameterValues;
	}

	/**
	 * Check mandatory properties.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(dataSource);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		if (fetchSize != VALUE_NOT_SET) {
			jdbcTemplate.setFetchSize(fetchSize);
		}
		jdbcTemplate.setMaxRows(getPageSize());
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(jdbcTemplate);
		Assert.notNull(queryProvider);
		queryProvider.init(dataSource);
		this.firstPageSql = queryProvider.generateFirstPageQuery(getPageSize());
		this.remainingPagesSql = queryProvider.generateRemainingPagesQuery(getPageSize());
	}

	@Override
	protected void doReadPage() {

		if (results == null) {
			results = new CopyOnWriteArrayList<T>();
		}
		else {
			results.clear();
		}

		PagingRowMapper rowCallback = new PagingRowMapper();

		List<?> query;

		if (getPage() == 0) {
			if (logger.isDebugEnabled()) {
				logger.debug("SQL used for reading first page: [" + firstPageSql + "]");
			}
			if (parameterValues != null && parameterValues.size() > 0) {
				if (this.queryProvider.isUsingNamedParameters()) {
					query = simpleJdbcTemplate.getNamedParameterJdbcOperations().query(firstPageSql,
							getParameterMap(parameterValues, null), rowCallback);
				}
				else {
					query = simpleJdbcTemplate.getJdbcOperations().query(firstPageSql,
							getParameterList(parameterValues, null).toArray(), rowCallback);
				}
			}
			else {
				query = simpleJdbcTemplate.getJdbcOperations().query(firstPageSql, rowCallback);
			}

		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("SQL used for reading remaining pages: [" + remainingPagesSql + "]");
			}
			if (this.queryProvider.isUsingNamedParameters()) {
				query = simpleJdbcTemplate.getNamedParameterJdbcOperations().query(remainingPagesSql,
						getParameterMap(parameterValues, startAfterValue), rowCallback);
			}
			else {
				query = simpleJdbcTemplate.getJdbcOperations().query(remainingPagesSql,
						getParameterList(parameterValues, startAfterValue).toArray(), rowCallback);
			}
		}
		
		@SuppressWarnings("unchecked")
		Collection<T> result = (Collection<T>) query;
		results.addAll(result);

	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		super.update(executionContext);
		if (isSaveState() && startAfterValue != null) {
			executionContext.put(getExecutionContextUserSupport().getKey(START_AFTER_VALUE), startAfterValue);
		}
	}

	@Override
	public void open(ExecutionContext executionContext) {
		if (isSaveState()) {
			startAfterValue = executionContext.get(getExecutionContextUserSupport().getKey(START_AFTER_VALUE));
		}
		super.open(executionContext);
	}

	@Override
	protected void doJumpToPage(int itemIndex) {
		/*
		 * Normally this would be false (the startAfterValue is enough
		 * information to restart from.
		 */
		if (startAfterValue == null && getPage() > 0) {

			String jumpToItemSql;
			jumpToItemSql = queryProvider.generateJumpToItemQuery(itemIndex, getPageSize());

			if (logger.isDebugEnabled()) {
				logger.debug("SQL used for jumping: [" + jumpToItemSql + "]");
			}

			RowMapper startMapper = new RowMapper() {
				public Object mapRow(ResultSet rs, int i) throws SQLException {
					return rs.getObject(1);
				}
			};
			if (this.queryProvider.isUsingNamedParameters()) {
				startAfterValue = simpleJdbcTemplate.getNamedParameterJdbcOperations().queryForObject(jumpToItemSql,
						getParameterMap(parameterValues, startAfterValue), startMapper);
			}
			else {
				startAfterValue = simpleJdbcTemplate.getJdbcOperations().queryForObject(jumpToItemSql,
						getParameterList(parameterValues, startAfterValue).toArray(), startMapper);
			}

		}
	}

	private Map<String, Object> getParameterMap(Map<String, Object> values, Object sortKeyValue) {
		Map<String, Object> parameterMap = new LinkedHashMap<String, Object>();
		if (values != null) {
			parameterMap.putAll(values);
		}
		if (sortKeyValue != null) {
			parameterMap.put("_sortKey", sortKeyValue);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Using parameterMap:" + parameterMap);
		}
		return parameterMap;
	}

	private List<Object> getParameterList(Map<String, Object> values, Object sortKeyValue) {
		SortedMap<String, Object> sm = new TreeMap<String, Object>();
		if (values != null) {
			sm.putAll(values);
		}
		List<Object> parameterList = new ArrayList<Object>();
		parameterList.addAll(sm.values());
		if (sortKeyValue != null) {
			parameterList.add(sortKeyValue);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Using parameterList:" + parameterList);
		}
		return parameterList;
	}

	private class PagingRowMapper implements RowMapper {
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			startAfterValue = rs.getObject(queryProvider.getSortKeyWithoutAlias());
			return rowMapper.mapRow(rs, rowNum);
		}
	}

}
