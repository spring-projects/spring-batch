/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.partition.support;

import java.util.Collection;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.util.Assert;

/**
 * Aggregates {@link StepExecution}s based on the rules outlined in JSR-352.  Specifically
 * it aggregates all counts and determines the correct BatchStatus.  However, the ExitStatus
 * for each child StepExecution is ignored.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrStepExecutionAggregator implements StepExecutionAggregator {

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.partition.support.StepExecutionAggregator#aggregate(org.springframework.batch.core.StepExecution, java.util.Collection)
	 */
	@Override
	public void aggregate(StepExecution result,
			Collection<StepExecution> executions) {
		Assert.notNull(result, "To aggregate into a result it must be non-null.");
		if (executions == null) {
			return;
		}
		for (StepExecution stepExecution : executions) {
			BatchStatus status = stepExecution.getStatus();
			result.setStatus(BatchStatus.max(result.getStatus(), status));
			result.setCommitCount(result.getCommitCount() + stepExecution.getCommitCount());
			result.setRollbackCount(result.getRollbackCount() + stepExecution.getRollbackCount());
			result.setReadCount(result.getReadCount() + stepExecution.getReadCount());
			result.setReadSkipCount(result.getReadSkipCount() + stepExecution.getReadSkipCount());
			result.setWriteCount(result.getWriteCount() + stepExecution.getWriteCount());
			result.setWriteSkipCount(result.getWriteSkipCount() + stepExecution.getWriteSkipCount());
		}
	}
}
