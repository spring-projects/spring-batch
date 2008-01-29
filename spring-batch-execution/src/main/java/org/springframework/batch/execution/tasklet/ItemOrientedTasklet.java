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

package org.springframework.batch.execution.tasklet;

import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.callback.ItemReaderRetryCallback;
import org.springframework.batch.retry.policy.ItemReaderRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A concrete implementation of the {@link Tasklet} interface that provides
 * 'split processing'. This type of processing is characterized by separating
 * the reading and processing of batch data into two separate classes:
 * {@link ItemReader} and {@link ItemWriter}. The {@link ItemReader} class
 * provides a solid means for re-usability and enforces good architecture
 * practices. Because an object <em>must</em> be returned by the
 * {@link ItemReader} to continue processing, (returning null indicates
 * processing should end) a developer is forced to read in all relevant data,
 * place it into a domain object, and return that object. The
 * {@link ItemWriter} will then use this object for calculations and output.<br/>
 * 
 * If a {@link RetryPolicy} is provided it will be used to construct a stateful
 * retry around the {@link ItemWriter}, delegating identity concerns to the
 * {@link ItemReader} and recovery concerns to the {@link ItemRecoverer} (if
 * present). In this case clients of this class do not need to take any
 * additional action at runtime to take advantage of the retry and recovery,
 * provided that when the {@link #execute()} method is called again the same
 * item is eventually re-presented (normally this would be the case because a
 * transaction would have rolled back and the {@link ItemReader} would go back
 * to its previous state).<br/>
 * 
 * If a {@link RetryPolicy} is not provided then the {@link ItemRecoverer} can
 * be used to attempt to recover immediately (with no retry) from a processing
 * error. Clients of this class should ensure that the recovery takes place in a
 * separate transaction (e.g. with propagation REQUIRES_NEW) if necessary. This
 * can be achieved by injecting an {@link ItemRecoverer} that has a
 * transactional recover method.
 * 
 * @see ItemReader
 * @see ItemWriter
 * @see RetryPolicy
 * @see Recoverable
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 */
public class ItemOrientedTasklet implements Tasklet, Skippable, InitializingBean {

	/**
	 * Prefix added to statistics keys from writer if needed to avoid
	 * ambiguity between reader and writer.
	 */
	public static final String PROCESSOR_STATISTICS_PREFIX = "writer.";

	/**
	 * Prefix added to statistics keys from reader if needed to avoid
	 * ambiguity between provider and writer.
	 */
	public static final String PROVIDER_STATISTICS_PREFIX = "provider.";

	private RetryPolicy retryPolicy = null;

	protected ItemReader itemProvider;

	protected ItemWriter itemWriter;

	private ItemRecoverer itemRecoverer;

	private RetryTemplate template = new RetryTemplate();

	private ItemReaderRetryCallback retryCallback;

	/**
	 * Check mandatory properties (reader and writer).
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(itemProvider, "ItemReader must be provided");
		Assert.notNull(itemWriter, "ItemProcessor must be provided");

		if (itemRecoverer == null && (itemProvider instanceof ItemRecoverer)) {
			itemRecoverer = (ItemRecoverer) itemProvider;
		}

		ItemReaderRetryPolicy itemProviderRetryPolicy = new ItemReaderRetryPolicy(retryPolicy);
		template.setRetryPolicy(itemProviderRetryPolicy);

		if (retryPolicy != null) {
			retryCallback = new ItemReaderRetryCallback(itemProvider, itemWriter);
			retryCallback.setRecoverer(itemRecoverer);
		}

	}

	/**
	 * Read from the {@link ItemReader} and process (if not null) with the
	 * {@link ItemWriter}. The call to {@link ItemWriter} is wrapped in a
	 * stateful retry, if a {@link RetryPolicy} is provided. The
	 * {@link ItemRecoverer} is used (if provided) in the case of an exception
	 * to apply alternate processing to the item. If the stateful retry is in
	 * place then the recovery will happen in the next transaction
	 * automatically, otherwise it might be necessary for clients to make the
	 * recover method transactional with appropriate propagation behaviour
	 * (probably REQUIRES_NEW because the call will happen in the context of a
	 * transaction that is about to rollback).
	 * 
	 * @see org.springframework.batch.core.tasklet.Tasklet#execute()
	 */
	public ExitStatus execute() throws Exception {

		if (retryCallback == null) {
			Object item = itemProvider.read();
			if (item == null) {
				return ExitStatus.FINISHED;
			}
			try {
				itemWriter.write(item);
			}
			catch (Exception e) {
				if (itemRecoverer != null) {
					itemRecoverer.recover(item, e);
				}
				// Re-throw the exception so that the surrounding transaction
				// rolls back if there is one
				throw e;
			}
			return ExitStatus.CONTINUABLE;
		}

		return new ExitStatus(template.execute(retryCallback) != null);

	}

	/**
	 * @param itemProvider
	 */
	public void setItemReader(ItemReader itemProvider) {
		this.itemProvider = itemProvider;
	}

	/**
	 * @param writer
	 */
	public void setItemWriter(ItemWriter writer) {
		this.itemWriter = writer;
	}

	/**
	 * Setter for injecting optional recovery handler.
	 * 
	 * @param itemRecoverer
	 */
	public void setItemRecoverer(ItemRecoverer itemRecoverer) {
		this.itemRecoverer = itemRecoverer;
	}

	/**
	 * Mark the current item as skipped if possible. If there is a retry policy
	 * in action there is no need to take any action now because it will be
	 * covered by the retry in the next transaction. Otherwise if the reader
	 * and / or writer are {@link Skippable} then delegate to them in that
	 * order.
	 * 
	 * @see org.springframework.batch.io.Skippable#skip()
	 */
	public void skip() {
		if (retryCallback != null) {
			// No need to skip because the recoverer will take any action
			// necessary.
			return;
		}
		if (this.itemProvider instanceof Skippable) {
			((Skippable) this.itemProvider).skip();
		}
		if (this.itemWriter instanceof Skippable) {
			((Skippable) this.itemWriter).skip();
		}
	}

	/**
	 * Public setter for the retryPolicy.
	 * 
	 * @param retyPolicy the retryPolicy to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}
}
