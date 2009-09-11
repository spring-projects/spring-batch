package org.springframework.batch.core.partition.support;

import java.util.Collection;

import org.springframework.batch.core.StepExecution;

/**
 * Strategy for a aggregating step executions, usually when they are the result
 * of partitioned or remote execution.
 * 
 * @author Dave Syer
 * 
 * @since 2.1
 * 
 */
public interface StepExecutionAggregator {

	/**
	 * Take the inputs and aggregate, putting the aggregates into the result.
	 * 
	 * @param result the result to overwrite
	 * @param executions the inputs
	 */
	void aggregate(StepExecution result, Collection<StepExecution> executions);

}