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

import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepContribution;
import org.springframework.batch.execution.step.ItemOrientedStep;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.exception.handler.SimpleLimitExceptionHandler;
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.callback.ItemReaderRetryCallback;
import org.springframework.batch.retry.policy.ItemReaderRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;

/**
 * Factory bean for step that executes its item processing with a stateful
 * retry. Failed items are never skipped, but always cause a rollback. Before a
 * rollback, the {@link Step} makes a record of the failed item, caching it
 * under a key given by the {@link ItemKeyGenerator}. Then when it is
 * re-presented by the {@link ItemReader} it is recognised and retried up to a
 * limit given by the {@link RetryPolicy}. When the retry is exhausted instead
 * of the item being skipped it is handled by an {@link ItemRecoverer}.<br/>
 * 
 * @author Dave Syer
 * 
 */
public class StatefulRetryStepFactoryBean extends DefaultStepFactoryBean {

	private RetryPolicy retryPolicy;

	private ItemKeyGenerator itemKeyGenerator;

	private ItemRecoverer itemRecoverer;

	/**
	 * Public setter for the {@link RetryPolicy}.
	 * @param retryPolicy the {@link RetryPolicy} to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Public setter for the {@link ItemKeyGenerator} which will be used to
	 * cache failed items between transactions. If it is not injected but the
	 * reader or writer implement {@link ItemKeyGenerator}, one of those will
	 * be used instead (preferring the reader to the writer if both would be
	 * appropriate). If neither can be used, then the default will be to just
	 * use the item itself as a cache key.
	 * 
	 * @param itemKeyGenerator the {@link ItemKeyGenerator} to set
	 */
	public void setItemKeyGenerator(ItemKeyGenerator itemKeyGenerator) {
		this.itemKeyGenerator = itemKeyGenerator;
	}

	/**
	 * Public setter for the {@link ItemRecoverer}. If this is set the
	 * {@link ItemRecoverer#recover(Object, Throwable)} will be called when
	 * retry is exhausted, and within the business transaction (which will not
	 * roll back because of any other item-related errors).
	 * 
	 * @param itemRecoverer the {@link ItemRecoverer} to set
	 */
	public void setItemRecoverer(ItemRecoverer itemRecoverer) {
		this.itemRecoverer = itemRecoverer;
	}

	/**
	 * @param step
	 * 
	 */
	protected void applyConfiguration(ItemOrientedStep step) {

		super.applyConfiguration(step);

		if (retryPolicy != null) {

			// TODO: actually we need to co-ordinate the retry policy with the
			// exception handler limit, so this is a hack for now.
			getStepOperations().setExceptionHandler(new SimpleLimitExceptionHandler(Integer.MAX_VALUE));

			ItemReaderRetryCallback retryCallback = new ItemReaderRetryCallback(getItemReader(), itemKeyGenerator,
					getItemWriter());
			retryCallback.setRecoverer(itemRecoverer);
			ItemReaderRetryPolicy itemProviderRetryPolicy = new ItemReaderRetryPolicy(retryPolicy);

			RetryTemplate retryTemplate = new RetryTemplate();
			retryTemplate.setRetryPolicy(itemProviderRetryPolicy);

			StatefulRetryItemHandler itemProcessor = new StatefulRetryItemHandler(getItemReader(), getItemWriter(),
					retryTemplate, retryCallback);

			step.setItemProcessor(itemProcessor);

		}

	}

	private static class StatefulRetryItemHandler extends SimpleItemHandler {

		final private RetryOperations retryOperations;

		final private ItemReaderRetryCallback retryCallback;

		/**
		 * @param itemReader
		 * @param itemWriter
		 * @param retryCallback
		 * @param retryTemplate
		 */
		public StatefulRetryItemHandler(ItemReader itemReader, ItemWriter itemWriter, RetryOperations retryTemplate,
				ItemReaderRetryCallback retryCallback) {
			super(itemReader, itemWriter);
			this.retryOperations = retryTemplate;
			this.retryCallback = retryCallback;
		}

		/**
		 * Execute the business logic, delegating to the reader and writer.
		 * Subclasses could extend the behaviour as long as they always return
		 * the value of this method call in their superclass.<br/>
		 * 
		 * Read from the {@link ItemReader} and process (if not null) with the
		 * {@link ItemWriter}. The call to {@link ItemWriter} is wrapped in a
		 * stateful retry. In that case the {@link ItemRecoverer} is used (if
		 * provided) in the case of an exception to apply alternate processing
		 * to the item. If the stateful retry is in place then the recovery will
		 * happen in the next transaction automatically, otherwise it might be
		 * necessary for clients to make the recover method transactional with
		 * appropriate propagation behaviour (probably REQUIRES_NEW because the
		 * call will happen in the context of a transaction that is about to
		 * rollback).<br/>
		 * 
		 * @param contribution the current step
		 * @return {@link ExitStatus#CONTINUABLE} if there is more processing to
		 * do
		 * @throws Exception if there is an error
		 */
		public ExitStatus handle(StepContribution contribution) throws Exception {
			return new ExitStatus(retryOperations.execute(retryCallback) != null);
		}

	}
}
