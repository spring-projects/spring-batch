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

package org.springframework.batch.core.partition.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.step.StepHolder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.util.Assert;

/**
 * A {@link PartitionHandler} that uses a {@link TaskExecutor} to execute the
 * partitioned {@link Step} locally in multiple threads. This can be an
 * effective approach for scaling batch steps that are IO intensive, like
 * directory and filesystem scanning and copying.
 * 
 * @author Dave Syer
 * @since 2.0
 */
public class TaskExecutorPartitionHandler implements PartitionHandler, StepHolder, InitializingBean {

	private int gridSize = 1;

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	private Step step;

	public void afterPropertiesSet() throws Exception {
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

	/**
	 * Setter for the {@link TaskExecutor} that is used to farm out step
	 * executions to multiple threads.
	 * @param taskExecutor a {@link TaskExecutor}
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Setter for the {@link Step} that will be used to execute the partitioned
	 * {@link StepExecution}. This is a regular Spring Batch step, with all the
	 * business logic required to complete an execution based on the input
	 * parameters in its {@link StepExecution} context.
	 * 
	 * @param step the {@link Step} instance to use to execute business logic
	 */
	public void setStep(Step step) {
		this.step = step;
	}
	
	/**
	 * The step instance that will be executed in parallel by this handler.
	 * 
	 * @return the step instance that will be used
	 * @see StepHolder#getStep()
	 */
	public Step getStep() {
		return this.step;
	}

	/**
	 * @see PartitionHandler#handle(StepExecutionSplitter, StepExecution)
	 */
	public Collection<StepExecution> handle(StepExecutionSplitter stepExecutionSplitter,
			StepExecution masterStepExecution) throws Exception {

		Assert.notNull(step, "A Step must be provided.");
		
		Set<Future<StepExecution>> tasks = new HashSet<Future<StepExecution>>(gridSize);

		Collection<StepExecution> result = new ArrayList<StepExecution>();

		for (final StepExecution stepExecution : stepExecutionSplitter.split(masterStepExecution, gridSize)) {

			final FutureTask<StepExecution> task = new FutureTask<StepExecution>(new Callable<StepExecution>() {
				public StepExecution call() throws Exception {
					step.execute(stepExecution);
					return stepExecution;
				}
			});

			try {
				taskExecutor.execute(task);
				tasks.add(task);
			}
			catch (TaskRejectedException e) {
				// couldn't execute one of the tasks
				ExitStatus exitStatus = ExitStatus.FAILED
						.addExitDescription("TaskExecutor rejected the task for this step.");
				/*
				 * Set the status in case the caller is tracking it through the
				 * JobExecution.
				 */
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.setExitStatus(exitStatus);
				result.add(stepExecution);
			}

		}

		for (Future<StepExecution> task : tasks) {
			result.add(task.get());
		}
		return result;

	}

}
