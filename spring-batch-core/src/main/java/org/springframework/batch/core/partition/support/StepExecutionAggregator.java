package org.springframework.batch.core.partition.support;

import java.util.Collection;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.util.Assert;

/**
 * Convenience class for aggregating a set of {@link StepExecution} instances
 * into a single result.
 * 
 * @author Dave Syer
 * 
 */
public class StepExecutionAggregator {

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
	public void aggregate(StepExecution result, Collection<StepExecution> executions) {
		Assert.notNull(result, "To aggregate into a result it must be non-null.");
		if (executions == null || executions.isEmpty()) {
			throw new IllegalArgumentException("Cannot aggregate empty or null executions: " + executions);
		}
		for (StepExecution stepExecution : executions) {
			BatchStatus status = stepExecution.getStatus();
			result.setStatus(BatchStatus.max(result.getStatus(), status));
			result.setExitStatus(result.getExitStatus().and(stepExecution.getExitStatus()));
			result.setCommitCount(result.getCommitCount() + stepExecution.getCommitCount());
			result.setRollbackCount(result.getRollbackCount() + stepExecution.getRollbackCount());
			result.setReadCount(result.getReadCount() + stepExecution.getReadCount());
			result.setReadSkipCount(result.getReadSkipCount() + stepExecution.getReadSkipCount());
			result.setWriteCount(result.getWriteCount() + stepExecution.getWriteCount());
			result.setWriteSkipCount(result.getWriteSkipCount() + stepExecution.getWriteSkipCount());
		}
	}

}
