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
import org.springframework.batch.item.database.support.AbstractHibernateQueryProvider;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Internal shared state helper for hibernate readers.
 * 
 * @author Dave Syer
 * 
 */
class HibernateItemReaderHelper<T> {

	private SessionFactory sessionFactory;

	private String queryString = "";

	private String queryName = "";

	private Map<String, Object> parameterValues;

	private HibernateQueryProvider queryProvider;

	private boolean useStatelessSession = true;

	private int fetchSize = 0;

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
	 * The parameter values to apply to a query (map of name:value).
	 * 
	 * @param parameterValues the parameter values to set
	 */
	public void setParameterValues(Map<String, Object> parameterValues) {
		this.parameterValues = parameterValues;
	}

	/**
	 * @param sessionFactory hibernate session factory
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void afterPropertiesSet() throws Exception {

		Assert.state(sessionFactory != null, "A SessionFactory must be provided");
		Assert.state(fetchSize >= 0, "fetchSize must not be negative");

		if (queryProvider == null) {
			Assert.notNull(sessionFactory, "session factory must be set");
			Assert.state(StringUtils.hasText(queryString) ^ StringUtils.hasText(queryName),
					"queryString or queryName must be set");
		}
		// making sure that the appropriate (Hibernate) query provider is set
		else {
			Assert.state(queryProvider instanceof AbstractHibernateQueryProvider,
					"Hibernate query provider must be set");
		}

	}

	/**
	 * Get a cursor over all of the results, with the forward-only flag set.
	 * 
	 * @return a forward-only {@link ScrollableResults}
	 */
	public ScrollableResults getForwardOnlyCursor() {
		Query query = createQuery();
		if (parameterValues != null) {
			query.setProperties(parameterValues);
		}
		return query.setFetchSize(fetchSize).scroll(ScrollMode.FORWARD_ONLY);
	}

	/**
	 * Open appropriate type of hibernate session and create the query.
	 */
	private Query createQuery() {

		if (useStatelessSession) {
			statelessSession = sessionFactory.openStatelessSession();
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
			statefulSession = sessionFactory.openSession();
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

		// if queryProvider is set use it to create a query
		return queryProvider.createQuery();

	}

	/**
	 * Scroll through the results up to the item specified.
	 * 
	 * @param cursor the results to scroll over
	 */
	public void jumpToItem(ScrollableResults cursor, int itemIndex) {
		int flushSize = Math.max(fetchSize, 100);
		for (int i = 0; i < itemIndex; i++) {
			cursor.next();
			if (i % flushSize == 0 && !useStatelessSession) {
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
		}
		if (statefulSession != null) {
			statefulSession.close();
		}
	}

	/**
	 * @param page the page to read
	 * @param pageSize the size of the page
	 * @return a collection of items
	 */
	public Collection<? extends T> readPage(int page, int pageSize) {

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
		if (statefulSession!=null) {
			statefulSession.clear();
		}
	}

}
