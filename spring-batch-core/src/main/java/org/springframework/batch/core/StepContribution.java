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
package org.springframework.batch.core;

import org.springframework.batch.item.ExecutionContext;

/**
 * Represents a contribution to a {@link StepExecution}, buffering changes
 * until they can be applied at a chunk boundary.
 * 
 * @author Dave Syer
 * 
 */
public class StepContribution {

	private int taskCount = 0;

	private StepExecution execution;

	private ExecutionContext executionContext;

	private int commitCount;

	private int skipCount;

	/**
	 * @param execution
	 */
	public StepContribution(StepExecution execution) {
		this.execution = execution;
	}

	/**
	 * Increment the counter for the number of tasks executed.
	 */
	public void incrementTaskCount() {
		taskCount++;
	}

	/**
	 * Public access to the task execution counter.
	 * 
	 * @return the task execution counter.
	 */
	public int getTaskCount() {
		return taskCount;
	}

	/**
	 * Increment the commit counter.
	 */
	public void incrementCommitCount() {
		commitCount++;
	}

	/**
	 * Set the statistics properties.
	 * 
	 * @param executionContext
	 */
	public void setExecutionContext(ExecutionContext executionContext) {
		this.executionContext = executionContext;
	}

	/**
	 * Public getter for the {@link ExecutionContext}.
	 * @return the stream context
	 */
	public ExecutionContext getExecutionContext() {
		return executionContext;
	}

	/**
	 * Public getter for the commit counter.
	 * @return the commitCount
	 */
	public int getCommitCount() {
		return commitCount;
	}

	/**
	 * Delegate call to the {@link StepExecution}.
	 * @return the flag from the underlying execution
	 */
	public boolean isTerminateOnly() {
		return execution.isTerminateOnly();
	}

	/**
	 * @return the sum of skips accumulated in the parent {@link StepExecution}
	 * and this <code>StepContribution</code>.
	 */
	public int getStepSkipCount() {
		return skipCount + execution.getSkipCount();
	}

	/**
	 * @return the number of skips collected in this
	 * <code>StepContribution</code> (not including skips accumulated in the
	 * parent {@link StepExecution}.
	 */
	public int getContributionSkipCount() {
		return skipCount;
	}

	public void incrementSkipCount() {
		skipCount++;
	}

}
