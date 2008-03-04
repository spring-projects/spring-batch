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
package org.springframework.batch.io.support;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.exception.ClearFailedException;
import org.springframework.batch.item.exception.FlushFailedException;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatListener;
import org.springframework.batch.repeat.synch.RepeatSynchronizationManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateOperations;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * {@link ItemWriter} that is aware of the Hibernate session and can take some
 * responsibilities to do with chunk boundaries away from a less smart
 * {@link ItemWriter} (the delegate). A delegate is required, and will be used
 * to do the actual writing of the item.<br/>
 * 
 * @author Dave Syer
 * 
 */
public class HibernateAwareItemWriter implements ItemWriter, InitializingBean {

	/**
	 * Key for items processed in the current transaction {@link RepeatContext}.
	 */
	private static final String ITEMS_PROCESSED = HibernateAwareItemWriter.class.getName() + ".ITEMS_PROCESSED";

	private Set failed = new HashSet();

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
	 * Check mandatory properties - there must be a delegate.
	 * 
	 * @see org.springframework.dao.support.DaoSupport#initDao()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "HibernateAwareItemWriter requires an ItemWriter as a delegate.");
		Assert.notNull(hibernateTemplate, "HibernateAwareItemWriter requires a HibernateOperations");
	}

	/**
	 * Use the delegate to actually do the writing, but flush aggressively if
	 * the item was previously part of a failed chunk.
	 * 
	 * @throws Exception
	 * 
	 * @see org.springframework.batch.io.OutputSource#write(java.lang.Object)
	 */
	public void write(Object output) throws Exception {
		bindTransactionResources();
		getProcessed().add(output);
		delegate.write(output);
		flushIfNecessary(output);
	}

	/**
	 * Accessor for the list of processed items in this transaction.
	 * 
	 * @return the processed
	 */
	private Set getProcessed() {
		Assert.state(TransactionSynchronizationManager.hasResource(ITEMS_PROCESSED),
				"Processed items not bound to transaction.");
		Set processed = (Set) TransactionSynchronizationManager.getResource(ITEMS_PROCESSED);
		return processed;
	}

	/**
	 * Set up the {@link RepeatContext} as a transaction resource.
	 * 
	 * @param context the context to set
	 */
	private void bindTransactionResources() {
		if (TransactionSynchronizationManager.hasResource(ITEMS_PROCESSED)) {
			return;
		}
		TransactionSynchronizationManager.bindResource(ITEMS_PROCESSED, new HashSet());
	}

	/**
	 * Remove the transaction resource associated with this context.
	 */
	private void unbindTransactionResources() {
		if (!TransactionSynchronizationManager.hasResource(ITEMS_PROCESSED)) {
			return;
		}
		TransactionSynchronizationManager.unbindResource(ITEMS_PROCESSED);
	}

	/**
	 * Accessor for the context property.
	 * 
	 * @param output
	 * 
	 * @return the context
	 */
	private void flushIfNecessary(Object output) throws Exception {
		boolean flush;
		synchronized (failed) {
			flush = failed.contains(output);
		}
		if (flush) {
			RepeatContext context = RepeatSynchronizationManager.getContext();
			// Force early completion to commit aggressively if we encounter a
			// failed item (from a failed chunk but we don't know which one was
			// the problem).
			context.setCompleteOnly();
			// Flush now, so that if there is a failure this record can be
			// skipped.
			doHibernateFlush();
		}
	}

	/**
	 * Flush the hibernate session from within a repeat context.
	 */
	private void doHibernateFlush() {
		try {
			hibernateTemplate.flush();
			// This should happen when the transaction commits anyway, but to be
			// sure...
			hibernateTemplate.clear();
		}
		catch (RuntimeException e) {
			synchronized (failed) {
				failed.addAll(getProcessed());
			}
			// This used to contain a call to onError, however, I think this
			// should be handled within the step.
			throw e;
		}
	}

	/**
	 * Call the delegate clear() method, and then clear the hibernate session.
	 * 
	 * @see org.springframework.batch.item.ItemWriter#clear()
	 */
	public void clear() throws ClearFailedException {
		unbindTransactionResources();
		hibernateTemplate.clear();
		delegate.clear();
	}

	/**
	 * Flush the Hibernate session and record failures if there are any. The
	 * delegate flush will also be called.
	 * 
	 * @see org.springframework.batch.item.ItemWriter#flush()
	 */
	public void flush() throws FlushFailedException {
		bindTransactionResources();
		doHibernateFlush();
		unbindTransactionResources();
		delegate.flush();
	}

}
