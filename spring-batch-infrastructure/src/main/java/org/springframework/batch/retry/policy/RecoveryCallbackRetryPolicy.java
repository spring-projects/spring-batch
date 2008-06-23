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

package org.springframework.batch.retry.policy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.batch.retry.ExhaustedRetryException;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.TerminatedRetryException;
import org.springframework.batch.retry.callback.RecoveryRetryCallback;
import org.springframework.batch.retry.context.RetryContextSupport;
import org.springframework.util.Assert;

/**
 * A {@link RetryPolicy} that detects an {@link RecoveryRetryCallback} when it
 * opens a new context, and uses it to make sure the item is in place for later
 * decisions about how to retry or backoff. The callback should be an instance
 * of {@link RecoveryRetryCallback} otherwise an exception will be thrown when
 * the context is created.
 * 
 * @author Dave Syer
 * 
 */
public class RecoveryCallbackRetryPolicy extends AbstractStatefulRetryPolicy {

	protected Log logger = LogFactory.getLog(getClass());

	public static final String EXHAUSTED = RecoveryCallbackRetryPolicy.class.getName() + ".EXHAUSTED";

	private RetryPolicy delegate;

	/**
	 * Convenience constructor to set delegate on init.
	 * 
	 * @param delegate
	 */
	public RecoveryCallbackRetryPolicy(RetryPolicy delegate) {
		super();
		this.delegate = delegate;
	}

	/**
	 * Default constructor. Creates a new {@link SimpleRetryPolicy} for the
	 * delegate.
	 */
	public RecoveryCallbackRetryPolicy() {
		this(new SimpleRetryPolicy());
	}

	/**
	 * Setter for delegate.
	 * 
	 * @param delegate
	 */
	public void setDelegate(RetryPolicy delegate) {
		this.delegate = delegate;
	}

	/**
	 * Check the history of this item, and if it has reached the retry limit,
	 * then return false.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#canRetry(org.springframework.batch.retry.RetryContext)
	 */
	public boolean canRetry(RetryContext context) {
		return ((RetryPolicy) context).canRetry(context);
	}

	/**
	 * Delegates to the delegate context.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#close(org.springframework.batch.retry.RetryContext)
	 */
	public void close(RetryContext context) {
		((RetryPolicy) context).close(context);
	}

	/**
	 * Create a new context for the execution of the callback, which must be an
	 * instance of {@link RecoveryRetryCallback}.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#open(org.springframework.batch.retry.RetryCallback,
	 * RetryContext)
	 * 
	 * @throws IllegalStateException if the callback is not of the required
	 * type.
	 */
	public RetryContext open(RetryCallback callback, RetryContext parent) {
		Assert.state(callback instanceof RecoveryRetryCallback, "Callback must be RecoveryRetryCallback");
		RecoveryCallbackRetryContext context = new RecoveryCallbackRetryContext((RecoveryRetryCallback) callback, parent);
		context.open(callback, null);
		return context;
	}

	/**
	 * If {@link #canRetry(RetryContext)} is false then take remedial action (if
	 * implemented by subclasses), and remove the current item from the history.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#registerThrowable(org.springframework.batch.retry.RetryContext,
	 * java.lang.Throwable)
	 */
	public void registerThrowable(RetryContext context, Throwable throwable) throws TerminatedRetryException {
		((RetryPolicy) context).registerThrowable(context, throwable);
		// The throwable is stored in the delegate context.
	}

	/**
	 * Call recovery path (if any) and clean up context history.
	 * 
	 * @see org.springframework.batch.retry.policy.AbstractStatefulRetryPolicy#handleRetryExhausted(org.springframework.batch.retry.RetryContext)
	 */
	public Object handleRetryExhausted(RetryContext context) throws ExhaustedRetryException {
		return ((RetryPolicy) context).handleRetryExhausted(context);
	}

	private class RecoveryCallbackRetryContext extends RetryContextSupport implements RetryPolicy {

		final private Object key;

		final private int initialHashCode;

		// The delegate context...
		private RetryContext delegateContext;

		final private RecoveryCallback recoverer;

		final private boolean forceRefresh;

		public RecoveryCallbackRetryContext(RecoveryRetryCallback callback, RetryContext parent) {
			super(parent);
			this.recoverer = callback.getRecoveryCallback();
			this.key = callback.getKey();
			this.forceRefresh = callback.isForceRefresh();
			this.initialHashCode = key.hashCode();
		}

		public boolean canRetry(RetryContext context) {
			return delegate.canRetry(this.delegateContext);
		}

		public void close(RetryContext context) {
			delegate.close(this.delegateContext);
		}

		public RetryContext open(RetryCallback callback, RetryContext parent) {
			if (forceRefresh) {
				// Avoid a cache hit if the caller tells us this is a fresh item
				this.delegateContext = delegate.open(callback, null);
			}
			else if (retryContextCache.containsKey(key)) {
				this.delegateContext = retryContextCache.get(key);
				if (this.delegateContext == null) {
					throw new RetryException("Inconsistent state for failed item: no history found. "
							+ "Consider whether equals() or hashCode() for the item might be inconsistent, "
							+ "or if you need to supply a better ItemKeyGenerator");
				}
			}
			else {
				// Only create a new context if we don't know the history of
				// this item:
				this.delegateContext = delegate.open(callback, null);
			}
			// The return value shouldn't be used...
			return null;
		}

		public void registerThrowable(RetryContext context, Throwable throwable) throws TerminatedRetryException {
			// TODO: this comparison assumes that hashCode is the limiting
			// factor. Actually the cache should be able to decide for us.
			if (this.initialHashCode != key.hashCode()) {
				throw new RetryException("Inconsistent state for failed item key: hashCode has changed. "
						+ "Consider whether equals() or hashCode() for the item might be inconsistent, "
						+ "or if you need to supply a better ItemKeyGenerator");
			}
			retryContextCache.put(key, this.delegateContext);
			delegate.registerThrowable(this.delegateContext, throwable);
		}

		public boolean shouldRethrow(RetryContext context) {
			// Not called...
			throw new UnsupportedOperationException("Not supported - this code should be unreachable.");
		}

		public Object handleRetryExhausted(RetryContext context) throws ExhaustedRetryException {
			// If there is no going back, then we can remove the history
			retryContextCache.remove(key);
			RepeatSynchronizationManager.setCompleteOnly();
			if (recoverer != null) {
				return recoverer.recover(context);
			}
			logger.info("No recovery callback provided.  Returning null from recovery step.");
			// Don't want to call the delegate here - it would throw an exception
			return null;
		}

		public Throwable getLastThrowable() {
			return delegateContext.getLastThrowable();
		}

		public int getRetryCount() {
			return delegateContext.getRetryCount();
		}

	}

}
