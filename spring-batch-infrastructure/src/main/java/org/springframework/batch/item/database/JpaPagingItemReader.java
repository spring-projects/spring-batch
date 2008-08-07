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
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.EntityTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.support.AbstractBufferedItemReaderItemStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.batch.item.ItemReader} for reading database records built on top of JPA.
 *
 * It executes the JPQL {@link #setQueryString(String)} to retrieve requested data.  The query is
 * executed using paged requests of a size specified in {@link #setPageSize(int)}.  Additional pages
 * are requested when needed as {@link #read()} method is called, returning
 * an object corresponding to current position.
 *
 * The performance of the paging depends on the JPA implementation and its use of database specific
 * features to limit the number of returned rows.
 *
 * Setting a fairly large page size and using a commit interval that matches the page size should provide
 * better performance.
 *
 * In order to reduce the memory usage for large results the persistence context is flushed and cleared
 * after each page is read.  This cuases any entities read to be detached. If you make changes to the
 * entities and want the changes persisted then you must explicitly merge the entities.
 *
 * The reader must be configured with an {@link javax.persistence.EntityManagerFactory}.  All entity access
 * is performed within a new transaction, independent of any existing Spring managed transactions.
 *
 * The implementation is *not* thread-safe.
 *
 * @author Thomas Risberg
 */
public class JpaPagingItemReader<T> extends AbstractBufferedItemReaderItemStream<T> implements InitializingBean {

	protected Log logger = LogFactory.getLog(getClass());

	private EntityManagerFactory entityManagerFactory;

	private final Map<String,Object> jpaPropertyMap = new HashMap<String,Object>();

	private String queryString;

	private boolean initialized = false;

	private int current = 0;

	private int page = 0;

	private int pageSize = 10;

	private List<T> entities;

	public JpaPagingItemReader() {
		setName(ClassUtils.getShortName(JpaPagingItemReader.class));
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(entityManagerFactory);
		Assert.hasLength(queryString);
		Assert.isTrue(pageSize > 0, "pageSize must be greater than zero");
	}

	/**
	 * @param queryString JPQL query string
	 */
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	@Override
	public void mark() {

		super.mark();

	}

	/**
	 * The number of entities to retreive at a time.
	 *
	 * @param pageSize the number of rows to fetch, 10 by default
	 * @see javax.persistence.Query#setMaxResults(int) 
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T doRead() throws Exception {

		if (entities == null || current >= pageSize) {
			EntityManager entityManager =
					entityManagerFactory.createEntityManager(jpaPropertyMap);
			if (entityManager == null) {
				throw new DataAccessResourceFailureException("Unable to obtain an EntityManager");
			}

			EntityTransaction tx = entityManager.getTransaction();
			tx.begin();
			
			Query query = entityManager.createQuery(queryString)
					.setFirstResult(page * pageSize)
					.setMaxResults(pageSize);

			entities = query.getResultList();

			entityManager.flush();
			entityManager.clear();

			tx.commit();
			
			if (current >= pageSize) {
				current = 0;
			}
			page++;
		}

		if (current < entities.size()) {
			return entities.get(current++);
		}
		else {
			return null;
		}

	}

	@Override
	protected void doOpen() throws Exception {

		Assert.state(!initialized, "Cannot open an already opened ItemReader, call close first");

		initialized = true;

	}

	@Override
	protected void doClose() throws Exception {

		initialized = false;

	}


	@Override
	protected void jumpToItem(int itemIndex) throws Exception {

		page = itemIndex / pageSize;
		current = itemIndex % pageSize;

		logger.debug("Jumping to page " + page + " and index " + current);

	}

}
