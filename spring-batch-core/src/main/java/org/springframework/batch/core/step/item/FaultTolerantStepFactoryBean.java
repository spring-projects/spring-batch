package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableReadException;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.RetryListener;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.backoff.BackOffPolicy;
import org.springframework.batch.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.batch.retry.policy.MapRetryContextCache;
import org.springframework.batch.retry.policy.RetryContextCache;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
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
public class FaultTolerantStepFactoryBean<T, S> extends SimpleStepFactoryBean<T, S> {

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

	private boolean isReaderTransactionalQueue = false;

	public void setIsReaderTransactionalQueue(boolean isReaderTransactionalQueue) {
		this.isReaderTransactionalQueue = isReaderTransactionalQueue;
	}

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
			addFatalExceptionIfMissing(NonSkippableReadException.class);
			addFatalExceptionIfMissing(SkipListenerFailedException.class);
			addFatalExceptionIfMissing(RetryException.class);

			if (retryPolicy == null) {

				SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(retryLimit);
				if (!retryableExceptionClasses.isEmpty()) { // otherwise we
					// retry all exceptions
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
			BatchRetryTemplate batchRetryTemplate = new BatchRetryTemplate();
			if (backOffPolicy != null) {
				batchRetryTemplate.setBackOffPolicy(backOffPolicy);
			}
			batchRetryTemplate.setRetryPolicy(retryPolicy);

			// Co-ordinate the retry policy with the exception handler:
			RepeatOperations stepOperations = getStepOperations();
			if (stepOperations instanceof RepeatTemplate) {
				SimpleRetryExceptionHandler exceptionHandler = new SimpleRetryExceptionHandler(retryPolicy,
						getExceptionHandler(), fatalExceptionClasses);
				((RepeatTemplate) stepOperations).setExceptionHandler(exceptionHandler);
			}

			if (retryContextCache == null) {
				if (cacheCapacity > 0) {
					batchRetryTemplate.setRetryContextCache(new MapRetryContextCache(cacheCapacity));
				}
			}
			else {
				batchRetryTemplate.setRetryContextCache(retryContextCache);
			}

			if (retryListeners != null) {
				batchRetryTemplate.setListeners(retryListeners);
			}

			List<Class<? extends Throwable>> exceptions = new ArrayList<Class<? extends Throwable>>(
					skippableExceptionClasses);
			SkipPolicy readSkipPolicy = new LimitCheckingItemSkipPolicy(skipLimit, skippableExceptionClasses,
					new ArrayList<Class<? extends Throwable>>(fatalExceptionClasses));
			exceptions.addAll(new ArrayList<Class<? extends Throwable>>(retryableExceptionClasses));
			SkipPolicy writeSkipPolicy = new LimitCheckingItemSkipPolicy(skipLimit, exceptions,
					new ArrayList<Class<? extends Throwable>>(fatalExceptionClasses));
			
			Classifier<Throwable, Boolean> rollbackClassifier = new Classifier<Throwable, Boolean>() {
				public Boolean classify(Throwable classifiable) {
					return getTransactionAttribute().rollbackOn(classifiable);
				}
			};

			FaultTolerantChunkProvider<T> chunkProvider = new FaultTolerantChunkProvider<T>(getItemReader(),
					getChunkOperations());
			chunkProvider.setSkipPolicy(readSkipPolicy);
			chunkProvider.setListeners(BatchListenerFactoryHelper.<ItemReadListener<T>>getListeners(getListeners(), ItemReadListener.class));
			chunkProvider.setListeners(BatchListenerFactoryHelper.<SkipListener<T,S>>getListeners(getListeners(), SkipListener.class));

			FaultTolerantChunkProcessor<T, S> chunkProcessor = new FaultTolerantChunkProcessor<T, S>(getItemProcessor(), getItemWriter(), batchRetryTemplate);
			chunkProcessor.setBuffering(!isReaderTransactionalQueue);
			chunkProcessor.setWriteSkipPolicy(writeSkipPolicy);
			chunkProcessor.setProcessSkipPolicy(writeSkipPolicy);
			chunkProcessor.setRollbackClassifier(rollbackClassifier);
			chunkProcessor.setListeners(BatchListenerFactoryHelper.<ItemProcessListener<T,S>>getListeners(getListeners(), ItemProcessListener.class));
			chunkProcessor.setListeners(BatchListenerFactoryHelper.<ItemWriteListener<S>>getListeners(getListeners(), ItemWriteListener.class));
			chunkProcessor.setListeners(BatchListenerFactoryHelper.<SkipListener<T,S>>getListeners(getListeners(), SkipListener.class));

			ChunkOrientedTasklet<T> tasklet = new ChunkOrientedTasklet<T>(chunkProvider, chunkProcessor);
			tasklet.setBuffering(!isReaderTransactionalQueue);

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

}
