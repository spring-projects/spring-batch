package org.springframework.batch.core.partition.support;

import java.util.Collection;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.util.Assert;

public class StepExecutionAggregator {

	public void aggregate(StepExecution result, Collection<StepExecution> executions) {
		Assert.notNull(result, "To aggregate into a result it must be non-null.");
		if (executions == null || executions.isEmpty()) {
			throw new IllegalArgumentException("Cannot aggregate empty or null executions: " + executions);
		}
		// Start with assumption that it is complete...
		result.setStatus(BatchStatus.COMPLETED);
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
