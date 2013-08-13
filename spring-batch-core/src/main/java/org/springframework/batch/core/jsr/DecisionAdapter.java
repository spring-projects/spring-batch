/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr;

import javax.batch.api.Decider;
import javax.batch.operations.BatchRuntimeException;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.util.Assert;

/**
 * Wrapper for {@link Decider} implementation to allow it to be used
 * by the rest of the framework.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class DecisionAdapter implements JobExecutionDecider {

	private Decider decider;

	/**
	 * @param decider a {@link Decider}
	 */
	public DecisionAdapter(Decider decider) {
		Assert.notNull(decider, "A Decider implementation is required");

		this.decider = decider;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.job.flow.JobExecutionDecider#decide(org.springframework.batch.core.JobExecution, org.springframework.batch.core.StepExecution)
	 */
	@Override
	public FlowExecutionStatus decide(JobExecution jobExecution,
			StepExecution stepExecution) {

		javax.batch.runtime.StepExecution [] executions = null;

		//TODO: Address splits
		if(stepExecution != null) {
			executions = new org.springframework.batch.core.jsr.StepExecution[1];
			executions[0] = new org.springframework.batch.core.jsr.StepExecution(stepExecution);
		}

		try {
			return new FlowExecutionStatus(decider.decide(executions));
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}
}
