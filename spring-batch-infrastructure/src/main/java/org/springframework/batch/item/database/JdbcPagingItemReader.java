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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.Assert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link org.springframework.batch.item.ItemReader} for reading database records using JDBC in a paging
 * fashion.
 *
 * It executes the SQL built by the {@link PagingQueryProvider} to retrieve requested data.
 * The query is executed using paged requests of a size specified in {@link #setPageSize(int)}.
 * Additional pages are requested when needed as {@link #read()} method is called, returning an
 * object corresponding to current position.
 *
 * The performance of the paging depends on the database specific features available to limit the number
 * of returned rows.
 *
 * Setting a fairly large page size and using a commit interval that matches the page size should provide
 * better performance.
 *
 * The implementation is *not* thread-safe.
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public class JdbcPagingItemReader<T> extends AbstractPagingItemReader<T> implements InitializingBean {

	private DataSource dataSource;

	private PagingQueryProvider queryProvider;

	private Map<String, Object> parameterValues;

	private SimpleJdbcTemplate simpleJdbcTemplate;

	private ParameterizedRowMapper<T> parameterizedRowMapper;

	private String firstPageSql;

	private String remainingPagesSql;

	private Object startAfterValue;

	public JdbcPagingItemReader() {
		setName(ClassUtils.getShortName(JdbcPagingItemReader.class));
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setQueryProvider(PagingQueryProvider queryProvider) {
		this.queryProvider = queryProvider;
	}

	/**
	 * The row mapper implementation to be used by this reader
	 *
	 * @param parameterizedRowMapper a {@link org.springframework.jdbc.core.simple.ParameterizedRowMapper} implementation
	 */
	public void setParameterizedRowMapper(ParameterizedRowMapper<T> parameterizedRowMapper) {
		this.parameterizedRowMapper = parameterizedRowMapper;
	}

	/**
	 * The parameter values to be used for the query execution.  If you use named parameters then the
	 * key should be the name used in the query clause.  If you use "?" placeholders then the key should be
	 * the relative index that the parameter appears in the query string built using the select, from and
	 * where cluases specified.
	 *
	 * @param parameterValues the values keyed by the parameter named/index used in the query string.
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
		jdbcTemplate.setMaxRows(pageSize);
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(jdbcTemplate);
		Assert.notNull(queryProvider);
		queryProvider.init(dataSource);
		this.firstPageSql = queryProvider.generateFirstPageQuery(pageSize);
		this.remainingPagesSql = queryProvider.generateRemainingPagesQuery(pageSize);
	}

	@Override
	protected void doReadPage() {

		if (results == null) {
			results = new ArrayList<T>();
		}
		else {
			results.clear();
		}

		if (page == 0) {
			if (logger.isDebugEnabled()) {
				logger.debug("SQL used for reading first page: [" + firstPageSql + "]");
			}
			if (parameterValues != null && parameterValues.size() > 0) {
				if (this.queryProvider.isUsingNamedParameters()) {
					simpleJdbcTemplate.getNamedParameterJdbcOperations().query(firstPageSql,
							getParameterMap(parameterValues, null),
							new RowCallbackHandler() {
								public void processRow(ResultSet rs) throws SQLException {
									startAfterValue = rs.getObject(1);
									results.add(parameterizedRowMapper.mapRow(rs, results.size()));
								}
							});
				}
				else {
					simpleJdbcTemplate.getJdbcOperations().query(firstPageSql,
							getParameterList(parameterValues, null).toArray(),
							new PagingRowCallbackHandler());
				}
			}
			else {
				simpleJdbcTemplate.getJdbcOperations().query(firstPageSql,
						new PagingRowCallbackHandler());
			}

		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("SQL used for reading remaining pages: [" + remainingPagesSql + "]");
			}
			if (this.queryProvider.isUsingNamedParameters()) {
				simpleJdbcTemplate.getNamedParameterJdbcOperations().query(remainingPagesSql,
						getParameterMap(parameterValues, startAfterValue),
						new PagingRowCallbackHandler());
			}
			else {
				simpleJdbcTemplate.getJdbcOperations().query(remainingPagesSql,
						getParameterList(parameterValues, startAfterValue).toArray(),
						new PagingRowCallbackHandler());
			}
		}

	}

	@Override
	protected void doJumpToPage(int itemIndex) {

		if (page > 0) {

			String jumpToItemSql;
			jumpToItemSql = queryProvider.generateJumpToItemQuery(itemIndex, pageSize);

			if (logger.isDebugEnabled()) {
				logger.debug("SQL used for jumping: [" + jumpToItemSql + "]");
			}

			startAfterValue = simpleJdbcTemplate.getJdbcOperations().queryForObject(jumpToItemSql,
					new RowMapper() {
						public Object mapRow(ResultSet rs, int i) throws SQLException {
							 return rs.getObject(1);
						}
					});

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

	private class PagingRowCallbackHandler implements RowCallbackHandler {
		public void processRow(ResultSet rs) throws SQLException {
			startAfterValue = rs.getObject(1);
			results.add(parameterizedRowMapper.mapRow(rs, results.size()));
		}
	}

}
