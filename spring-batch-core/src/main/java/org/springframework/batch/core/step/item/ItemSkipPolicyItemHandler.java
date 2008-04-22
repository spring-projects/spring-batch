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
package org.springframework.batch.core.step.item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.listener.CompositeSkipListener;
import org.springframework.batch.core.step.skip.ItemSkipPolicy;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link ItemHandler} that implements skip behavior. It delegates to
 * {@link #setItemSkipPolicy(ItemSkipPolicy)} to decide whether skip should be called or not.
 * 
 * If exception is thrown while reading the item, skip is called on the
 * {@link ItemReader}. If exception is thrown while writing the item, skip is
 * called on both {@link ItemReader} and {@link ItemWriter}.
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class ItemSkipPolicyItemHandler extends SimpleItemHandler {

	/**
	 * Key for transaction resource that holds skipped keys until they can be
	 * removed
	 */
	private static final String TO_BE_REMOVED = ItemSkipPolicyItemHandler.class.getName() + ".TO_BE_REMOVED";

	protected final Log logger = LogFactory.getLog(getClass());

	private ItemSkipPolicy itemSkipPolicy = new NeverSkipItemSkipPolicy();

	private int skipCacheCapacity = 1024;

	private Map skippedExceptions = new HashMap();

	private ItemKeyGenerator defaultItemKeyGenerator = new ItemKeyGenerator() {
		public Object getKey(Object item) {
			return item;
		}
	};

	private ItemKeyGenerator itemKeyGenerator = defaultItemKeyGenerator;

	private CompositeSkipListener listener = new CompositeSkipListener();

	/**
	 * Register some {@link SkipListener}s with the handler. Each will get the
	 * callbacks in the order specified at the correct stage if a skip occurs.
	 * 
	 * @param listeners
	 */
	public void setSkipListeners(SkipListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			registerSkipListener(listeners[i]);
		}
	}

	/**
	 * Register a listener for callbacks at the appropriate stages in a skip
	 * process.
	 * 
	 * @param listener a {@link SkipListener}
	 */
	public void registerSkipListener(SkipListener listener) {
		this.listener.register(listener);
	}

	/**
	 * Public setter for the {@link ItemKeyGenerator}. Defaults to just return
	 * the item, and since it will be used before a write operation.
	 * Implementations must ensure that items always have the same key when they
	 * are read from the {@link ItemReader} (so if the item is mutable and the
	 * reader does any buffering the key generator might need to take care to
	 * only use data that do not change on write).
	 * 
	 * @param itemKeyGenerator the {@link ItemKeyGenerator} to set. If null
	 * resets to default value.
	 */
	public void setItemKeyGenerator(ItemKeyGenerator itemKeyGenerator) {
		if (itemKeyGenerator == null) {
			itemKeyGenerator = defaultItemKeyGenerator;
		}
		this.itemKeyGenerator = itemKeyGenerator;
	}

	/**
	 * @param itemReader
	 * @param itemWriter
	 */
	public ItemSkipPolicyItemHandler(ItemReader itemReader, ItemWriter itemWriter) {
		super(itemReader, itemWriter);
	}

	/**
	 * @param itemSkipPolicy
	 */
	public void setItemSkipPolicy(ItemSkipPolicy itemSkipPolicy) {
		this.itemSkipPolicy = itemSkipPolicy;
	}

	/**
	 * Public setter for the capacity of the skipped item cache. If a large
	 * number of items are failing and not being recognized as skipped, it
	 * usually signals a problem with the key generation (often equals and
	 * hashCode in the item itself). So it is better to enforce a strict limit
	 * than have weird looking errors, where a skip limit is reached without
	 * anything being skipped.
	 * 
	 * @param skipCacheCapacity the capacity to set
	 */
	public void setSkipCacheCapacity(int skipCacheCapacity) {
		this.skipCacheCapacity = skipCacheCapacity;
	}

	/**
	 * Tries to read the item from the reader, in case of exception skip the
	 * item if the skip policy allows, otherwise re-throw.
	 * 
	 * @param contribution current StepContribution holding skipped items count
	 * @return next item for processing
	 */
	protected Object read(StepContribution contribution) throws Exception {

		while (true) {

			try {

				Object item = doRead();
				Object key = itemKeyGenerator.getKey(item);
				Throwable throwable = getSkippedException(key);
				while (item != null && throwable != null) {
					logger.debug("Skipping item on input, previously failed on output; key=[" + key + "]");
					scheduleForRemoval(key);
					if (listener != null) {
						listener.onSkipInWrite(item, throwable);
					}
					item = doRead();
					key = itemKeyGenerator.getKey(item);
					throwable = getSkippedException(key);
				}
				return item;

			}
			catch (Exception e) {
				try {
					if (itemSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
						// increment skip count and try again
						contribution.incrementReadSkipCount();
						if (listener != null) {
							listener.onSkipInRead(e);
						}
						logger.debug("Skipping failed input", e);
					}
					else {
						// re-throw only when the skip policy runs out of
						// patience
						throw e;
					}
				}
				catch (SkipLimitExceededException ex) {
					// we are headed for a abnormal ending so bake in the skip
					// count
					contribution.combineSkipCounts();
					throw ex;
				}
			}

		}

	}

	/**
	 * Tries to write the item using the writer. In case of exception consults
	 * skip policy before re-throwing the exception. The exception is always
	 * re-thrown, but if the item is seen again on read it will be skipped.
	 * 
	 * @param item item to write
	 * @param contribution current StepContribution holding skipped items count
	 */
	protected void write(Object item, StepContribution contribution) throws Exception {
		doWriteWithSkip(item, contribution);
	}

	/**
	 * @param item
	 * @param contribution
	 * @throws Exception
	 */
	protected final void doWriteWithSkip(Object item, StepContribution contribution) throws Exception {
		// Get the key as early as possible, otherwise it might change in
		// doWrite()
		Object key = itemKeyGenerator.getKey(item);
		try {
			doWrite(item);
		}
		catch (Exception e) {
			if (itemSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
				contribution.incrementSkipCount();
				// don't call the listener here - the transaction is going to
				// roll back
				addSkippedException(key, e);
				logger.debug("Added item to skip list; key=" + key);
			}
			// always re-throw exception on write
			throw e;
		}
	}

	public void mark() throws MarkFailedException {
		super.mark();
		clearSkippedExceptions();
	}

	/**
	 * Synchronized setter for a skipped exception.
	 */
	private void addSkippedException(Object key, Throwable e) {
		synchronized (skippedExceptions) {
			if (skippedExceptions.size() >= skipCacheCapacity) {
				throw new UnexpectedJobExecutionException(
						"The cache of failed items to skipped unexpectedly reached its capacity ("
								+ skipCacheCapacity
								+ "). "
								+ "This often indicates a problem with the key generation strategy, and/or a mistake in the implementation of hashCode and equals in the items being processed.");
			}
			skippedExceptions.put(key, e);
		}
	}

	/**
	 * Schedule this key for removal from the skipped exception cache at the end
	 * of this transaction (only removed if business transaction is successful).
	 * 
	 * @param key
	 */
	private void scheduleForRemoval(Object key) {
		if (!TransactionSynchronizationManager.hasResource(TO_BE_REMOVED)) {
			TransactionSynchronizationManager.bindResource(TO_BE_REMOVED, new HashSet());
		}
		((Set) TransactionSynchronizationManager.getResource(TO_BE_REMOVED)).add(key);
	}

	/**
	 * Clear the map of skipped exception corresponding to key.
	 * @param key the key to clear
	 */
	private void clearSkippedExceptions() {
		if (!TransactionSynchronizationManager.hasResource(TO_BE_REMOVED)) {
			return;
		}
		synchronized (skippedExceptions) {
			for (Iterator iterator = ((Set) TransactionSynchronizationManager.getResource(TO_BE_REMOVED)).iterator(); iterator
					.hasNext();) {
				Object key = (Object) iterator.next();
				skippedExceptions.remove(key);
			}
			TransactionSynchronizationManager.unbindResource(TO_BE_REMOVED);
		}
	}

	/**
	 * Synchronized getter for a skipped exception.
	 * @return the skippedExceptions
	 */
	private Throwable getSkippedException(Object key) {
		synchronized (skippedExceptions) {
			return (Throwable) skippedExceptions.get(key);
		}
	}

}
