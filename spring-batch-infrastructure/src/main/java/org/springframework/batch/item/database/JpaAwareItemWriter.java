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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.batch.item.ItemWriter} that is aware of the JPA
 * EntityManagerFactory and can take some responsibilities to do with chunk
 * boundaries away from a less smart
 * {@link org.springframework.batch.item.ItemWriter} (the delegate). A delegate
 * is required, and will be used to do the actual writing of the item.<br/>
 * 
 * It is required that {@link #write(List)} is called inside a transaction.<br/>
 * 
 * The reader must be configured with an
 * {@link javax.persistence.EntityManagerFactory} that is capable of
 * participating in Spring managed transactions.
 * 
 * The writer is thread safe after its properties are set (normal singleton
 * behaviour), so it can be used to write in multiple concurrent transactions.
 * Note, however, that the set of failed items is stored in a collection
 * internally, and this collection is never cleared, so it is not a great idea
 * to go on using the writer indefinitely. Normally it would be used for the
 * duration of a batch job and then discarded.
 * 
 * @author Dave Syer
 * @author Thomas Risberg
 * 
 */
public class JpaAwareItemWriter<T> implements ItemWriter<T>, InitializingBean {

	private ItemWriter<? super T> delegate;

	private EntityManagerFactory entityManagerFactory;

	/**
	 * Public setter for the {@link org.springframework.batch.item.ItemWriter}
	 * property.
	 * 
	 * @param delegate the delegate to set
	 */
	public void setDelegate(ItemWriter<? super T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * Set the EntityManager to be used internally.
	 * 
	 * @param entityManagerFactory the entityManagerFactory to set
	 */
	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	/**
	 * Check mandatory properties - there must be a delegate and
	 * entityManagerFactory.
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "An ItemWriter to be used as a delegate is required.");
		Assert.notNull(entityManagerFactory, "An EntityManagerFactory is required");
	}

	/**
	 * Delegate the writing to the delegate writer and then flush and clear the
	 * entity manager.
	 * 
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	public void write(List<? extends T> items) throws Exception {
		EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
		if (entityManager == null) {
			throw new DataAccessResourceFailureException("Unable to obtain a transactional EntityManager");
		}
		delegate.write(items);
		try {
			entityManager.flush();
		}
		finally {
			entityManager.clear();
		}
	}

}
