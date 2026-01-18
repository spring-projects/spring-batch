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

package org.springframework.batch.core.repository.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.explore.support.SimpleJobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * <p>
 * Implementation of {@link JobRepository} that stores job instances, job executions, and
 * step executions using the injected DAOs.
 * </p>
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * @author David Turanski
 * @author Mahmoud Ben Hassine
 * @author Baris Cubukcuoglu
 * @author Parikshit Dutta
 * @author Mark John Moreno
 * @author Yanming Zhou
 * @see JobRepository
 * @see JobInstanceDao
 * @see JobExecutionDao
 * @see StepExecutionDao
 *
 */
@SuppressWarnings("removal")
public class SimpleJobRepository extends SimpleJobExplorer implements JobRepository {

	private static final Log logger = LogFactory.getLog(SimpleJobRepository.class);

	public SimpleJobRepository(JobInstanceDao jobInstanceDao, JobExecutionDao jobExecutionDao,
			StepExecutionDao stepExecutionDao, ExecutionContextDao ecDao) {
		super(jobInstanceDao, jobExecutionDao, stepExecutionDao, ecDao);
	}

	/**
	 * Fetch all {@link JobInstance} values for a given job name.
	 * @param jobName The name of the job.
	 * @return the {@link JobInstance} values.
	 * @since 6.0
	 */
	@Override
	public List<JobInstance> findJobInstances(String jobName) {
		return this.jobInstanceDao.getJobInstances(jobName);
	}

	@Nullable
	@Override
	public StepExecution getStepExecution(long executionId) {
		StepExecution stepExecution = this.stepExecutionDao.getStepExecution(executionId);
		if (stepExecution != null) {
			fillStepExecutionDependencies(stepExecution);
			ExecutionContext jobExecutionContext = this.ecDao.getExecutionContext(stepExecution.getJobExecution());
			stepExecution.getJobExecution().setExecutionContext(jobExecutionContext);
		}
		return stepExecution;
	}

	/**
	 * Create a new {@link JobExecution} for the given {@link JobInstance} and
	 * {@link JobParameters}, and associate the provided {@link ExecutionContext} with the
	 * new {@link JobExecution}.
	 * @param jobInstance the job instance to which the execution belongs
	 * @param jobParameters the runtime parameters for the job
	 * @param executionContext the execution context to associate with the job execution
	 * @return the new job execution
	 * @since 6.0
	 */
	public JobExecution createJobExecution(JobInstance jobInstance, JobParameters jobParameters,
			ExecutionContext executionContext) {

		JobExecution jobExecution = jobExecutionDao.createJobExecution(jobInstance, jobParameters);
		jobExecution.setExecutionContext(executionContext);
		ecDao.saveExecutionContext(jobExecution);

		// add the jobExecution to the jobInstance
		jobInstance.addJobExecution(jobExecution);

		return jobExecution;
	}

	/**
	 * Create a new {@link StepExecution} for the given {@link JobExecution} and step
	 * name, associate a new {@link ExecutionContext} with the new {@link StepExecution},
	 * and add the new {@link StepExecution} to the {@link JobExecution}.
	 * @param stepName the name of the step
	 * @param jobExecution the job execution to which the step execution belongs
	 * @return the new step execution
	 * @since 6.0
	 */
	public StepExecution createStepExecution(String stepName, JobExecution jobExecution) {
		Assert.notNull(jobExecution, "JobExecution must not be null.");
		Assert.notNull(stepName, "Step name must not be null.");

		StepExecution stepExecution = stepExecutionDao.createStepExecution(stepName, jobExecution);
		ecDao.saveExecutionContext(stepExecution);
		jobExecution.addStepExecution(stepExecution);

		return stepExecution;
	}

	@Override
	public void update(JobExecution jobExecution) {

		Assert.notNull(jobExecution, "JobExecution cannot be null.");

		jobExecution.setLastUpdated(LocalDateTime.now());

		jobExecutionDao.synchronizeStatus(jobExecution);
		if (jobExecution.getStatus() == BatchStatus.STOPPING && jobExecution.getEndTime() != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Upgrading job execution status from STOPPING to STOPPED since it has already ended.");
			}
			jobExecution.upgradeStatus(BatchStatus.STOPPED);
		}
		jobExecutionDao.updateJobExecution(jobExecution);
	}

	@Override
	public void update(StepExecution stepExecution) {
		validateStepExecution(stepExecution);
		Assert.notNull(stepExecution.getId(), "StepExecution must already be saved (have an id assigned)");

		stepExecution.setLastUpdated(LocalDateTime.now());

		StepExecution latestStepExecution = getStepExecution(stepExecution.getId());
		Assert.state(latestStepExecution != null,
				"StepExecution with id " + stepExecution.getId() + "not found. Batch metadata state may be corrupted.");

		if (latestStepExecution.getJobExecution().isStopped() || latestStepExecution.getJobExecution().isStopping()) {
			Integer version = latestStepExecution.getVersion();
			if (version != null) {
				stepExecution.setVersion(version);
			}
			stepExecution.setTerminateOnly();
		}

		stepExecutionDao.updateStepExecution(stepExecution);
	}

	private void validateStepExecution(StepExecution stepExecution) {
		Assert.notNull(stepExecution, "StepExecution cannot be null.");
		Assert.notNull(stepExecution.getStepName(), "StepExecution's step name cannot be null.");
		Assert.notNull(stepExecution.getJobExecutionId(), "StepExecution must belong to persisted JobExecution");
	}

	@Override
	public void updateExecutionContext(StepExecution stepExecution) {
		validateStepExecution(stepExecution);
		Assert.notNull(stepExecution.getId(), "StepExecution must already be saved (have an id assigned)");
		ecDao.updateExecutionContext(stepExecution);
	}

	@Override
	public void updateExecutionContext(JobExecution jobExecution) {
		ecDao.updateExecutionContext(jobExecution);
	}

	@Override
	public void deleteStepExecution(StepExecution stepExecution) {
		this.ecDao.deleteExecutionContext(stepExecution);
		this.stepExecutionDao.deleteStepExecution(stepExecution);
	}

	@Override
	public void deleteJobExecution(JobExecution jobExecution) {
		this.ecDao.deleteExecutionContext(jobExecution);
		this.jobExecutionDao.deleteJobExecutionParameters(jobExecution);
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			deleteStepExecution(stepExecution);
		}
		this.jobExecutionDao.deleteJobExecution(jobExecution);
	}

	@Override
	public void deleteJobInstance(JobInstance jobInstance) {
		List<JobExecution> jobExecutions = getJobExecutions(jobInstance);
		for (JobExecution jobExecution : jobExecutions) {
			deleteJobExecution(jobExecution);
		}
		this.jobInstanceDao.deleteJobInstance(jobInstance);
	}

	@Override
	public JobInstance createJobInstance(String jobName, JobParameters jobParameters) {
		Assert.notNull(jobName, "A job name is required to create a JobInstance");
		Assert.notNull(jobParameters, "Job parameters are required to create a JobInstance");

		return jobInstanceDao.createJobInstance(jobName, jobParameters);
	}

}
