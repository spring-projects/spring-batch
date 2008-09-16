package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.skip.ItemSkipPolicy;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.RetryListener;
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.backoff.BackOffPolicy;
import org.springframework.batch.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.batch.retry.policy.MapRetryContextCache;
import org.springframework.batch.retry.policy.RetryContextCache;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
import org.springframework.batch.retry.support.DefaultRetryState;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.batch.support.Classifier;

/**
 * Factory bean for step that provides options for configuring skip behaviour.
 * User can set {@link #setSkipLimit(int)} to set how many exceptions of
 * {@link #setSkippableExceptionClasses(Collection)} types are tolerated.
 * {@link #setFatalExceptionClasses(Collection)} will cause immediate
 * termination of job - they are treated as higher priority than
 * {@link #setSkippableExceptionClasses(Collection)}, so the two lists don't
 * need to be exclusive.
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
public class SkipLimitStepFactoryBean<T, S> extends SimpleStepFactoryBean<T, S> {

	private int skipLimit = 0;

	private Collection<Class<? extends Throwable>> skippableExceptionClasses = new HashSet<Class<? extends Throwable>>();

	private Collection<Class<? extends Throwable>> fatalExceptionClasses = new HashSet<Class<? extends Throwable>>();

	{
		fatalExceptionClasses.add(Error.class);
		skippableExceptionClasses.add(Exception.class);
	}

	private int cacheCapacity = 0;

	private int retryLimit = 0;

	private Collection<Class<? extends Throwable>> retryableExceptionClasses = new HashSet<Class<? extends Throwable>>();

	private BackOffPolicy backOffPolicy;

	private RetryListener[] retryListeners;

	private RetryPolicy retryPolicy;

	private RetryContextCache retryContextCache;

	/**
	 * Setter for the retry policy. If this is specified the other retry
	 * properties are ignored (retryLimit, backOffPolicy,
	 * retryableExceptionClasses).
	 * 
	 * @param retryPolicy a stateless {@link RetryPolicy}
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Public setter for the retry limit. Each item can be retried up to this
	 * limit.
	 * @param retryLimit the retry limit to set
	 */
	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	/**
	 * Public setter for the capacity of the cache in the retry policy. If more
	 * items than this fail without being skipped or recovered an exception will
	 * be thrown. This is to guard against inadvertent infinite loops generated
	 * by item identity problems.<br/>
	 * 
	 * The default value should be high enough and more for most purposes. To
	 * breach the limit in a single-threaded step typically you have to have
	 * this many failures in a single transaction. Defaults to the value in the
	 * {@link MapRetryContextCache}.<br/>
	 * 
	 * This property is ignored if the
	 * {@link #setRetryContextCache(RetryContextCache)} is set directly.
	 * 
	 * @param cacheCapacity the cache capacity to set (greater than 0 else
	 * ignored)
	 */
	public void setCacheCapacity(int cacheCapacity) {
		this.cacheCapacity = cacheCapacity;
	}

	/**
	 * Override the default retry context cache for retry of chunk processing.
	 * If this property is set then {@link #setCacheCapacity(int)} is ignored.
	 * 
	 * @param retryContextCache the {@link RetryContextCache} to set
	 */
	public void setRetryContextCache(RetryContextCache retryContextCache) {
		this.retryContextCache = retryContextCache;
	}

	/**
	 * Public setter for the Class[].
	 * @param retryableExceptionClasses the retryableExceptionClasses to set
	 */
	public void setRetryableExceptionClasses(Collection<Class<? extends Throwable>> retryableExceptionClasses) {
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
	 * Public setter for a limit that determines skip policy. If this value is
	 * positive then an exception in chunk processing will cause the item to be
	 * skipped and no exception propagated until the limit is reached. If it is
	 * zero then all exceptions will be propagated from the chunk and cause the
	 * step to abort.
	 * 
	 * @param skipLimit the value to set. Default is 0 (never skip).
	 */
	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	/**
	 * Public setter for exception classes that when raised won't crash the job
	 * but will result in transaction rollback and the item which handling
	 * caused the exception will be skipped.
	 * 
	 * @param exceptionClasses defaults to <code>Exception</code>
	 */
	public void setSkippableExceptionClasses(Collection<Class<? extends Throwable>> exceptionClasses) {
		this.skippableExceptionClasses = exceptionClasses;
	}

	/**
	 * Public setter for exception classes that should cause immediate failure.
	 * 
	 * @param fatalExceptionClasses {@link Error} by default
	 */
	public void setFatalExceptionClasses(Collection<Class<? extends Throwable>> fatalExceptionClasses) {
		this.fatalExceptionClasses = fatalExceptionClasses;
	}

	/**
	 * Uses the {@link #setSkipLimit(int)} value to configure item handler and
	 * and exception handler.
	 */
	protected void applyConfiguration(TaskletStep step) {
		super.applyConfiguration(step);

		if (retryLimit > 0 || skipLimit > 0 || retryPolicy != null) {

			addFatalExceptionIfMissing(SkipLimitExceededException.class);
			addFatalExceptionIfMissing(SkipListenerFailedException.class);
			addFatalExceptionIfMissing(RetryException.class);

			if (retryPolicy == null) {

				SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(retryLimit);
				if (!retryableExceptionClasses.isEmpty()) { // otherwise we
					// retry
					// all exceptions
					simpleRetryPolicy.setRetryableExceptionClasses(retryableExceptionClasses);
				}
				simpleRetryPolicy.setFatalExceptionClasses(fatalExceptionClasses);

				ExceptionClassifierRetryPolicy classifierRetryPolicy = new ExceptionClassifierRetryPolicy();
				HashMap<Class<? extends Throwable>, RetryPolicy> exceptionTypeMap = new HashMap<Class<? extends Throwable>, RetryPolicy>();
				for (Class<? extends Throwable> cls : retryableExceptionClasses) {
					exceptionTypeMap.put(cls, simpleRetryPolicy);
				}
				classifierRetryPolicy.setPolicyMap(exceptionTypeMap);
				retryPolicy = classifierRetryPolicy;

			}
			RetryTemplate retryTemplate = new RetryTemplate();
			if (backOffPolicy != null) {
				retryTemplate.setBackOffPolicy(backOffPolicy);
			}
			retryTemplate.setRetryPolicy(retryPolicy);
			Classifier<Throwable, Boolean> rollbackClassifier = new Classifier<Throwable, Boolean>() {
				public Boolean classify(Throwable classifiable) {
					return getTransactionAttribute().rollbackOn(classifiable);
				}
			};

			// Co-ordinate the retry policy with the exception handler:
			RepeatOperations stepOperations = getStepOperations();
			if (stepOperations instanceof RepeatTemplate) {
				((RepeatTemplate) stepOperations).setExceptionHandler(new SimpleRetryExceptionHandler(retryPolicy,
						getExceptionHandler(), fatalExceptionClasses));
			}

			if (retryContextCache == null) {
				if (cacheCapacity > 0) {
					retryTemplate.setRetryContextCache(new MapRetryContextCache(cacheCapacity));
				}
			}
			else {
				retryTemplate.setRetryContextCache(retryContextCache);
			}

			if (retryListeners != null) {
				retryTemplate.setListeners(retryListeners);
			}

			List<Class<? extends Throwable>> exceptions = new ArrayList<Class<? extends Throwable>>(
					skippableExceptionClasses);
			ItemSkipPolicy readSkipPolicy = new LimitCheckingItemSkipPolicy(skipLimit, skippableExceptionClasses,
					new ArrayList<Class<? extends Throwable>>(fatalExceptionClasses));
			exceptions.addAll(new ArrayList<Class<? extends Throwable>>(retryableExceptionClasses));
			ItemSkipPolicy writeSkipPolicy = new LimitCheckingItemSkipPolicy(skipLimit, exceptions,
					new ArrayList<Class<? extends Throwable>>(fatalExceptionClasses));
			ChunkOrientedTasklet<T, S> tasklet = new StatefulRetryTasklet<T, S>(getItemReader(), getItemProcessor(),
					getItemWriter(), getChunkOperations(), retryTemplate, rollbackClassifier, readSkipPolicy,
					writeSkipPolicy, writeSkipPolicy);
			tasklet.setListeners(getListeners());

			step.setTasklet(tasklet);

		}

	}

	public void addFatalExceptionIfMissing(Class<? extends Throwable> cls) {
		List<Class<? extends Throwable>> fatalExceptionList = new ArrayList<Class<? extends Throwable>>();
		for (Class<? extends Throwable> exceptionClass : fatalExceptionClasses) {
			fatalExceptionList.add(exceptionClass);
		}
		if (!fatalExceptionList.contains(cls)) {
			fatalExceptionList.add(cls);
		}
		fatalExceptionClasses = fatalExceptionList;
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
	static class StatefulRetryTasklet<T, S> extends ChunkOrientedTasklet<T, S> {

		final private RetryOperations retryOperations;

		final private ItemSkipPolicy readSkipPolicy;

		final private ItemSkipPolicy writeSkipPolicy;

		final private ItemSkipPolicy processSkipPolicy;

		final private Classifier<Throwable, Boolean> rollbackClassifier;

		/**
		 * @param itemReader
		 * @param itemWriter
		 * @param retryTemplate
		 */
		public StatefulRetryTasklet(ItemReader<? extends T> itemReader,
				ItemProcessor<? super T, ? extends S> itemProcessor, ItemWriter<? super S> itemWriter,
				RepeatOperations chunkOperations, RetryOperations retryTemplate,
				Classifier<Throwable, Boolean> rollbackClassifier, ItemSkipPolicy readSkipPolicy,
				ItemSkipPolicy writeSkipPolicy, ItemSkipPolicy processSkipPolicy) {
			super(itemReader, itemProcessor, itemWriter, chunkOperations);
			this.retryOperations = retryTemplate;
			this.rollbackClassifier = rollbackClassifier;
			this.readSkipPolicy = readSkipPolicy;
			this.writeSkipPolicy = writeSkipPolicy;
			this.processSkipPolicy = processSkipPolicy;
		}

		/**
		 * Tries to read the item from the reader, in case of exception skip the
		 * item if the skip policy allows, otherwise re-throw.
		 * 
		 * @param contribution current StepContribution holding skipped items
		 * count
		 * @return next item for processing
		 */
		@Override
		protected T read(StepContribution contribution) throws Exception {

			while (true) {
				try {
					return doRead();
				}
				catch (Exception e) {
					try {
						if (readSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
							// increment skip count and try again
							contribution.incrementReadSkipCount();
							try {
								listener.onSkipInRead(e);
							}
							catch (RuntimeException ex) {
								throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, e);
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
						throw ex;
					}
				}
			}

		}

		/**
		 * Incorporate retry into the item processor stage.
		 * 
		 * @see org.springframework.batch.core.step.item.ChunkOrientedTasklet#process(org.springframework.batch.core.StepContribution,
		 * org.springframework.batch.core.step.item.Chunk,
		 * org.springframework.batch.core.step.item.Chunk)
		 */
		@Override
		protected void process(final StepContribution contribution, final Chunk<T> inputs, final Chunk<S> outputs)
				throws Exception {

			int filtered = 0;

			for (final Chunk<T>.ChunkIterator iterator = inputs.iterator(); iterator.hasNext();) {

				final T item = iterator.next();

				RetryCallback<S> retryCallback = new RetryCallback<S>() {

					public S doWithRetry(RetryContext context) throws Exception {
						S output = doProcess(item);
						return output;
					}

				};

				RecoveryCallback<S> recoveryCallback = new RecoveryCallback<S>() {

					public S recover(RetryContext context) throws Exception {
						Exception e = (Exception) context.getLastThrowable();
						if (processSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
							contribution.incrementProcessSkipCount();
							iterator.remove(e);
							return null;
						}
						else {
							throw new RetryException("Non-skippable exception in recoverer while processing", e);
						}
					}

				};

				S output = retryOperations.execute(retryCallback, recoveryCallback, new DefaultRetryState(item, rollbackClassifier));
				if (output != null) {
					outputs.add(output);
				}
				else {
					filtered++;
				}

			}

			for (ItemWrapper<T> skip : inputs.getSkips()) {
				Exception exception = skip.getException();
				try {
					listener.onSkipInProcess(skip.getItem(), exception);
				}
				catch (RuntimeException e) {
					throw new SkipListenerFailedException("Fatal exception in SkipListener.", e, exception);
				}
			}

			contribution.incrementFilterCount(filtered);

			inputs.clear();

		}

		/**
		 * Execute the business logic, delegating to the writer.<br/>
		 * 
		 * Process the items with the {@link ItemWriter} in a stateful retry.
		 * Any {@link SkipListener} provided is called when retry attempts are
		 * exhausted. The listener callback (on write failure) will happen in
		 * the next transaction automatically.<br/>
		 */
		@Override
		protected void write(final Chunk<S> chunk, final StepContribution contribution) throws Exception {

			RetryCallback<Object> retryCallback = new RetryCallback<Object>() {
				public Object doWithRetry(RetryContext context) throws Exception {
					doWrite(chunk.getItems());
					contribution.incrementWriteCount(chunk.size());
					return null;
				}
			};

			RecoveryCallback<Object> recoveryCallback = new RecoveryCallback<Object>() {

				public Object recover(RetryContext context) throws Exception {

					// small optimisation: if there was only one item, then we
					// don't have to try writing it again to see if it fails...
					if (chunk.size() == 1) {
						Exception e = (Exception) context.getLastThrowable();
						checkSkipPolicy(contribution, chunk.iterator(), e);
						return null;
					}

					for (Chunk<S>.ChunkIterator iterator = chunk.iterator(); iterator.hasNext();) {
						S item = iterator.next();
						try {
							doWrite(Collections.singletonList(item));
							contribution.incrementWriteCount(1);
						}
						catch (Exception e) {
							checkSkipPolicy(contribution, iterator, e);
							if (rollbackClassifier.classify(e)) {
								throw e;
							}
							else {
								logger.error("Exception encountered that does not classify for rollback: ", e);
							}
						}
					}

					return null;

				}

				private void checkSkipPolicy(final StepContribution contribution, Chunk<S>.ChunkIterator iterator,
						Exception e) throws Exception {
					if (writeSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
						contribution.incrementWriteSkipCount();
						iterator.remove(e);
					}
					else {
						throw new RetryException("Non-skippable exception in recoverer", e);
					}
				}
			};

			retryOperations.execute(retryCallback, recoveryCallback, new DefaultRetryState(chunk,rollbackClassifier));

			for (ItemWrapper<S> skip : chunk.getSkips()) {
				Exception exception = skip.getException();
				try {
					listener.onSkipInWrite(skip.getItem(), exception);
				}
				catch (RuntimeException e) {
					throw new SkipListenerFailedException("Fatal exception in SkipListener.", e, exception);
				}
			}

			chunk.clear();

		}
	}

}
