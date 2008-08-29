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

package org.springframework.batch.repeat.context;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.util.Assert;

/**
 * Helper class for policies that need to count the number of occurrences of
 * some event (e.g. an exception type in the context) in the scope of a batch.
 * The value of the counter can be stored between batches in a nested context,
 * so that the termination decision is based on the aggregate of a number of
 * sibling batches.
 * 
 * @author Dave Syer
 * 
 */
public class RepeatContextCounter {

	final private String countKey;

	/**
	 * Flag to indicate whether the count is stored at the level of the parent
	 * context, or just local to the current context. Default value is false.
	 */
	final private boolean useParent;

	final private RepeatContext context;

	/**
	 * Increment the counter.
	 * 
	 * @param delta the amount by which to increment the counter.
	 */
	final public void increment(int delta) {
		AtomicInteger count = getCounter();
		count.addAndGet(delta);
	}
	
	/**
	 * Increment by 1.
	 */
	final public void increment() {
		increment(1);
	}

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
	 * 
	 * @param context the current context.
	 * @param countKey the key to use to store the counter in the context.
	 * @param useParent true if the counter is to be shared between siblings.
	 * The state will be stored in the parent of the context (if it exists)
	 * instead of the context itself.
	 */
	public RepeatContextCounter(RepeatContext context, String countKey, boolean useParent) {

		super();
		
		Assert.notNull(context, "The context must be provided to initialize a counter");

		this.countKey = countKey;
		this.useParent = useParent;

		RepeatContext parent = context.getParent();

		if (this.useParent && parent != null) {
			this.context = parent;
		}
		else {
			this.context = context;
		}
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

	private AtomicInteger getCounter() {
		return ((AtomicInteger) context.getAttribute(countKey));
	}

}
