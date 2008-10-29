package org.springframework.batch.core.partition.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.util.Assert;

public class TaskExecutorPartitionHandler implements PartitionHandler, InitializingBean {

	private int gridSize = 1;

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	private Step step;

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(step, "A Step must be provided.");
	}

	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setStep(Step step) {
		this.step = step;
	}

	/**
	 * @see PartitionHandler#handle(StepExecutionSplitter, StepExecution)
	 */
	public Collection<StepExecution> handle(StepExecutionSplitter stepExecutionSplitter,
			StepExecution masterStepExecution) throws Exception {

		Set<FutureTask<StepExecution>> tasks = new HashSet<FutureTask<StepExecution>>(gridSize);

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
				 * This stepExecution hasn't been saved yet, but we'll set the
				 * status anyway in case the caller is tracking it through the
				 * JobExecution.
				 */
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.setExitStatus(exitStatus);
				result.add(stepExecution);
			}

		}

		for (FutureTask<StepExecution> task : tasks) {
			// TODO: timeout / heart beat
			result.add(task.get());
		}
		return result;

	}

}
