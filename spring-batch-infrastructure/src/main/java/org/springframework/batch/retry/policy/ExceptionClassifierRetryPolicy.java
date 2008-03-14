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
import java.util.Iterator;
import java.util.Map;

import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.TerminatedRetryException;
import org.springframework.batch.retry.context.RetryContextSupport;
import org.springframework.batch.retry.support.RetrySynchronizationManager;
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

	private ExceptionClassifier exceptionClassifier = new ExceptionClassifierSupport();

	private Map policyMap = new HashMap();

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
	public void setPolicyMap(Map policyMap) {
		this.policyMap = policyMap;
	}

	/**
	 * Setter for an exception classifier. The classifier is responsible for
	 * translating exceptions to keys in the policy map.
	 * 
	 * @param exceptionClassifier
	 */
	public void setExceptionClassifier(ExceptionClassifier exceptionClassifier) {
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
	 * @see org.springframework.batch.retry.RetryPolicy#close(org.springframework.batch.retry.RetryContext)
	 */
	public void close(RetryContext context) {
		RetryPolicy policy = (RetryPolicy) context;
		policy.close(context);
	}

	/**
	 * Create an active context that proxies a retry policy by chosing a target
	 * from the policy map.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#open(org.springframework.batch.retry.RetryCallback)
	 */
	public RetryContext open(RetryCallback callback) {
		return new ExceptionClassifierRetryContext(exceptionClassifier).open(callback);
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

		private ExceptionClassifier exceptionClassifier;

		// Dynamic: depends on the latest exception:
		RetryPolicy policy;

		// Dynamic: depends on the policy:
		RetryContext context;

		// The same for the life of the context:
		RetryCallback callback;

		Map contexts = new HashMap();

		public ExceptionClassifierRetryContext(ExceptionClassifier exceptionClassifier) {
			super(RetrySynchronizationManager.getContext());
			this.exceptionClassifier = exceptionClassifier;
			Object key = exceptionClassifier.getDefault();
			policy = getPolicy(key);
			Assert.notNull(policy, "Could not locate default policy: key=[" + key + "].");
		}

		public boolean canRetry(RetryContext context) {
			return policy.canRetry(this.context);
		}

		public boolean shouldRethrow(RetryContext context) {
			return policy.shouldRethrow(context);
		}

		public void close(RetryContext context) {
			// Only close those policies that have been used (opened):
			for (Iterator iter = contexts.keySet().iterator(); iter.hasNext();) {
				RetryPolicy policy = (RetryPolicy) iter.next();
				policy.close(getContext(policy));
			}
		}

		public RetryContext open(RetryCallback callback) {
			this.callback = callback;
			return this;
		}

		public void registerThrowable(RetryContext context, Throwable throwable) throws TerminatedRetryException {
			policy = getPolicy(exceptionClassifier.classify(throwable));
			this.context = getContext(policy);
			policy.registerThrowable(this.context, throwable);
		}

		private RetryContext getContext(RetryPolicy policy) {
			RetryContext context = (RetryContext) contexts.get(policy);
			if (context == null) {
				context = policy.open(callback);
				contexts.put(policy, context);
			}
			return context;
		}

		private RetryPolicy getPolicy(Object key) {
			RetryPolicy result = (RetryPolicy) policyMap.get(key);
			Assert.notNull(result, "Could not locate policy for key=[" + key + "].");
			return result;
		}

		public Object handleRetryExhausted(RetryContext context) throws Exception {
			// Not called...
			throw new UnsupportedOperationException("Not supported - this code should be unreachable.");
		}

	}

}
