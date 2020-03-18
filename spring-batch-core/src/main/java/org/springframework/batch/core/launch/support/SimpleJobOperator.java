/*
 * Copyright 2006-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.JobLauncher;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Simple implementation of the JobOperator interface.  Due to the amount of
 * functionality the implementation is combining, the following dependencies
 * are required:
 *
 * <ul>
 * 	<li> {@link JobLauncher}
 *  <li> {@link JobExplorer}
 *  <li> {@link JobRepository}
 *  <li> {@link JobRegistry}
 * </ul>
 *
 * @author Dave Syer
 * @author Lucas Ward
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class SimpleJobOperator implements JobOperator, InitializingBean {

	private static final String ILLEGAL_STATE_MSG = "Illegal state (only happens on a race condition): "
			+ "%s with name=%s and parameters=%s";

	private ListableJobLocator jobRegistry;

	private JobExplorer jobExplorer;

	private JobLauncher jobLauncher;

	private JobRepository jobRepository;

	private JobParametersConverter jobParametersConverter = new DefaultJobParametersConverter();

	private final Log logger = LogFactory.getLog(getClass());

	/**
	 * Check mandatory properties.
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jobLauncher, "JobLauncher must be provided");
		Assert.notNull(jobRegistry, "JobLocator must be provided");
		Assert.notNull(jobExplorer, "JobExplorer must be provided");
		Assert.notNull(jobRepository, "JobRepository must be provided");
	}

	/**
	 * Public setter for the {@link JobParametersConverter}.
	 * @param jobParametersConverter the {@link JobParametersConverter} to set
	 */
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

	/**
	 * Public setter for the {@link JobExplorer}.
	 * @param jobExplorer the {@link JobExplorer} to set
	 */
	public void setJobExplorer(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
	}

	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Public setter for the {@link JobLauncher}.
	 * @param jobLauncher the {@link JobLauncher} to set
	 */
	public void setJobLauncher(JobLauncher jobLauncher) {
		this.jobLauncher = jobLauncher;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.launch.JobOperator#getExecutions(java.lang.Long)
	 */
	@Override
	public List<Long> getExecutions(long instanceId) throws NoSuchJobInstanceException {
		JobInstance jobInstance = jobExplorer.getJobInstance(instanceId);
		if (jobInstance == null) {
			throw new NoSuchJobInstanceException(String.format("No job instance with id=%d", instanceId));
		}
		List<Long> list = new ArrayList<>();
		for (JobExecution jobExecution : jobExplorer.getJobExecutions(jobInstance)) {
			list.add(jobExecution.getId());
		}
		return list;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.launch.JobOperator#getJobNames()
	 */
	@Override
	public Set<String> getJobNames() {
		return new TreeSet<>(jobRegistry.getJobNames());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see JobOperator#getLastInstances(String, int, int)
	 */
	@Override
	public List<Long> getJobInstances(String jobName, int start, int count) throws NoSuchJobException {
		List<Long> list = new ArrayList<>();
		List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, start, count);
		for (JobInstance jobInstance : jobInstances) {
			list.add(jobInstance.getId());
		}
		if (list.isEmpty() && !jobRegistry.getJobNames().contains(jobName)) {
			throw new NoSuchJobException("No such job (either in registry or in historical data): " + jobName);
		}
		return list;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.launch.JobOperator#getParameters(java.
	 * lang.Long)
	 */
	@Override
	public String getParameters(long executionId) throws NoSuchJobExecutionException {
		JobExecution jobExecution = findExecutionById(executionId);

		return PropertiesConverter.propertiesToString(jobParametersConverter.getProperties(jobExecution
				.getJobParameters()));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.launch.JobOperator#getRunningExecutions
	 * (java.lang.String)
	 */
	@Override
	public Set<Long> getRunningExecutions(String jobName) throws NoSuchJobException {
		Set<Long> set = new LinkedHashSet<>();
		for (JobExecution jobExecution : jobExplorer.findRunningJobExecutions(jobName)) {
			set.add(jobExecution.getId());
		}
		if (set.isEmpty() && !jobRegistry.getJobNames().contains(jobName)) {
			throw new NoSuchJobException("No such job (either in registry or in historical data): " + jobName);
		}
		return set;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.launch.JobOperator#getStepExecutionSummaries
	 * (java.lang.Long)
	 */
	@Override
	public Map<Long, String> getStepExecutionSummaries(long executionId) throws NoSuchJobExecutionException {
		JobExecution jobExecution = findExecutionById(executionId);

		Map<Long, String> map = new LinkedHashMap<>();
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			map.put(stepExecution.getId(), stepExecution.toString());
		}
		return map;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.launch.JobOperator#getSummary(java.lang
	 * .Long)
	 */
	@Override
	public String getSummary(long executionId) throws NoSuchJobExecutionException {
		JobExecution jobExecution = findExecutionById(executionId);
		return jobExecution.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.launch.JobOperator#resume(java.lang.Long)
	 */
	@Override
	public Long restart(long executionId) throws JobInstanceAlreadyCompleteException, NoSuchJobExecutionException, NoSuchJobException, JobRestartException, JobParametersInvalidException {

		logger.info("Checking status of job execution with id=" + executionId);

		JobExecution jobExecution = findExecutionById(executionId);

		String jobName = jobExecution.getJobInstance().getJobName();
		Job job = jobRegistry.getJob(jobName);
		JobParameters parameters = jobExecution.getJobParameters();

		logger.info(String.format("Attempting to resume job with name=%s and parameters=%s", jobName, parameters));
		try {
			return jobLauncher.run(job, parameters).getId();
		}
		catch (JobExecutionAlreadyRunningException e) {
			throw new UnexpectedJobExecutionException(String.format(ILLEGAL_STATE_MSG, "job execution already running",
					jobName, parameters), e);
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.launch.JobOperator#start(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public Long start(String jobName, String parameters) throws NoSuchJobException, JobInstanceAlreadyExistsException, JobParametersInvalidException {

		logger.info("Checking status of job with name=" + jobName);

		JobParameters jobParameters = jobParametersConverter.getJobParameters(PropertiesConverter
				.stringToProperties(parameters));

		if (jobRepository.isJobInstanceExists(jobName, jobParameters)) {
			throw new JobInstanceAlreadyExistsException(String.format(
					"Cannot start a job instance that already exists with name=%s and parameters=%s", jobName,
					parameters));
		}

		Job job = jobRegistry.getJob(jobName);

		logger.info(String.format("Attempting to launch job with name=%s and parameters=%s", jobName, parameters));
		try {
			return jobLauncher.run(job, jobParameters).getId();
		}
		catch (JobExecutionAlreadyRunningException e) {
			throw new UnexpectedJobExecutionException(String.format(ILLEGAL_STATE_MSG, "job execution already running",
					jobName, parameters), e);
		}
		catch (JobRestartException e) {
			throw new UnexpectedJobExecutionException(String.format(ILLEGAL_STATE_MSG, "job not restartable", jobName,
					parameters), e);
		}
		catch (JobInstanceAlreadyCompleteException e) {
			throw new UnexpectedJobExecutionException(String.format(ILLEGAL_STATE_MSG, "job already complete", jobName,
					parameters), e);
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see JobOperator#startNextInstance(String )
	 */
	@Override
	public Long startNextInstance(String jobName) throws NoSuchJobException,
	UnexpectedJobExecutionException, JobParametersInvalidException {

		logger.info("Locating parameters for next instance of job with name=" + jobName);

		Job job = jobRegistry.getJob(jobName);
		JobParameters parameters = new JobParametersBuilder(jobExplorer)
				.getNextJobParameters(job)
				.toJobParameters();

		logger.info(String.format("Attempting to launch job with name=%s and parameters=%s", jobName, parameters));
		try {
			return jobLauncher.run(job, parameters).getId();
		}
		catch (JobExecutionAlreadyRunningException e) {
			throw new UnexpectedJobExecutionException(String.format(ILLEGAL_STATE_MSG, "job already running", jobName,
					parameters), e);
		}
		catch (JobRestartException e) {
			throw new UnexpectedJobExecutionException(String.format(ILLEGAL_STATE_MSG, "job not restartable", jobName,
					parameters), e);
		}
		catch (JobInstanceAlreadyCompleteException e) {
			throw new UnexpectedJobExecutionException(String.format(ILLEGAL_STATE_MSG, "job instance already complete",
					jobName, parameters), e);
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.launch.JobOperator#stop(java.lang.Long)
	 */
	@Override
	@Transactional
	public boolean stop(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {

		JobExecution jobExecution = findExecutionById(executionId);
		// Indicate the execution should be stopped by setting it's status to
		// 'STOPPING'. It is assumed that
		// the step implementation will check this status at chunk boundaries.
		BatchStatus status = jobExecution.getStatus();
		if (!(status == BatchStatus.STARTED || status == BatchStatus.STARTING)) {
			throw new JobExecutionNotRunningException("JobExecution must be running so that it can be stopped: "+jobExecution);
		}
		jobExecution.setStatus(BatchStatus.STOPPING);
		jobRepository.update(jobExecution);

		try {
			Job job = jobRegistry.getJob(jobExecution.getJobInstance().getJobName());
			if (job instanceof StepLocator) {//can only process as StepLocator is the only way to get the step object
				//get the current stepExecution
				for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
					if (stepExecution.getStatus().isRunning()) {
						try {
							//have the step execution that's running -> need to 'stop' it
							Step step = ((StepLocator)job).getStep(stepExecution.getStepName());
							if (step instanceof TaskletStep) {
								Tasklet tasklet = ((TaskletStep)step).getTasklet();
								if (tasklet instanceof StoppableTasklet) {
									StepSynchronizationManager.register(stepExecution);
									((StoppableTasklet)tasklet).stop();
									StepSynchronizationManager.release();
								}
							}
						}
						catch (NoSuchStepException e) {
							logger.warn("Step not found",e);
						}
					}
				}
			}
		}
		catch (NoSuchJobException e) {
			logger.warn("Cannot find Job object in the job registry. StoppableTasklet#stop() will not be called",e);
		}

		return true;
	}

	@Override
	public JobExecution abandon(long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionAlreadyRunningException {
		JobExecution jobExecution = findExecutionById(jobExecutionId);

		if (jobExecution.getStatus().isLessThan(BatchStatus.STOPPING)) {
			throw new JobExecutionAlreadyRunningException(
					"JobExecution is running or complete and therefore cannot be aborted");
		}

		logger.info("Aborting job execution: " + jobExecution);
		jobExecution.upgradeStatus(BatchStatus.ABANDONED);
		jobExecution.setEndTime(new Date());
		jobRepository.update(jobExecution);

		return jobExecution;
	}

	private JobExecution findExecutionById(long executionId) throws NoSuchJobExecutionException {
		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);

		if (jobExecution == null) {
			throw new NoSuchJobExecutionException("No JobExecution found for id: [" + executionId + "]");
		}
		return jobExecution;

	}
}
