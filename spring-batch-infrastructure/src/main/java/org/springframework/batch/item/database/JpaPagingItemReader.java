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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * <p>
 * {@link org.springframework.batch.item.ItemReader} for reading database
 * records built on top of JPA.
 * </p>
 * 
 * <p>
 * It executes the JPQL {@link #setQueryString(String)} to retrieve requested
 * data. The query is executed using paged requests of a size specified in
 * {@link #setPageSize(int)}. Additional pages are requested when needed as
 * {@link #read()} method is called, returning an object corresponding to
 * current position.
 * </p>
 * 
 * <p>
 * The performance of the paging depends on the JPA implementation and its use
 * of database specific features to limit the number of returned rows.
 * </p>
 * 
 * <p>
 * Setting a fairly large page size and using a commit interval that matches the
 * page size should provide better performance.
 * </p>
 * 
 * <p>
 * In order to reduce the memory usage for large results the persistence context
 * is flushed and cleared after each page is read. This causes any entities read
 * to be detached. If you make changes to the entities and want the changes
 * persisted then you must explicitly merge the entities.
 * </p>
 * 
 * <p>
 * The reader must be configured with an
 * {@link javax.persistence.EntityManagerFactory}. All entity access is
 * performed within a new transaction, independent of any existing Spring
 * managed transactions.
 * </p>
 * 
 * <p>
 * The implementation is thread-safe in between calls to
 * {@link #open(ExecutionContext)}, but remember to use
 * <code>saveState=false</code> if used in a multi-threaded client (no restart
 * available).
 * </p>
 * 
 * 
 * @author Thomas Risberg
 * @author Dave Syer
 * @since 2.0
 */
public class JpaPagingItemReader<T> extends AbstractPagingItemReader<T> {

	private EntityManagerFactory entityManagerFactory;

	private EntityManager entityManager;

	private final Map<String, Object> jpaPropertyMap = new HashMap<String, Object>();

	private String queryString;

	private Map<String, Object> parameterValues;

	public JpaPagingItemReader() {
		setName(ClassUtils.getShortName(JpaPagingItemReader.class));
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
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

	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(entityManagerFactory);
		Assert.hasLength(queryString);
	}

	/**
	 * @param queryString JPQL query string
	 */
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	@Override
	protected void doOpen() throws Exception {
		super.doOpen();
		entityManager = entityManagerFactory.createEntityManager(jpaPropertyMap);
		if (entityManager == null) {
			throw new DataAccessResourceFailureException("Unable to obtain an EntityManager");
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void doReadPage() {

		EntityTransaction tx = entityManager.getTransaction();
		tx.begin();

		entityManager.flush();
		entityManager.clear();

		Query query = entityManager.createQuery(queryString).setFirstResult(getPage() * getPageSize()).setMaxResults(
				getPageSize());

		if (parameterValues != null) {
			for (Map.Entry<String, Object> me : parameterValues.entrySet()) {
				query.setParameter(me.getKey(), me.getValue());
			}
		}

		if (results == null) {
			results = new CopyOnWriteArrayList<T>();
		}
		else {
			results.clear();
		}
		results.addAll(query.getResultList());

		tx.commit();
	}

	@Override
	protected void doJumpToPage(int itemIndex) {
	}

	@Override
	protected void doClose() throws Exception {
		entityManager.close();
		super.doClose();
	}

}
