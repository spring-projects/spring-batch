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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecution;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.batch.core.job.flow.support.State;

/**
 * {@link State} implementation for ending a job if it is in progress and
 * continuing if just starting.
 * 
 * @author Dave Syer
 * 
 */
public class EndState extends AbstractState {

	private final BatchStatus status;

	/**
	 * @param name
	 */
	public EndState(BatchStatus status, String name) {
		super(name);
		this.status = status;
	}

	/**
	 * Set the status as long the {@link JobExecution} is in progress. If this
	 * is the first place we came after a restart we do nothing (otherwise the
	 * same outcome that ended the job on the last run will occur).
	 * 
	 * @see State#handle(FlowExecutor)
	 */
	@Override
	public String handle(FlowExecutor executor) throws Exception {
		JobExecution jobExecution = executor.getJobExecution();
		// If there are no step executions, then we are at the beginning of a
		// restart
		synchronized (jobExecution) {
			if (!jobExecution.getStepExecutions().isEmpty()) {
				jobExecution.upgradeStatus(status);
			}
			return FlowExecution.COMPLETED;
		}
	}

}