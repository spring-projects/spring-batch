package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.listener.CompositeSkipListener;
import org.springframework.batch.core.step.skip.ItemSkipPolicy;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableException;
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
import org.springframework.batch.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.batch.retry.policy.MapRetryContextCache;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.policy.RecoveryCallbackRetryPolicy;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.batch.support.SubclassExceptionClassifier;

/**
 * Factory bean for step that provides options for configuring skip behavior.
 * User can set {@link #setSkipLimit(int)} to set how many exceptions of
 * {@link #setSkippableExceptionClasses(Class[])} types are tolerated.
 * {@link #setFatalExceptionClasses(Class[])} will cause immediate termination
 * of job - they are treated as higher priority than
 * {@link #setSkippableExceptionClasses(Class[])}, so the two lists don't need
 * to be exclusive.
 * 
 * Skippable exceptions on write will by default cause transaction rollback - to
 * avoid rollback for specific exception class include it in the transaction
 * attribute as "no rollback for".
 * 
 * @see SimpleStepFactoryBean
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 */
public class SkipLimitStepFactoryBean extends SimpleStepFactoryBean {

	private int skipLimit = 0;

	private Class[] skippableExceptionClasses = new Class[] { Exception.class };

	private Class[] fatalExceptionClasses = new Class[] { Error.class };

	private ItemKeyGenerator itemKeyGenerator;

	private int cacheCapacity = 0;

	private int retryLimit = 0;

	private Class[] retryableExceptionClasses = new Class[] {};

	private BackOffPolicy backOffPolicy;

	private RetryListener[] retryListeners;

	private RetryPolicy retryPolicy;

	/**
	 * Setter for the retry policy. If this is specified the other retry
	 * properties are ignored (retryLimit, backOffPolicy,
	 * retryableExceptionClasses).
	 * 
	 * @param retryPolicy
	 *            a stateless {@link RetryPolicy}
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Public setter for the retry limit. Each item can be retried up to this
	 * limit.
	 * 
	 * @param retryLimit
	 *            the retry limit to set
	 */
	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	/**
	 * Public setter for the capacity of the cache in the retry policy. If more
	 * items than this fail without being skipped or recovered an exception will
	 * be thrown. This is to guard against inadvertent infinite loops generated
	 * by item identity problems. If a large number of items are failing and not
	 * being recognized as skipped, it usually signals a problem with the key
	 * generation (often equals and hashCode in the item itself). So it is
	 * better to enforce a strict limit than have weird looking errors, where a
	 * skip limit is reached without anything being skipped.<br/>
	 * 
	 * The default value should be high enough and more for most purposes. To
	 * breach the limit in a single-threaded step typically you have to have
	 * this many failures in a single transaction. Defaults to the value in the
	 * {@link MapRetryContextCache}.
	 * 
	 * @param cacheCapacity
	 *            the cacheCapacity to set
	 */
	public void setCacheCapacity(int cacheCapacity) {
		this.cacheCapacity = cacheCapacity;
	}

	/**
	 * Public setter for the Class[].
	 * 
	 * @param retryableExceptionClasses
	 *            the retryableExceptionClasses to set
	 */
	public void setRetryableExceptionClasses(Class[] retryableExceptionClasses) {
		this.retryableExceptionClasses = retryableExceptionClasses;
	}

