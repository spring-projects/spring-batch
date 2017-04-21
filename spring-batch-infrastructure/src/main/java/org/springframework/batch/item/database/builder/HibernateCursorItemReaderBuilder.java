/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.batch.item.database.builder;

import java.util.Map;

import org.hibernate.SessionFactory;

import org.springframework.batch.item.database.HibernateCursorItemReader;
import org.springframework.batch.item.database.orm.HibernateQueryProvider;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This is a builder for the {@link HibernateCursorItemReader}.  When configuring, one of
 * the following should be provided:
 * <ul>
 *     <li>{@link #queryString(String)}</li>
 *     <li>{@link #queryName(String)}</li>
 *     <li>{@link #queryProvider(HibernateQueryProvider)}</li>
 * </ul>
 *
 * @author Michael Minella
 * @since 4.0
 * @see HibernateCursorItemReader
 */
public class HibernateCursorItemReaderBuilder<T> {

	private Map<String, Object> parameterValues;

	private String queryName;

	private int fetchSize;

	private HibernateQueryProvider<T> queryProvider;

	private String queryString;

	private SessionFactory sessionFactory;

	private boolean useStatelessSession;

	private int currentItem;

	private int maxItemCount = Integer.MAX_VALUE;

	private boolean saveState = true;

	private String name;

	/**
	 * A name used to prevent key collisions while saving the state in the
	 * {@link org.springframework.batch.item.ExecutionContext}
	 *
	 * @param name unique name for this reader instance
	 * @return this instance for method chaining
	 * @see HibernateCursorItemReader#setName(String)
	 */
	public HibernateCursorItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * A map of parameter values to be set on the query.   The key of the map is the name
	 * of the parameter to be set with the value being the value to be set.
	 *
	 * @param parameterValues map of values
	 * @return this instance for method chaining
	 * @see HibernateCursorItemReader#setParameterValues(Map)
	 */
	public HibernateCursorItemReaderBuilder<T> parameterValues(Map<String, Object> parameterValues) {
		this.parameterValues = parameterValues;

		return this;
	}

	/**
	 * The name of the Hibernate named query to be executed for this reader.
	 *
	 * @param queryName name of the query to execute
	 * @return this instance for method chaining
	 * @see HibernateCursorItemReader#setQueryName(String)
	 */
	public HibernateCursorItemReaderBuilder<T> queryName(String queryName) {
		this.queryName = queryName;

		return this;
	}

	/**
	 * The number of items to be returned with each round trip to the database.  Used
	 * internally by Hibernate.
	 *
	 * @param fetchSize number of records to return per fetch
	 * @return this instance for method chaining
	 * @see HibernateCursorItemReader#setFetchSize(int)
	 */
	public HibernateCursorItemReaderBuilder<T> fetchSize(int fetchSize) {
		this.fetchSize = fetchSize;

		return this;
	}

	/**
	 * A query provider.  This should be set only if {@link #queryString(String)} and
	 * {@link #queryName(String)} have not been set.
	 *
	 * @param queryProvider the query provider
	 * @return this instance for method chaining
	 * @see HibernateCursorItemReader#setQueryProvider(HibernateQueryProvider)
	 */
	public HibernateCursorItemReaderBuilder<T> queryProvider(HibernateQueryProvider<T> queryProvider) {
		this.queryProvider = queryProvider;

		return this;
	}

	/**
	 * The HQL query string to execute.  This should only be set if
	 * {@link #queryProvider(HibernateQueryProvider)} and {@link #queryName(String)} have
	 * not been set.
	 *
	 * @param queryString the HQL query
	 * @return this instance for method chaining
	 * @see HibernateCursorItemReader#setQueryString(String)
	 */
	public HibernateCursorItemReaderBuilder<T> queryString(String queryString) {
		this.queryString = queryString;

		return this;
	}

	/**
	 * The Hibernate {@link SessionFactory} to execute the query against.
	 *
	 * @param sessionFactory the session factorry
	 * @return this instance for method chaining
	 * @see HibernateCursorItemReader#setSessionFactory(SessionFactory)
	 */
	public HibernateCursorItemReaderBuilder<T> sessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;

		return this;
	}

	/**
	 * Indicator for whether to use a {@link org.hibernate.StatelessSession}
	 * (<code>true</code>) or a {@link org.hibernate.Session} (<code>false</code>).
	 *
	 * @param useStatelessSession Defaults to false
	 * @return this instance for method chaining
	 * @see HibernateCursorItemReader#setUseStatelessSession(boolean)
	 */
	public HibernateCursorItemReaderBuilder<T> useSatelessSession(boolean useStatelessSession) {
		this.useStatelessSession = useStatelessSession;

		return this;
	}

	/**
	 * Index for the current item.  Used on restarts to indicate where to start from.
	 *
	 * @param currentItem current index
	 * @return this instance for method chaining
	 * @see HibernateCursorItemReader#setCurrentItemCount(int)
	 */
	public HibernateCursorItemReaderBuilder<T> currentItem(int currentItem) {
		this.currentItem = currentItem;

		return this;
	}

	/**
	 * The index of the max item to be read.
	 *
	 * @param maxItemCount max index
	 * @return this instance for method chaining
	 * @see HibernateCursorItemReader#setMaxItemCount(int)
	 */
	public HibernateCursorItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;

		return this;
	}

	/**
	 * Indicates if the state should be saved.  If set to false, restarts will begin at
	 * the beginning of the dataset.  Defaults to true
	 *
	 * @param saveState indicator
	 * @return this instance for method chaining
	 * @see HibernateCursorItemReader#setSaveState(boolean)
	 */
	public HibernateCursorItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * Returns a fully constructed {@link HibernateCursorItemReader}.
	 *
	 * @return a new {@link HibernateCursorItemReader}
	 */
	public HibernateCursorItemReader<T> build() {
		Assert.state(this.fetchSize >= 0, "fetchSize must not be negative");
		Assert.state(this.sessionFactory != null, "A SessionFactory must be provided");

		if(this.saveState) {
			Assert.state(StringUtils.hasText(this.name),
					"A name is required when saveState is set to true.");
		}

		if (queryProvider == null) {
			Assert.state(StringUtils.hasText(queryString) ^ StringUtils.hasText(queryName),
					"queryString or queryName must be set");
		}

		HibernateCursorItemReader<T> reader = new HibernateCursorItemReader<>();

		reader.setFetchSize(this.fetchSize);
		reader.setParameterValues(this.parameterValues);
		reader.setQueryName(this.queryName);
		reader.setQueryProvider(this.queryProvider);
		reader.setQueryString(this.queryString);
		reader.setSessionFactory(this.sessionFactory);
		reader.setUseStatelessSession(this.useStatelessSession);
		reader.setCurrentItemCount(this.currentItem);
		reader.setMaxItemCount(this.maxItemCount);
		reader.setName(this.name);
		reader.setSaveState(this.saveState);

		return reader;
	}

 }
