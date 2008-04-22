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

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextCounter;
import org.springframework.batch.repeat.context.RepeatContextSupport;

/**
 * Abstract base class for policies that need to count the number of occurrences
 * of some event (e.g. an exception type in the context), and terminate based on
 * a limit for the counter. The value of the counter can be stored between
 * batches in a nested context, so that the termination decision is based on the
 * aggregate of a number of sibling batches.
 * 
 * @author Dave Syer
 * 
 */
public abstract class CountingCompletionPolicy extends DefaultResultCompletionPolicy {

	/**
	 * Session key for global counter.
	 */
	public static final String COUNT = CountingCompletionPolicy.class.getName() + ".COUNT";

	private boolean useParent = false;

	private int maxCount = 0;

	/**
	 * Flag to indicate whether the count is at the level of the parent context,
	 * or just local to the context. If true then the count is aggregated among
	 * siblings in a nested batch.
	 * 
	 * @param useParent whether to use the parent context to cache the total
	 * count. Default value is false.
	 */
	public void setUseParent(boolean useParent) {
		this.useParent = useParent;
	}

	/**
	 * Setter for maximum value of count before termination.
	 * 
	 * @param maxCount the maximum number of counts before termination. Default
	 * 0 so termination is immediate.
	 */
	public void setMaxCount(int maxCount) {
		this.maxCount = maxCount;
	}

	/**
	 * Extension point for subclasses. Obtain the value of the count in the
	 * current context. Subclasses can count the number of attempts or
	 * violations and store the result in their context. This policy base class
	 * will take care of the termination contract and aggregating at the level
	 * of the session if required.
	 * 
	 * @param context the current context, specific to the subclass.
	 * @return the value of the counter in the context.
	 */
	protected abstract int getCount(RepeatContext context);

	/**
	 * Extension point for subclasses. Inspect the context and update the state
	 * of a counter in whatever way is appropriate. This will be added to the
	 * session-level counter if {@link #setUseParent(boolean)} is true.
	 * 
	 * @param context the current context.
	 * 
	 * @return the change in the value of the counter (default 0).
	 */
	protected int doUpdate(RepeatContext context) {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.repeat.policy.CompletionPolicySupport#isComplete(org.springframework.batch.repeat.BatchContext)
	 */
	final public boolean isComplete(RepeatContext context) {
		int count = ((CountingBatchContext) context).getCounter().getCount();
		return count >= maxCount;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.repeat.policy.CompletionPolicySupport#start(org.springframework.batch.repeat.BatchContext)
	 */
	public RepeatContext start(RepeatContext parent) {
		return new CountingBatchContext(parent);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.repeat.policy.CompletionPolicySupport#update(org.springframework.batch.repeat.BatchContext)
	 */
	final public void update(RepeatContext context) {
		super.update(context);
		int delta = doUpdate(context);
		((CountingBatchContext) context).getCounter().increment(delta);
	}

	protected class CountingBatchContext extends RepeatContextSupport {

		RepeatContextCounter counter;

		public CountingBatchContext(RepeatContext parent) {
			super(parent);
			counter = new RepeatContextCounter(this, COUNT, useParent);
		}

		public RepeatContextCounter getCounter() {
			return counter;
		}

	}
}