	/**
	 * Public setter for the {@link BackOffPolicy}.
	 * 
	 * @param backOffPolicy
	 *            the {@link BackOffPolicy} to set
	 */
	public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
		this.backOffPolicy = backOffPolicy;
	}

	/**
	 * Public setter for the {@link RetryListener}s.
	 * 
	 * @param retryListeners
	 *            the {@link RetryListener}s to set
	 */
	public void setRetryListeners(RetryListener[] retryListeners) {
		this.retryListeners = retryListeners;
	}

	/**
	 * Public setter for a limit that determines skip policy. If this value is
	 * positive then an exception in chunk processing will cause the item to be
	 * skipped and no exception propagated until the limit is reached. If it is
	 * zero then all exceptions will be propagated from the chunk and cause the
	 * step to abort.
	 * 
	 * @param skipLimit
	 *            the value to set. Default is 0 (never skip).
	 */
	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	/**
	 * Public setter for exception classes that when raised won't crash the job
	 * but will result in transaction rollback and the item which handling
	 * caused the exception will be skipped.
	 * 
	 * @param exceptionClasses
	 *            defaults to <code>Exception</code>
	 */
	public void setSkippableExceptionClasses(Class[] exceptionClasses) {
		this.skippableExceptionClasses = exceptionClasses;
	}

	/**
	 * Public setter for exception classes that should cause immediate failure.
	 * 
	 * @param fatalExceptionClasses
	 *            {@link Error} by default
	 */
	public void setFatalExceptionClasses(Class[] fatalExceptionClasses) {
		this.fatalExceptionClasses = fatalExceptionClasses;
	}

	/**
	 * Public setter for the {@link ItemKeyGenerator}. This is used to identify
	 * failed items so they can be skipped if encountered again, generally in
	 * another transaction.
	 * 
	 * @param itemKeyGenerator
	 *            the {@link ItemKeyGenerator} to set.
	 */
	public void setItemKeyGenerator(ItemKeyGenerator itemKeyGenerator) {
		this.itemKeyGenerator = itemKeyGenerator;
	}

	/**
	 * Uses the {@link #setSkipLimit(int)} value to configure item handler and
	 * and exception handler.
	 */
	protected void applyConfiguration(ItemOrientedStep step) {
		super.applyConfiguration(step);

		if (retryLimit > 0 || skipLimit > 0 || retryPolicy != null) {

			addFatalExceptionIfMissing(SkipLimitExceededException.class);
			addFatalExceptionIfMissing(NonSkippableException.class);
			addFatalExceptionIfMissing(RetryException.class);

			if (retryPolicy == null) {

				SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(
						retryLimit);
				if (retryableExceptionClasses.length > 0) { // otherwise we
															// retry
					// all exceptions
					simpleRetryPolicy
							.setRetryableExceptionClasses(retryableExceptionClasses);
				}
				simpleRetryPolicy
						.setFatalExceptionClasses(fatalExceptionClasses);

				ExceptionClassifierRetryPolicy classifierRetryPolicy = new ExceptionClassifierRetryPolicy();
				SubclassExceptionClassifier exceptionClassifier = new SubclassExceptionClassifier();
				HashMap exceptionTypeMap = new HashMap();
				for (int i = 0; i < retryableExceptionClasses.length; i++) {
					Class cls = retryableExceptionClasses[i];
					exceptionTypeMap.put(cls, "retry");
				}
				exceptionClassifier.setTypeMap(exceptionTypeMap);
				HashMap retryPolicyMap = new HashMap();
				retryPolicyMap.put("retry", simpleRetryPolicy);
				retryPolicyMap.put("default", new NeverRetryPolicy());
				classifierRetryPolicy.setPolicyMap(retryPolicyMap);
				classifierRetryPolicy
						.setExceptionClassifier(exceptionClassifier);
				retryPolicy = classifierRetryPolicy;

			}

			// Co-ordinate the retry policy with the exception handler:
			getStepOperations().setExceptionHandler(
					new SimpleRetryExceptionHandler(retryPolicy,
							getExceptionHandler(), fatalExceptionClasses));

			RecoveryCallbackRetryPolicy recoveryCallbackRetryPolicy = new RecoveryCallbackRetryPolicy(
					retryPolicy) {
				protected boolean recoverForException(Throwable ex) {
					return !getTransactionAttribute().rollbackOn(ex);
				}
			};
			if (cacheCapacity > 0) {
				recoveryCallbackRetryPolicy
						.setRetryContextCache(new MapRetryContextCache(
								cacheCapacity));
			}

			RetryTemplate retryTemplate = new RetryTemplate();
			if (retryListeners != null) {
				retryTemplate.setListeners(retryListeners);
			}
			retryTemplate.setRetryPolicy(recoveryCallbackRetryPolicy);
			if (retryPolicy == null && backOffPolicy != null) {
				retryTemplate.setBackOffPolicy(backOffPolicy);
			}

			List exceptions = new ArrayList(Arrays
					.asList(skippableExceptionClasses));
			ItemSkipPolicy readSkipPolicy = new LimitCheckingItemSkipPolicy(
					skipLimit, exceptions, Arrays.asList(fatalExceptionClasses));
			exceptions.addAll(Arrays.asList(retryableExceptionClasses));
			ItemSkipPolicy writeSkipPolicy = new LimitCheckingItemSkipPolicy(
					skipLimit, exceptions, Arrays.asList(fatalExceptionClasses));
			StatefulRetryItemHandler itemHandler = new StatefulRetryItemHandler(
					getItemReader(), getItemWriter(), retryTemplate,
					itemKeyGenerator, readSkipPolicy, writeSkipPolicy);
			itemHandler.setSkipListeners(BatchListenerFactoryHelper
					.getSkipListeners(getListeners()));

			step.setItemHandler(itemHandler);

		} else {
			// This is the default in ItemOrientedStep anyway...
			step.setItemHandler(new SimpleItemHandler(getItemReader(),
					getItemWriter()));
		}

	}

	public void addFatalExceptionIfMissing(Class cls) {
		List fatalExceptionList = new ArrayList(Arrays
				.asList(fatalExceptionClasses));
		if (!fatalExceptionList.contains(cls)) {
			fatalExceptionList.add(cls);
		}
		fatalExceptionClasses = (Class[]) fatalExceptionList
				.toArray(new Class[0]);
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

		final private CompositeSkipListener listener = new CompositeSkipListener();

		final private ItemSkipPolicy readSkipPolicy;

		final private ItemSkipPolicy writeSkipPolicy;

		/**
		 * @param itemReader
		 * @param itemWriter
		 * @param retryTemplate
		 * @param itemKeyGenerator
		 */
		public StatefulRetryItemHandler(ItemReader itemReader,
				ItemWriter itemWriter, RetryOperations retryTemplate,
				ItemKeyGenerator itemKeyGenerator,
				ItemSkipPolicy readSkipPolicy, ItemSkipPolicy writeSkipPolicy) {
			super(itemReader, itemWriter);
			this.retryOperations = retryTemplate;
			this.itemKeyGenerator = itemKeyGenerator;
			this.readSkipPolicy = readSkipPolicy;
			this.writeSkipPolicy = writeSkipPolicy;
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
		 * @param listener
		 *            a {@link SkipListener}
		 */
		public void registerSkipListener(SkipListener listener) {
			this.listener.register(listener);
		}

		/**
		 * Tries to read the item from the reader, in case of exception skip the
		 * item if the skip policy allows, otherwise re-throw.
		 * 
		 * @param contribution
		 *            current StepContribution holding skipped items count
		 * @return next item for processing
		 */
		protected Object read(StepContribution contribution) throws Exception {

			while (true) {
				try {
					return doRead();
				} catch (Exception e) {
					try {
						if (readSkipPolicy.shouldSkip(e, contribution
								.getStepSkipCount())) {
							// increment skip count and try again
							contribution.incrementTemporaryReadSkipCount();
							onSkipInRead(e);
							logger.debug("Skipping failed input", e);
						} else {
							throw new NonSkippableException("Non-skippable exception during read", e);
						}
					} catch (SkipLimitExceededException ex) {
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
		 *      org.springframework.batch.core.StepContribution)
		 */
		protected void write(final Object item,
				final StepContribution contribution) throws Exception {
			RecoveryRetryCallback retryCallback = new RecoveryRetryCallback(
					item, new RetryCallback() {
						public Object doWithRetry(RetryContext context)
								throws Throwable {
							doWrite(item);
							return null;
						}
					}, itemKeyGenerator != null ? itemKeyGenerator.getKey(item)
							: item);
			retryCallback.setRecoveryCallback(new RecoveryCallback() {
				public Object recover(RetryContext context) {
					Throwable t = context.getLastThrowable();
					if (writeSkipPolicy.shouldSkip(t, contribution
							.getStepSkipCount())) {
						listener.onSkipInWrite(item, t);
					}
					else {
						throw new NonSkippableException("Non-skippable exception on write", t);
					}
					contribution.incrementWriteSkipCount();
					return null;
				}
			});
			retryOperations.execute(retryCallback);
		}
		
		private void onSkipInRead(Exception e){
			
			try{
				listener.onSkipInRead(e);
			}
			catch(Exception ex){
				logger.debug("Error in SkipListener onSkipInReader encountered and ignored.", ex);
			}
		}

	}

}
