/*
 * Copyright 2026 the original author or authors.
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

package org.springframework.batch.infrastructure.item.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.hibernate.SpringSessionContext;
import org.springframework.util.Assert;

/**
 * {@link ItemWriter} that is using a Hibernate StatelessSession to persist any entities.
 * <p>
 * It is required that {@link #write(Chunk)} is called inside a transaction.<br>
 *
 * The reader must be configured with an {@link org.hibernate.SessionFactory} that is
 * capable of participating in Spring managed transactions.
 * <p>
 * The writer is thread-safe after its properties are set (normal singleton behaviour), so
 * it can be used to write in multiple concurrent transactions.
 *
 * @author Philippe Marschall
 * @since 6.1
 */
public class StatelessSessionItemWriter<T> implements ItemWriter<T> {

	protected static final Log logger = LogFactory.getLog(StatelessSessionItemWriter.class);

	private final SessionFactory sessionFactory;

	/**
	 * Create a new {@link StatelessSessionItemWriter} instance.
	 * @param sessionFactory the session factory to use
	 */
	public StatelessSessionItemWriter(SessionFactory sessionFactory) {
		Assert.notNull(sessionFactory, "SessionFactory must not be null");
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Insert all provided items.
	 *
	 * @see ItemWriter#write(Chunk)
	 */
	@Override
	public void write(Chunk<? extends T> items) {
		StatelessSession statelessSession;
		try {
			statelessSession = SpringSessionContext.currentStatelessSession(this.sessionFactory);
		}
		catch (HibernateException e) {
			throw new DataAccessResourceFailureException("Unable to obtain a transactional StatelessSession", e);
		}
		doWrite(statelessSession, items);
	}

	/**
	 * Do perform the actual write operation. This can be overridden in a subclass if
	 * necessary.
	 * @param statelessSession the StatelessSession to use for the operation
	 * @param items the list of items to use for the write
	 */
	protected void doWrite(StatelessSession statelessSession, Chunk<? extends T> items) {

		if (logger.isDebugEnabled()) {
			logger.debug("Writing to Hibernate with " + items.size() + " items.");
		}

		if (!items.isEmpty()) {
			statelessSession.insertMultiple(items.getItems());
		}

	}

}
