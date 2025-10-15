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

import java.time.LocalDateTime;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;

/**
 * Extension of the {@link Step} interface to be implemented by steps that support being
 * stopped.
 *
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public interface StoppableStep extends Step {

	/**
	 * Callback to signal the step to stop. The default implementation marks the
	 * {@link StepExecution} as terminate only and set its status to stopped and its end
	 * time to the current time. Concrete implementations can override this method to add
	 * custom stop logic.
	 * @param stepExecution the current step execution
	 */
	default void stop(StepExecution stepExecution) {
		stepExecution.setTerminateOnly();
		stepExecution.setStatus(BatchStatus.STOPPED);
		stepExecution.setExitStatus(ExitStatus.STOPPED);
		stepExecution.setEndTime(LocalDateTime.now());
	}

}
