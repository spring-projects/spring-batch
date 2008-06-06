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
import org.springframework.batch.item.AbstractBufferedItemReaderItemStream;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link ItemReader} for reading database records built on top of Hibernate.
 * 
 * It executes the HQL {@link #setQueryString(String)} when initialized and
 * iterates over the result set as {@link #read()} method is called, returning
 * an object corresponding to current row.
 * 
 * The reader can be configured to use either {@link StatelessSession}
 * sufficient for simple mappings without the need to cascade to associated
 * objects or standard hibernate {@link Session} for more advanced mappings or
 * when caching is desired.
 * 
 * When stateful session is used it will be cleared after successful commit
 * without being flushed (no inserts or updates are expected).
 * 
 * Reset(rollback) functionality is implemented by item buffering allowing the
 * cursor used to be forward-only.
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class HibernateCursorItemReader extends AbstractBufferedItemReaderItemStream implements ItemStream,
		InitializingBean {

	private SessionFactory sessionFactory;

	private StatelessSession statelessSession;

	private Session statefulSession;

	private ScrollableResults cursor;

	private String queryString;

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
			return statelessSession.createQuery(queryString);
		}
		else {
			statefulSession = sessionFactory.openSession();
			return statefulSession.createQuery(queryString);
		}
	}

	/**
	 * @param sessionFactory hibernate session factory
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(sessionFactory);
		Assert.hasLength(queryString);
		Assert.isTrue(fetchSize >= 0, "fetchSize must not be negative");
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
	 * Mark is supported as long as this {@link ItemStream} is used in a
	 * single-threaded environment. The state backing the mark is a single
	 * counter, keeping track of the current position, so multiple threads
	 * cannot be accommodated.
	 * 
	 * @see org.springframework.batch.item.ItemReader#mark()
	 */
	public void mark() {

		super.mark();

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

	protected Object doRead() throws Exception {
		if (cursor.next()) {
			Object[] data = cursor.get();
			Object item;

			if (data.length > 1) {
				item = data;
			}
			else {
				item = data[0];
			}

			return item;
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
