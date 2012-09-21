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

package org.springframework.batch.retry.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.repeat.RepeatException;
import org.springframework.batch.retry.ExhaustedRetryException;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.RetryListener;
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.RetryState;
import org.springframework.batch.retry.TerminatedRetryException;
import org.springframework.batch.retry.backoff.BackOffContext;
import org.springframework.batch.retry.backoff.BackOffInterruptedException;
import org.springframework.batch.retry.backoff.BackOffPolicy;
import org.springframework.batch.retry.backoff.NoBackOffPolicy;
import org.springframework.batch.retry.policy.MapRetryContextCache;
import org.springframework.batch.retry.policy.RetryContextCache;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;

/**
 * Template class that simplifies the execution of operations with retry
 * semantics. <br/>
 * Retryable operations are encapsulated in implementations of the
 * {@link RetryCallback} interface and are executed using one of the supplied
 * execute methods. <br/>
 * 
 * By default, an operation is retried if is throws any {@link Exception} or
 * subclass of {@link Exception}. This behaviour can be changed by using the
 * {@link #setRetryPolicy(RetryPolicy)} method. <br/>
 * 
 * Also by default, each operation is retried for a maximum of three attempts
 * with no back off in between. This behaviour can be configured using the
 * {@link #setRetryPolicy(RetryPolicy)} and
 * {@link #setBackOffPolicy(BackOffPolicy)} properties. The
 * {@link org.springframework.batch.retry.backoff.BackOffPolicy} controls how
 * long the pause is between each individual retry attempt. <br/>
 * 
 * This class is thread-safe and suitable for concurrent access when executing
 * operations and when performing configuration changes. As such, it is possible
 * to change the number of retries on the fly, as well as the
 * {@link BackOffPolicy} used and no in progress retryable operations will be
 * affected.
 * 
 * @author Rob Harrop
 * @author Dave Syer
 */
public class RetryTemplate implements RetryOperations {

	protected final Log logger = LogFactory.getLog(getClass());

	private volatile BackOffPolicy backOffPolicy = new NoBackOffPolicy();

