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
 * Step builder for {@link PartitionStep} instances. A partition step executes the same step (possibly remotely)
 * multiple times with different input parameters (in the form of execution context). Useful for parallelization.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Dimitrios Liapis
 *
 * @since 2.2
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

	/**
	 * Create a new builder initialized with any properties in the parent. The parent is copied, so it can be re-used.
	 *
	 * @param parent a parent helper containing common step properties
	 */
	public PartitionStepBuilder(StepBuilderHelper<?> parent) {
		super(parent);
	}

	/**
	 * Add a partitioner which can be used to create a {@link StepExecutionSplitter}. Use either this or an explicit
	 * {@link #splitter(StepExecutionSplitter)} but not both.
	 *
	 * @param slaveStepName the name of the slave step (used to construct step execution names)
	 * @param partitioner a partitioner to use
	 * @return this for fluent chaining
	 */
	public PartitionStepBuilder partitioner(String slaveStepName, Partitioner partitioner) {
		this.stepName = slaveStepName;
		this.partitioner = partitioner;
		return this;
	}

	/**
	 * Provide an actual step instance to execute in parallel. If an explicit
	 * {@link #partitionHandler(PartitionHandler)} is provided, the step is optional and is only used to extract
	 * configuration data (name and other basic properties of a step).
	 *
	 * @param step a step to execute in parallel
	 * @return this for fluent chaining
	 */
	public PartitionStepBuilder step(Step step) {
		this.step = step;
		return this;
	}

	/**
	 * Provide a task executor to use when constructing a {@link PartitionHandler} from the {@link #step(Step)}. Mainly
	 * used for running a step locally in parallel, but can be used to execute remotely if the step is remote. Not used
	 * if an explicit {@link #partitionHandler(PartitionHandler)} is provided.
	 *
	 * @param taskExecutor a task executor to use when executing steps in parallel
	 * @return this for fluent chaining
	 */
	public PartitionStepBuilder taskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		return this;
	}

	/**
	 * Provide an explicit partition handler that will carry out the work of the partition step. The partition handler
	 * is the main SPI for adapting a partition step to a specific distributed computation environment. Optional if you
	 * only need local or remote processing through the Step interface.
	 *
	 * @see #step(Step) for setting up a default handler that works with a local or remote Step
	 *
	 * @param partitionHandler a partition handler
	 * @return this for fluent chaining
	 */
	public PartitionStepBuilder partitionHandler(PartitionHandler partitionHandler) {
		this.partitionHandler = partitionHandler;
		return this;
	}

	/**
	 * A hint to the {@link #splitter(StepExecutionSplitter)} about how many step executions are required. If running
	 * locally or remotely through a {@link #taskExecutor(TaskExecutor)} determines precisely the number of step
	 * executions in the first attempt at a partition step execution.
	 *
	 * @param gridSize the grid size
	 * @return this for fluent chaining
	 */
	public PartitionStepBuilder gridSize(int gridSize) {
		this.gridSize = gridSize;
		return this;
	}

	/**
	 * Provide an explicit {@link StepExecutionSplitter} instead of having one build from the
	 * {@link #partitioner(String, Partitioner)}. Useful if you need more control over the splitting.
	 *
	 * @param splitter a step execution splitter
	 * @return this for fluent chaining
	 */
	public PartitionStepBuilder splitter(StepExecutionSplitter splitter) {
		this.splitter = splitter;
		return this;
	}

	/**
	 * Provide a step execution aggregator for aggregating partitioned step executions into a single result for the
	 * {@link PartitionStep} itself.  Default is a simple implementation that works in most cases.
	 *
	 * @param aggregator a step execution aggregator
	 * @return this for fluent chaining
	 */
	public PartitionStepBuilder aggregator(StepExecutionAggregator aggregator) {
		this.aggregator = aggregator;
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

		if (splitter != null) {
			step.setStepExecutionSplitter(splitter);
		}
		else {

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

		if (aggregator != null) {
			step.setStepExecutionAggregator(aggregator);
		}

		try {
			step.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new StepBuilderException(e);
		}

		return step;

	}

	protected TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	protected Partitioner getPartitioner() {
		return partitioner;
	}

	protected Step getStep() {
		return step;
	}

	protected PartitionHandler getPartitionHandler() {
		return partitionHandler;
	}

	protected int getGridSize() {
		return gridSize;
	}

	protected StepExecutionSplitter getSplitter() {
		return splitter;
	}

	protected StepExecutionAggregator getAggregator() {
		return aggregator;
	}

	protected String getStepName() {
		return stepName;
	}
}
