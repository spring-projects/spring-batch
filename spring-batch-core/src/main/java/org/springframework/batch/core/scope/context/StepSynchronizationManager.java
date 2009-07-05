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
package org.springframework.batch.core.scope.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;

/**
 * Central convenience class for framework use in managing the step scope
 * context. Generally only to be used by implementations of {@link Step}. N.B.
 * it is the responsibility of every {@link Step} implementation to ensure that
 * a {@link StepContext} is available on every thread that might be involved in
 * a step execution, including worker threads from a pool.
 * 
 * @author Dave Syer
 * 
 */
public class StepSynchronizationManager {

	/*
	 * We have to deal with single and multi-threaded execution, with a single
	 * and with multiple step execution instances. That's 2x2 = 4 scenarios.
	 */

	/**
	 * Storage for the current step execution; has to be ThreadLocal because it
	 * is needed to locate a StepContext in components that are not part of a
	 * Step (like when re-hydrating a scoped proxy). Doesn't use
	 * InheritableThreadLocal because there are side effects if a step is trying
	 * to run multiple child steps (e.g. with partitioning). The Stack is used
	 * to cover the single threaded case, so that the API is the same as
	 * multi-threaded.
	 */
	private static final ThreadLocal<Stack<StepExecution>> executionHolder = new ThreadLocal<Stack<StepExecution>>();

	/**
	 * Reference counter for each step execution: how many threads are using the
	 * same one?
	 */
	private static final Map<StepExecution, AtomicInteger> counts = new HashMap<StepExecution, AtomicInteger>();

	/**
	 * Simple map from a running step execution to the associated context.
	 */
	private static final Map<StepExecution, StepContext> contexts = new HashMap<StepExecution, StepContext>();

	/**
	 * Getter for the current context if there is one, otherwise returns null.
	 * 
	 * @return the current {@link StepContext} or null if there is none (if one
	 * has not been registered for this thread).
	 */
	public static StepContext getContext() {
		if (getCurrent().isEmpty()) {
			return null;
		}
		synchronized (contexts) {
			return contexts.get(getCurrent().peek());
		}
	}

	/**
	 * Register a context with the current thread - always put a matching
	 * {@link #close()} call in a finally block to ensure that the correct
	 * context is available in the enclosing block.
	 * 
	 * @param stepExecution the step context to register
	 * @return a new {@link StepContext} or the current one if it has the same
	 * {@link StepExecution}
	 */
	public static StepContext register(StepExecution stepExecution) {
		if (stepExecution == null) {
			return null;
		}
		getCurrent().push(stepExecution);
		StepContext context;
		synchronized (contexts) {
			context = contexts.get(stepExecution);
			if (context == null) {
				context = new StepContext(stepExecution);
				contexts.put(stepExecution, context);
			}
		}
		increment();
		return context;
	}

	/**
	 * Method for de-registering the current context - should always and only be
	 * used by in conjunction with a matching {@link #register(StepExecution)}
	 * to ensure that {@link #getContext()} always returns the correct value.
	 * Does not call {@link StepContext#close()} - that is left up to the caller
	 * because he has a reference to the context (having registered it) and only
	 * he has knowledge of when the step actually ended.
	 */
	public static void close() {
		StepContext oldSession = getContext();
		if (oldSession == null) {
			return;
		}
		decrement();
	}

	private static void decrement() {
		StepExecution current = getCurrent().pop();
		if (current != null) {
			int remaining = counts.get(current).decrementAndGet();
			if (remaining <= 0) {
				synchronized (contexts) {
					contexts.remove(current);
					counts.remove(current);
				}
			}
		}
	}

	private static void increment() {
		StepExecution current = getCurrent().peek();
		if (current != null) {
			AtomicInteger count;
			synchronized (counts) {
				count = counts.get(current);
				if (count == null) {
					count = new AtomicInteger();
					counts.put(current, count);
				}
			}
			count.incrementAndGet();
		}
	}

	private static Stack<StepExecution> getCurrent() {
		if (executionHolder.get() == null) {
			executionHolder.set(new Stack<StepExecution>());
		}
		return executionHolder.get();
	}

	/**
	 * A convenient "deep" close operation. Call this instead of
	 * {@link #close()} if the step execution for the current context is ending.
	 * Delegates to {@link StepContext#close()} and then ensures that
	 * {@link #close()} is also called in a finally block.
	 */
	public static void release() {
		StepContext context = getContext();
		try {
			if (context != null) {
				context.close();
			}
		}
		finally {
			close();
		}
	}

}
