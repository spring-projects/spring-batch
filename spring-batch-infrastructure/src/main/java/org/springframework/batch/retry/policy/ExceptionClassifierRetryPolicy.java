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

import org.springframework.batch.classify.Classifier;
import org.springframework.batch.classify.ClassifierSupport;
import org.springframework.batch.classify.SubclassClassifier;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.context.RetryContextSupport;
import org.springframework.util.Assert;

/**
 * A {@link RetryPolicy} that dynamically adapts to one of a set of injected
 * policies according to the value of the latest exception.
 * 
 * @author Dave Syer
 * 
 */
public class ExceptionClassifierRetryPolicy implements RetryPolicy {

	private Classifier<Throwable, RetryPolicy> exceptionClassifier = new ClassifierSupport<Throwable, RetryPolicy>(
			new NeverRetryPolicy());

	/**
	 * Setter for policy map used to create a classifier. Either this property
	 * or the exception classifier directly should be set, but not both.
	 * 
	 * @param policyMap a map of Throwable class to {@link RetryPolicy} that
	 * will be used to create a {@link Classifier} to locate a policy.
	 */
	public void setPolicyMap(Map<Class<? extends Throwable>, RetryPolicy> policyMap) {
		SubclassClassifier<Throwable, RetryPolicy> subclassClassifier = new SubclassClassifier<Throwable, RetryPolicy>(
				policyMap, (RetryPolicy) new NeverRetryPolicy());
		this.exceptionClassifier = subclassClassifier;
	}

	/**
	 * Setter for an exception classifier. The classifier is responsible for
	 * translating exceptions to concrete retry policies. Either this property
	 * or the policy map should be used, but not both.
	 * 
	 * @param exceptionClassifier ExceptionClassifier to use
	 */
	public void setExceptionClassifier(Classifier<Throwable, RetryPolicy> exceptionClassifier) {
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
	 * @see org.springframework.batch.retry.RetryPolicy#open(RetryContext)
	 */
	public RetryContext open(RetryContext parent) {
		return new ExceptionClassifierRetryContext(parent, exceptionClassifier).open(parent);
	}

	/**
	 * Delegate to the policy currently activated in the context.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#registerThrowable(org.springframework.batch.retry.RetryContext,
	 * Throwable)
	 */
	public void registerThrowable(RetryContext context, Throwable throwable) {
		RetryPolicy policy = (RetryPolicy) context;
		policy.registerThrowable(context, throwable);
		((RetryContextSupport) context).registerThrowable(throwable);
	}

	private static class ExceptionClassifierRetryContext extends RetryContextSupport implements RetryPolicy {

		final private Classifier<Throwable, RetryPolicy> exceptionClassifier;

		// Dynamic: depends on the latest exception:
		private RetryPolicy policy;

		// Dynamic: depends on the policy:
		private RetryContext context;

		final private Map<RetryPolicy, RetryContext> contexts = new HashMap<RetryPolicy, RetryContext>();

		public ExceptionClassifierRetryContext(RetryContext parent,
				Classifier<Throwable, RetryPolicy> exceptionClassifier) {
			super(parent);
			this.exceptionClassifier = exceptionClassifier;
		}

		public boolean canRetry(RetryContext context) {
			if (this.context == null) {
				// there was no error yet
				return true;
			}
			return policy.canRetry(this.context);
		}

		public void close(RetryContext context) {
			// Only close those policies that have been used (opened):
			for (RetryPolicy policy : contexts.keySet()) {
				policy.close(getContext(policy, context.getParent()));
			}
		}

		public RetryContext open(RetryContext parent) {
			return this;
		}

		public void registerThrowable(RetryContext context, Throwable throwable) {
			policy = exceptionClassifier.classify(throwable);
			Assert.notNull(policy, "Could not locate policy for exception=[" + throwable + "].");
			this.context = getContext(policy, context.getParent());
			policy.registerThrowable(this.context, throwable);
		}

		private RetryContext getContext(RetryPolicy policy, RetryContext parent) {
			RetryContext context = contexts.get(policy);
			if (context == null) {
				context = policy.open(parent);
				contexts.put(policy, context);
			}
			return context;
		}

	}

}
