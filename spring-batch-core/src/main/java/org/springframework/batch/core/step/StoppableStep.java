/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.core.step;

import java.util.concurrent.CompletableFuture;

/**
 * Extension of the {@link Step} interface to be implemented by steps that support being
 * stopped.
 *
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public interface StoppableStep extends Step {

	/**
	 * Callback to signal the step to stop. The default implementation sets the
	 * {@link StepExecution} to terminate only. Concrete implementations can override this
	 * method to add custom stop logic.
	 * @param stepExecution the current step execution
	 */
	default void stop(StepExecution stepExecution) {
		stepExecution.setTerminateOnly();
	}

	/**
	 * Subscribe to the termination of the given step execution in the current JVM. The
	 * returned future completes once the step execution running in this JVM has reached a
	 * terminal state and its final metadata has been saved.
	 * @param stepExecution the step execution to observe
	 * @return a future completed when the step execution terminates
	 * @since 6.0
	 */
	default CompletableFuture<StepExecution> subscribeToTermination(StepExecution stepExecution) {
		return CompletableFuture.completedFuture(stepExecution);
	}

}
