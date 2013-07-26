/*
 * Copyright 2006-2013 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.spi.CurrentSessionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateOperations;
import org.springframework.util.Assert;

/**
 * {@link ItemWriter} that uses a Hibernate session to save or update entities
 * that are not part of the current Hibernate session. It will also flush the
 * session after writing (i.e. at chunk boundaries if used in a Spring Batch
 * TaskletStep). It will also clear the session on write
 * default (see {@link #setClearSession(boolean) clearSession} property).<br/>
 * <br/>
 *
 * The writer is thread safe once properties are set (normal singleton behavior)
 * if a {@link CurrentSessionContext} that uses only one session per thread is
 * used.
 *
 * @author Dave Syer
 * @author Thomas Risberg
 * @author Michael Minella
 *
 */
public class HibernateItemWriter<T> implements ItemWriter<T>, InitializingBean {

	protected static final Log logger = LogFactory
			.getLog(HibernateItemWriter.class);

	private HibernateOperations hibernateTemplate;
	private SessionFactory sessionFactory;

	private boolean clearSession = true;

	/**
	 * Flag to indicate that the session should be cleared and flushed at the
	 * end of the write (default true).
	 *
	 * @param clearSession
	 *            the flag value to set
	 */
	public void setClearSession(boolean clearSession) {
		this.clearSession = clearSession;
	}

	/**
	 * Public setter for the {@link HibernateOperations} property.
	 *
	 * @param hibernateTemplate
	 *            the hibernateTemplate to set
	 * @deprecated As of 2.2 in favor of using Hibernate's session management APIs directly
	 */
	public void setHibernateTemplate(HibernateOperations hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * Set the Hibernate SessionFactory to be used internally.
	 *
	 * @param sessionFactory session factory to be used by the writer
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Check mandatory properties - there must be a hibernateTemplate.
	 */
	@Override
	public void afterPropertiesSet() {
		Assert.state(!(hibernateTemplate == null && sessionFactory == null),
				"Either HibernateOperations or SessionFactory must be provided");
	}

	/**
	 * Save or update any entities not in the current hibernate session and then
	 * flush the hibernate session.
	 *
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Override
	public void write(List<? extends T> items) {
		if(sessionFactory == null) {
			doWrite(hibernateTemplate, items);
			hibernateTemplate.flush();
			if (clearSession) {
				hibernateTemplate.clear();
			}
		}
		else {
			doWrite(sessionFactory, items);
			sessionFactory.getCurrentSession().flush();
			if(clearSession) {
				sessionFactory.getCurrentSession().clear();
			}
		}
	}

	/**
	 * Do perform the actual write operation using Hibernate's API.
	 * This can be overridden in a subclass if necessary.
	 *
	 * @param items
	 *            the list of items to use for the write
	 * @deprecated As of 2.2 in favor of using Hibernate's session management APIs directly
	 */
	protected void doWrite(SessionFactory sessionFactory, List<? extends T> items) {
		if (logger.isDebugEnabled()) {
			logger.debug("Writing to Hibernate with " + items.size()
					+ " items.");
		}

		Session currentSession = sessionFactory.getCurrentSession();

		if (!items.isEmpty()) {
			long saveOrUpdateCount = 0;
			for (T item : items) {
				if (!currentSession.contains(item)) {
					currentSession.saveOrUpdate(item);
					saveOrUpdateCount++;
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug(saveOrUpdateCount + " entities saved/updated.");
				logger.debug((items.size() - saveOrUpdateCount)
						+ " entities found in session.");
			}
		}
	}

	/**
	 * Do perform the actual write operation using {@link HibernateOperations}.
	 * This can be overridden in a subclass if necessary.
	 *
	 * @param hibernateTemplate
	 *            the HibernateTemplate to use for the operation
	 * @param items
	 *            the list of items to use for the write
	 * @deprecated As of 2.2 in favor of using Hibernate's session management APIs directly
	 */
	protected void doWrite(HibernateOperations hibernateTemplate,
			List<? extends T> items) {

		if (logger.isDebugEnabled()) {
			logger.debug("Writing to Hibernate with " + items.size()
					+ " items.");
		}

		if (!items.isEmpty()) {
			long saveOrUpdateCount = 0;
			for (T item : items) {
				if (!hibernateTemplate.contains(item)) {
					hibernateTemplate.saveOrUpdate(item);
					saveOrUpdateCount++;
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug(saveOrUpdateCount + " entities saved/updated.");
				logger.debug((items.size() - saveOrUpdateCount)
						+ " entities found in session.");
			}
		}

	}

}
