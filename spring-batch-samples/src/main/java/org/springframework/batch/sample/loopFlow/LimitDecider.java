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
package org.springframework.batch.sample.loopFlow;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

/**
 * This decider will return "CONTINUE" until the limit it reached, at which
 * point it will return "COMPLETE".
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public class LimitDecider implements JobExecutionDecider {

	private int count = 0;
	private int limit = 1;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.core.job.flow.support.state.JobExecutionDecider#decide(org.springframework.batch.core.JobExecution,
	 *      org.springframework.batch.core.StepExecution)
	 */
	public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
		if (++count >= limit) {
			return new FlowExecutionStatus("COMPLETE");
		}
		else {
			return new FlowExecutionStatus("CONTINUE");
		}
	}

	/**
	 * @param limit
	 */
	public void setLimit(int limit) {
		this.limit = limit;
	}
}
