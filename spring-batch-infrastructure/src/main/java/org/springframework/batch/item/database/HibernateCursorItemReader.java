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

import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ItemReader} for reading database records built on top of Hibernate. It
 * executes the HQL query when initialized iterates over the result set as
 * {@link #read()} method is called, returning an object corresponding to
 * current row. The query can be set directly using
 * {@link #setQueryString(String)} or a named query can be used by
 * {@link #setQueryName(String)}.
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
public class HibernateCursorItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> implements ItemStream,
		InitializingBean {

	private SessionFactory sessionFactory;

	private StatelessSession statelessSession;

	private Session statefulSession;

	private ScrollableResults cursor;

	private String queryString = "";

	private String queryName = "";

	private boolean useStatelessSession = true;

	private boolean initialized = false;

	private int fetchSize = 0;

	public HibernateCursorItemReader() {
		setName(ClassUtils.getShortName(HibernateCursorItemReader.class));
	}

	/**
	 * Open appropriate type of hibernate session and create the query.
	 */
	private Query createQuery() {
		if (useStatelessSession) {
			statelessSession = sessionFactory.openStatelessSession();
			if (StringUtils.hasText(queryName)) {
				return statelessSession.getNamedQuery(queryName);
			}
			else {
				return statelessSession.createQuery(queryString);
			}
		}
		else {
			statefulSession = sessionFactory.openSession();
			if (StringUtils.hasText(queryName)) {
				return statefulSession.getNamedQuery(queryName);
			}
			else {
				return statefulSession.createQuery(queryString);
			}
		}
	}

	/**
	 * @param sessionFactory hibernate session factory
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(sessionFactory, "session factory must be set");
		Assert.isTrue(fetchSize >= 0, "fetchSize must not be negative");
		Assert.isTrue(StringUtils.hasText(queryString) ^ StringUtils.hasText(queryName),
				"exactly one of queryString or queryName must be set");
	}

	/**
	 * @param queryName name of a hibernate named query
	 */
	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

	/**
	 * @param queryString HQL query string
	 */
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	/**
	 * Can be set only in uninitialized state.
	 * 
	 * @param useStatelessSession <code>true</code> to use
	 * {@link StatelessSession} <code>false</code> to use standard hibernate
	 * {@link Session}
	 */
	public void setUseStatelessSession(boolean useStatelessSession) {
		Assert.state(!initialized);
		this.useStatelessSession = useStatelessSession;
	}

	/**
	 * Clears the session if not stateful and delegates to super class.
	 */
	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		super.update(executionContext);
		if (!useStatelessSession) {
			statefulSession.clear();
		}
	}

	/**
	 * Gives the JDBC driver a hint as to the number of rows that should be
	 * fetched from the database when more rows are needed for this
	 * <code>ResultSet</code> object. If the fetch size specified is zero, the
	 * JDBC driver ignores the value.
	 * 
	 * @param fetchSize the number of rows to fetch, 0 by default
	 * @see Query#setFetchSize(int)
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

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
	 * Open hibernate session and create a forward-only cursor for the
	 * {@link #setQueryString(String)}.
	 */
	protected void doOpen() throws Exception {
		Assert.state(!initialized, "Cannot open an already opened ItemReader, call close first");

		cursor = createQuery().setFetchSize(fetchSize).scroll(ScrollMode.FORWARD_ONLY);

		initialized = true;

	}

	/**
	 * Close the cursor and hibernate session.
	 */
	protected void doClose() throws Exception {
		initialized = false;

		if (cursor != null) {
			cursor.close();
		}
		if (useStatelessSession) {
			if (statelessSession != null) {
				statelessSession.close();
			}
		}
		else {
			if (statefulSession != null) {
				statefulSession.close();
			}
		}

	}
}
