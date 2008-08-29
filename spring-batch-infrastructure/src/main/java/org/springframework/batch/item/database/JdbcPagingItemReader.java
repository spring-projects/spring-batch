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

import org.springframework.batch.item.database.support.PagingQueryProvider;
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
		//TODO: add support for parameter map

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
			simpleJdbcTemplate.getJdbcOperations().query(firstPageSql,
					new RowCallbackHandler() {
						public void processRow(ResultSet rs) throws SQLException {
							startAfterValue = rs.getObject(1);
							results.add(parameterizedRowMapper.mapRow(rs, results.size()));
						}
					});
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("SQL used for reading remaining pages: [" + remainingPagesSql + "]");
			}
			simpleJdbcTemplate.getJdbcOperations().query(remainingPagesSql,
					new Object[] {startAfterValue},
					new RowCallbackHandler() {
						public void processRow(ResultSet rs) throws SQLException {
							startAfterValue = rs.getObject(1);
							results.add(parameterizedRowMapper.mapRow(rs, results.size()));
						}
					});
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

}
