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

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.item.AbstractItemWriter;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.RetryListener;
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.backoff.BackOffPolicy;
import org.springframework.batch.retry.callback.RecoveryRetryCallback;
import org.springframework.batch.retry.policy.RecoveryCallbackRetryPolicy;
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
 * instead of counting all exceptions.<br/>
 * 
 * This class is not designed for extension. Do not subclass it.
 * 
 * @author Dave Syer
 * 
 */
public class StatefulRetryStepFactoryBean extends SkipLimitStepFactoryBean {

	private ItemRecoverer itemRecoverer;

	private int retryLimit;

	private Class[] retryableExceptionClasses;

	private BackOffPolicy backOffPolicy;

	private RetryListener[] retryListeners;

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
	 * Public setter for the {@link RetryListener}s.
	 * @param retryListeners the {@link RetryListener}s to set
	 */
	public void setRetryListeners(RetryListener[] retryListeners) {
		this.retryListeners = retryListeners;
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

			addFatalExceptionIfMissing(RetryException.class);

			SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(retryLimit);
			if (retryableExceptionClasses != null) {
				retryPolicy.setRetryableExceptionClasses(retryableExceptionClasses);
				retryPolicy.setFatalExceptionClasses(getFatalExceptionClasses());
			}

			// Co-ordinate the retry policy with the exception handler:
			getStepOperations().setExceptionHandler(
					new SimpleRetryExceptionHandler(retryPolicy, getExceptionHandler(), getFatalExceptionClasses()));

			RecoveryCallbackRetryPolicy recoveryCallbackRetryPolicy = new RecoveryCallbackRetryPolicy(retryPolicy);

			RetryTemplate retryTemplate = new RetryTemplate();
			if (retryListeners != null) {
				retryTemplate.setListeners(retryListeners);
			}
			retryTemplate.setRetryPolicy(recoveryCallbackRetryPolicy);
			if (backOffPolicy != null) {
				retryTemplate.setBackOffPolicy(backOffPolicy);
			}

			StatefulRetryItemHandler itemHandler = new StatefulRetryItemHandler(getItemReader(), getItemWriter(),
					retryTemplate, getItemKeyGenerator(), itemRecoverer);
			itemHandler.setItemSkipPolicy(getItemSkipPolicy());

			step.setItemHandler(itemHandler);

		}

	}

	/**
	 * Extend the skipping handler because we want to take advantage of that
	 * behaviour as well as the retry. So if there is an exception on input it
	 * is skipped if allowed. If there is an exception on output, it will be
	 * re-thrown in any case, and the behaviour when the item is next
	 * encountered depends on the retryable and skippable exception
	 * configuration. Skip takes precedence, so if the exception was skippable
	 * the item will be skipped on input and the reader moves to the next item.
	 * If the exception is retryable but not skippable, then the write will be
	 * attempted up again up to the retry limit. Beyond the retry limit recovery
	 * takes over.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private static class StatefulRetryItemHandler extends ItemSkipPolicyItemHandler {

		final private RetryOperations retryOperations;

		final private ItemKeyGenerator itemKeyGenerator;

		final private ItemRecoverer itemRecoverer;

		/**
		 * @param itemReader
		 * @param itemWriter
		 * @param retryTemplate
		 * @param itemKeyGenerator
		 * @param itemRecoverer
		 */
		public StatefulRetryItemHandler(ItemReader itemReader, ItemWriter itemWriter, RetryOperations retryTemplate,
				ItemKeyGenerator itemKeyGenerator, ItemRecoverer itemRecoverer) {
			super(itemReader, itemWriter);
			this.retryOperations = retryTemplate;
			this.itemKeyGenerator = itemKeyGenerator;
			this.itemRecoverer = itemRecoverer;
		}

		/**
		 * Execute the business logic, delegating to the writer.<br/>
		 * 
		 * Process the item with the {@link ItemWriter} in a stateful retry. The
		 * {@link ItemRecoverer} is used (if provided) in the case of an
		 * exception to apply alternate processing to the item. If the stateful
		 * retry is in place then the recovery will happen in the next
		 * transaction automatically, otherwise it might be necessary for
		 * clients to make the recover method transactional with appropriate
		 * propagation behaviour (probably REQUIRES_NEW because the call will
		 * happen in the context of a transaction that is about to rollback).<br/>
		 * 
		 * @see org.springframework.batch.core.step.item.SimpleItemHandler#write(java.lang.Object,
		 * org.springframework.batch.core.StepContribution)
		 */
		protected void write(final Object item, final StepContribution contribution) throws Exception {
			final ItemWriter writer = new RetryableItemWriter(contribution);
			RecoveryRetryCallback retryCallback = new RecoveryRetryCallback(item, new RetryCallback() {
				public Object doWithRetry(RetryContext context) throws Throwable {
					writer.write(item);
					return null;
				}
			}, itemKeyGenerator != null ? itemKeyGenerator.getKey(item) : item);
			retryCallback.setRecoveryCallback(new RecoveryCallback() {
				public Object recover(RetryContext context) {
					if (itemRecoverer != null) {
						return itemRecoverer.recover(item, context.getLastThrowable());
					}
					return null;
				}
			});
			retryOperations.execute(retryCallback);
		}

		/**
		 * @author Dave Syer
		 * 
		 */
		private class RetryableItemWriter extends AbstractItemWriter {

			private StepContribution contribution;

			/**
			 * @param contribution
			 */
			public RetryableItemWriter(StepContribution contribution) {
				this.contribution = contribution;
			}

			public void write(Object item) throws Exception {
				doWriteWithSkip(item, contribution);
			}

		}

	}

}
