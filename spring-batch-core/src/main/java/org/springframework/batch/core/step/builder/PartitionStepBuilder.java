/*
 * Copyright 2006-2011 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.partition.support.PartitionStep;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.SimpleStepExecutionSplitter;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * @author Dave Syer
 * 
 */
public class PartitionStepBuilder extends StepBuilderHelper<PartitionStepBuilder> {

	private TaskExecutor taskExecutor;

	private Partitioner partitioner;

	private static final int DEFAULT_GRID_SIZE = 6;

	private Step step;

	private PartitionHandler partitionHandler;

	private int gridSize = DEFAULT_GRID_SIZE;

	private StepExecutionSplitter splitter;

	private StepExecutionAggregator aggregator;

	private String stepName;

	public PartitionStepBuilder(StepBuilderHelper<?> parent) {
		super(parent);
	}

	public PartitionStepBuilder partitioner(String slaveStepName, Partitioner partitioner) {
		this.stepName = slaveStepName;
		this.partitioner = partitioner;
		return this;
	}

	public PartitionStepBuilder step(Step step) {
		this.step = step;
		return this;
	}

	public PartitionStepBuilder taskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		return this;
	}

	public PartitionStepBuilder partitionHandler(PartitionHandler partitionHandler) {
		this.partitionHandler = partitionHandler;
		return this;
	}

	public PartitionStepBuilder gridSize(int gridSize) {
		this.gridSize = gridSize;
		return this;
	}

	public Step build() {

		PartitionStep step = new PartitionStep();
		step.setName(getName());
		super.enhance(step);

		if (partitionHandler != null) {
			step.setPartitionHandler(partitionHandler);
		}
		else {
			TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
			partitionHandler.setStep(this.step);
			if (taskExecutor == null) {
				taskExecutor = new SyncTaskExecutor();
			}
			partitionHandler.setGridSize(gridSize);
			partitionHandler.setTaskExecutor(taskExecutor);
			step.setPartitionHandler(partitionHandler);
		}

		if (splitter!=null) {
			step.setStepExecutionSplitter(splitter);
		} else {

			boolean allowStartIfComplete = isAllowStartIfComplete();
			String name = stepName;
			if (this.step != null) {
				try {
					allowStartIfComplete = this.step.isAllowStartIfComplete();
					name = this.step.getName();
				}
				catch (Exception e) {
					logger.info("Ignored exception from step asking for name and allowStartIfComplete flag. "
							+ "Using default from enclosing PartitionStep (" + name + "," + allowStartIfComplete + ").");
				}
			}
			SimpleStepExecutionSplitter splitter = new SimpleStepExecutionSplitter();
			splitter.setPartitioner(partitioner);
			splitter.setJobRepository(getJobRepository());
			splitter.setAllowStartIfComplete(allowStartIfComplete);
			splitter.setStepName(name);
			this.splitter = splitter;
			step.setStepExecutionSplitter(splitter);

		}

		if (aggregator!=null) {
			step.setStepExecutionAggregator(aggregator);
		}

		try {
			step.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}

		return step;

	}

	public PartitionStepBuilder splitter(StepExecutionSplitter splitter) {
		this.splitter = splitter;
		return this;
	}

	public PartitionStepBuilder aggregator(StepExecutionAggregator aggregator) {
		this.aggregator = aggregator;
		return this;
	}

}
