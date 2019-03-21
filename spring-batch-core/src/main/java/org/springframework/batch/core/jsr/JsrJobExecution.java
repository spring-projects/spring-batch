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
package org.springframework.batch.core.jsr;

import java.util.Date;
import java.util.Properties;

import javax.batch.runtime.BatchStatus;

import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.util.Assert;

/**
 * Wrapper class to adapt the {@link javax.batch.runtime.JobExecution} to
 * a {@link org.springframework.batch.core.JobExecution}.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrJobExecution implements javax.batch.runtime.JobExecution {

	private org.springframework.batch.core.JobExecution execution;
	private JobParametersConverter parametersConverter;

	/**
	 * @param execution for all information to be delegated from.
	 * @param parametersConverter instance of {@link JobParametersConverter}.
	 */
	public JsrJobExecution(org.springframework.batch.core.JobExecution execution, JobParametersConverter parametersConverter) {
		Assert.notNull(execution, "A JobExecution is required");
		this.execution = execution;

		this.parametersConverter = parametersConverter;
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JobExecution#getExecutionId()
	 */
	@Override
	public long getExecutionId() {
		return this.execution.getId();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JobExecution#getJobName()
	 */
	@Override
	public String getJobName() {
		return this.execution.getJobInstance().getJobName();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JobExecution#getBatchStatus()
	 */
	@Override
	public BatchStatus getBatchStatus() {
		return this.execution.getStatus().getBatchStatus();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JobExecution#getStartTime()
	 */
	@Override
	public Date getStartTime() {
		return this.execution.getStartTime();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JobExecution#getEndTime()
	 */
	@Override
	public Date getEndTime() {
		return this.execution.getEndTime();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JobExecution#getExitStatus()
	 */
	@Override
	public String getExitStatus() {
		return this.execution.getExitStatus().getExitCode();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JobExecution#getCreateTime()
	 */
	@Override
	public Date getCreateTime() {
		return this.execution.getCreateTime();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JobExecution#getLastUpdatedTime()
	 */
	@Override
	public Date getLastUpdatedTime() {
		return this.execution.getLastUpdated();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JobExecution#getJobParameters()
	 */
	@Override
	public Properties getJobParameters() {
		Properties properties = parametersConverter.getProperties(this.execution.getJobParameters());
		properties.remove(JsrJobParametersConverter.JOB_RUN_ID);
		return properties;
	}
}
