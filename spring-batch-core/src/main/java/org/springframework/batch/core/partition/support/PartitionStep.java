package org.springframework.batch.core.partition.support;

import java.util.Collection;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.Assert;

/**
 * Implementation of {@link Step} which partitions the execution and spreads the
 * load using a {@link PartitionHandler}.
 * 
 * @author Dave Syer
 * 
 */
public class PartitionStep extends AbstractStep {

	private StepExecutionSplitter stepExecutionSplitter;

	private PartitionHandler partitionHandler;

	private StepExecutionAggregator aggregator = new StepExecutionAggregator();

	/**
	 * Public setter for mandatory property {@link PartitionHandler}.
	 * @param partitionHandler the {@link PartitionHandler} to set
	 */
	public void setPartitionHandler(PartitionHandler partitionHandler) {
		this.partitionHandler = partitionHandler;
	}

	/**
	 * Public setter for mandatory property {@link StepExecutionSplitter}.
	 * @param stepExecutionSplitter the {@link StepExecutionSplitter} to set
	 */
	public void setStepExecutionSplitter(StepExecutionSplitter stepExecutionSplitter) {
		this.stepExecutionSplitter = stepExecutionSplitter;
	}

	/**
	 * Assert that mandatory properties are set (stepExecutionSplitter,
	 * partitionHandler) and delegate top superclass.
	 * 
	 * @see AbstractStep#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(stepExecutionSplitter, "StepExecutionSplitter must be provided");
		Assert.notNull(partitionHandler, "PartitionHandler must be provided");
		super.afterPropertiesSet();
	}

	/**
	 * Delegate execution to the {@link PartitionHandler} provided. The
	 * {@link StepExecution} passed in here becomes the parent or master
	 * execution for the partition, summarising the status on exit of the
	 * logical grouping of work carried out by the {@link PartitionHandler}. The
	 * individual step executions and their input parameters (through
	 * {@link ExecutionContext}) for the partition elements are provided by the
	 * {@link StepExecutionSplitter}.
	 * 
	 * @param stepExecution the master step execution for the partition
	 * 
	 * @see Step#execute(StepExecution)
	 */
	@Override
	protected void doExecute(StepExecution stepExecution) throws Exception {

		// Wait for task completion and then aggregate the results
		Collection<StepExecution> executions = partitionHandler.handle(stepExecutionSplitter, stepExecution);
		stepExecution.upgradeStatus(BatchStatus.COMPLETED);
		aggregator.aggregate(stepExecution, executions);

		// If anything failed or had a problem we need to crap out
		if (stepExecution.getStatus().isUnsuccessful()) {
			throw new JobExecutionException("Partition handler returned an unsuccessful step");
		}

		if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
			stepExecution.pauseAndWait();
		}

	}

}
