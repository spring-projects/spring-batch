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

import java.util.Date;
import java.util.Properties;

import javax.batch.runtime.BatchStatus;

import org.springframework.util.Assert;

/**
 * Wrapper class to adapt the {@link javax.batch.runtime.JobExecution} to
 * a {@link JobExecution}.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JobExecution implements javax.batch.runtime.JobExecution {

	private org.springframework.batch.core.JobExecution execution;

	/**
	 * @param execution for all information to be delegated from
	 */
	public JobExecution(org.springframework.batch.core.JobExecution execution) {
		Assert.notNull(execution, "A JobExecution is required");
		this.execution = execution;
	}

	@Override
	public long getExecutionId() {
		return this.execution.getId();
	}

	@Override
	public String getJobName() {
		return this.execution.getJobInstance().getJobName();
	}

	@Override
	public BatchStatus getBatchStatus() {
		return this.execution.getStatus().getBatchStatus();
	}

	@Override
	public Date getStartTime() {
		return this.execution.getStartTime();
	}

	@Override
	public Date getEndTime() {
		return this.execution.getEndTime();
	}

	@Override
	public String getExitStatus() {
		return this.execution.getExitStatus().getExitCode();
	}

	@Override
	public Date getCreateTime() {
		return this.execution.getCreateTime();
	}

	@Override
	public Date getLastUpdatedTime() {
		return this.execution.getLastUpdated();
	}

	@Override
	public Properties getJobParameters() {
		return this.execution.getJobParameters().toProperties();
	}
}
