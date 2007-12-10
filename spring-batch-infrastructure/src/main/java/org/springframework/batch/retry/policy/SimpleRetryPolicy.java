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

import org.springframework.batch.common.BinaryExceptionClassifier;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.context.RetryContextSupport;
import org.springframework.batch.retry.synch.RetrySynchronizationManager;

/**
 * 
 * Simple retry policy that retries a fixed number of times for a set of named
 * exceptions (and subclasses). The number of attempts includes the initial try,
 * so e.g.
 * 
 * <pre>
 * retryTemplate = new RetryTemplate(new SimpleRetryPolicy(3));
 * retryTemplate.execute(callback);
 * </pre>
 * 
 * will execute the callback at least once, and as many as 3 times.
 * 
 * @author Dave Syer
 * @author Rob Harrop
 * 
 */
public class SimpleRetryPolicy extends AbstractStatelessRetryPolicy {

	/**
	 * The default limit to the number of attempts for a new policy.
	 */
	public final static int DEFAULT_MAX_ATTEMPTS = 3;

	private volatile int maxAttempts;

	private BinaryExceptionClassifier classifier = new BinaryExceptionClassifier();

	/**
	 * Create a {@link SimpleRetryPolicy} with the default number of retry
	 * attempts.
	 */
	public SimpleRetryPolicy() {
		this(DEFAULT_MAX_ATTEMPTS);
	}

	/**
	 * Create a {@link SimpleRetryPolicy} with the specified number of retry
	 * attempts, and default exceptions to retry.
	 * 
	 * @param maxAttempts
	 */
	public SimpleRetryPolicy(int maxAttempts) {
		super();
		setRetryableExceptionClasses(new Class[] { Exception.class });
		this.maxAttempts = maxAttempts;
	}

	/**
	 * Setter for retry attempts.
	 * @param retryAttempts the number of attempts before a retry becomes
	 * impossible.
	 */
	public void setMaxAttempts(int retryAttempts) {
		this.maxAttempts = retryAttempts;
	}

	/**
	 * Test for retryable operation based on the status.
	 * @see org.springframework.batch.retry.RetryPolicy#canRetry(org.springframework.batch.retry.RetryContext)
	 * 
	 * @return true if the last exception was retryable and the number of
	 * attempts so far is less than the limit.
	 */
	public boolean canRetry(RetryContext context) {
		SimpleRetryContext simpleContext = ((SimpleRetryContext) context);
		Throwable t = simpleContext.getLastThrowable();
		// N.B. since the contract is defined to include the initial attempt
		// in the count, we have to subtract one from the max attempts in this
		// test
		return (t == null || retryForException(t)) && simpleContext.getRetryCount() < maxAttempts;
	}

	/**
	 * Set the retryable exceptions. Any exception on the list, or subclasses
	 * thereof, will be retryable. Others will be rethrown without retry.
	 * 
	 * @param retryableExceptionClasses defaults to {@link Exception}.
	 */
	public final void setRetryableExceptionClasses(Class[] retryableExceptionClasses) {
		classifier.setExceptionClasses(retryableExceptionClasses);
	}

	/**
	 * @see org.springframework.batch.retry.RetryManager#close(RepeatContext)
	 */
	public void close(RetryContext status) {
	}

	/**
	 * Update the status with another attempted retry and the latest exception.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#registerThrowable(org.springframework.batch.retry.RetryContext,
	 * java.lang.Throwable)
	 */
	public void registerThrowable(RetryContext context, Throwable throwable) {
		SimpleRetryContext simpleContext = ((SimpleRetryContext) context);
		simpleContext.registerThrowable(throwable);
	}

	/**
	 * Get a status object that can be used to track the current operation
	 * according to this policy. Has to be aware of the latest exception and the
	 * number of attempts.
	 * @see org.springframework.batch.retry.RetryPolicy#open(org.springframework.batch.retry.RetryCallback)
	 */
	public RetryContext open(RetryCallback callback) {
		return new SimpleRetryContext();
	}

	private static class SimpleRetryContext extends RetryContextSupport {

		public SimpleRetryContext() {
			this(RetrySynchronizationManager.getContext());

		}

		public SimpleRetryContext(RetryContext parent) {
			super(parent);
		}

	}

	/**
	 * Delegates to an exception classifier.
	 * 
	 * @param ex
	 * @return true if this exception or its ancestors have been registered as
	 * retryable.
	 */
	private boolean retryForException(Throwable ex) {
		return !classifier.isDefault(ex);
	}
}
