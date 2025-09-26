/*
 * Copyright 2013-present the original author or authors.
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
package org.springframework.batch.core.step.tasklet;

import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepContribution;

/**
 * An extension to the {@link Tasklet} interface to allow users to add logic for stopping
 * a tasklet. It is up to each implementation as to how the stop will behave. The only
 * guarantee provided by the framework is that a call to {@link JobOperator#stop(long)}
 * will attempt to call the stop method on any currently running StoppableTasklet. The
 * call to {@link StoppableTasklet#stop()} will be from a thread other than the thread
 * executing
 * {@link org.springframework.batch.core.step.tasklet.Tasklet#execute(StepContribution, org.springframework.batch.core.scope.context.ChunkContext)}
 * so the appropriate thread safety and visibility controls should be put in place.
 *
 * @author Will Schipp
 * @author Hyunsang Han
 * @since 3.0
 */
public interface StoppableTasklet extends Tasklet {

	/**
	 * Used to signal that the job this {@link Tasklet} is executing within has been
	 * requested to stop.
	 * @deprecated Since 6.0, use {@link #stop(StepExecution)} instead.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	void stop();

	/**
	 * Used to signal that the job should stop, providing access to the current
	 * {@link StepExecution} context.
	 * @param stepExecution the current {@link StepExecution} context in which the job is
	 * being executed
	 * @since 6.0
	 */
	default void stop(StepExecution stepExecution) {
		stop();
	}

}
