/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.infrastructure.repeat.context;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.batch.infrastructure.repeat.RepeatContext;
import org.springframework.util.Assert;

/**
 * Helper class for policies that need to count the number of occurrences of some event
 * (e.g. an exception type in the context) in the scope of a batch. The value of the
 * counter can be stored between batches in a nested context, so that the termination
 * decision is based on the aggregate of a number of sibling batches.
 *
 * @author Dave Syer
 * @author Stefano Cordio
 */
public class RepeatContextCounter {

	private final String countKey;

	private final RepeatContext context;

	/**
	 * Convenience constructor with useParent=false.
	 * @param context the current context.
	 * @param countKey the key to use to store the counter in the context.
	 */
	public RepeatContextCounter(RepeatContext context, String countKey) {
		this(context, countKey, false);
	}

	/**
	 * Construct a new {@link RepeatContextCounter}.
	 * @param context the current context.
	 * @param countKey the key to use to store the counter in the context.
	 * @param useParent true if the counter is to be shared between siblings. The state
	 * will be stored in the parent of the context (if it exists) instead of the context
	 * itself.
	 */
	public RepeatContextCounter(RepeatContext context, String countKey, boolean useParent) {
		Assert.notNull(context, "The context must be provided to initialize a counter");

		this.countKey = countKey;

		RepeatContext parent = context.getParent();

		this.context = useParent && parent != null ? parent : context;
		if (!this.context.hasAttribute(countKey)) {
			this.context.setAttribute(countKey, new AtomicInteger());
		}
	}

	/**
	 * @return the current value of the counter
	 */
	public int getCount() {
		return getCounter().intValue();
	}

	/**
	 * Increment the counter.
	 * @param delta the amount by which to increment the counter.
	 */
	public final void increment(int delta) {
		getCounter().addAndGet(delta);
	}

	/**
	 * Increment by 1.
	 */
	public final void increment() {
		increment(1);
	}

	@SuppressWarnings("DataFlowIssue")
	private AtomicInteger getCounter() {
		return ((AtomicInteger) context.getAttribute(countKey));
	}

}
