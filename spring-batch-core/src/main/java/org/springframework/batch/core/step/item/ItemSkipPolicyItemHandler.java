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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.listener.CompositeSkipListener;
import org.springframework.batch.core.step.skip.ItemSkipPolicy;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;

/**
 * {@link ItemHandler} that implements skip behavior. It delegates to
 * {@link #itemSkipPolicy} to decide whether skip should be called or not.
 * 
 * If exception is thrown while reading the item, skip is called on the
 * {@link ItemReader}. If exception is thrown while writing the item, skip is
 * called on both {@link ItemReader} and {@link ItemWriter}.
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class ItemSkipPolicyItemHandler extends SimpleItemHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private ItemSkipPolicy itemSkipPolicy = new NeverSkipItemSkipPolicy();

	private ItemKeyGenerator defaultItemKeyGenerator = new ItemKeyGenerator() {
		public Object getKey(Object item) {
			return item;
		}
	};

	private ItemKeyGenerator itemKeyGenerator = defaultItemKeyGenerator;

	private CompositeSkipListener listener = new CompositeSkipListener();

	private Map skippedExceptions = new HashMap();

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
				while (item != null && skippedExceptions.containsKey(key)) {
					logger.debug("Skipping item on input, previously failed on output; key=[" + key + "]");
					if (listener != null) {
						listener.onSkipInWrite(item, (Throwable) skippedExceptions.get(key));
					}
					item = doRead();
					key = itemKeyGenerator.getKey(item);
				}
				return item;

			}
			catch (Exception e) {
				if (itemSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
					// increment skip count and try again
					contribution.incrementSkipCount();
					if (listener != null) {
						listener.onSkipInRead(e);
					}
					logger.debug("Skipping failed input", e);
				}
				else {
					// re-throw only when the skip policy runs out of patience
					throw e;
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
				skippedExceptions.put(key, e);
				logger.debug("Added item to skip list; key=" + key);
			}
			// always re-throw exception on write
			throw e;
		}
	}

}