	private volatile RetryPolicy retryPolicy = new SimpleRetryPolicy(3, Collections
			.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true));

	private volatile RetryListener[] listeners = new RetryListener[0];

	private RetryContextCache retryContextCache = new MapRetryContextCache();

	/**
	 * Public setter for the {@link RetryContextCache}.
	 * 
	 * @param retryContextCache the {@link RetryContextCache} to set.
	 */
	public void setRetryContextCache(RetryContextCache retryContextCache) {
		this.retryContextCache = retryContextCache;
	}

	/**
	 * Setter for listeners. The listeners are executed before and after a retry
	 * block (i.e. before and after all the attempts), and on an error (every
	 * attempt).
	 * 
	 * @param listeners
	 * @see RetryListener
	 */
	public void setListeners(RetryListener[] listeners) {
		this.listeners = Arrays.asList(listeners).toArray(new RetryListener[listeners.length]);
	}

	/**
	 * Register an additional listener.
	 * 
	 * @param listener
	 * @see #setListeners(RetryListener[])
	 */
	public void registerListener(RetryListener listener) {
		List<RetryListener> list = new ArrayList<RetryListener>(Arrays.asList(listeners));
		list.add(listener);
		listeners = list.toArray(new RetryListener[list.size()]);
	}

	/**
	 * Setter for {@link BackOffPolicy}.
	 * 
	 * @param backOffPolicy
	 */
	public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
		this.backOffPolicy = backOffPolicy;
	}

	/**
	 * Setter for {@link RetryPolicy}.
	 * 
	 * @param retryPolicy
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Keep executing the callback until it either succeeds or the policy
	 * dictates that we stop, in which case the most recent exception thrown by
	 * the callback will be rethrown.
	 * 
	 * @see RetryOperations#execute(RetryCallback)
	 * 
	 * @throws TerminatedRetryException if the retry has been manually
	 * terminated by a listener.
	 */
	public final <T> T execute(RetryCallback<T> retryCallback) throws Exception {
		return doExecute(retryCallback, null, null);
	}

	/**
	 * Keep executing the callback until it either succeeds or the policy
	 * dictates that we stop, in which case the recovery callback will be
	 * executed.
	 * 
	 * @see RetryOperations#execute(RetryCallback, RecoveryCallback)
	 * 
	 * @throws TerminatedRetryException if the retry has been manually
	 * terminated by a listener.
	 */
	public final <T> T execute(RetryCallback<T> retryCallback, RecoveryCallback<T> recoveryCallback) throws Exception {
		return doExecute(retryCallback, recoveryCallback, null);
	}

	/**
	 * Execute the callback once if the policy dictates that we can, re-throwing
	 * any exception encountered so that clients can re-present the same task
	 * later.
	 * 
	 * @see RetryOperations#execute(RetryCallback, RetryState)
	 * 
	 * @throws ExhaustedRetryException if the retry has been exhausted.
	 */
	public final <T> T execute(RetryCallback<T> retryCallback, RetryState retryState) throws Exception,
			ExhaustedRetryException {
		return doExecute(retryCallback, null, retryState);
	}

	/**
	 * Execute the callback once if the policy dictates that we can, re-throwing
	 * any exception encountered so that clients can re-present the same task
	 * later.
	 * 
	 * @see RetryOperations#execute(RetryCallback, RetryState)
	 */
	public final <T> T execute(RetryCallback<T> retryCallback, RecoveryCallback<T> recoveryCallback,
			RetryState retryState) throws Exception, ExhaustedRetryException {
		return doExecute(retryCallback, recoveryCallback, retryState);
	}

	/**
	 * Execute the callback once if the policy dictates that we can, otherwise
	 * execute the recovery callback.
	 * 
	 * @see RetryOperations#execute(RetryCallback, RecoveryCallback, RetryState)
	 * @throws ExhaustedRetryException if the retry has been exhausted.
	 */
	protected <T> T doExecute(RetryCallback<T> retryCallback, RecoveryCallback<T> recoveryCallback, RetryState state)
			throws Exception, ExhaustedRetryException {

		RetryPolicy retryPolicy = this.retryPolicy;
		BackOffPolicy backOffPolicy = this.backOffPolicy;

		// Allow the retry policy to initialise itself...
		RetryContext context = open(retryPolicy, state);
		if (logger.isTraceEnabled()) {
			logger.trace("RetryContext retrieved: " + context);
		}

		// Make sure the context is available globally for clients who need
		// it...
		RetrySynchronizationManager.register(context);

		Throwable lastException = null;

		try {

			// Give clients a chance to enhance the context...
			boolean running = doOpenInterceptors(retryCallback, context);

			if (!running) {
				throw new TerminatedRetryException("Retry terminated abnormally by interceptor before first attempt");
			}

			// Start the backoff context...
			BackOffContext backOffContext = backOffPolicy.start(context);

			/*
			 * We allow the whole loop to be skipped if the policy or context
			 * already forbid the first try. This is used in the case of
			 * stateful retry to allow a recovery in handleRetryExhausted
			 * without the callback processing (which would throw an exception).
			 */
			while (canRetry(retryPolicy, context) && !context.isExhaustedOnly()) {

				try {
					logger.debug("Retry: count=" + context.getRetryCount());
					// Reset the last exception, so if we are successful
					// the close interceptors will not think we failed...
					lastException = null;
					return retryCallback.doWithRetry(context);
				}
				catch (Throwable e) {

					lastException = e;

					doOnErrorInterceptors(retryCallback, context, e);

					try {
						registerThrowable(retryPolicy, state, context, e);
					} catch (Exception ex) {
						throw new TerminatedRetryException("Terminated retry after error in policy", ex);
					}

					if (canRetry(retryPolicy, context) && !context.isExhaustedOnly()) {
						try {
							backOffPolicy.backOff(backOffContext);
						}
						catch (BackOffInterruptedException ex) {
							lastException = e;
							// back off was prevented by another thread - fail
							// the retry
							logger.debug("Abort retry because interrupted: count=" + context.getRetryCount());
							throw ex;
						}
					}

					logger.debug("Checking for rethrow: count=" + context.getRetryCount());
					if (shouldRethrow(retryPolicy, context, state)) {
						logger.debug("Rethrow in retry for policy: count=" + context.getRetryCount());
						throw wrapIfNecessary(e);
					}

				}

				/*
				 * A stateful attempt that can retry should have rethrown the
				 * exception by now - i.e. we shouldn't get this far for a
				 * stateful attempt if it can retry.
				 */
			}

			logger.debug("Retry failed last attempt: count=" + context.getRetryCount());

			if (context.isExhaustedOnly()) {
				throw new ExhaustedRetryException("Retry exhausted after last attempt with no recovery path.", context
						.getLastThrowable());
			}

			return handleRetryExhausted(recoveryCallback, context, state);

		}
		finally {
			close(retryPolicy, context, state, lastException == null);
			doCloseInterceptors(retryCallback, context, lastException);
			RetrySynchronizationManager.clear();
		}

	}

	/**
	 * Decide whether to proceed with the ongoing retry attempt. This method is
	 * called before the {@link RetryCallback} is executed, but after the
	 * backoff and open interceptors.
	 * 
	 * @param retryPolicy the policy to apply
	 * @param context the current retry context
	 * @return true if we can continue with the attempt
	 */
	protected boolean canRetry(RetryPolicy retryPolicy, RetryContext context) {
		return retryPolicy.canRetry(context);
	}

	/**
	 * Clean up the cache if necessary and close the context provided (if the
	 * flag indicates that processing was successful).
	 * 
	 * @param context
	 * @param state
	 * @param succeeded
	 */
	protected void close(RetryPolicy retryPolicy, RetryContext context, RetryState state, boolean succeeded) {
		if (state != null) {
			if (succeeded) {
				retryContextCache.remove(state.getKey());
				retryPolicy.close(context);
			}
		}
		else {
			retryPolicy.close(context);
		}
	}

	/**
	 * @param retryPolicy
	 * @param state
	 * @param context
	 * @param e
	 */
	protected void registerThrowable(RetryPolicy retryPolicy, RetryState state, RetryContext context, Throwable e) {
		if (state != null) {
			Object key = state.getKey();
			if (context.getRetryCount() > 0 && !retryContextCache.containsKey(key)) {
				throw new RetryException("Inconsistent state for failed item key: cache key has changed. "
						+ "Consider whether equals() or hashCode() for the key might be inconsistent, "
						+ "or if you need to supply a better key");
			}
			retryContextCache.put(key, context);
		}
		retryPolicy.registerThrowable(context, e);
	}

	/**
	 * Delegate to the {@link RetryPolicy} having checked in the cache for an
	 * existing value if the state is not null.
	 * 
	 * @param retryPolicy a {@link RetryPolicy} to delegate the context creation
	 * @return a retry context, either a new one or the one used last time the
	 * same state was encountered
	 */
	protected RetryContext open(RetryPolicy retryPolicy, RetryState state) {

		if (state == null) {
			return doOpenInternal(retryPolicy);
		}

		Object key = state.getKey();
		if (state.isForceRefresh()) {
			return doOpenInternal(retryPolicy);
		}

		// If there is no cache hit we can avoid the possible expense of the
		// cache re-hydration.
		if (!retryContextCache.containsKey(key)) {
			// The cache is only used if there is a failure.
			return doOpenInternal(retryPolicy);
		}

		RetryContext context = retryContextCache.get(key);
		if (context == null) {
			if (retryContextCache.containsKey(key)) {
				throw new RetryException("Inconsistent state for failed item: no history found. "
						+ "Consider whether equals() or hashCode() for the item might be inconsistent, "
						+ "or if you need to supply a better ItemKeyGenerator");
			}
			// The cache could have been expired in between calls to
			// containsKey(), so we have to live with this:
			return doOpenInternal(retryPolicy);
		}

		return context;

	}

	/**
	 * @param retryPolicy
	 * @return
	 */
	private RetryContext doOpenInternal(RetryPolicy retryPolicy) {
		return retryPolicy.open(RetrySynchronizationManager.getContext());
	}

	/**
	 * Actions to take after final attempt has failed. If there is state clean
	 * up the cache. If there is a recovery callback, execute that and return
	 * its result. Otherwise throw an exception.
	 * 
	 * @param recoveryCallback the callback for recovery (might be null)
	 * @param context the current retry context
	 * @throws Exception if the callback does, and if there is no callback and
	 * the state is null then the last exception from the context
	 * @throws ExhaustedRetryException if the state is not null and there is no
	 * recovery callback
	 */
	protected <T> T handleRetryExhausted(RecoveryCallback<T> recoveryCallback, RetryContext context, RetryState state)
			throws Exception {
		if (state != null) {
			retryContextCache.remove(state.getKey());
		}
		if (recoveryCallback != null) {
			return recoveryCallback.recover(context);
		}
		if (state != null) {
			logger.debug("Retry exhausted after last attempt with no recovery path.");
			throw new ExhaustedRetryException("Retry exhausted after last attempt with no recovery path", context
					.getLastThrowable());
		}
		throw wrapIfNecessary(context.getLastThrowable());
	}

	/**
	 * Extension point for subclasses to decide on behaviour after catching an
	 * exception in a {@link RetryCallback}. Normal stateless behaviour is not
	 * to rethrow, and if there is state we rethrow.
	 * 
	 * @param retryPolicy
	 * @param context the current context
	 * 
	 * @return true if the state is not null but subclasses might choose
	 * otherwise
	 */
	protected boolean shouldRethrow(RetryPolicy retryPolicy, RetryContext context, RetryState state) {
		if (state == null) {
			return false;
		}
		else {
			return state.rollbackFor(context.getLastThrowable());
		}
	}

	private <T> boolean doOpenInterceptors(RetryCallback<T> callback, RetryContext context) {

		boolean result = true;

		for (int i = 0; i < listeners.length; i++) {
			result = result && listeners[i].open(context, callback);
		}

		return result;

	}

	private <T> void doCloseInterceptors(RetryCallback<T> callback, RetryContext context, Throwable lastException) {
		for (int i = listeners.length; i-- > 0;) {
			listeners[i].close(context, callback, lastException);
		}
	}

	private <T> void doOnErrorInterceptors(RetryCallback<T> callback, RetryContext context, Throwable throwable) {
		for (int i = listeners.length; i-- > 0;) {
			listeners[i].onError(context, callback, throwable);
		}
	}

	/**
	 * Re-throws the original throwable if it is unchecked, wraps checked
	 * exceptions into {@link RepeatException}.
	 */
	private static Exception wrapIfNecessary(Throwable throwable) {
		if (throwable instanceof Error) {
			throw (Error) throwable;
		}
		else if (throwable instanceof Exception) {
			return (Exception) throwable;
		}
		else {
			return new RetryException("Exception in batch process", throwable);
		}
	}

}
