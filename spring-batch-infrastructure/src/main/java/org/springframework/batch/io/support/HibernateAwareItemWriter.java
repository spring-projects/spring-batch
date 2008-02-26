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
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.AttributeAccessor;
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
 * This class implements {@link RepeatListener} and it will only work if
 * properly registered. If the delegate is also a {@link RepeatListener} then it
 * does not need to be separately registered as we make the callbacks here in
 * the right places.
 * 
 * @author Dave Syer
 * 
 */
public class HibernateAwareItemWriter implements ItemWriter, RepeatListener, InitializingBean {

	/**
	 * Key for items processed in the current transaction {@link RepeatContext}.
	 */
	private static final String ITEMS_PROCESSED = HibernateAwareItemWriter.class.getName() + ".ITEMS_PROCESSED";

	/**
	 * Key for {@link RepeatContext} in transaction resource context.
	 */
	private static final String WRITER_REPEAT_CONTEXT = HibernateAwareItemWriter.class.getName()
			+ ".WRITER_REPEAT_CONTEXT";

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
		getProcessed().add(output);
		delegate.write(output);
		flushIfNecessary(output);
	}

	/**
	 * Does nothing unless the delegate is also a {@link RepeatListener} in
	 * which case pass on the call to him.
	 * 
	 * @see org.springframework.batch.repeat.RepeatListener#before(org.springframework.batch.repeat.RepeatContext)
	 */
	public void before(RepeatContext context) {
		if (delegate instanceof RepeatListener) {
			RepeatListener interceptor = (RepeatListener) delegate;
			interceptor.before(context);
		}
	}

	/**
	 * Does nothing unless the delegate is also a {@link RepeatListener} in
	 * which case pass on the call to him.
	 * 
	 * @see org.springframework.batch.repeat.RepeatListener#after(org.springframework.batch.repeat.RepeatContext,
	 * org.springframework.batch.repeat.ExitStatus)
	 */
	public void after(RepeatContext context, ExitStatus result) {
		if (delegate instanceof RepeatListener) {
			RepeatListener interceptor = (RepeatListener) delegate;
			interceptor.after(context, result);
		}
	}

	/**
	 * If the delegate is also a {@link RepeatListener} then it will be given
	 * the call before flushing.
	 * 
	 * 
	 * @see org.springframework.batch.repeat.RepeatListener#close(org.springframework.batch.repeat.RepeatContext)
	 */
	public void close(RepeatContext context) {
		try {
			flushInContext();
		}
		finally {
			unsetContext();
			if (delegate instanceof RepeatListener) {
				RepeatListener interceptor = (RepeatListener) delegate;
				interceptor.close(context);
			}
		}
	}

	/**
	 * Does nothing unless the delegate is also a {@link RepeatListener} in
	 * which case pass on the call to him.
	 * 
	 * @see org.springframework.batch.repeat.RepeatListener#onError(org.springframework.batch.repeat.RepeatContext,
	 * java.lang.Throwable)
	 */
	public void onError(RepeatContext context, Throwable e) {
		if (delegate instanceof RepeatListener) {
			RepeatListener interceptor = (RepeatListener) delegate;
			interceptor.onError(context, e);
		}
	}

	/**
	 * Sets up the context as a transaction resource so that we can store state
	 * and refer back to it in the {@link #write(Object)} method. If the
	 * delegate is also a {@link RepeatListener} then it will be given the call
	 * afterwards.
	 * 
	 * @see org.springframework.batch.repeat.RepeatListener#open(org.springframework.batch.repeat.RepeatContext)
	 */
	public void open(RepeatContext context) {
		this.setContext(context);
		getProcessed().clear();
		if (delegate instanceof RepeatListener) {
			RepeatListener interceptor = (RepeatListener) delegate;
			interceptor.open(context);
		}
	}

	/**
	 * Accessor for the list of processed items in this transaction.
	 * 
	 * @return the processed
	 */
	private Set getProcessed() {
		Assert.state(TransactionSynchronizationManager.hasResource(WRITER_REPEAT_CONTEXT),
				"RepeatContext not bound to transaction.");
		Set processed = (Set) ((AttributeAccessor) TransactionSynchronizationManager.getResource(WRITER_REPEAT_CONTEXT))
				.getAttribute(ITEMS_PROCESSED);
		return processed;
	}

	/**
	 * Set up the {@link RepeatContext} as a transaction resource.
	 * 
	 * @param context the context to set
	 */
	private void setContext(RepeatContext context) {
		if (TransactionSynchronizationManager.hasResource(WRITER_REPEAT_CONTEXT)) {
			return;
		}
		TransactionSynchronizationManager.bindResource(WRITER_REPEAT_CONTEXT, context);
		context.setAttribute(ITEMS_PROCESSED, new HashSet());
	}

	/**
	 * Remove the transaction resource associated with this context.
	 */
	private void unsetContext() {
		if (!TransactionSynchronizationManager.hasResource(WRITER_REPEAT_CONTEXT)) {
			return;
		}
		TransactionSynchronizationManager.unbindResource(WRITER_REPEAT_CONTEXT);
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
			RepeatContext context = (RepeatContext) TransactionSynchronizationManager
					.getResource(WRITER_REPEAT_CONTEXT);
			// Force early completion to commit aggressively if we encounter a
			// failed item (from a failed chunk but we don't know which one was
			// the problem).
			context.setCompleteOnly();
			// Flush now, so that if there is a failure this record can be
			// skipped.
			flushInContext();
		}

	}

	/**
	 * 
	 */
	private void flushInContext() {
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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemWriter#clear()
	 */
	public void clear() throws ClearFailedException {
		if (delegate != null) {
			delegate.clear();
		}
		hibernateTemplate.clear();
	}

	/**
	 * Flush the Hibernate session. The delegate flush will also be called
	 * before finishing.
	 */
	public void flush() throws FlushFailedException {
		if (delegate != null) {
			delegate.flush();
		}
	}

}
