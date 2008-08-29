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

import java.util.Collection;
import java.util.HashSet;

import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.context.RetryContextSupport;
import org.springframework.batch.support.BinaryExceptionClassifier;

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
public class SimpleRetryPolicy implements RetryPolicy {

	/**
	 * The default limit to the number of attempts for a new policy.
	 */
	public final static int DEFAULT_MAX_ATTEMPTS = 3;

	private volatile int maxAttempts;

	private BinaryExceptionClassifier retryableClassifier = new BinaryExceptionClassifier();

	private BinaryExceptionClassifier fatalClassifier = new BinaryExceptionClassifier();

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
		Collection<Class<? extends Throwable>> classes;
		classes = new HashSet<Class<? extends Throwable>>();
		classes.add(Exception.class);
		setRetryableExceptionClasses(classes);
		classes = new HashSet<Class<? extends Throwable>>();
		setFatalExceptionClasses(classes);
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
		Throwable t = context.getLastThrowable();
		return (t == null || retryForException(t)) && context.getRetryCount() < maxAttempts;
	}

	/**
	 * Set the retryable exceptions. Any exception on the list, or subclasses
	 * thereof, will be retryable. Others will be re-thrown without retry.
	 * 
	 * @param retryableExceptionClasses defaults to {@link Exception}.
	 */
	public final void setRetryableExceptionClasses(Collection<Class<? extends Throwable>> retryableExceptionClasses) {
		retryableClassifier.setTypes(retryableExceptionClasses);
	}

	/**
	 * Set the fatal exceptions. Any exception on the list, or subclasses
	 * thereof, will be re-thrown without retry. This list takes precedence over
	 * the retryable list.
	 * 
	 * @param fatalExceptionClasses defaults to {@link Exception}.
	 */
	public final void setFatalExceptionClasses(Collection<Class<? extends Throwable>> fatalExceptionClasses) {
		fatalClassifier.setTypes(fatalExceptionClasses);
	}

	/**
	 * @see org.springframework.batch.retry.RetryPolicy#close(RetryContext)
	 */
	public void close(RetryContext status) {
	}

	/**
	 * Update the status with another attempted retry and the latest exception.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#registerThrowable(org.springframework.batch.retry.RetryContext,
	 * Exception)
	 */
	public void registerThrowable(RetryContext context, Exception throwable) {
		SimpleRetryContext simpleContext = ((SimpleRetryContext) context);
		simpleContext.registerThrowable(throwable);
	}

	/**
	 * Get a status object that can be used to track the current operation
	 * according to this policy. Has to be aware of the latest exception and the
	 * number of attempts.
	 * @see org.springframework.batch.retry.RetryPolicy#open(RetryContext)
	 */
	public RetryContext open(RetryContext parent) {
		return new SimpleRetryContext(parent);
	}

	private static class SimpleRetryContext extends RetryContextSupport {
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
		return !fatalClassifier.classify(ex) && retryableClassifier.classify(ex);
	}
}
