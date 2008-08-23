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

import java.util.HashSet;
import java.util.Set;

import org.springframework.batch.retry.ExhaustedRetryException;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;

/**
 * Base class for stateful retry policies: those that operate in the context of
 * a callback that is called once per retry execution (usually to enforce that
 * it is only called once per transaction). Stateful policies need to remember
 * the context for the operation that failed (e.g. the data item that was being
 * processed), and decide based on its history what to do in the current
 * context. For example: the retry operation includes receiving a message, and
 * we need it to roll back and be re-delivered so that we can have another crack
 * at it.
 * 
 * @see RetryPolicy#handleRetryExhausted(RetryContext)
 * @see AbstractStatelessRetryPolicy
 * 
 * @author Dave Syer
 * 
 */
public abstract class AbstractStatefulRetryPolicy implements RetryPolicy {

	private volatile Set<Class<?>> recoverableExceptionClasses = new HashSet<Class<?>>();

	protected RetryContextCache retryContextCache = new MapRetryContextCache();

	/**
	 * Optional setter for the retry context cache. The default value is a
	 * {@link MapRetryContextCache}.
	 * 
	 * @param retryContextCache
	 */
	public void setRetryContextCache(RetryContextCache retryContextCache) {
		this.retryContextCache = retryContextCache;
	}

	/**
	 * Return null. Subclasses should provide a recovery path if possible.
	 * Subclasses are also encouraged not to declare throws Exception if they
	 * can (e.g. in the plausible and common case that the recovery is a last
	 * ditch effort to prevent a message going back to the middleware, for
	 * instance). Any subclass that actually does throw an Exception of any type
	 * should be aware that it will simply be propagated and the caller will
	 * have top deal with it.
	 * 
	 * @throws Exception if the recovery path demands it
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#handleRetryExhausted(org.springframework.batch.retry.RetryContext)
	 */
	public Object handleRetryExhausted(RetryContext context) throws ExhaustedRetryException, Exception {
		return null;
	}

	/**
	 * For a stateful policy the default is to always rethrow. This is the
	 * cautious approach: we assume that the failed processing may have written
	 * data to a transactional resource, so we rethrow and force a rollback. Any
	 * recovery path that may be available has to be taken on the next attempt,
	 * before any processing has taken place.
	 * 
	 * @return true unless the last exception registered was recoverable.
	 */
	public boolean shouldRethrow(RetryContext context) {
		return !recoverForException(context.getLastThrowable());
	}

	/**
	 * Set the recoverable exceptions. Any exception on the list, or subclasses
	 * thereof, will be recoverable. If it is encountered in a retry block it
	 * will not be rethrown. Others will be rethrown. The recovery action (if
	 * any) is left to subclasses - normally they would override
	 * {@link #handleRetryExhausted(RetryContext)}.
	 * 
	 * @param retryableExceptionClasses defaults to {@link Exception}.
	 */
	public final void setRecoverableExceptionClasses(Class<?>[] retryableExceptionClasses) {
		Set<Class<?>> temp = new HashSet<Class<?>>();
		for (int i = 0; i < retryableExceptionClasses.length; i++) {
			addRecoverableExceptionClass(retryableExceptionClasses[i], temp);
		}
		this.recoverableExceptionClasses = temp;
	}

	private void addRecoverableExceptionClass(Class<?> retryableExceptionClass, Set<Class<?>> set) {
		if (!Throwable.class.isAssignableFrom(retryableExceptionClass)) {
			throw new IllegalArgumentException("Class '" + retryableExceptionClass.getName()
					+ "' is not a subtype of Throwable.");
		}
		set.add(retryableExceptionClass);
	}

	protected boolean recoverForException(Throwable ex) {

		// Default is false (but this shouldn't really happen in practice -
		// maybe in tests):
		if (ex == null) {
			return false;
		}

		Class<? extends Throwable> exceptionClass = ex.getClass();
		if (recoverableExceptionClasses.contains(exceptionClass)) {
			return true;
		}

		// check for subclasses
		for (Class<?> cls : recoverableExceptionClasses) {
			if (cls.isAssignableFrom(exceptionClass)) {
				addRecoverableExceptionClass(exceptionClass, this.recoverableExceptionClasses);
				return true;
			}
		}

		return false;
	}

}
