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

import java.util.Properties;

import javax.batch.runtime.BatchStatus;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.util.Assert;

/**
 * Wrapper class to provide the {@link javax.batch.runtime.context.JobContext} functionality
 * as specified in JSR-352.  Wrapper delegates to the underlying {@link JobExecution} to
 * obtain the related contextual information.
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 */
public class JobContext implements javax.batch.runtime.context.JobContext {
	private Object transientUserData;
	private Properties properties;
	private JobExecution jobExecution;

	/**
	 * @param jobExecution for the related job
	 */
	public JobContext(JobExecution jobExecution, Properties properties) {
		Assert.notNull(jobExecution, "A JobExecution is required");

		this.jobExecution = jobExecution;
		this.properties = properties != null ? properties : new Properties();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.JobContext#getJobName()
	 */
	@Override
	public String getJobName() {
		return jobExecution.getJobInstance().getJobName();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.JobContext#getTransientUserData()
	 */
	@Override
	public Object getTransientUserData() {
		return transientUserData;
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.JobContext#setTransientUserData(java.lang.Object)
	 */
	@Override
	public void setTransientUserData(Object data) {
		transientUserData = data;
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.JobContext#getInstanceId()
	 */
	@Override
	public long getInstanceId() {
		return jobExecution.getJobInstance().getId();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.JobContext#getExecutionId()
	 */
	@Override
	public long getExecutionId() {
		return jobExecution.getId();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.JobContext#getProperties()
	 */
	@Override
	public Properties getProperties() {
		return properties;
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.JobContext#getBatchStatus()
	 */
	@Override
	public BatchStatus getBatchStatus() {
		return jobExecution.getStatus().getBatchStatus();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.JobContext#getExitStatus()
	 */
	@Override
	public String getExitStatus() {
		return jobExecution.getExitStatus().getExitCode();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.JobContext#setExitStatus(java.lang.String)
	 */
	@Override
	public void setExitStatus(String status) {
		jobExecution.setExitStatus(new ExitStatus(status));
	}
}
