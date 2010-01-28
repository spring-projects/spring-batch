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
package org.springframework.batch.item.database;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.database.orm.HibernateQueryProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link ItemReader} for reading database records built on top of Hibernate and
 * reading only up to a fixed number of items at a time. It executes an HQL
 * query when initialized is paged as the {@link #read()} method is called. The
 * query can be set directly using {@link #setQueryString(String)}, a named
 * query can be used by {@link #setQueryName(String)}, or a query provider
 * strategy can be supplied via
 * {@link #setQueryProvider(HibernateQueryProvider)}.
 * 
 * <p>
 * The reader can be configured to use either {@link StatelessSession}
 * sufficient for simple mappings without the need to cascade to associated
 * objects or standard hibernate {@link Session} for more advanced mappings or
 * when caching is desired. When stateful session is used it will be cleared in
 * the {@link #update(ExecutionContext)} method without being flushed (no data
 * modifications are expected).
 * </p>
 * 
 * <p>
 * The implementation is thread-safe in between calls to
 * {@link #open(ExecutionContext)}, but remember to use
 * <code>saveState=false</code> if used in a multi-threaded client (no restart
 * available).
 * </p>
 * 
 * @author Dave Syer
 * 
 * @since 2.1
 */
public class HibernatePagingItemReader<T> extends AbstractPagingItemReader<T> implements ItemStream, InitializingBean {

	private HibernateItemReaderHelper<T> helper = new HibernateItemReaderHelper<T>();

	private Map<String, Object> parameterValues;

	private int fetchSize;

	public HibernatePagingItemReader() {
		setName(ClassUtils.getShortName(HibernatePagingItemReader.class));
	}

	/**
	 * The parameter values to apply to a query (map of name:value).
	 * 
	 * @param parameterValues the parameter values to set
	 */
	public void setParameterValues(Map<String, Object> parameterValues) {
		this.parameterValues = parameterValues;
	}

	/**
	 * A query name for an externalized query. Either this or the {
	 * {@link #setQueryString(String) query string} or the {
	 * {@link #setQueryProvider(HibernateQueryProvider) query provider} should
	 * be set.
	 * 
	 * @param queryName name of a hibernate named query
	 */
	public void setQueryName(String queryName) {
		helper.setQueryName(queryName);
	}

	/**
	 * Fetch size used internally by Hibernate to limit amount of data fetched
	 * from database per round trip.
	 * 
	 * @param fetchSize the fetch size to pass down to Hibernate
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * A query provider. Either this or the {{@link #setQueryString(String)
	 * query string} or the {{@link #setQueryName(String) query name} should be
	 * set.
	 * 
	 * @param queryProvider Hibernate query provider
	 */
	public void setQueryProvider(HibernateQueryProvider queryProvider) {
		helper.setQueryProvider(queryProvider);
	}

	/**
	 * A query string in HQL. Either this or the {
	 * {@link #setQueryProvider(HibernateQueryProvider) query provider} or the {
	 * {@link #setQueryName(String) query name} should be set.
	 * 
	 * @param queryString HQL query string
	 */
	public void setQueryString(String queryString) {
		helper.setQueryString(queryString);
	}

	/**
	 * The Hibernate SessionFactory to use the create a session.
	 * 
	 * @param sessionFactory the {@link SessionFactory} to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		helper.setSessionFactory(sessionFactory);
	}

	/**
	 * Can be set only in uninitialized state.
	 * 
	 * @param useStatelessSession <code>true</code> to use
	 * {@link StatelessSession} <code>false</code> to use standard hibernate
	 * {@link Session}
	 */
	public void setUseStatelessSession(boolean useStatelessSession) {
		helper.setUseStatelessSession(useStatelessSession);
	}

	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.state(fetchSize >= 0, "fetchSize must not be negative");
		helper.afterPropertiesSet();
	}

	@Override
	protected void doOpen() throws Exception {
		super.doOpen();
	}

	@Override
	protected void doReadPage() {

		if (results == null) {
			results = new CopyOnWriteArrayList<T>();
		}
		else {
			results.clear();
		}

		results.addAll(helper.readPage(getPage(), getPageSize(), fetchSize, parameterValues));

	}

	@Override
	protected void doJumpToPage(int itemIndex) {
	}

	@Override
	protected void doClose() throws Exception {
		helper.close();
		super.doClose();
	}

}
