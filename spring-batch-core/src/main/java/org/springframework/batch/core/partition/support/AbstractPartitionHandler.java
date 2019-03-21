/*
 * Copyright 2006-2013 the original author or authors.
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
package org.springframework.batch.core.partition.support;

import java.util.Collection;
import java.util.Set;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;

/**
 * Base {@link PartitionHandler} implementation providing common base
 * features. Subclasses are expected to implement only the
 * {@link #doHandle(org.springframework.batch.core.StepExecution, java.util.Set)}
 * method which returns with the result of the execution(s) or an exception if
 * the step failed to process.
 *
 * @author Sebastien Gerard
 * @author Dave Syer
 */
public abstract class AbstractPartitionHandler implements PartitionHandler {

	private int gridSize = 1;

	/**
	 * Executes the specified {@link StepExecution} instances and returns an updated
	 * view of them. Throws an {@link Exception} if anything goes wrong.
	 *
	 * @param masterStepExecution the whole partition execution
	 * @param partitionStepExecutions the {@link StepExecution} instances to execute
	 * @return an updated view of these completed {@link StepExecution} instances
	 * @throws Exception if anything goes wrong. This allows implementations to
	 * be liberal and rely on the caller to translate an exception into a step
	 * failure as necessary.
	 */
	protected abstract Set<StepExecution> doHandle(StepExecution masterStepExecution,
			Set<StepExecution> partitionStepExecutions) throws Exception;

	/**
	 * @see PartitionHandler#handle(StepExecutionSplitter, StepExecution)
	 */
	@Override
	public Collection<StepExecution> handle(final StepExecutionSplitter stepSplitter,
			final StepExecution masterStepExecution) throws Exception {
		final Set<StepExecution> stepExecutions = stepSplitter.split(masterStepExecution, gridSize);

		return doHandle(masterStepExecution, stepExecutions);
	}

	/**
	 * Returns the number of step executions.
	 *
	 * @return the number of step executions
	 */
	public int getGridSize() {
		return gridSize;
	}

	/**
	 * Passed to the {@link StepExecutionSplitter} in the
	 * {@link #handle(StepExecutionSplitter, StepExecution)} method, instructing
	 * it how many {@link StepExecution} instances are required, ideally. The
	 * {@link StepExecutionSplitter} is allowed to ignore the grid size in the
	 * case of a restart, since the input data partitions must be preserved.
	 *
	 * @param gridSize the number of step executions that will be created
	 */
	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}

}

