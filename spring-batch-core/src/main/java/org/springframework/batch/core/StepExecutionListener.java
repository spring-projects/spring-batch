/*
 * Copyright 2006-2018 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * Listener interface for the lifecycle of a {@link Step}.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public interface StepExecutionListener extends StepListener {

	/**
	 * Initialize the state of the listener with the {@link StepExecution} from
	 * the current scope.
	 *
	 * @param stepExecution instance of {@link StepExecution}.
	 */
	void beforeStep(StepExecution stepExecution);

	/**
	 * Give a listener a chance to modify the exit status from a step. The value
	 * returned will be combined with the normal exit status using
	 * {@link ExitStatus#and(ExitStatus)}.
	 *
	 * Called after execution of step's processing logic (both successful or
	 * failed). Throwing exception in this method has no effect, it will only be
	 * logged.
	 *
	 * @param stepExecution {@link StepExecution} instance.
	 * @return an {@link ExitStatus} to combine with the normal value. Return
	 * {@code null} to leave the old value unchanged.
	 */
	@Nullable
	ExitStatus afterStep(StepExecution stepExecution);
}
