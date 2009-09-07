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
import org.springframework.batch.item.ExecutionContext;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * <p>
 * {@link org.springframework.batch.item.ItemReader} for reading database
 * records using iBATIS in a paging fashion.
 * </p>
 * 
 * <p>
 * It executes the query specified as the {@link #setQueryId(String)} to
 * retrieve requested data. The query is executed using paged requests of a size
 * specified in {@link #setPageSize(int)}. Additional pages are requested when
 * needed as {@link #read()} method is called, returning an object corresponding
 * to current position. Some standard query parameters are provided by the
 * reader and the SQL in the named query must use some or all of these parameters
 * (depending on the SQL variant) to construct a result set of the required
 * size. The parameters are:
 * <ul>
 * <li><code>_page</code>: the page number to be read (starting at 0)</li>
 * <li><code>_pagesize</code>: the size of the pages, i.e. the number of rows to
 * return</li>
 * <li><code>_skiprows</code>: the product of <code>_page</code> and
 * <code>_pagesize</code></li>
 * </ul>
 * Failure to write the correct platform-specific SQL often results in an
 * infinite loop in the reader because it keeps asking for the next page and
 * gets the same result set over and over.
 * </p>
 * 
 * <p>
 * The performance of the paging depends on the iBATIS implementation.
 * Setting a fairly large page size and using a commit interval that matches the
 * page size should provide better performance.
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
	 * @param parameterValues the values keyed by the parameter named used in
	 * the query string.
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
		parameters.put("_page", getPage());
		parameters.put("_pagesize", getPageSize());
		parameters.put("_skiprows", getPage() * getPageSize());
		if (results == null) {
			results = new CopyOnWriteArrayList<T>();
		}
		else {
			results.clear();
		}
		results.addAll(sqlMapClientTemplate.queryForList(queryId, parameters));
	}

	@Override
	protected void doJumpToPage(int itemIndex) {
	}

}
