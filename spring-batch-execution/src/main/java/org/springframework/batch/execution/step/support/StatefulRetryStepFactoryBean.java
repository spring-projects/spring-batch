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
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.backoff.BackOffPolicy;
import org.springframework.batch.retry.callback.ItemReaderRetryCallback;
import org.springframework.batch.retry.policy.ItemReaderRetryPolicy;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
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
 * The skipLimit property is still used to control the overall exception
 * handling policy. Only exhausted retries count against the exception handler,
 * instead of counting all exceptions.
 * 
 * @author Dave Syer
 * 
 */
public class StatefulRetryStepFactoryBean extends DefaultStepFactoryBean {

	private ItemKeyGenerator itemKeyGenerator;

	private ItemRecoverer itemRecoverer;

	private int retryLimit;

	private Class[] retryableExceptionClasses;

	private BackOffPolicy backOffPolicy;

	/**
	 * Public setter for the retry limit. Each item can be retried up to this
	 * limit.
	 * @param retryLimit the retry limit to set
	 */
	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	/**
	 * Public setter for the Class[].
	 * @param retryableExceptionClasses the retryableExceptionClasses to set
	 */
	public void setRetryableExceptionClasses(Class[] retryableExceptionClasses) {
		this.retryableExceptionClasses = retryableExceptionClasses;
	}

	/**
	 * Public setter for the {@link BackOffPolicy}.
	 * @param backOffPolicy the {@link BackOffPolicy} to set
	 */
	public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
		this.backOffPolicy = backOffPolicy;
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

		if (retryLimit > 0) {

			SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(retryLimit);
			if (retryableExceptionClasses != null) {
				retryPolicy.setRetryableExceptionClasses(retryableExceptionClasses);
			}

			// Co-ordinate the retry policy with the exception handler:
			getStepOperations()
					.setExceptionHandler(new SimpleRetryExceptionHandler(retryPolicy, getExceptionHandler()));

			ItemReaderRetryCallback retryCallback = new ItemReaderRetryCallback(getItemReader(), itemKeyGenerator,
					getItemWriter());
			retryCallback.setRecoverer(itemRecoverer);
			ItemReaderRetryPolicy itemProviderRetryPolicy = new ItemReaderRetryPolicy(retryPolicy);

			RetryTemplate retryTemplate = new RetryTemplate();
			retryTemplate.setRetryPolicy(itemProviderRetryPolicy);
			if (backOffPolicy != null) {
				retryTemplate.setBackOffPolicy(backOffPolicy);
			}

			StatefulRetryItemHandler itemProcessor = new StatefulRetryItemHandler(getItemReader(), getItemWriter(),
					retryTemplate, retryCallback);

			step.setItemHandler(itemProcessor);

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
