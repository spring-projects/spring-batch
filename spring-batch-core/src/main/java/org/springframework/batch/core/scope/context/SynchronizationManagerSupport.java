/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.batch.core.scope.context;

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.lang.Nullable;


/**
 * Central convenience class for framework use in managing the scope
 * context.
 *
 * @author Dave Syer
 * @author Jimmy Praet
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public abstract class SynchronizationManagerSupport<E, C> {

	/*
	 * We have to deal with single and multi-threaded execution, with a single
	 * and with multiple step execution instances. That's 2x2 = 4 scenarios.
	 */

	/**
	 * Storage for the current execution; has to be ThreadLocal because it
	 * is needed to locate a context in components that are not part of a
	 * step/job (like when re-hydrating a scoped proxy). Doesn't use
	 * InheritableThreadLocal because there are side effects if a step is trying
	 * to run multiple child steps (e.g. with partitioning). The Stack is used
	 * to cover the single threaded case, so that the API is the same as
	 * multi-threaded.
	 */
	private final ThreadLocal<Stack<E>> executionHolder = new ThreadLocal<>();

	/**
	 * Reference counter for each execution: how many threads are using the
	 * same one?
	 */
	private final Map<E, AtomicInteger> counts = new ConcurrentHashMap<>();

	/**
	 * Simple map from a running execution to the associated context.
	 */
	private final Map<E, C> contexts = new ConcurrentHashMap<>();

	/**
	 * Getter for the current context if there is one, otherwise returns {@code null}.
	 *
	 * @return the current context or {@code null} if there is none (if one
	 *         has not been registered for this thread).
	 */
	@Nullable
	public C getContext() {
		if (getCurrent().isEmpty()) {
			return null;
		}
		synchronized (contexts) {
			return contexts.get(getCurrent().peek());
		}
	}

	/**
	 * Register a context with the current thread - always put a matching {@link #close()} call in a finally block to
	 * ensure that the correct
	 * context is available in the enclosing block.
	 *
	 * @param execution the execution to register
	 * @return a new context or the current one if it has the same
	 *         execution
	 */
	@Nullable
	public C register(@Nullable E execution) {
		if (execution == null) {
			return null;
		}
		getCurrent().push(execution);
		C context;
		synchronized (contexts) {
			context = contexts.get(execution);
			if (context == null) {
				context = createNewContext(execution, null);
				contexts.put(execution, context);
			}
		}
		increment();
		return context;
	}

	/**
	 * Register a context with the current thread - always put a matching {@link #close()} call in a finally block to
	 * ensure that the correct
	 * context is available in the enclosing block.
	 *
	 * @param execution the execution to register
	 * @param propertyContext instance of {@link BatchPropertyContext} to be registered with this thread.
	 * @return a new context or the current one if it has the same
	 *         execution
	 */
	@Nullable
	public C register(@Nullable E execution, @Nullable BatchPropertyContext propertyContext) {
		if (execution == null) {
			return null;
		}
		getCurrent().push(execution);
		C context;
		synchronized (contexts) {
			context = contexts.get(execution);
			if (context == null) {
				context = createNewContext(execution, propertyContext);
				contexts.put(execution, context);
			}
		}
		increment();
		return context;
	}

	/**
	 * Method for unregistering the current context - should always and only be
	 * used by in conjunction with a matching {@link #register(Object)} to ensure that {@link #getContext()} always returns
	 * the correct value.
	 * Does not call close on the context - that is left up to the caller
	 * because he has a reference to the context (having registered it) and only
	 * he has knowledge of when the execution actually ended.
	 */
	public void close() {
		C oldSession = getContext();
		if (oldSession == null) {
			return;
		}
		decrement();
	}

	private void decrement() {
		E current = getCurrent().pop();
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

	public void increment() {
		E current = getCurrent().peek();
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

	public Stack<E> getCurrent() {
		if (executionHolder.get() == null) {
			executionHolder.set(new Stack<>());
		}
		return executionHolder.get();
	}

	/**
	 * A convenient "deep" close operation. Call this instead of {@link #close()} if the execution for the current
	 * context is ending.
	 * Delegates to {@link #close(Object)} and then ensures that {@link #close()} is also called in a finally block.
	 */
	public void release() {
		C context = getContext();
		try {
			if (context != null) {
				close(context);
			}
		} finally {
			close();
		}
	}

	protected abstract void close(C context);

	protected abstract C createNewContext(E execution, @Nullable BatchPropertyContext propertyContext);

}
