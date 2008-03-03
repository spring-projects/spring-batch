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
package org.springframework.batch.execution.step.support;

import org.springframework.batch.core.domain.ItemSkipPolicy;
import org.springframework.batch.core.domain.StepContribution;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.callback.ItemReaderRetryCallback;
import org.springframework.batch.retry.support.RetryTemplate;

/**
 * @author Dave Syer
 * 
 */
public class KitchenSinkItemProcessor extends SimpleItemProcessor {

	private RetryOperations retryOperations = new RetryTemplate();

	private ItemReaderRetryCallback retryCallback;

	private ItemSkipPolicy itemSkipPolicy = new NeverSkipItemSkipPolicy();

	/**
	 * @param itemReader
	 * @param itemWriter
	 */
	public KitchenSinkItemProcessor(ItemReader itemReader, ItemWriter itemWriter) {
		super(itemReader, itemWriter);
	}

	/**
	 * @param itemSkipPolicy
	 */
	public void setItemSkipPolicy(ItemSkipPolicy itemSkipPolicy) {
		this.itemSkipPolicy = itemSkipPolicy;
	}

	/**
	 * Public setter for the {@link RetryOperations}.
	 * @param retryOperations the {@link RetryOperations} to set
	 */
	public void setRetryOperations(RetryOperations retryOperations) {
		this.retryOperations = retryOperations;
	}

	/**
	 * Public setter for the ItemReaderRetryCallback. TODO: get rid of this.
	 * @param retryCallback the retryCallback to set
	 */
	public void setRetryCallback(ItemReaderRetryCallback retryCallback) {
		this.retryCallback = retryCallback;
	}

	/**
	 * Execute the business logic, delegating to the reader and writer.
	 * Subclasses could extend the behaviour as long as they always return the
	 * value of this method call in their superclass.<br/>
	 * 
	 * Read from the {@link ItemReader} and process (if not null) with the
	 * {@link ItemWriter}. If a {@link RetryCallback} is provided, then the
	 * call to {@link ItemWriter} is wrapped in a stateful retry. In that case
	 * the {@link ItemRecoverer} is used (if provided) in the case of an
	 * exception to apply alternate processing to the item. If the stateful
	 * retry is in place then the recovery will happen in the next transaction
	 * automatically, otherwise it might be necessary for clients to make the
	 * recover method transactional with appropriate propagation behaviour
	 * (probably REQUIRES_NEW because the call will happen in the context of a
	 * transaction that is about to rollback).<br/>
	 * 
	 * If there is an exception and the reader or writer implements
	 * {@link Skippable} then the skip method is called.
	 * 
	 * @param contribution the current step
	 * @return {@link ExitStatus#CONTINUABLE} if there is more processing to do
	 * @throws Exception if there is an error
	 */
	public ExitStatus process(StepContribution contribution) throws Exception {
		ExitStatus exitStatus = ExitStatus.CONTINUABLE;

		if (retryCallback != null) {
			return new ExitStatus(retryOperations.execute(retryCallback) != null);
		}

		try {

			exitStatus = super.process(contribution);

		}
		catch (Exception e) {

			if (retryCallback == null && itemSkipPolicy.shouldSkip(e, contribution.getSkipCount())) {
				contribution.incrementSkipCount();
				skip();
			}
			else {
				// Rethrow so that outer transaction is rolled back properly
				throw e;
			}

		}

		return exitStatus;
	}

	/**
	 * Mark the current item as skipped if possible. If there is a retry policy
	 * in action there is no need to take any action now because it will be
	 * covered by the retry in the next transaction. Otherwise if the reader and /
	 * or writer are {@link Skippable} then delegate to them in that order.
	 * 
	 * @see org.springframework.batch.io.Skippable#skip()
	 */
	private void skip() {
		if (retryCallback != null) {
			// No need to skip because the recoverer will take any action
			// necessary.
			return;
		}
		if (getItemReader() instanceof Skippable) {
			((Skippable) getItemReader()).skip();
		}
		if (getItemWriter() instanceof Skippable) {
			((Skippable) getItemWriter()).skip();
		}
	}

}
