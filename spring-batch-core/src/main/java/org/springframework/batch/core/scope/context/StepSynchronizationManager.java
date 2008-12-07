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

import java.util.Stack;

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

	/**
	 * Don't use InheritableThreadLocal because there are side effects if a step
	 * is trying to run multiple child steps (e.g. with partitioning).
	 */
	private static final ThreadLocal<Stack<StepContext>> contextHolder = new ThreadLocal<Stack<StepContext>>();

	/**
	 * Getter for the current context if there is one, otherwise returns null.
	 * 
	 * @return the current {@link StepContext} or null if there is none (if one
	 * has not been registered for this thread).
	 */
	public static StepContext getContext() {
		Stack<StepContext> current = getCurrent();
		if (current.isEmpty()) {
			return null;
		}
		return current.peek();
	}

	/**
	 * Method for registering a context with the current thread - always put a
	 * matching {@link #close()} call in a finally block to ensure that the
	 * correct context is available in the enclosing block.
	 * 
	 * @param stepExecution the step context to register
	 * @return a new {@link StepContext} or the current one if it has the same
	 * {@link StepExecution}
	 */
	public static StepContext register(StepExecution stepExecution) {
		if (stepExecution == null) {
			return null;
		}
		StepContext current = getContext();
		StepContext context;
		if (current != null && current.getStepExecution().equals(stepExecution)) {
			/*
			 * If the new context has the same step execution we don't want a
			 * new set of attributes, otherwise auto proxied beans get created
			 * twice for the same execution.
			 */
			context = current;
		} else {
			context = new StepContext(stepExecution);
		}
		getCurrent().push(context);
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
		getCurrent().pop();
	}

	private static Stack<StepContext> getCurrent() {
		if (contextHolder.get() == null) {
			contextHolder.set(new Stack<StepContext>());
		}
		return contextHolder.get();
	}

	/**
	 * A "deep" close operation. Call this instead of {@link #close()} if the
	 * step execution for the current context is ending. Delegates to
	 * {@link StepContext#close()} and then ensures that {@link #close()} is
	 * also called in a finally block.
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
