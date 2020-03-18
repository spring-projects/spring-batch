/*
 * Copyright 2006-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.step.factory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.item.KeyGenerator;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.policy.MapRetryContextCache;
import org.springframework.retry.policy.RetryContextCache;

/**
 * Factory bean for step that provides options for configuring skip behavior. User can set {@link #setSkipLimit(int)}
 * to set how many exceptions of {@link #setSkippableExceptionClasses(Map)} types are tolerated.
 *
 * Skippable exceptions on write will by default cause transaction rollback - to avoid rollback for specific exception
 * class include it in the transaction attribute as "no rollback for".
 *
 * @see SimpleStepFactoryBean
 *
 * @author Dave Syer
 * @author Robert Kasanicky
 * @author Morten Andersen-Gott
 *
 */
public class FaultTolerantStepFactoryBean<T, S> extends SimpleStepFactoryBean<T, S> {

	private Map<Class<? extends Throwable>, Boolean> skippableExceptionClasses = new HashMap<>();

	private Collection<Class<? extends Throwable>> noRollbackExceptionClasses = new HashSet<>();

	private Map<Class<? extends Throwable>, Boolean> retryableExceptionClasses = new HashMap<>();

	private int cacheCapacity = 0;

	private int retryLimit = 0;

	private int skipLimit = 0;

	private SkipPolicy skipPolicy;

	private BackOffPolicy backOffPolicy;

	private RetryListener[] retryListeners;

	private RetryPolicy retryPolicy;

	private RetryContextCache retryContextCache;

	private KeyGenerator keyGenerator;

	private boolean processorTransactional = true;

	/**
	 * The {@link KeyGenerator} to use to identify failed items across rollback. Not used in the case of the
	 * {@link #setIsReaderTransactionalQueue(boolean) transactional queue flag} being false (the default).
	 *
	 * @param keyGenerator the {@link KeyGenerator} to set
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	/**
	 * Setter for the retry policy. If this is specified the other retry properties are ignored (retryLimit,
	 * backOffPolicy, retryableExceptionClasses).
	 *
	 * @param retryPolicy a stateless {@link RetryPolicy}
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Public setter for the retry limit. Each item can be retried up to this limit. Note this limit includes the
	 * initial attempt to process the item, therefore <code>retryLimit == 1</code> by default.
	 *
	 * @param retryLimit the retry limit to set, must be greater or equal to 1.
	 */
	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	/**
	 * Public setter for the capacity of the cache in the retry policy. If more items than this fail without being
	 * skipped or recovered an exception will be thrown. This is to guard against inadvertent infinite loops generated
	 * by item identity problems.<br>
	 *
	 * The default value should be high enough and more for most purposes. To breach the limit in a single-threaded step
	 * typically you have to have this many failures in a single transaction. Defaults to the value in the
	 * {@link MapRetryContextCache}.<br>
	 *
	 * This property is ignored if the {@link #setRetryContextCache(RetryContextCache)} is set directly.
	 *
	 * @param cacheCapacity the cache capacity to set (greater than 0 else ignored)
	 */
	public void setCacheCapacity(int cacheCapacity) {
		this.cacheCapacity = cacheCapacity;
	}

	/**
	 * Override the default retry context cache for retry of chunk processing. If this property is set then
	 * {@link #setCacheCapacity(int)} is ignored.
	 *
	 * @param retryContextCache the {@link RetryContextCache} to set
	 */
	public void setRetryContextCache(RetryContextCache retryContextCache) {
		this.retryContextCache = retryContextCache;
	}

	/**
	 * Public setter for the retryable exceptions classifier map (from throwable class to boolean, true is retryable).
	 *
	 * @param retryableExceptionClasses the retryableExceptionClasses to set
	 */
	public void setRetryableExceptionClasses(Map<Class<? extends Throwable>, Boolean> retryableExceptionClasses) {
		this.retryableExceptionClasses = retryableExceptionClasses;
	}

