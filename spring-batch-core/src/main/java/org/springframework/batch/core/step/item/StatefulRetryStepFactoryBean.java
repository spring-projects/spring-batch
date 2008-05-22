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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.listener.CompositeSkipListener;
import org.springframework.batch.core.step.skip.ItemSkipPolicy;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
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
 * retry. Failed items where the exception is classified as retryable always
 * cause a rollback. Before a rollback, the {@link Step} makes a record of the
 * failed item, caching it under a key given by the {@link ItemKeyGenerator}.
 * Then when it is re-presented by the {@link ItemReader} it is recognised and
 * retried up to a limit given by the {@link RetryPolicy}. When the retry is
 * exhausted the item is skipped and handled by a {@link SkipListener} if one is
 * present.<br/>
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
	 * @param step
	 * 
	 */
	protected void applyConfiguration(ItemOrientedStep step) {

		super.applyConfiguration(step);

		if (retryLimit > 0) {

			addFatalExceptionIfMissing(RetryException.class);

			SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(retryLimit);
			if (retryableExceptionClasses != null) { // otherwise we retry
				// all exceptions
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

			List exceptions = new ArrayList(Arrays.asList(getSkippableExceptionClasses()));
			if (retryableExceptionClasses != null) {
				exceptions.addAll(Arrays.asList(retryableExceptionClasses));
			}
			LimitCheckingItemSkipPolicy itemSkipPolicy = new LimitCheckingItemSkipPolicy(getSkipLimit(), exceptions,
					Arrays.asList(getFatalExceptionClasses()));
			StatefulRetryItemHandler itemHandler = new StatefulRetryItemHandler(getItemReader(), getItemWriter(),
					retryTemplate, getItemKeyGenerator(), itemSkipPolicy);
			itemHandler.setSkipListeners(new BatchListenerFactoryHelper().getSkipListeners(getListeners()));

			step.setItemHandler(itemHandler);

		}

	}

	/**
	 * If there is an exception on input it is skipped if allowed. If there is
	 * an exception on output, it will be re-thrown in any case, and the
	 * behaviour when the item is next encountered depends on the retryable and
	 * skippable exception configuration. If the exception is retryable the
	 * write will be attempted again up to the retry limit. When retry attempts
	 * are exhausted the skip listener is invoked and the skip count
	 * incremented. A retryable exception is thus also effectively also
	 * implicitly skippable.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private static class StatefulRetryItemHandler extends SimpleItemHandler {

		final private RetryOperations retryOperations;

		final private ItemKeyGenerator itemKeyGenerator;

		private CompositeSkipListener listener = new CompositeSkipListener();

		final private ItemSkipPolicy itemSkipPolicy;

		/**
		 * @param itemReader
		 * @param itemWriter
		 * @param retryTemplate
		 * @param itemKeyGenerator
		 */
		public StatefulRetryItemHandler(ItemReader itemReader, ItemWriter itemWriter, RetryOperations retryTemplate,
				ItemKeyGenerator itemKeyGenerator, ItemSkipPolicy itemSkipPolicy) {
			super(itemReader, itemWriter);
			this.retryOperations = retryTemplate;
			this.itemKeyGenerator = itemKeyGenerator;
			this.itemSkipPolicy = itemSkipPolicy;
		}

		/**
		 * Register some {@link SkipListener}s with the handler. Each will get
		 * the callbacks in the order specified at the correct stage if a skip
		 * occurs.
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
		 * Tries to read the item from the reader, in case of exception skip the
		 * item if the skip policy allows, otherwise re-throw.
		 * 
		 * @param contribution current StepContribution holding skipped items
		 * count
		 * @return next item for processing
		 */
		protected Object read(StepContribution contribution) throws Exception {

			while (true) {
				try {
					return doRead();
				}
				catch (Exception e) {
					try {
						if (itemSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
							// increment skip count and try again
							contribution.incrementTemporaryReadSkipCount();
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
						// we are headed for a abnormal ending so bake in the
						// skip count
						contribution.combineSkipCounts();
						throw ex;
					}
				}
			}

		}

		/**
		 * Execute the business logic, delegating to the writer.<br/>
		 * 
		 * Process the item with the {@link ItemWriter} in a stateful retry. Any
		 * {@link SkipListener} provided is called when retry attempts are
		 * exhausted. The listener callback (on write failure) will happen in
		 * the next transaction automatically.<br/>
		 * 
		 * @see org.springframework.batch.core.step.item.SimpleItemHandler#write(java.lang.Object,
		 * org.springframework.batch.core.StepContribution)
		 */
		protected void write(final Object item, final StepContribution contribution) throws Exception {
			RecoveryRetryCallback retryCallback = new RecoveryRetryCallback(item, new RetryCallback() {
				public Object doWithRetry(RetryContext context) throws Throwable {
					doWrite(item);
					return null;
				}
			}, itemKeyGenerator != null ? itemKeyGenerator.getKey(item) : item);
			retryCallback.setRecoveryCallback(new RecoveryCallback() {
				public Object recover(RetryContext context) {
					Throwable t = context.getLastThrowable();
					// TODO: add retryable exceptions as well? (Or ensure that
					// all retryable exceptions are skippable?)
					if (itemSkipPolicy.shouldSkip(t, contribution.getStepSkipCount())) {
						listener.onSkipInWrite(item, t);
					}
					contribution.incrementWriteSkipCount();
					return null;
				}
			});
			retryOperations.execute(retryCallback);
		}

	}

}
