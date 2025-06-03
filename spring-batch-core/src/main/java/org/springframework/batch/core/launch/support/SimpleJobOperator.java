/*
 * Copyright 2006-2025 the original author or authors.
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.ListableJobLocator;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.batch.core.step.tasklet.StoppableTasklet;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple implementation of the {@link JobOperator} interface. the following dependencies
 * are required:
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
 * @since 2.0
 * @deprecated since 6.0 in favor of {@link TaskExecutorJobOperator}. Scheduled for
 * removal in 6.2 or later.
 */
@Deprecated(since = "6.0", forRemoval = true)
public class SimpleJobOperator extends TaskExecutorJobLauncher implements JobOperator, InitializingBean {

	private static final String ILLEGAL_STATE_MSG = "Illegal state (only happens on a race condition): "
			+ "%s with name=%s and parameters=%s";

	protected ListableJobLocator jobRegistry;

	protected JobParametersConverter jobParametersConverter = new DefaultJobParametersConverter();

	private final Log logger = LogFactory.getLog(getClass());

	/**
	 * Check mandatory properties.
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.state(jobRegistry != null, "JobLocator must be provided");
	}

	/**
	 * Public setter for the {@link JobParametersConverter}.
	 * @param jobParametersConverter the {@link JobParametersConverter} to set
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	public void setJobParametersConverter(JobParametersConverter jobParametersConverter) {
		this.jobParametersConverter = jobParametersConverter;
	}

	/**
	 * Public setter for the {@link ListableJobLocator}.
	 * @param jobRegistry the {@link ListableJobLocator} to set
	 */
	public void setJobRegistry(ListableJobLocator jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public Long start(String jobName, Properties parameters)
			throws NoSuchJobException, JobInstanceAlreadyExistsException, JobParametersInvalidException {
		if (logger.isInfoEnabled()) {
			logger.info("Checking status of job with name=" + jobName);
		}

		JobParameters jobParameters = jobParametersConverter.getJobParameters(parameters);

		if (jobRepository.getJobInstance(jobName, jobParameters) != null) {
			throw new JobInstanceAlreadyExistsException(
					String.format("Cannot start a job instance that already exists with name=%s and parameters={%s}",
							jobName, parameters));
		}

		Job job = jobRegistry.getJob(jobName);
		if (logger.isInfoEnabled()) {
			logger
				.info(String.format("Attempting to launch job with name=%s and parameters={%s}", jobName, parameters));
		}
		try {
			return run(job, jobParameters).getId();
		}
		catch (JobExecutionAlreadyRunningException e) {
			throw new UnexpectedJobExecutionException(
					String.format(ILLEGAL_STATE_MSG, "job execution already running", jobName, parameters), e);
		}
		catch (JobRestartException e) {
			throw new UnexpectedJobExecutionException(
					String.format(ILLEGAL_STATE_MSG, "job not restartable", jobName, parameters), e);
		}
		catch (JobInstanceAlreadyCompleteException e) {
			throw new UnexpectedJobExecutionException(
					String.format(ILLEGAL_STATE_MSG, "job already complete", jobName, parameters), e);
		}

	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public Long restart(long executionId) throws JobInstanceAlreadyCompleteException, NoSuchJobExecutionException,
			NoSuchJobException, JobRestartException, JobParametersInvalidException {

		if (logger.isInfoEnabled()) {
			logger.info("Checking status of job execution with id=" + executionId);
		}
		JobExecution jobExecution = findExecutionById(executionId);

		String jobName = jobExecution.getJobInstance().getJobName();
		Job job = jobRegistry.getJob(jobName);
		JobParameters parameters = jobExecution.getJobParameters();

		if (logger.isInfoEnabled()) {
			logger.info(String.format("Attempting to resume job with name=%s and parameters=%s", jobName, parameters));
		}
		try {
			return run(job, parameters).getId();
		}
		catch (JobExecutionAlreadyRunningException e) {
			throw new UnexpectedJobExecutionException(
					String.format(ILLEGAL_STATE_MSG, "job execution already running", jobName, parameters), e);
		}

	}

	@Override
	public JobExecution restart(JobExecution jobExecution) throws JobInstanceAlreadyCompleteException,
			NoSuchJobExecutionException, NoSuchJobException, JobRestartException, JobParametersInvalidException {

		String jobName = jobExecution.getJobInstance().getJobName();
		Job job = jobRegistry.getJob(jobName);
		JobParameters parameters = jobExecution.getJobParameters();

		if (logger.isInfoEnabled()) {
			logger.info(String.format("Attempting to resume job with name=%s and parameters=%s", jobName, parameters));
		}
		try {
			return run(job, parameters);
		}
		catch (JobExecutionAlreadyRunningException e) {
			throw new UnexpectedJobExecutionException(
					String.format(ILLEGAL_STATE_MSG, "job execution already running", jobName, parameters), e);
		}

	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public Long startNextInstance(String jobName)
			throws NoSuchJobException, UnexpectedJobExecutionException, JobParametersInvalidException {
		if (logger.isInfoEnabled()) {
			logger.info("Locating parameters for next instance of job with name=" + jobName);
		}

		Job job = jobRegistry.getJob(jobName);
		JobParameters parameters = new JobParametersBuilder(jobRepository).getNextJobParameters(job).toJobParameters();
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Attempting to launch job with name=%s and parameters=%s", jobName, parameters));
		}
		try {
			return run(job, parameters).getId();
		}
		catch (JobExecutionAlreadyRunningException e) {
			throw new UnexpectedJobExecutionException(
					String.format(ILLEGAL_STATE_MSG, "job already running", jobName, parameters), e);
		}
		catch (JobRestartException e) {
			throw new UnexpectedJobExecutionException(
					String.format(ILLEGAL_STATE_MSG, "job not restartable", jobName, parameters), e);
		}
		catch (JobInstanceAlreadyCompleteException e) {
			throw new UnexpectedJobExecutionException(
					String.format(ILLEGAL_STATE_MSG, "job instance already complete", jobName, parameters), e);
		}

	}

	@Override
	public JobExecution startNextInstance(Job job)
			throws NoSuchJobException, UnexpectedJobExecutionException, JobParametersInvalidException {

		JobParameters parameters = new JobParametersBuilder(jobRepository).getNextJobParameters(job).toJobParameters();
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Attempting to launch job with name=%s and parameters=%s", job.getName(),
					parameters));
		}
		try {
			return run(job, parameters);
		}
		catch (JobExecutionAlreadyRunningException e) {
			throw new UnexpectedJobExecutionException(
					String.format(ILLEGAL_STATE_MSG, "job already running", job.getName(), parameters), e);
		}
		catch (JobRestartException e) {
			throw new UnexpectedJobExecutionException(
					String.format(ILLEGAL_STATE_MSG, "job not restartable", job.getName(), parameters), e);
		}
		catch (JobInstanceAlreadyCompleteException e) {
			throw new UnexpectedJobExecutionException(
					String.format(ILLEGAL_STATE_MSG, "job instance already complete", job.getName(), parameters), e);
		}

	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public boolean stop(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {

		JobExecution jobExecution = findExecutionById(executionId);
		return stop(jobExecution);
	}

	@Override
	public boolean stop(JobExecution jobExecution) throws JobExecutionNotRunningException {

		// Indicate the execution should be stopped by setting it's status to
		// 'STOPPING'. It is assumed that
		// the step implementation will check this status at chunk boundaries.
		BatchStatus status = jobExecution.getStatus();
		if (!(status == BatchStatus.STARTED || status == BatchStatus.STARTING)) {
			throw new JobExecutionNotRunningException(
					"JobExecution must be running so that it can be stopped: " + jobExecution);
		}
		jobExecution.setStatus(BatchStatus.STOPPING);
		jobRepository.update(jobExecution);

		try {
			Job job = jobRegistry.getJob(jobExecution.getJobInstance().getJobName());
			if (job instanceof StepLocator stepLocator) {
				// can only process as StepLocator is the only way to get the step object
				// get the current stepExecution
				for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
					if (stepExecution.getStatus().isRunning()) {
						try {
							// have the step execution that's running -> need to 'stop' it
							Step step = stepLocator.getStep(stepExecution.getStepName());
							if (step instanceof TaskletStep taskletStep) {
								Tasklet tasklet = taskletStep.getTasklet();
								if (tasklet instanceof StoppableTasklet stoppableTasklet) {
									StepSynchronizationManager.register(stepExecution);
									stoppableTasklet.stop();
									StepSynchronizationManager.release();
								}
							}
						}
						catch (NoSuchStepException e) {
							logger.warn("Step not found", e);
						}
					}
				}
			}
		}
		catch (NoSuchJobException e) {
			logger.warn("Cannot find Job object in the job registry. StoppableTasklet#stop() will not be called", e);
		}

		return true;
	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public JobExecution abandon(long jobExecutionId)
			throws NoSuchJobExecutionException, JobExecutionAlreadyRunningException {
		JobExecution jobExecution = findExecutionById(jobExecutionId);

		return abandon(jobExecution);
	}

	@Override
	public JobExecution abandon(JobExecution jobExecution) throws JobExecutionAlreadyRunningException {

		if (jobExecution.getStatus().isLessThan(BatchStatus.STOPPING)) {
			throw new JobExecutionAlreadyRunningException(
					"JobExecution is running or complete and therefore cannot be aborted");
		}
		if (logger.isInfoEnabled()) {
			logger.info("Aborting job execution: " + jobExecution);
		}
		jobExecution.upgradeStatus(BatchStatus.ABANDONED);
		jobExecution.setEndTime(LocalDateTime.now());
		jobRepository.update(jobExecution);

		return jobExecution;
	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public Set<String> getJobNames() {
		return new TreeSet<>(jobRegistry.getJobNames());
	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public List<Long> getExecutions(long instanceId) throws NoSuchJobInstanceException {
		JobInstance jobInstance = jobRepository.getJobInstance(instanceId);
		if (jobInstance == null) {
			throw new NoSuchJobInstanceException(String.format("No job instance with id=%d", instanceId));
		}
		List<Long> list = new ArrayList<>();
		for (JobExecution jobExecution : jobRepository.getJobExecutions(jobInstance)) {
			list.add(jobExecution.getId());
		}
		return list;
	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public List<Long> getJobInstances(String jobName, int start, int count) throws NoSuchJobException {
		List<Long> list = new ArrayList<>();
		List<JobInstance> jobInstances = jobRepository.getJobInstances(jobName, start, count);
		for (JobInstance jobInstance : jobInstances) {
			list.add(jobInstance.getId());
		}
		if (list.isEmpty() && !jobRegistry.getJobNames().contains(jobName)) {
			throw new NoSuchJobException("No such job (either in registry or in historical data): " + jobName);
		}
		return list;
	}

	@SuppressWarnings("removal")
	@Override
	@Nullable
	@Deprecated(since = "6.0", forRemoval = true)
	public JobInstance getJobInstance(String jobName, JobParameters jobParameters) {
		return this.jobRepository.getJobInstance(jobName, jobParameters);
	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public String getParameters(long executionId) throws NoSuchJobExecutionException {
		JobExecution jobExecution = findExecutionById(executionId);

		Properties properties = this.jobParametersConverter.getProperties(jobExecution.getJobParameters());

		return PropertiesConverter.propertiesToString(properties);
	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public Set<Long> getRunningExecutions(String jobName) throws NoSuchJobException {
		Set<Long> set = new LinkedHashSet<>();
		for (JobExecution jobExecution : jobRepository.findRunningJobExecutions(jobName)) {
			set.add(jobExecution.getId());
		}
		if (set.isEmpty() && !jobRegistry.getJobNames().contains(jobName)) {
			throw new NoSuchJobException("No such job (either in registry or in historical data): " + jobName);
		}
		return set;
	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public Map<Long, String> getStepExecutionSummaries(long executionId) throws NoSuchJobExecutionException {
		JobExecution jobExecution = findExecutionById(executionId);

		Map<Long, String> map = new LinkedHashMap<>();
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			map.put(stepExecution.getId(), stepExecution.toString());
		}
		return map;
	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public String getSummary(long executionId) throws NoSuchJobExecutionException {
		JobExecution jobExecution = findExecutionById(executionId);
		return jobExecution.toString();
	}

	private JobExecution findExecutionById(long executionId) throws NoSuchJobExecutionException {
		JobExecution jobExecution = jobRepository.getJobExecution(executionId);

		if (jobExecution == null) {
			throw new NoSuchJobExecutionException("No JobExecution found for id: [" + executionId + "]");
		}
		return jobExecution;

	}

}
