/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.core.jsr.job.flow.support.state;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.batch.core.job.flow.State;

/**
 * {@link State} implementation for ending a job per JSR-352 rules if it is
 * in progress and continuing if just starting.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class EndState extends org.springframework.batch.core.job.flow.support.state.EndState {

	/**
	 * @param status The {@link FlowExecutionStatus} to end with
	 * @param name The name of the state
	 */
	public EndState(FlowExecutionStatus status, String name) {
		super(status, status.getName(), name);
	}

	/**
	 * @param status The {@link FlowExecutionStatus} to end with
	 * @param name The name of the state
	 */
	public EndState(FlowExecutionStatus status, String code, String name) {
		super(status, code, name, false);
	}

	/**
	 * @param status The {@link FlowExecutionStatus} to end with
	 * @param name The name of the state
	 * @param abandon flag to indicate that previous step execution can be
	 * marked as abandoned (if there is one)
	 *
	 */
	public EndState(FlowExecutionStatus status, String code, String name, boolean abandon) {
		super(status, code, name, abandon);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.job.flow.support.state.EndState#setExitStatus(org.springframework.batch.core.job.flow.FlowExecutor, java.lang.String)
	 */
	@Override
	protected void setExitStatus(FlowExecutor executor, String code) {
		StepExecution stepExecution = executor.getStepExecution();

		ExitStatus status = new ExitStatus(code);
		if(!ExitStatus.isNonDefaultExitStatus(status)) {
			stepExecution.getJobExecution().setExitStatus(status);
		}
	}
}
