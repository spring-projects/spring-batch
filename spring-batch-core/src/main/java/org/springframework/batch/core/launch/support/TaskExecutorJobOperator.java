/*
 * Copyright 2022-2025 the original author or authors.
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

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.UnexpectedJobExecutionException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.core.task.TaskExecutor}-based implementation of the
 * {@link JobOperator} interface. The following dependencies are required:
 *
 * <ul>
 * <li>{@link JobRepository}
 * <li>{@link JobRegistry}
 * </ul>
 *
 * This class can be instantiated with a {@link JobOperatorFactoryBean} to create a
 * transactional proxy around the job operator.
 *
 * @see JobOperatorFactoryBean
 * @author Dave Syer
 * @author Lucas Ward
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @author Yejeong Ham
 * @since 6.0
 */
@SuppressWarnings("removal")
public class TaskExecutorJobOperator extends SimpleJobOperator {

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

	@Override
	public void setJobRegistry(JobRegistry jobRegistry) {
		Assert.notNull(jobRegistry, "JobRegistry must not be null");
		this.jobRegistry = jobRegistry;
	}

	@Override
	public void setJobRepository(JobRepository jobRepository) {
		Assert.notNull(jobRepository, "JobRepository must not be null");
		this.jobRepository = jobRepository;
	}

	@Override
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}

	@Override
	public void setMeterRegistry(MeterRegistry meterRegistry) {
		Assert.notNull(meterRegistry, "MeterRegistry must not be null");
		this.meterRegistry = meterRegistry;
	}

	@Override
	public JobExecution start(Job job, JobParameters jobParameters)
			throws NoSuchJobException, JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
			JobRestartException, JobParametersInvalidException {
		Assert.notNull(job, "Job must not be null");
		Assert.notNull(jobParameters, "JobParameters must not be null");
		return super.start(job, jobParameters);
	}

	@Override
	public JobExecution restart(JobExecution jobExecution) throws JobInstanceAlreadyCompleteException,
			NoSuchJobExecutionException, NoSuchJobException, JobRestartException, JobParametersInvalidException {
		Assert.notNull(jobExecution, "JobExecution must not be null");
		return super.restart(jobExecution);
	}

	@Override
	public JobExecution startNextInstance(Job job) throws UnexpectedJobExecutionException {
		Assert.notNull(job, "Job must not be null");
		return super.startNextInstance(job);
	}

	@Override
	public boolean stop(JobExecution jobExecution) throws JobExecutionNotRunningException {
		Assert.notNull(jobExecution, "JobExecution must not be null");
		return super.stop(jobExecution);
	}

	@Override
	public JobExecution abandon(JobExecution jobExecution) throws JobExecutionAlreadyRunningException {
		Assert.notNull(jobExecution, "JobExecution must not be null");
		return super.abandon(jobExecution);
	}

	@Override
	public JobExecution recover(JobExecution jobExecution) {
		Assert.notNull(jobExecution, "JobExecution must not be null");
		return super.recover(jobExecution);
	}

}
