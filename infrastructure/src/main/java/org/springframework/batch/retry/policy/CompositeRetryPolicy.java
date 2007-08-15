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

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.context.RetryContextSupport;
import org.springframework.batch.retry.exception.TerminatedRetryException;
import org.springframework.batch.retry.synch.RetrySynchronizationManager;

/**
 * A {@link RetryPolicy} that composes a list of other policies and delegates
 * calls to them in order.
 * 
 * @author Dave Syer
 * 
 */
public class CompositeRetryPolicy extends AbstractStatelessRetryPolicy {

	RetryPolicy[] policies = new RetryPolicy[0];

	/**
	 * Setter for policies.
	 * 
	 * @param policies
	 */
	public void setPolicies(RetryPolicy[] policies) {
		this.policies = policies;
	}

	/**
	 * Delegate to the policies that were in operation when the context was
	 * created. If any of them cannot retry then return false, oetherwise return
	 * true.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#canRetry(org.springframework.batch.retry.RetryContext)
	 */
	public boolean canRetry(RetryContext context) {
		RetryContext[] contexts = ((CompositeRetryContext) context).contexts;
		RetryPolicy[] policies = ((CompositeRetryContext) context).policies;
		for (int i = 0; i < contexts.length; i++) {
			if (!policies[i].canRetry(contexts[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Delegate to the policies that were in operation when the context was
	 * created.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#close(org.springframework.batch.retry.RetryContext)
	 */
	public void close(RetryContext context) {
		RetryContext[] contexts = ((CompositeRetryContext) context).contexts;
		RetryPolicy[] policies = ((CompositeRetryContext) context).policies;
		// TODO: throw some sort of composite exception if any of the close
		// methods fail?
		for (int i = 0; i < contexts.length; i++) {
			policies[i].close(contexts[i]);
		}
	}

	/**
	 * Creates a new context that copies the existing policies and keeps a list
	 * of the contexts from each one.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#open(org.springframework.batch.retry.RetryCallback)
	 */
	public RetryContext open(RetryCallback callback) {
		List list = new ArrayList();
		for (int i = 0; i < policies.length; i++) {
			list.add(policies[i].open(callback));
		}
		return new CompositeRetryContext(list);
	}

	/**
	 * Delegate to the policies that were in operation when the context was
	 * created.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#close(org.springframework.batch.retry.RetryContext)
	 */
	public void registerThrowable(RetryContext context, Throwable throwable) throws TerminatedRetryException {
		RetryContext[] contexts = ((CompositeRetryContext) context).contexts;
		RetryPolicy[] policies = ((CompositeRetryContext) context).policies;
		for (int i = 0; i < contexts.length; i++) {
			policies[i].registerThrowable(contexts[i], throwable);
		}
		((RetryContextSupport) context).registerThrowable(throwable);
	}

	private class CompositeRetryContext extends RetryContextSupport {
		RetryContext[] contexts;

		RetryPolicy[] policies;

		public CompositeRetryContext(List contexts) {
			super(RetrySynchronizationManager.getContext());
			this.contexts = (RetryContext[]) contexts.toArray(new RetryContext[0]);
			this.policies = CompositeRetryPolicy.this.policies;
		}

	}

}
