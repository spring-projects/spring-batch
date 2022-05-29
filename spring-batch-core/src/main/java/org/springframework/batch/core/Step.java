/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core;

/**
 * Batch domain interface representing the configuration of a step. As with a {@link Job},
 * a {@link Step} is meant to explicitly represent the configuration of a step by a
 * developer but also the ability to execute the step.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public interface Step {

	/**
	 * The key to use when retrieving the batch step type.
	 */
	String STEP_TYPE_KEY = "batch.stepType";

	/**
	 * @return the name of this step.
	 */
	String getName();

	/**
	 * @return {@code true} if a step that is already marked as complete can be started
	 * again. Defaults to {@code false}.
	 */
	default boolean isAllowStartIfComplete() {
		return false;
	}

	/**
	 * @return the number of times a step can be (re)started for the same job instance.
	 * Defaults to {@code Integer.MAX_VALUE}
	 */
	default int getStartLimit() {
		return Integer.MAX_VALUE;
	}

	/**
	 * Process the step and assign progress and status meta information to the
	 * {@link StepExecution} provided. The {@link Step} is responsible for setting the
	 * meta information and also saving it, if required by the implementation.<br>
	 *
	 * It is not safe to reuse an instance of {@link Step} to process multiple concurrent
	 * executions.
	 * @param stepExecution an entity representing the step to be executed.
	 * @throws JobInterruptedException if the step is interrupted externally.
	 */
	void execute(StepExecution stepExecution) throws JobInterruptedException;

}
