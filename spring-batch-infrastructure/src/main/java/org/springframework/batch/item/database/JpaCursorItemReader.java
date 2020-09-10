/*
 * Copyright 2020 the original author or authors.
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

import java.util.Iterator;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.database.orm.JpaQueryProvider;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.batch.item.ItemStreamReader} implementation based
 *  on JPA {@link Query#getResultStream()}. It executes the JPQL query when
 *  initialized and iterates over the result set as {@link #read()} method is called,
 *  returning an object corresponding to the current row. The query can be set
 *  directly using {@link #setQueryString(String)}, or using a query provider via
 *  {@link #setQueryProvider(JpaQueryProvider)}.
 * 
 * The implementation is <b>not</b> thread-safe.
 * 
 * @author Mahmoud Ben Hassine
 * @param <T> type of items to read
 * @since 4.3
 */
public class JpaCursorItemReader<T> extends AbstractItemCountingItemStreamItemReader<T>
		implements InitializingBean {

	private EntityManagerFactory entityManagerFactory;
	private EntityManager entityManager;
	private String queryString;
	private JpaQueryProvider queryProvider;
	private Map<String, Object> parameterValues;
	private Iterator<T> iterator;

	/**
	 * Create a new {@link JpaCursorItemReader}.
	 */
	public JpaCursorItemReader() {
		setName(ClassUtils.getShortName(JpaCursorItemReader.class));
	}

	/**
	 * Set the JPA entity manager factory.
	 *
	 * @param entityManagerFactory JPA entity manager factory
	 */
	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	/**
	 * Set the JPA query provider.
	 *
	 * @param queryProvider JPA query provider
	 */
	public void setQueryProvider(JpaQueryProvider queryProvider) {
		this.queryProvider = queryProvider;
	}

	/**
	 * Set the JPQL query string.
	 *
	 * @param queryString JPQL query string
	 */
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	/**
	 * Set the parameter values to be used for the query execution.
	 *
	 * @param parameterValues the values keyed by parameter names used in
	 * the query string.
	 */
	public void setParameterValues(Map<String, Object> parameterValues) {
		this.parameterValues = parameterValues;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.entityManagerFactory, "EntityManagerFactory is required");
		if (this.queryProvider == null) {
			Assert.hasLength(this.queryString, "Query string is required when queryProvider is null");
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void doOpen() throws Exception {
		this.entityManager = this.entityManagerFactory.createEntityManager();
		if (this.entityManager == null) {
			throw new DataAccessResourceFailureException("Unable to create an EntityManager");
		}
		if (this.queryProvider != null) {
			this.queryProvider.setEntityManager(this.entityManager);
		}
		Query query = createQuery();
		if (this.parameterValues != null) {
			this.parameterValues.forEach(query::setParameter);
		}
		this.iterator = query.getResultStream().iterator();
	}

	private Query createQuery() {
		if (this.queryProvider == null) {
			return this.entityManager.createQuery(this.queryString);
		}
		else {
			return this.queryProvider.createQuery();
		}
	}

	@Override
	protected T doRead() {
		return this.iterator.hasNext() ? this.iterator.next() : null;
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		super.update(executionContext);
		this.entityManager.clear();
	}

	@Override
	protected void doClose() {
		if (this.entityManager != null) {
			this.entityManager.close();
		}
	}
}
