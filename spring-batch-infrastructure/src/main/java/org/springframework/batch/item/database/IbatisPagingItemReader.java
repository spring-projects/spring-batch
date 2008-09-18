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

import org.springframework.util.ClassUtils;
import org.springframework.util.Assert;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import java.util.Map;
import java.util.HashMap;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * {@link org.springframework.batch.item.ItemReader} for reading database records using iBATIS in a paging
 * fashion.
 *
 * It executes the query specified as the {@link #setQueryId(String)} to retrieve requested data.
 * The query is executed using paged requests of a size specified in {@link #setPageSize(int)}.
 * Additional pages are requested when needed as {@link #read()} method is called, returning an
 * object corresponding to current position.
 *
 * The performance of the paging depends on the iBATIS implementation.
 *
 * Setting a fairly large page size and using a commit interval that matches the page size should provide
 * better performance.
 *
 * The implementation is *not* thread-safe.
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public class IbatisPagingItemReader<T> extends AbstractPagingItemReader<T> {

	private SqlMapClient sqlMapClient;

	private String queryId;

	private SqlMapClientTemplate sqlMapClientTemplate;

	private Map<String, Object> parameterValues;

	public IbatisPagingItemReader() {
		setName(ClassUtils.getShortName(IbatisPagingItemReader.class));
	}

	public void setSqlMapClient(SqlMapClient sqlMapClient) {
		this.sqlMapClient = sqlMapClient;
	}

	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}

	/**
	 * The parameter values to be used for the query execution.
	 *
	 * @param parameterValues the values keyed by the parameter named used in the query string.
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
		Assert.notNull(sqlMapClient);
		sqlMapClientTemplate = new SqlMapClientTemplate(sqlMapClient);
		Assert.notNull(queryId);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void doReadPage() {
		Map<String, Object> parameters = new HashMap<String, Object>();
		if (parameterValues != null) {
			parameters.putAll(parameterValues);
		}
		results = sqlMapClientTemplate.queryForList(queryId, parameters, (page * pageSize), pageSize);
	}

	@Override
	protected void doJumpToPage(int itemIndex) {
	}

}
