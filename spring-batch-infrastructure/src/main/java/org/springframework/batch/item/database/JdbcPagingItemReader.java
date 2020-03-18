/*
 * Copyright 2006-2018 the original author or authors.
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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
 * first page to read (so it doesn't matter if the successfully processed items
 * have been removed or modified). It is important to have a unique key constraint
 * on the sort key to guarantee that no data is lost between executions.
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
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class JdbcPagingItemReader<T> extends AbstractPagingItemReader<T> implements InitializingBean {
	private static final String START_AFTER_VALUE = "start.after";

	public static final int VALUE_NOT_SET = -1;

	private DataSource dataSource;

	private PagingQueryProvider queryProvider;

	private Map<String, Object> parameterValues;

	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private RowMapper<T> rowMapper;

	private String firstPageSql;

	private String remainingPagesSql;

	private Map<String, Object> startAfterValues;
	
	private Map<String, Object> previousStartAfterValues;

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
	 * {@link RowMapper}
	 * implementation
	 */
	public void setRowMapper(RowMapper<T> rowMapper) {
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
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(dataSource, "DataSource may not be null");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		if (fetchSize != VALUE_NOT_SET) {
			jdbcTemplate.setFetchSize(fetchSize);
		}
		jdbcTemplate.setMaxRows(getPageSize());
		namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		Assert.notNull(queryProvider, "QueryProvider may not be null");
		queryProvider.init(dataSource);
		this.firstPageSql = queryProvider.generateFirstPageQuery(getPageSize());
		this.remainingPagesSql = queryProvider.generateRemainingPagesQuery(getPageSize());
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void doReadPage() {
		if (results == null) {
			results = new CopyOnWriteArrayList<>();
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
					query = namedParameterJdbcTemplate.query(firstPageSql,
							getParameterMap(parameterValues, null), rowCallback);
				}
				else {
					query = getJdbcTemplate().query(firstPageSql,
							getParameterList(parameterValues, null).toArray(), rowCallback);
				}
			}
			else {
				query = getJdbcTemplate().query(firstPageSql, rowCallback);
			}

		}
		else {
			previousStartAfterValues = startAfterValues;
			if (logger.isDebugEnabled()) {
				logger.debug("SQL used for reading remaining pages: [" + remainingPagesSql + "]");
			}
			if (this.queryProvider.isUsingNamedParameters()) {
				query = namedParameterJdbcTemplate.query(remainingPagesSql,
						getParameterMap(parameterValues, startAfterValues), rowCallback);
			}
			else {
				query = getJdbcTemplate().query(remainingPagesSql,
						getParameterList(parameterValues, startAfterValues).toArray(), rowCallback);
			}
		}

		Collection<T> result = (Collection<T>) query;
		results.addAll(result);
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		super.update(executionContext);
		if (isSaveState()) {
			if (isAtEndOfPage() && startAfterValues != null) {
				// restart on next page
				executionContext.put(getExecutionContextKey(START_AFTER_VALUE), startAfterValues);	
			} else if (previousStartAfterValues != null) {
				// restart on current page
				executionContext.put(getExecutionContextKey(START_AFTER_VALUE), previousStartAfterValues);
			}
		}
	}
	
	private boolean isAtEndOfPage() {
		return getCurrentItemCount() % getPageSize() == 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void open(ExecutionContext executionContext) {
		if (isSaveState()) {
			startAfterValues = (Map<String, Object>) executionContext.get(getExecutionContextKey(START_AFTER_VALUE));

			if(startAfterValues == null) {
				startAfterValues = new LinkedHashMap<>();
			}
		}

		super.open(executionContext);
	}

	@Override
	protected void doJumpToPage(int itemIndex) {
		/*
		 * Normally this would be false (the startAfterValue is enough
		 * information to restart from.
		 */
		// TODO: this is dead code, startAfterValues is never null - see #open(ExecutionContext)
		if (startAfterValues == null && getPage() > 0) {

			String jumpToItemSql = queryProvider.generateJumpToItemQuery(itemIndex, getPageSize());

			if (logger.isDebugEnabled()) {
				logger.debug("SQL used for jumping: [" + jumpToItemSql + "]");
			}
			
			if (this.queryProvider.isUsingNamedParameters()) {
				startAfterValues = namedParameterJdbcTemplate.queryForMap(jumpToItemSql, getParameterMap(parameterValues, null));
			}
			else {
				startAfterValues = getJdbcTemplate().queryForMap(jumpToItemSql, getParameterList(parameterValues, null).toArray());
			}
		}
	}

	private Map<String, Object> getParameterMap(Map<String, Object> values, Map<String, Object> sortKeyValues) {
		Map<String, Object> parameterMap = new LinkedHashMap<>();
		if (values != null) {
			parameterMap.putAll(values);
		}
		if (sortKeyValues != null && !sortKeyValues.isEmpty()) {
			for (Map.Entry<String, Object> sortKey : sortKeyValues.entrySet()) {
				parameterMap.put("_" + sortKey.getKey(), sortKey.getValue());
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Using parameterMap:" + parameterMap);
		}
		return parameterMap;
	}

	private List<Object> getParameterList(Map<String, Object> values, Map<String, Object> sortKeyValue) {
		SortedMap<String, Object> sm = new TreeMap<>();
		if (values != null) {
			sm.putAll(values);
		}
		List<Object> parameterList = new ArrayList<>();
		parameterList.addAll(sm.values());
		if (sortKeyValue != null && sortKeyValue.size() > 0) {
			List<Map.Entry<String, Object>> keys = new ArrayList<>(sortKeyValue.entrySet());

			for(int i = 0; i < keys.size(); i++) {
				for(int j = 0; j < i; j++) {
					parameterList.add(keys.get(j).getValue());
				}

				parameterList.add(keys.get(i).getValue());
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Using parameterList:" + parameterList);
		}
		return parameterList;
	}

	private class PagingRowMapper implements RowMapper<T> {
		@Override
		public T mapRow(ResultSet rs, int rowNum) throws SQLException {
			startAfterValues = new LinkedHashMap<>();
			for (Map.Entry<String, Order> sortKey : queryProvider.getSortKeys().entrySet()) {
				startAfterValues.put(sortKey.getKey(), rs.getObject(sortKey.getKey()));
			}

			return rowMapper.mapRow(rs, rowNum);
		}
	}

	private JdbcTemplate getJdbcTemplate() {
		return (JdbcTemplate) namedParameterJdbcTemplate.getJdbcOperations();
	}
}
