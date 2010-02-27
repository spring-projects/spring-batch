/*
 * Copyright 2006-2010 the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.item.database.orm.HibernateQueryProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Internal shared state helper for hibernate readers managing sessions and
 * queries.
 * 
 * @author Dave Syer
 * 
 */
public class HibernateItemReaderHelper<T> implements InitializingBean {

	private SessionFactory sessionFactory;

	private String queryString = "";

	private String queryName = "";

	private HibernateQueryProvider queryProvider;

	private boolean useStatelessSession = true;

	private StatelessSession statelessSession;

	private Session statefulSession;

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
	 * @param queryProvider Hibernate query provider
	 */
	public void setQueryProvider(HibernateQueryProvider queryProvider) {
		this.queryProvider = queryProvider;
	}

	/**
	 * Can be set only in uninitialized state.
	 * 
	 * @param useStatelessSession <code>true</code> to use
	 * {@link StatelessSession} <code>false</code> to use standard hibernate
	 * {@link Session}
	 */
	public void setUseStatelessSession(boolean useStatelessSession) {
		Assert.state(statefulSession == null && statelessSession == null,
				"The useStatelessSession flag can only be set before a session is initialized.");
		this.useStatelessSession = useStatelessSession;
	}

	/**
	 * @param sessionFactory hibernate session factory
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void afterPropertiesSet() throws Exception {

		Assert.state(sessionFactory != null, "A SessionFactory must be provided");

		if (queryProvider == null) {
			Assert.notNull(sessionFactory, "session factory must be set");
			Assert.state(StringUtils.hasText(queryString) ^ StringUtils.hasText(queryName),
					"queryString or queryName must be set");
		}
		// making sure that the appropriate (Hibernate) query provider is set
		else {
			Assert.state(queryProvider != null, "Hibernate query provider must be set");
		}

	}

	/**
	 * Get a cursor over all of the results, with the forward-only flag set.
	 * 
	 * @param fetchSize the fetch size to use retrieving the results
	 * @param parameterValues the parameter values to use (or null if none).
	 * 
	 * @return a forward-only {@link ScrollableResults}
	 */
	public ScrollableResults getForwardOnlyCursor(int fetchSize, Map<String, Object> parameterValues) {
		Query query = createQuery();
		if (parameterValues != null) {
			query.setProperties(parameterValues);
		}
		return query.setFetchSize(fetchSize).scroll(ScrollMode.FORWARD_ONLY);
	}

	/**
	 * Open appropriate type of hibernate session and create the query.
	 */
	public Query createQuery() {

		if (useStatelessSession) {
			if (statelessSession == null) {
				statelessSession = sessionFactory.openStatelessSession();
			}
			if (queryProvider != null) {
				queryProvider.setStatelessSession(statelessSession);
			}
			else {
				if (StringUtils.hasText(queryName)) {
					return statelessSession.getNamedQuery(queryName);
				}
				else {
					return statelessSession.createQuery(queryString);
				}
			}
		}
		else {
			if (statefulSession == null) {
				statefulSession = sessionFactory.openSession();
			}
			if (queryProvider != null) {
				queryProvider.setSession(statefulSession);
			}
			else {
				if (StringUtils.hasText(queryName)) {
					return statefulSession.getNamedQuery(queryName);
				}
				else {
					return statefulSession.createQuery(queryString);
				}
			}
		}

		// If queryProvider is set use it to create a query
		return queryProvider.createQuery();

	}

	/**
	 * Scroll through the results up to the item specified.
	 * 
	 * @param cursor the results to scroll over
	 */
	public void jumpToItem(ScrollableResults cursor, int itemIndex, int flushInterval) {
		for (int i = 0; i < itemIndex; i++) {
			cursor.next();
			if (i % flushInterval == 0 && !useStatelessSession) {
				statefulSession.clear(); // Clears in-memory cache
			}
		}
	}

	/**
	 * Close the open session (stateful or otherwise).
	 */
	public void close() {
		if (statelessSession != null) {
			statelessSession.close();
			statelessSession = null;
		}
		if (statefulSession != null) {
			statefulSession.close();
			statefulSession = null;
		}
	}

	/**
	 * Read a page of data, clearing the existing session (if necessary) first,
	 * and creating a new session before executing the query.
	 * 
	 * @param page the page to read (starting at 0)
	 * @param pageSize the size of the page or maximum number of items to read
	 * @param fetchSize the fetch size to use
	 * @param parameterValues the parameter values to use (if any, otherwise
	 * null)
	 * @return a collection of items
	 */
	public Collection<? extends T> readPage(int page, int pageSize, int fetchSize, Map<String, Object> parameterValues) {

		clear();

		Query query = createQuery();
		if (parameterValues != null) {
			query.setProperties(parameterValues);
		}
		@SuppressWarnings("unchecked")
		List<T> result = query.setFetchSize(fetchSize).setFirstResult(page * pageSize).setMaxResults(pageSize).list();
		return result;

	}

	/**
	 * Clear the session if stateful.
	 */
	public void clear() {
		if (statefulSession != null) {
			statefulSession.clear();
		}
	}

}
