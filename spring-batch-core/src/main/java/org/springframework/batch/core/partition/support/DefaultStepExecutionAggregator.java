/*
 * Copyright 2006-2007 the original author or authors.
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
 * @since 2.1
 */
public class DefaultStepExecutionAggregator implements StepExecutionAggregator {

	/**
	 * Aggregates the input executions into the result {@link StepExecution}.
	 * The aggregated fields are
	 * <ul>
	 * <li>status - choosing the highest value using
	 * {@link BatchStatus#max(BatchStatus, BatchStatus)}</li>
	 * <li>exitStatus - using {@link ExitStatus#and(ExitStatus)}</li>
	 * <li>commitCount, rollbackCount, etc. - by arithmetic sum</li>
	 * </ul>
	 * @see StepExecutionAggregator #aggregate(StepExecution, Collection)
	 */
	public void aggregate(StepExecution result, Collection<StepExecution> executions) {
		Assert.notNull(result, "To aggregate into a result it must be non-null.");
		if (executions == null) {
			return;
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
