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

import java.util.HashSet;
import java.util.Set;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Stores items in transactional resource and flushes aggressively in case of
 * failure. This is useful for batch update writers which need to identify the
 * failed item after failed flush.
 * 
 * @see BatchSqlUpdateItemWriter
 * @see HibernateAwareItemWriter
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public abstract class AbstractTransactionalResourceItemWriter implements ItemWriter {

	private Set failed = new HashSet();

	/**
	 * Flushing delegated to subclass surrounded by binding and unbinding of
	 * transactional resources.
	 * 
	 * @see org.springframework.batch.item.ItemWriter#flush()
	 */
	public void flush() throws FlushFailedException {
		bindTransactionResources();
		try {
			doFlush();
		}
		catch (RuntimeException e) {
			synchronized (failed) {
				failed.addAll(getProcessed());
			}
			// This used to contain a call to onError, however, I think this
			// should be handled within the step.
			throw e;
		}
		finally {
			unbindTransactionResources();
		}
	}

	/**
	 * Delegate to subclass to actually do the writing, but flushes aggressively
	 * if the item was previously part of a failed chunk.
	 * 
	 * @throws Exception
	 * 
	 * @see org.springframework.batch.io.OutputSource#write(java.lang.Object)
	 */
	public void write(Object output) throws Exception {
		bindTransactionResources();
		getProcessed().add(output);
		doWrite(output);
		flushIfNecessary(output);
	}

	private void flushIfNecessary(Object output) {
		boolean flush;
		synchronized (failed) {
			flush = failed.contains(output);
		}
		if (flush) {
			// Force early completion to commit aggressively if we encounter a
			// failed item (from a failed chunk but we don't know which one was
			// the problem).
			RepeatSynchronizationManager.setCompleteOnly();
			// Flush now, so that if there is a failure this record can be
			// skipped.
			flush();
		}

	}

	/**
	 * Delegate to subclass and unbind transactional resources, effectively
	 * clearing the item buffer.
	 */
	public void clear() throws ClearFailedException {
		try {
			doClear();
		}
		finally {
			unbindTransactionResources();
		}
	}

	/**
	 * Callback method of {@link #flush()}.
	 */
	protected abstract void doFlush() throws FlushFailedException;

	/**
	 * Callback method of {@link #clear()}.
	 */
	protected abstract void doClear() throws ClearFailedException;

	/**
	 * Callback method of {@link #write(Object)}.
	 */
	protected abstract void doWrite(Object output) throws Exception;

	/**
	 * @return Key for items processed in the current transaction
	 * {@link RepeatContext}.
	 */
	protected abstract String getResourceKey();

	/**
	 * Set up the {@link RepeatContext} as a transaction resource.
	 * 
	 * @param context the context to set
	 */
	private void bindTransactionResources() {
		if (TransactionSynchronizationManager.hasResource(getResourceKey())) {
			return;
		}
		TransactionSynchronizationManager.bindResource(getResourceKey(), new HashSet());
	}

	/**
	 * Remove the transaction resource associated with this context.
	 */
	private void unbindTransactionResources() {
		if (!TransactionSynchronizationManager.hasResource(getResourceKey())) {
			return;
		}
		TransactionSynchronizationManager.unbindResource(getResourceKey());
	}

	/**
	 * Accessor for the list of processed items in this transaction.
	 * 
	 * @return the processed
	 */
	protected Set getProcessed() {
		Assert.state(TransactionSynchronizationManager.hasResource(getResourceKey()),
				"Processed items not bound to transaction.");
		Set processed = (Set) TransactionSynchronizationManager.getResource(getResourceKey());
		return processed;
	}
}
