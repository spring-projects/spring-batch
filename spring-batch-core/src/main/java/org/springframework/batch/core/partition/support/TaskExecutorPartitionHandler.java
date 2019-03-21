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

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.step.StepHolder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * A {@link PartitionHandler} that uses a {@link TaskExecutor} to execute the
 * partitioned {@link Step} locally in multiple threads. This can be an
 * effective approach for scaling batch steps that are IO intensive, like
 * directory and filesystem scanning and copying.
 * <br>
 * By default, the thread pool is synchronous.
 *
 * @author Sebastien Gerard
 * @author Dave Syer
 * @since 2.0
 */
public class TaskExecutorPartitionHandler extends AbstractPartitionHandler implements StepHolder, InitializingBean {

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	private Step step;

    @Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(step != null, "A Step must be provided.");
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
    @Override
	public Step getStep() {
		return this.step;
	}

    @Override
    protected Set<StepExecution> doHandle(StepExecution masterStepExecution,
                                          Set<StepExecution> partitionStepExecutions) throws Exception {
        Assert.notNull(step, "A Step must be provided.");
        final Set<Future<StepExecution>> tasks = new HashSet<>(getGridSize());
        final Set<StepExecution> result = new HashSet<>();

        for (final StepExecution stepExecution : partitionStepExecutions) {
            final FutureTask<StepExecution> task = createTask(step, stepExecution);

            try {
                taskExecutor.execute(task);
                tasks.add(task);
            } catch (TaskRejectedException e) {
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

    /**
     * Creates the task executing the given step in the context of the given execution.
     *
     * @param step the step to execute
     * @param stepExecution the given execution
     * @return the task executing the given step
     */
    protected FutureTask<StepExecution> createTask(final Step step,
                                                   final StepExecution stepExecution) {
        return new FutureTask<>(new Callable<StepExecution>() {
            @Override
            public StepExecution call() throws Exception {
                step.execute(stepExecution);
                return stepExecution;
            }
        });
    }

}
