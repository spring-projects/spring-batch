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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NullUnmarked;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.observability.jfr.events.job.JobLaunchEvent;
import org.springframework.batch.core.observability.micrometer.MicrometerMetrics;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

import static org.springframework.batch.core.observability.BatchMetrics.METRICS_PREFIX;

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
@NullUnmarked
public class TaskExecutorJobOperator extends SimpleJobOperator {

	private static final Log logger = LogFactory.getLog(TaskExecutorJobOperator.class.getName());

	protected ObservationRegistry observationRegistry;

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		if (this.observationRegistry == null) {
			logger.info("No ObservationRegistry has been set, defaulting to ObservationRegistry NOOP");
			this.observationRegistry = ObservationRegistry.NOOP;
		}
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

	/**
	 * Set the observation registry to use for observations. Defaults to
	 * {@link ObservationRegistry#NOOP}.
	 * @param observationRegistry the observation registry
	 * @since 6.0
	 */
	public void setObservationRegistry(ObservationRegistry observationRegistry) {
		Assert.notNull(observationRegistry, "ObservationRegistry must not be null");
		this.observationRegistry = observationRegistry;
	}

	@Override
	public JobExecution start(Job job, JobParameters jobParameters) throws JobInstanceAlreadyCompleteException,
			JobExecutionAlreadyRunningException, JobRestartException, InvalidJobParametersException {
		Assert.notNull(job, "Job must not be null");
		Assert.notNull(jobParameters, "JobParameters must not be null");
		new JobLaunchEvent(job.getName(), jobParameters.toString()).commit();
		Observation observation = MicrometerMetrics
			.createObservation(METRICS_PREFIX + "job.launch.count", this.observationRegistry)
			.start();
		try (var scope = observation.openScope()) {
			return super.start(job, jobParameters);
		}
		finally {
			observation.stop();
		}
	}

	@Override
	public JobExecution restart(JobExecution jobExecution) throws JobRestartException {
		Assert.notNull(jobExecution, "JobExecution must not be null");
		return super.restart(jobExecution);
	}

	@Override
	public JobExecution startNextInstance(Job job) {
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
