/*
 * Copyright 2006-2019 the original author or authors.
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

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.lang.Nullable;

/**
 * Central convenience class for framework use in managing the step scope
 * context. Generally only to be used by implementations of {@link Step}. N.B.
 * it is the responsibility of every {@link Step} implementation to ensure that
 * a {@link StepContext} is available on every thread that might be involved in
 * a step execution, including worker threads from a pool.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
public class StepSynchronizationManager {

	private static final SynchronizationManagerSupport<StepExecution, StepContext> manager =
			new SynchronizationManagerSupport<StepExecution, StepContext>() {

		@Override
		protected StepContext createNewContext(StepExecution execution, @Nullable BatchPropertyContext propertyContext) {
			StepContext context;

			if(propertyContext != null) {
				context = new StepContext(execution, propertyContext);
			} else {
				context = new StepContext(execution);
			}

			return context;
		}

		@Override
		protected void close(StepContext context) {
			context.close();
		}
	};

	/**
	 * Getter for the current context if there is one, otherwise returns {@code null}.
	 *
	 * @return the current {@link StepContext} or {@code null} if there is none (if one
	 * has not been registered for this thread).
	 */
	@Nullable
	public static StepContext getContext() {
		return manager.getContext();
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
		return manager.register(stepExecution);
	}

	/**
	 * Register a context with the current thread - always put a matching
	 * {@link #close()} call in a finally block to ensure that the correct
	 * context is available in the enclosing block.
	 *
	 * @param stepExecution the step context to register
	 * @param propertyContext an instance of {@link BatchPropertyContext} to be
	 * used by the StepSynchronizationManager.
	 * @return a new {@link StepContext} or the current one if it has the same
	 * {@link StepExecution}
	 */
	public static StepContext register(StepExecution stepExecution, BatchPropertyContext propertyContext) {
		return manager.register(stepExecution, propertyContext);
	}

	/**
	 * Method for unregistering the current context - should always and only be
	 * used by in conjunction with a matching {@link #register(StepExecution)}
	 * to ensure that {@link #getContext()} always returns the correct value.
	 * Does not call {@link StepContext#close()} - that is left up to the caller
	 * because he has a reference to the context (having registered it) and only
	 * he has knowledge of when the step actually ended.
	 */
	public static void close() {
		manager.close();
	}

	/**
	 * A convenient "deep" close operation. Call this instead of
	 * {@link #close()} if the step execution for the current context is ending.
	 * Delegates to {@link StepContext#close()} and then ensures that
	 * {@link #close()} is also called in a finally block.
	 */
	public static void release() {
		manager.release();
	}
}
