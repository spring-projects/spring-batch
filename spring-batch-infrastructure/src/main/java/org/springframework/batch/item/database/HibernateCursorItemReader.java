/*
 * Copyright 2006-2019 the original author or authors.
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

import java.util.Map;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.database.orm.HibernateQueryProvider;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link ItemStreamReader} for reading database records built on top of Hibernate. It
 * executes the HQL query when initialized iterates over the result set as
 * {@link #read()} method is called, returning an object corresponding to
 * current row. The query can be set directly using
 * {@link #setQueryString(String)}, a named query can be used by
 * {@link #setQueryName(String)}, or a query provider strategy can be supplied
 * via {@link #setQueryProvider(HibernateQueryProvider)}.
 *
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
 * The implementation is <b>not</b> thread-safe.
 *
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class HibernateCursorItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> 
        implements InitializingBean {

	private HibernateItemReaderHelper<T> helper = new HibernateItemReaderHelper<>();

	public HibernateCursorItemReader() {
		setName(ClassUtils.getShortName(HibernateCursorItemReader.class));
	}

	private ScrollableResults cursor;

	private boolean initialized = false;

	private int fetchSize;

	private Map<String, Object> parameterValues;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(fetchSize >= 0, "fetchSize must not be negative");
		helper.afterPropertiesSet();
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
	public void setQueryProvider(HibernateQueryProvider<T> queryProvider) {
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

	@Nullable
	@Override
	protected T doRead() throws Exception {
		if (cursor.next()) {
			Object[] data = cursor.get();

			if (data.length > 1) {
				// If there are multiple items this must be a projection
				// and T is an array type.
				@SuppressWarnings("unchecked")
				T item = (T) data;
				return item;
			}
			else {
				// Assume if there is only one item that it is the data the user
				// wants.
				// If there is only one item this is going to be a nasty shock
				// if T is an array type but there's not much else we can do...
				@SuppressWarnings("unchecked")
				T item = (T) data[0];
				return item;
			}

		}
		return null;
	}

	/**
	 * Open hibernate session and create a forward-only cursor for the query.
	 */
	@Override
	protected void doOpen() throws Exception {
		Assert.state(!initialized, "Cannot open an already opened ItemReader, call close first");
		cursor = helper.getForwardOnlyCursor(fetchSize, parameterValues);
		initialized = true;
	}

	/**
	 * Update the context and clear the session if stateful.
	 *
	 * @param executionContext the current {@link ExecutionContext}
	 * @throws ItemStreamException if there is a problem
	 */
	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		super.update(executionContext);
		helper.clear();
	}

	/**
	 * Wind forward through the result set to the item requested. Also clears
	 * the session every now and then (if stateful) to avoid memory problems.
	 * The frequency of session clearing is the larger of the fetch size (if
	 * set) and 100.
	 *
	 * @param itemIndex the first item to read
	 * @throws Exception if there is a problem
	 * @see AbstractItemCountingItemStreamItemReader#jumpToItem(int)
	 */
	@Override
	protected void jumpToItem(int itemIndex) throws Exception {
		int flushSize = Math.max(fetchSize, 100);
		helper.jumpToItem(cursor, itemIndex, flushSize);
	}

	/**
	 * Close the cursor and hibernate session.
	 */
	@Override
	protected void doClose() throws Exception {

		if(initialized) {
			if (cursor != null) {
				cursor.close();
			}

			helper.close();
		}

		initialized = false;
	}
}
