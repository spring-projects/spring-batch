package org.springframework.batch.core.partition.support;

import java.util.Collection;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;

public interface StepExecutionAggregator {

	/**
	 * Take the inputs and aggregate certain fields, putting the aggregates into
	 * the result.  The aggregated fields are
	 * <ul>
	 * <li>status - choosing the highest value using {@link BatchStatus#max(BatchStatus, BatchStatus)}</li>
	 * <li>exitStatus - using {@link ExitStatus#and(ExitStatus)}</li>
	 * <li>commitCount, rollbackCount, etc. - by arithmetic sum</li>
	 * </ul>
	 * 
	 * @param result the result to overwrite
	 * @param executions the inputs
	 */
	void aggregate(StepExecution result, Collection<StepExecution> executions);

}