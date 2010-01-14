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

package org.springframework.batch.repeat.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.context.RepeatContextSupport;

/**
 * Composite policy that loops through a list of delegate policies and answers
 * calls by a concensus.
 * 
 * @author Dave Syer
 * 
 */
public class CompositeCompletionPolicy implements CompletionPolicy {

	CompletionPolicy[] policies = new CompletionPolicy[0];

	/**
	 * Setter for the policies.
	 * 
	 * @param policies
	 */
	public void setPolicies(CompletionPolicy[] policies) {
		this.policies = Arrays.asList(policies).toArray(new CompletionPolicy[policies.length]);
	}

	/**
	 * This policy is complete if any of the composed policies is complete.
	 * 
	 * @see org.springframework.batch.repeat.CompletionPolicy#isComplete(org.springframework.batch.repeat.RepeatContext,
	 * RepeatStatus)
	 */
	public boolean isComplete(RepeatContext context, RepeatStatus result) {
		RepeatContext[] contexts = ((CompositeBatchContext) context).contexts;
		CompletionPolicy[] policies = ((CompositeBatchContext) context).policies;
		for (int i = 0; i < policies.length; i++) {
			if (policies[i].isComplete(contexts[i], result)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This policy is complete if any of the composed policies is complete.
	 * 
	 * @see org.springframework.batch.repeat.CompletionPolicy#isComplete(org.springframework.batch.repeat.RepeatContext)
	 */
	public boolean isComplete(RepeatContext context) {
		RepeatContext[] contexts = ((CompositeBatchContext) context).contexts;
		CompletionPolicy[] policies = ((CompositeBatchContext) context).policies;
		for (int i = 0; i < policies.length; i++) {
			if (policies[i].isComplete(contexts[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Create a new composite context from all the available policies.
	 * 
	 * @see org.springframework.batch.repeat.CompletionPolicy#start(RepeatContext)
	 */
	public RepeatContext start(RepeatContext context) {
		List<RepeatContext> list = new ArrayList<RepeatContext>();
		for (int i = 0; i < policies.length; i++) {
			list.add(policies[i].start(context));
		}
		return new CompositeBatchContext(context, list);

	}

	/**
	 * Update all the composed contexts, and also increment the parent context.
	 * 
	 * @see org.springframework.batch.repeat.CompletionPolicy#update(org.springframework.batch.repeat.RepeatContext)
	 */
	public void update(RepeatContext context) {
		RepeatContext[] contexts = ((CompositeBatchContext) context).contexts;
		CompletionPolicy[] policies = ((CompositeBatchContext) context).policies;
		for (int i = 0; i < policies.length; i++) {
			policies[i].update(contexts[i]);
		}
		((RepeatContextSupport) context).increment();
	}

	/**
	 * Composite context that knows about the policies and contexts is was
	 * created with.
	 * 
	 * @author Dave Syer
	 * 
	 */
	protected class CompositeBatchContext extends RepeatContextSupport {

		private RepeatContext[] contexts;

		// Save a reference to the policies when we were created - gives some
		// protection against reference changes (e.g. if the number of policies
		// change).
		private CompletionPolicy[] policies;

		public CompositeBatchContext(RepeatContext context, List<RepeatContext> contexts) {
			super(context);
			this.contexts = contexts.toArray(new RepeatContext[contexts.size()]);
			this.policies = CompositeCompletionPolicy.this.policies;
		}

	}

}
