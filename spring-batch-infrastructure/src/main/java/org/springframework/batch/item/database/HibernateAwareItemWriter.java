/*
 * Copyright 2006-2007 the original author or authors.
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

import org.hibernate.SessionFactory;
import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateOperations;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.util.Assert;

/**
 * {@link ItemWriter} that is aware of the Hibernate session and can take some
 * responsibilities to do with chunk boundaries away from a less smart
 * {@link ItemWriter} (the delegate). A delegate is required, and will be used
 * to do the actual writing of the item.<br/>
 * 
 * It is expected that {@link #write(Object)} is called inside a transaction,
 * and that {@link #flush()} is then subsequently called before the transaction
 * commits, or {@link #clear()} before it rolls back.<br/>
 * 
 * The writer is thread safe after its properties are set (normal singleton
 * behaviour), so it can be used to write in multiple concurrent transactions.
 * Note, however, that the set of failed items is stored in a collection
 * internally, and this collection is never cleared, so it is not a great idea
 * to go on using the writer indefinitely. Normally it would be used for the
 * duration of a batch job and then discarded.
 * 
 * @author Dave Syer
 * 
 */
public class HibernateAwareItemWriter extends AbstractTransactionalResourceItemWriter implements InitializingBean {

	/**
	 * Key for items processed in the current transaction {@link RepeatContext}.
	 */
	private static final String ITEMS_PROCESSED = HibernateAwareItemWriter.class.getName() + ".ITEMS_PROCESSED";

	private ItemWriter delegate;

	private HibernateOperations hibernateTemplate;

	/**
	 * Public setter for the {@link ItemWriter} property.
	 * 
	 * @param delegate the delegate to set
	 */
	public void setDelegate(ItemWriter delegate) {
		this.delegate = delegate;
	}

	/**
	 * Public setter for the {@link HibernateOperations} property.
	 * 
	 * @param hibernateTemplate the hibernateTemplate to set
	 */
	public void setHibernateTemplate(HibernateOperations hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * Set the Hibernate SessionFactory to be used internally. Will
	 * automatically create a HibernateTemplate for the given SessionFactory.
	 * 
	 * @see #setHibernateTemplate
	 */
	public final void setSessionFactory(SessionFactory sessionFactory) {
		this.hibernateTemplate = new HibernateTemplate(sessionFactory);
	}

	/**
	 * Check mandatory properties - there must be a delegate and hibernateTemplate.
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "HibernateAwareItemWriter requires an ItemWriter as a delegate.");
		Assert.notNull(hibernateTemplate, "HibernateAwareItemWriter requires a HibernateOperations");
	}

	/**
	 * Delegate to subclass and flush the hibernate session.
	 */
	protected void doFlush() {
		delegate.flush();
		hibernateTemplate.flush();
		// This should happen when the transaction commits anyway, but to be
		// sure...
		hibernateTemplate.clear();
	}

	/**
	 * Call the delegate clear() method, and then clear the hibernate session.
	 */
	protected void doClear() throws ClearFailedException {
		delegate.clear();
		hibernateTemplate.clear();
	}

	protected String getResourceKey() {
		return ITEMS_PROCESSED;
	}

	protected void doWrite(Object output) throws Exception {
		delegate.write(output);
	}

}
