/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.core.partition;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.observability.jfr.events.step.partition.PartitionAggregateEvent;
import org.springframework.batch.core.observability.jfr.events.step.partition.PartitionSplitEvent;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.partition.support.DefaultStepExecutionAggregator;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.util.Assert;

import java.util.Collection;

import org.jspecify.annotations.NullUnmarked;

/**
 * Implementation of {@link Step} which partitions the execution and spreads the load
 * using a {@link PartitionHandler}.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
@NullUnmarked // FIXME to remove once default constructors (required by the batch XML
				// namespace) are removed
public class PartitionStep extends AbstractStep {

	private StepExecutionSplitter stepExecutionSplitter;

	private PartitionHandler partitionHandler;

	private StepExecutionAggregator stepExecutionAggregator = new DefaultStepExecutionAggregator();

	/**
	 * Create a new instance of a {@link PartitionStep} with the given job repository.
	 * @param jobRepository the job repository to use. Must not be null.
	 * @since 6.0
	 */
	public PartitionStep(JobRepository jobRepository) {
		super(jobRepository);
	}

	/**
	 * A {@link PartitionHandler} which can send out step executions for remote processing
	 * and bring back the results.
	 * @param partitionHandler the {@link PartitionHandler} to set
	 */
	public void setPartitionHandler(PartitionHandler partitionHandler) {
		this.partitionHandler = partitionHandler;
	}

	/**
	 * A {@link StepExecutionAggregator} that can aggregate step executions when they come
	 * back from the handler. Defaults to a {@link DefaultStepExecutionAggregator}.
	 * @param stepExecutionAggregator the {@link StepExecutionAggregator} to set
	 */
	public void setStepExecutionAggregator(StepExecutionAggregator stepExecutionAggregator) {
		this.stepExecutionAggregator = stepExecutionAggregator;
	}

	/**
	 * Public setter for mandatory property {@link StepExecutionSplitter}.
	 * @param stepExecutionSplitter the {@link StepExecutionSplitter} to set
	 */
	public void setStepExecutionSplitter(StepExecutionSplitter stepExecutionSplitter) {
		this.stepExecutionSplitter = stepExecutionSplitter;
	}

	/**
	 * Assert that mandatory properties are set (stepExecutionSplitter, partitionHandler)
	 * and delegate top superclass.
	 *
	 * @see AbstractStep#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(stepExecutionSplitter != null, "StepExecutionSplitter must be provided");
		Assert.state(partitionHandler != null, "PartitionHandler must be provided");
		super.afterPropertiesSet();
	}

	/**
	 * Delegate execution to the {@link PartitionHandler} provided. The
	 * {@link StepExecution} passed in here becomes the parent or manager execution for
	 * the partition, summarising the status on exit of the logical grouping of work
	 * carried out by the {@link PartitionHandler}. The individual step executions and
	 * their input parameters (through {@link ExecutionContext}) for the partition
	 * elements are provided by the {@link StepExecutionSplitter}.
	 * @param stepExecution the manager step execution for the partition
	 *
	 * @see Step#execute(StepExecution)
	 */
	@Override
	protected void doExecute(StepExecution stepExecution) throws Exception {
		stepExecution.getExecutionContext().put(STEP_TYPE_KEY, this.getClass().getName());

		// Split execution into partitions and wait for task completion
		PartitionSplitEvent partitionSplitEvent = new PartitionSplitEvent(stepExecution.getStepName(),
				stepExecution.getId());
		partitionSplitEvent.begin();
		Collection<StepExecution> executions = partitionHandler.handle(stepExecutionSplitter, stepExecution);
		partitionSplitEvent.partitionCount = executions.size();
		stepExecution.upgradeStatus(BatchStatus.COMPLETED);
		partitionSplitEvent.commit();

		// aggregate the results of the executions
		PartitionAggregateEvent partitionAggregateEvent = new PartitionAggregateEvent(stepExecution.getStepName(),
				stepExecution.getId());
		partitionAggregateEvent.begin();
		stepExecutionAggregator.aggregate(stepExecution, executions);
		partitionAggregateEvent.commit();

		// If anything failed or had a problem we need to crap out
		if (stepExecution.getStatus().isUnsuccessful()) {
			throw new JobExecutionException("Partition handler returned an unsuccessful step");
		}
	}

	protected StepExecutionSplitter getStepExecutionSplitter() {
		return stepExecutionSplitter;
	}

	protected PartitionHandler getPartitionHandler() {
		return partitionHandler;
	}

}
