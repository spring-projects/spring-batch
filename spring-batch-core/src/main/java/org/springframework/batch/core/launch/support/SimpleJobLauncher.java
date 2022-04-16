/*
 * Copyright 2006-2021 the original author or authors.
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

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.observability.BatchMetrics;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.util.Assert;

/**
 * Simple implementation of the {@link JobLauncher} interface. The Spring Core
 * {@link TaskExecutor} interface is used to launch a {@link Job}. This means
 * that the type of executor set is very important. If a
 * {@link SyncTaskExecutor} is used, then the job will be processed
 * <strong>within the same thread that called the launcher.</strong> Care should
 * be taken to ensure any users of this class understand fully whether or not
 * the implementation of TaskExecutor used will start tasks synchronously or
 * asynchronously. The default setting uses a synchronous task executor.
 *
 * There is only one required dependency of this Launcher, a
 * {@link JobRepository}. The JobRepository is used to obtain a valid
 * JobExecution. The Repository must be used because the provided {@link Job}
 * could be a restart of an existing {@link JobInstance}, and only the
 * Repository can reliably recreate it.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Will Schipp
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 * @since 1.0
 *
 * @see JobRepository
 * @see TaskExecutor
 */
public class SimpleJobLauncher implements JobLauncher, InitializingBean {

	protected static final Log logger = LogFactory.getLog(SimpleJobLauncher.class);

	private JobRepository jobRepository;

	private TaskExecutor taskExecutor;

	/**
	 * Run the provided job with the given {@link JobParameters}. The
	 * {@link JobParameters} will be used to determine if this is an execution
	 * of an existing job instance, or if a new one should be created.
	 *
	 * @param job the job to be run.
	 * @param jobParameters the {@link JobParameters} for this particular
	 * execution.
	 * @return the {@link JobExecution} if it returns synchronously. If the
	 * implementation is asynchronous, the status might well be unknown.
	 * @throws JobExecutionAlreadyRunningException if the JobInstance already
	 * exists and has an execution already running.
	 * @throws JobRestartException if the execution would be a re-start, but a
	 * re-start is either not allowed or not needed.
	 * @throws JobInstanceAlreadyCompleteException if this instance has already
	 * completed successfully
	 * @throws JobParametersInvalidException thrown if jobParameters is invalid.
	 */
	@Override
	public JobExecution run(final Job job, final JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException,
			JobParametersInvalidException {

		Assert.notNull(job, "The Job must not be null.");
		Assert.notNull(jobParameters, "The JobParameters must not be null.");

		final JobExecution jobExecution;
		JobExecution lastExecution = jobRepository.getLastJobExecution(job.getName(), jobParameters);
		if (lastExecution != null) {
			if (!job.isRestartable()) {
				throw new JobRestartException("JobInstance already exists and is not restartable");
			}
			/*
			 * validate here if it has stepExecutions that are UNKNOWN, STARTING, STARTED and STOPPING
			 * retrieve the previous execution and check
			 */
			for (StepExecution execution : lastExecution.getStepExecutions()) {
				BatchStatus status = execution.getStatus();
				if (status.isRunning() || status == BatchStatus.STOPPING) {
					throw new JobExecutionAlreadyRunningException("A job execution for this job is already running: "
							+ lastExecution);
				} else if (status == BatchStatus.UNKNOWN) {
					throw new JobRestartException(
							"Cannot restart step [" + execution.getStepName() + "] from UNKNOWN status. "
								+ "The last execution ended with a failure that could not be rolled back, "
								+ "so it may be dangerous to proceed. Manual intervention is probably necessary.");
				}
			}
		}

		// Check the validity of the parameters before doing creating anything
		// in the repository...
		job.getJobParametersValidator().validate(jobParameters);

		/*
		 * There is a very small probability that a non-restartable job can be
		 * restarted, but only if another process or thread manages to launch
		 * <i>and</i> fail a job execution for this instance between the last
		 * assertion and the next method returning successfully.
		 */
		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);

		try {
			taskExecutor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						if (logger.isInfoEnabled()) {
							logger.info("Job: [" + job + "] launched with the following parameters: [" + jobParameters
									+ "]");
						}
						job.execute(jobExecution);
						if (logger.isInfoEnabled()) {
							Duration jobExecutionDuration = BatchMetrics.calculateDuration(jobExecution.getStartTime(), jobExecution.getEndTime());
							logger.info("Job: [" + job + "] completed with the following parameters: [" + jobParameters
									+ "] and the following status: [" + jobExecution.getStatus() + "]"
									+ (jobExecutionDuration == null ? "" : " in " + BatchMetrics.formatDuration(jobExecutionDuration)));
						}
					}
					catch (Throwable t) {
						if (logger.isInfoEnabled()) {
							logger.info("Job: [" + job
									+ "] failed unexpectedly and fatally with the following parameters: [" + jobParameters
									+ "]", t);
						}
						rethrow(t);
					}
				}

				private void rethrow(Throwable t) {
					if (t instanceof RuntimeException) {
						throw (RuntimeException) t;
					}
					else if (t instanceof Error) {
						throw (Error) t;
					}
					throw new IllegalStateException(t);
				}
			});
		}
		catch (TaskRejectedException e) {
			jobExecution.upgradeStatus(BatchStatus.FAILED);
			if (jobExecution.getExitStatus().equals(ExitStatus.UNKNOWN)) {
				jobExecution.setExitStatus(ExitStatus.FAILED.addExitDescription(e));
			}
			jobRepository.update(jobExecution);
		}

		return jobExecution;
	}

	/**
	 * Set the JobRepository.
	 *
	 * @param jobRepository instance of {@link JobRepository}.
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Set the TaskExecutor. (Optional)
	 *
	 * @param taskExecutor instance of {@link TaskExecutor}.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Ensure the required dependencies of a {@link JobRepository} have been
	 * set.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(jobRepository != null, "A JobRepository has not been set.");
		if (taskExecutor == null) {
			logger.info("No TaskExecutor has been set, defaulting to synchronous executor.");
			taskExecutor = new SyncTaskExecutor();
		}
	}
}
