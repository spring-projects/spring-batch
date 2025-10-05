/*
 * Copyright 2006-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.infrastructure.repeat.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.batch.infrastructure.repeat.CompletionPolicy;
import org.springframework.batch.infrastructure.repeat.RepeatContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.repeat.context.RepeatContextSupport;

/**
 * Composite policy that loops through a list of delegate policies and answers calls by a
 * consensus.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class CompositeCompletionPolicy implements CompletionPolicy {

	CompletionPolicy[] policies = new CompletionPolicy[0];

	/**
	 * Setter for the policies.
	 * @param policies an array of completion policies to be used to determine
	 * {@link #isComplete(RepeatContext)} by consensus.
	 */
	public void setPolicies(CompletionPolicy[] policies) {
		this.policies = Arrays.asList(policies).toArray(new CompletionPolicy[policies.length]);
	}

	/**
	 * This policy is complete if any of the composed policies is complete.
	 *
	 * @see CompletionPolicy#isComplete(RepeatContext, RepeatStatus)
	 */
	@Override
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
	 * @see CompletionPolicy#isComplete(RepeatContext)
	 */
	@Override
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
	 * @see CompletionPolicy#start(RepeatContext)
	 */
	@Override
	public RepeatContext start(RepeatContext context) {
		List<RepeatContext> list = new ArrayList<>();
		for (CompletionPolicy policy : policies) {
			list.add(policy.start(context));
		}
		return new CompositeBatchContext(context, list);

	}

	/**
	 * Update all the composed contexts, and also increment the parent context.
	 *
	 * @see CompletionPolicy#update(RepeatContext)
	 */
	@Override
	public void update(RepeatContext context) {
		RepeatContext[] contexts = ((CompositeBatchContext) context).contexts;
		CompletionPolicy[] policies = ((CompositeBatchContext) context).policies;
		for (int i = 0; i < policies.length; i++) {
			policies[i].update(contexts[i]);
		}
		((RepeatContextSupport) context).increment();
	}

	/**
	 * Composite context that knows about the policies and contexts is was created with.
	 *
	 * @author Dave Syer
	 *
	 */
	protected class CompositeBatchContext extends RepeatContextSupport {

		private final RepeatContext[] contexts;

		// Save a reference to the policies when we were created - gives some
		// protection against reference changes (e.g. if the number of policies
		// change).
		private final CompletionPolicy[] policies;

		public CompositeBatchContext(RepeatContext context, List<RepeatContext> contexts) {
			super(context);
			this.contexts = contexts.toArray(new RepeatContext[contexts.size()]);
			this.policies = CompositeCompletionPolicy.this.policies;
		}

	}

}
