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

package org.springframework.batch.core.job.flow.support.state;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.batch.core.job.flow.State;

/**
 * {@link State} implementation for ending a job if it is in progress and
 * continuing if just starting.
 * 
 * @author Dave Syer
 * 
 */
public class EndState extends AbstractState {

	private final BatchStatus status;
	private final ExitStatus exitStatus;

	/**
	 * ExitStatus will be defaulted to the given BatchStatus
	 * 
	 * @param status The BatchStatus to end with
	 * @param name The name of the state
	 */
	public EndState(BatchStatus status, String name) {
		this(status, new ExitStatus(status.toString()), name);
	}

	/**
	 * @param status The BatchStatus to end with
	 * @param exitStatus The ExitStatus to end with
	 * @param name The name of the state
	 */
	public EndState(BatchStatus status, ExitStatus exitStatus, String name) {
		super(name);
		this.status = status;
		this.exitStatus = exitStatus;
	}

	/**
	 * Return the {@link BatchStatus} and {@link ExitStatus} stored. If the
	 * {@link BatchStatus} is {@link BatchStatus#INCOMPLETE}, then mark it on the
	 * {@link JobExecution} so that the job will know to stop.
	 * 
	 * @see State#handle(FlowExecutor)
	 */
	@Override
	public FlowExecutionStatus handle(FlowExecutor executor) throws Exception {
		JobExecution jobExecution = executor.getJobExecution();
		// If there are no step executions, then we are at the beginning of a
		// restart
		synchronized (jobExecution) {
			if (!jobExecution.getStepExecutions().isEmpty()) {
				if (status == BatchStatus.INCOMPLETE) {
					jobExecution.upgradeStatus(status);
					jobExecution.setExitStatus(exitStatus);
				}
			}
			return new FlowExecutionStatus(status, exitStatus);
		}
	}


	/* (non-Javadoc)
	 * @see org.springframework.batch.core.job.flow.State#validate(java.lang.String)
	 */
	public void validate(String pattern, String nextState) {
		if (status != BatchStatus.INCOMPLETE && nextState != null) {
			throw new IllegalStateException("The transition for " + getClass().getSimpleName() + " [" + getName()
					+ "] may not have a 'next' state.");
		}
		if (pattern != null) {
			throw new IllegalStateException("The transition for " + getClass().getSimpleName() + " [" + getName()
					+ "] may not have a 'pattern'.");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return super.toString() + " status=[" + status + "] exitcode=[" + exitStatus.getExitCode() + "] ";
	}
}