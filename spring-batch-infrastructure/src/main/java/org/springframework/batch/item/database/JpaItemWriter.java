/*
 * Copyright 2006-2021 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;

/**
 * {@link org.springframework.batch.item.ItemWriter} that is using a JPA
 * EntityManagerFactory to merge any Entities that aren't part of the persistence context.
 *
 * It is required that {@link #write(List)} is called inside a transaction.<br>
 *
 * The reader must be configured with an {@link jakarta.persistence.EntityManagerFactory}
 * that is capable of participating in Spring managed transactions.
 *
 * The writer is thread-safe after its properties are set (normal singleton behaviour), so
 * it can be used to write in multiple concurrent transactions.
 *
 * @author Thomas Risberg
 * @author Mahmoud Ben Hassine
 *
 */
public class JpaItemWriter<T> implements ItemWriter<T>, InitializingBean {

	protected static final Log logger = LogFactory.getLog(JpaItemWriter.class);

	private EntityManagerFactory entityManagerFactory;

	private boolean usePersist = false;

	/**
	 * Set the EntityManager to be used internally.
	 * @param entityManagerFactory the entityManagerFactory to set
	 */
	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	/**
	 * Set whether the EntityManager should perform a persist instead of a merge.
	 * @param usePersist whether to use persist instead of merge.
	 */
	public void setUsePersist(boolean usePersist) {
		this.usePersist = usePersist;
	}

	/**
	 * Check mandatory properties - there must be an entityManagerFactory.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(entityManagerFactory, "An EntityManagerFactory is required");
	}

	/**
	 * Merge all provided items that aren't already in the persistence context and then
	 * flush the entity manager.
	 *
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Override
	public void write(List<? extends T> items) {
		EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
		if (entityManager == null) {
			throw new DataAccessResourceFailureException("Unable to obtain a transactional EntityManager");
		}
		doWrite(entityManager, items);
		entityManager.flush();
	}

	/**
	 * Do perform the actual write operation. This can be overridden in a subclass if
	 * necessary.
	 * @param entityManager the EntityManager to use for the operation
	 * @param items the list of items to use for the write
	 */
	protected void doWrite(EntityManager entityManager, List<? extends T> items) {

		if (logger.isDebugEnabled()) {
			logger.debug("Writing to JPA with " + items.size() + " items.");
		}

		if (!items.isEmpty()) {
			long addedToContextCount = 0;
			for (T item : items) {
				if (!entityManager.contains(item)) {
					if (usePersist) {
						entityManager.persist(item);
					}
					else {
						entityManager.merge(item);
					}
					addedToContextCount++;
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug(addedToContextCount + " entities " + (usePersist ? " persisted." : "merged."));
				logger.debug((items.size() - addedToContextCount) + " entities found in persistence context.");
			}
		}

	}

}
