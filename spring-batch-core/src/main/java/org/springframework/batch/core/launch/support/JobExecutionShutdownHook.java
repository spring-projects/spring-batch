/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.core.launch.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.JobOperator;

/**
 * A shutdown hook that attempts to gracefully stop a running job execution when the JVM
 * is exiting.
 *
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public class JobExecutionShutdownHook extends Thread {

	protected Log logger = LogFactory.getLog(JobExecutionShutdownHook.class);

	private final JobExecution jobExecution;

	private final JobOperator jobOperator;

	/**
	 * Create a new {@link JobExecutionShutdownHook}.
	 * @param jobExecution the job execution to stop
	 * @param jobOperator the job operator to use to stop the job execution
	 */
	public JobExecutionShutdownHook(JobExecution jobExecution, JobOperator jobOperator) {
		this.jobExecution = jobExecution;
		this.jobOperator = jobOperator;
	}

	public void run() {
		this.logger.info("Received JVM shutdown signal");
		long jobExecutionId = this.jobExecution.getId();
		try {
			this.logger.info("Attempting to gracefully stop job execution " + jobExecutionId);
			this.jobOperator.stop(this.jobExecution);
			this.logger.info("Successfully stopped job execution " + jobExecutionId);
		}
		catch (Exception e) {
			throw new RuntimeException("Unable to gracefully stop job execution " + jobExecutionId, e);
		}
	}

}