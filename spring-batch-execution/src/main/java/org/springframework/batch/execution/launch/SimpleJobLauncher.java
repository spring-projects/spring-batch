/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.execution.launch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobSupport;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * Simple implementation of the {@link JobLauncher} interface. The Spring Core
 * {@link TaskExecutor} interface is used to launch a {@link JobSupport}. This
 * means that the type of executor set is very important. If a
 * {@link SyncTaskExecutor} is used, then the job will be processed
 * <strong>within the same thread that called the launcher.</strong> Care
 * should be taken to ensure any users of this class understand fully whether or
 * not the implementation of TaskExecutor used will start tasks synchronously or
 * asynchronously. The default setting uses a synchronous task executor.
 * 
 * There is only one required dependency of this Launcher, a
 * {@link JobRepository}. The JobRepository is used to obtain a valid
 * JobExecution. The Repository must be used because the provided
 * {@link JobSupport} could be a restart of an existing {@link JobInstance},
 * and only the Repository can reliably recreate it.
 * 
 * @author Lucas Ward
 * @since 1.0
 * @see JobRepository
 * @see TaskExecutor
 */
public class SimpleJobLauncher implements JobLauncher, InitializingBean {

	protected static final Log logger = LogFactory.getLog(SimpleJobLauncher.class);

	private JobRepository jobRepository;

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	/**
	 * Run the provided job with the given {@link JobParameters}. The
	 * {@link JobParameters} will be used to determine if this is an execution
	 * of an existing job instance, or if a new one should be created.
	 * 
	 * @param job, the job to be run.
	 * @param jobParameters, the {@link JobParameters} for this particular
	 * execution.
	 * @return JobExecutionAlreadyRunningException if the JobInstance already
	 * exists and has an execution already running.
	 */
	public JobExecution run(final Job job, final JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException {

		Assert.notNull(job, "The Job must not be null.");
		Assert.notNull(jobParameters, "The JobParameters must not be null.");

		final JobExecution jobExecution = jobRepository.createJobExecution(job, jobParameters);

		taskExecutor.execute(new Runnable() {

			public void run() {
				try {
					logger.info("Job: [" + job + "] launched with the following parameters: [" + jobParameters + "]");
					ExitStatus exitStatus = job.run(jobExecution);
					// The exit status should be set by the Job. TODO: remove
					// this line...
					jobExecution.setExitStatus(exitStatus);
					logger.info("Job: [" + job + "] completed successfully with the following parameters: ["
							+ jobParameters + "]");
				}
				catch (Throwable t) {
					logger.info("Job: [" + job + "] failed with the following parameters: [" + jobParameters + "]", t);
					rethrow(t);
				}
			}

			private void rethrow(Throwable t) {
				if (t instanceof RuntimeException) {
					throw (RuntimeException) t;
				}
				throw new RuntimeException(t);
			}
		});

		return jobExecution;
	}

	/**
	 * Set the JobRepsitory.
	 * 
	 * @param jobRepository
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Set the TaskExecutor. (Optional)
	 * 
	 * @param taskExecutor
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Ensure the required dependencies of a {@link JobRepository} have been
	 * set.
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.state(jobRepository != null, "A JobRepository has not been set.");
		logger.info("No TaskExecutor has been set, defaulting to synchronous executor.");
	}

}
