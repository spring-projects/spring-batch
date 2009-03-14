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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.springframework.batch.classify.Classifier;
import org.springframework.batch.core.JobInterruptedException;
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
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.policy.RetryContextCache;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;

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

	private Collection<Class<? extends Throwable>> skippableExceptionClasses = new HashSet<Class<? extends Throwable>>();

	private Collection<Class<? extends Throwable>> fatalExceptionClasses = new HashSet<Class<? extends Throwable>>();

	private Collection<Class<? extends Throwable>> retryableExceptionClasses = new HashSet<Class<? extends Throwable>>();

	{
		fatalExceptionClasses.add(Error.class);
		skippableExceptionClasses.add(Exception.class);
		retryableExceptionClasses.add(Exception.class);
	}

	private int cacheCapacity = 0;

	private int retryLimit = 0;

	private int skipLimit = 0;

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
	 * limit. Note this limit includes the initial attempt to process the item,
	 * therefore <code>retryLimit == 1</code> by default.
	 * 
	 * @param retryLimit the retry limit to set, must be greater or equal to 1.
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
	public void setRetryListeners(RetryListener... retryListeners) {
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

	@Override
	protected void applyConfiguration(TaskletStep step) {

		addFatalExceptionIfMissing(SkipLimitExceededException.class, NonSkippableReadException.class,
				SkipListenerFailedException.class, RetryException.class, JobInterruptedException.class);

		super.applyConfiguration(step);

	}
	
	/**
	 * @return {@link ChunkProvider} configured for fault-tolerance.
	 */
	@Override
	protected FaultTolerantChunkProvider<T> configureChunkProvider() {
		
		SkipPolicy readSkipPolicy = new LimitCheckingItemSkipPolicy(skipLimit, skippableExceptionClasses,
				fatalExceptionClasses);
		FaultTolerantChunkProvider<T> chunkProvider = new FaultTolerantChunkProvider<T>(getItemReader(),
				getChunkOperations());
		chunkProvider.setSkipPolicy(readSkipPolicy);
		
		return chunkProvider;
	}

	/**
	 * @return {@link ChunkProcessor} configured for fault-tolerance.
	 */
	@Override
	protected FaultTolerantChunkProcessor<T, S> configureChunkProcessor() {

		SkipPolicy writeSkipPolicy = new LimitCheckingItemSkipPolicy(skipLimit, skippableExceptionClasses,
				fatalExceptionClasses);

		Classifier<Throwable, Boolean> rollbackClassifier = new Classifier<Throwable, Boolean>() {
			public Boolean classify(Throwable classifiable) {
				return getTransactionAttribute().rollbackOn(classifiable);
			}
		};

		BatchRetryTemplate batchRetryTemplate = configureRetry();

		FaultTolerantChunkProcessor<T, S> chunkProcessor = new FaultTolerantChunkProcessor<T, S>(getItemProcessor(),
				getItemWriter(), batchRetryTemplate);
		chunkProcessor.setBuffering(!isReaderTransactionalQueue());
		chunkProcessor.setWriteSkipPolicy(writeSkipPolicy);
		chunkProcessor.setProcessSkipPolicy(writeSkipPolicy);
		chunkProcessor.setRollbackClassifier(rollbackClassifier);

		return chunkProcessor;
	}


	/**
	 * @return fully configured retry template for item processing phase.
	 */
	private BatchRetryTemplate configureRetry() {

		if (retryPolicy == null) {
			SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(retryLimit);
			simpleRetryPolicy.setRetryableExceptionClasses(retryableExceptionClasses);
			retryPolicy = simpleRetryPolicy;
		}

		RetryPolicy retryPolicyWrapper = fatalExceptionAwareProxy(retryPolicy);

		BatchRetryTemplate batchRetryTemplate = new BatchRetryTemplate();
		if (backOffPolicy != null) {
			batchRetryTemplate.setBackOffPolicy(backOffPolicy);
		}
		batchRetryTemplate.setRetryPolicy(retryPolicyWrapper);

		// Co-ordinate the retry policy with the exception handler:
		RepeatOperations stepOperations = getStepOperations();
		if (stepOperations instanceof RepeatTemplate) {
			SimpleRetryExceptionHandler exceptionHandler = new SimpleRetryExceptionHandler(retryPolicyWrapper,
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
		return batchRetryTemplate;
	}

	/**
	 * Wrap the provided {@link #setRetryPolicy(RetryPolicy)} so that it never
	 * retries fatal exceptions.
	 */
	private RetryPolicy fatalExceptionAwareProxy(final RetryPolicy retryPolicy) {

		final NeverRetryPolicy neverRetryPolicy = new NeverRetryPolicy();
		ExceptionClassifierRetryPolicy retryPolicyWrapper = new ExceptionClassifierRetryPolicy();
		retryPolicyWrapper.setExceptionClassifier(new Classifier<Throwable, RetryPolicy>() {

			public RetryPolicy classify(Throwable classifiable) {

				for (Class<? extends Throwable> fatal : fatalExceptionClasses) {
					if (fatal.isAssignableFrom(classifiable.getClass())) {
						return neverRetryPolicy;
					}
				}
				return retryPolicy;
			}
		});
		return retryPolicyWrapper;
	}

	@SuppressWarnings("unchecked")
	private void addFatalExceptionIfMissing(Class... cls) {
		List fatalExceptionList = new ArrayList<Class<? extends Throwable>>();
		for (Class exceptionClass : fatalExceptionClasses) {
			fatalExceptionList.add(exceptionClass);
		}
		for (Class fatal : cls) {
			if (!fatalExceptionList.contains(fatal)) {
				fatalExceptionList.add(fatal);
			}
		}
		fatalExceptionClasses = fatalExceptionList;
	}

}