	/**
	 * Public setter for the {@link BackOffPolicy}.
	 *
	 * @param backOffPolicy the {@link BackOffPolicy} to set
	 */
	public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
		this.backOffPolicy = backOffPolicy;
	}

	/**
	 * Public setter for the {@link RetryListener}s.
	 *
	 * @param retryListeners the {@link RetryListener}s to set
	 */
	public void setRetryListeners(RetryListener... retryListeners) {
		this.retryListeners = retryListeners;
	}

	/**
	 * A limit that determines skip policy. If this value is positive then an exception in chunk processing will cause
	 * the item to be skipped and no exception propagated until the limit is reached. If it is zero then all exceptions
	 * will be propagated from the chunk and cause the step to abort.
	 *
	 * @param skipLimit the value to set. Default is 0 (never skip).
	 */
	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	/**
	 * A {@link SkipPolicy} that determines the outcome of an exception when processing an item. Overrides the
	 * {@link #setSkipLimit(int) skipLimit}. The {@link #setSkippableExceptionClasses(Map) skippableExceptionClasses}
	 * are also ignored if this is set.
	 *
	 * @param skipPolicy the {@link SkipPolicy} to set
	 */
	public void setSkipPolicy(SkipPolicy skipPolicy) {
		this.skipPolicy = skipPolicy;
	}

	/**
	 * Exception classes that when raised won't crash the job but will result in the item which handling caused the
	 * exception being skipped. Any exception which is marked for "no rollback" is also skippable, but not vice versa.
	 * Remember to set the {@link #setSkipLimit(int) skip limit} as well.
	 * <br>
	 * Defaults to all no exception.
	 *
	 * @param exceptionClasses defaults to <code>Exception</code>
	 */
	public void setSkippableExceptionClasses(Map<Class<? extends Throwable>, Boolean> exceptionClasses) {
		this.skippableExceptionClasses = exceptionClasses;
	}

	/**
	 * Exception classes that are candidates for no rollback. The {@link Step} can not honour the no rollback hint in
	 * all circumstances, but any exception on this list is counted as skippable, so even if there has to be a rollback,
	 * then the step will not fail as long as the skip limit is not breached.
	 * <br>
	 * Defaults is empty.
	 *
	 * @param noRollbackExceptionClasses the exception classes to set
	 */
	public void setNoRollbackExceptionClasses(Collection<Class<? extends Throwable>> noRollbackExceptionClasses) {
		this.noRollbackExceptionClasses = noRollbackExceptionClasses;
	}

	/**
	 * @param processorTransactional boolean indicates if the {@code ItemProcessor} participates in the transaction.
	 */
	public void setProcessorTransactional(boolean processorTransactional) {
		this.processorTransactional = processorTransactional;
	}

	@Override
	protected SimpleStepBuilder<T, S> createBuilder(String name) {
		return new FaultTolerantStepBuilder<>(new StepBuilder(name));
	}

	@Override
	protected void applyConfiguration(SimpleStepBuilder<T, S> builder) {

		FaultTolerantStepBuilder<T, S> faultTolerantBuilder = (FaultTolerantStepBuilder<T, S>) builder;

		if (retryContextCache == null && cacheCapacity > 0) {
			retryContextCache = new MapRetryContextCache(cacheCapacity);
		}
		faultTolerantBuilder.retryContextCache(retryContextCache);
		for (SkipListener<T, S> listener : BatchListenerFactoryHelper.<SkipListener<T, S>> getListeners(getListeners(),
				SkipListener.class)) {
			faultTolerantBuilder.listener(listener);
		}

		if (retryListeners != null) {
			for (RetryListener listener : retryListeners) {
				faultTolerantBuilder.listener(listener);
			}
		}

		faultTolerantBuilder.skipPolicy(skipPolicy);
		faultTolerantBuilder.skipLimit(skipLimit);
		for (Class<? extends Throwable> type : skippableExceptionClasses.keySet()) {
			if (skippableExceptionClasses.get(type)) {
				faultTolerantBuilder.skip(type);
			}
			else {
				faultTolerantBuilder.noSkip(type);
			}
		}

		if (!processorTransactional) {
			faultTolerantBuilder.processorNonTransactional();
		}

		faultTolerantBuilder.retryContextCache(retryContextCache);
		faultTolerantBuilder.keyGenerator(keyGenerator);
		faultTolerantBuilder.retryPolicy(retryPolicy);
		faultTolerantBuilder.retryLimit(retryLimit);
		faultTolerantBuilder.backOffPolicy(backOffPolicy);
		for (Class<? extends Throwable> type : retryableExceptionClasses.keySet()) {
			if (retryableExceptionClasses.get(type)) {
				faultTolerantBuilder.retry(type);
			}
			else {
				faultTolerantBuilder.noRetry(type);
			}
		}

		for (Class<? extends Throwable> type : noRollbackExceptionClasses) {
			faultTolerantBuilder.noRollback(type);
		}
		super.applyConfiguration(builder);

	}

}
