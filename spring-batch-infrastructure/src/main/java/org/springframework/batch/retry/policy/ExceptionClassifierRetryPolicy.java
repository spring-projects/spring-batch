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

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.TerminatedRetryException;
import org.springframework.batch.retry.context.RetryContextSupport;
import org.springframework.batch.support.ExceptionClassifier;
import org.springframework.batch.support.ExceptionClassifierSupport;
import org.springframework.util.Assert;

/**
 * A {@link RetryPolicy} that dynamically adapts to one of a set of injected
 * policies according to the value of the latest exception.
 * 
 * @author Dave Syer
 * 
 */
public class ExceptionClassifierRetryPolicy extends AbstractStatelessRetryPolicy {

	private ExceptionClassifier<String,Throwable> exceptionClassifier = new ExceptionClassifierSupport();

	private Map<String, RetryPolicy> policyMap = new HashMap<String, RetryPolicy>();

	public ExceptionClassifierRetryPolicy() {
		policyMap.put(ExceptionClassifierSupport.DEFAULT, new NeverRetryPolicy());
	}

	/**
	 * Setter for policy map. This property should not be changed dynamically -
	 * set it once, e.g. in configuration, and then don't change it during a
	 * running application.
	 * 
	 * @param policyMap a map of String to {@link RetryPolicy} that will be
	 * applied to the result of the {@link ExceptionClassifier} to locate a
	 * policy.
	 */
	public void setPolicyMap(Map<String, RetryPolicy> policyMap) {
		this.policyMap = policyMap;
	}

	/**
	 * Setter for an exception classifier. The classifier is responsible for
	 * translating exceptions to keys in the policy map.
	 * 
	 * @param exceptionClassifier ExceptionClassifier to use
	 */
	public void setExceptionClassifier(ExceptionClassifier<String,Throwable> exceptionClassifier) {
		this.exceptionClassifier = exceptionClassifier;
	}

	/**
	 * Delegate to the policy currently activated in the context.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#canRetry(org.springframework.batch.retry.RetryContext)
	 */
	public boolean canRetry(RetryContext context) {
		RetryPolicy policy = (RetryPolicy) context;
		return policy.canRetry(context);
	}

	/**
	 * Delegate to the policy currently activated in the context.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#close(org.springframework.batch.retry.RetryContext, boolean)
	 */
	public void close(RetryContext context, boolean succeeded) {
		RetryPolicy policy = (RetryPolicy) context;
		policy.close(context, succeeded);
	}

	/**
	 * Create an active context that proxies a retry policy by chosing a target
	 * from the policy map.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#open(org.springframework.batch.retry.RetryCallback,
	 * RetryContext)
	 */
	public RetryContext open(RetryCallback callback, RetryContext parent) {
		return new ExceptionClassifierRetryContext(parent, exceptionClassifier).open(callback, parent);
	}

	/**
	 * Delegate to the policy currently activated in the context.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#registerThrowable(org.springframework.batch.retry.RetryContext,
	 * java.lang.Throwable)
	 */
	public void registerThrowable(RetryContext context, Throwable throwable) throws TerminatedRetryException {
		RetryPolicy policy = (RetryPolicy) context;
		policy.registerThrowable(context, throwable);
		((RetryContextSupport) context).registerThrowable(throwable);
	}

	private class ExceptionClassifierRetryContext extends RetryContextSupport implements RetryPolicy {

		private ExceptionClassifier<String,Throwable> exceptionClassifier;

		// Dynamic: depends on the latest exception:
		RetryPolicy policy;

		// Dynamic: depends on the policy:
		RetryContext context;

		// The same for the life of the context:
		RetryCallback callback;

		Map<RetryPolicy, RetryContext> contexts = new HashMap<RetryPolicy, RetryContext>();

		public ExceptionClassifierRetryContext(RetryContext parent, ExceptionClassifier<String,Throwable> exceptionClassifier) {
			super(parent);
			this.exceptionClassifier = exceptionClassifier;
			Object key = exceptionClassifier.getDefault();
			policy = getPolicy(key);
			Assert.notNull(policy, "Could not locate default policy: key=[" + key + "].");
		}

		public boolean canRetry(RetryContext context) {
			if (this.context == null) {
				// there was no error yet
				return true;
			}
			return policy.canRetry(this.context);
		}

		public boolean shouldRethrow(RetryContext context) {
			return policy.shouldRethrow(context);
		}

		public void close(RetryContext context, boolean succeeded) {
			// Only close those policies that have been used (opened):
			for (RetryPolicy policy : contexts.keySet()) {
				policy.close(getContext(policy), succeeded);
			}
		}

		public RetryContext open(RetryCallback callback, RetryContext parent) {
			this.callback = callback;
			return this;
		}

		public void registerThrowable(RetryContext context, Throwable throwable) throws TerminatedRetryException {
			policy = getPolicy(exceptionClassifier.classify(throwable));
			this.context = getContext(policy);
			policy.registerThrowable(this.context, throwable);
		}

		private RetryContext getContext(RetryPolicy policy) {
			RetryContext context = contexts.get(policy);
			if (context == null) {
				context = policy.open(callback, null);
				contexts.put(policy, context);
			}
			return context;
		}

		private RetryPolicy getPolicy(Object key) {
			RetryPolicy result = policyMap.get(key);
			Assert.notNull(result, "Could not locate policy for key=[" + key + "].");
			return result;
		}

		public Object handleRetryExhausted(RetryContext context) throws UnsupportedOperationException {
			// Not called...
			throw new UnsupportedOperationException("Not supported - this code should be unreachable.");
		}

	}

}
