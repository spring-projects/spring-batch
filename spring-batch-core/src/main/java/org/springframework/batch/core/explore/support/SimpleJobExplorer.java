/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.core.explore.support;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link JobExplorer} that uses the injected DAOs.
 *
 * @author Dave Syer
 * @author Lucas Ward
 * @author Michael Minella
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 * @author Glenn Renfro
 * @see JobExplorer
 * @see JobInstanceDao
 * @see JobExecutionDao
 * @see StepExecutionDao
 * @since 2.0
 */
public class SimpleJobExplorer implements JobExplorer {

	private JobInstanceDao jobInstanceDao;

	private JobExecutionDao jobExecutionDao;

	private StepExecutionDao stepExecutionDao;

	private ExecutionContextDao ecDao;

	/**
	 * Provides a default constructor with low visibility in case you want to use
	 * aop:proxy-target-class="true" for the AOP interceptor.
	 */
	SimpleJobExplorer() {
	}

	/**
	 * Constructor to initialize the job {@link SimpleJobExplorer}.
	 * @param jobInstanceDao The {@link JobInstanceDao} to be used by the repository.
	 * @param jobExecutionDao The {@link JobExecutionDao} to be used by the repository.
	 * @param stepExecutionDao The {@link StepExecutionDao} to be used by the repository.
	 * @param ecDao The {@link ExecutionContextDao} to be used by the repository.
	 */
	public SimpleJobExplorer(JobInstanceDao jobInstanceDao, JobExecutionDao jobExecutionDao,
			StepExecutionDao stepExecutionDao, ExecutionContextDao ecDao) {
		super();
		this.jobInstanceDao = jobInstanceDao;
		this.jobExecutionDao = jobExecutionDao;
		this.stepExecutionDao = stepExecutionDao;
		this.ecDao = ecDao;
	}

	@Override
	public List<JobExecution> getJobExecutions(JobInstance jobInstance) {
		List<JobExecution> executions = jobExecutionDao.findJobExecutions(jobInstance);
		for (JobExecution jobExecution : executions) {
			getJobExecutionDependencies(jobExecution);
			for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
				getStepExecutionDependencies(stepExecution);
			}
		}
		return executions;
	}

	@Nullable
	@Override
	public JobExecution getLastJobExecution(JobInstance jobInstance) {
		JobExecution lastJobExecution = jobExecutionDao.getLastJobExecution(jobInstance);
		if (lastJobExecution != null) {
			getJobExecutionDependencies(lastJobExecution);
			for (StepExecution stepExecution : lastJobExecution.getStepExecutions()) {
				getStepExecutionDependencies(stepExecution);
			}
		}
		return lastJobExecution;
	}

	@Override
	public Set<JobExecution> findRunningJobExecutions(@Nullable String jobName) {
		Set<JobExecution> executions = jobExecutionDao.findRunningJobExecutions(jobName);
		for (JobExecution jobExecution : executions) {
			getJobExecutionDependencies(jobExecution);
			for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
				getStepExecutionDependencies(stepExecution);
			}
		}
		return executions;
	}

	@Nullable
	@Override
	public JobExecution getJobExecution(@Nullable Long executionId) {
		if (executionId == null) {
			return null;
		}
		JobExecution jobExecution = jobExecutionDao.getJobExecution(executionId);
		if (jobExecution == null) {
			return null;
		}
		getJobExecutionDependencies(jobExecution);
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			getStepExecutionDependencies(stepExecution);
		}
		return jobExecution;
	}

	@Nullable
	@Override
	public StepExecution getStepExecution(@Nullable Long jobExecutionId, @Nullable Long executionId) {
		JobExecution jobExecution = jobExecutionDao.getJobExecution(jobExecutionId);
		if (jobExecution == null) {
			return null;
		}
		getJobExecutionDependencies(jobExecution);
		StepExecution stepExecution = stepExecutionDao.getStepExecution(jobExecution, executionId);
		getStepExecutionDependencies(stepExecution);
		return stepExecution;
	}

	@Nullable
	@Override
	public Set<StepExecution> getStepExecutions(Long jobExecutionId, Set<Long> stepExecutionIds) {
		JobExecution jobExecution = jobExecutionDao.getJobExecution(jobExecutionId);
		if (jobExecution == null) {
			return null;
		}
		getJobExecutionDependencies(jobExecution);
		Set<StepExecution> stepExecutions = stepExecutionDao.getStepExecutions(jobExecution, stepExecutionIds);
		stepExecutions.forEach(this::getStepExecutionDependencies);
		return stepExecutions;
	}

	@Nullable
	@Override
	public JobInstance getJobInstance(@Nullable Long instanceId) {
		return jobInstanceDao.getJobInstance(instanceId);
	}

	@Nullable
	@Override
	public JobInstance getJobInstance(String jobName, JobParameters jobParameters) {
		return jobInstanceDao.getJobInstance(jobName, jobParameters);
	}

	@Nullable
	@Override
	public JobInstance getLastJobInstance(String jobName) {
		return jobInstanceDao.getLastJobInstance(jobName);
	}

	@Override
	public List<JobInstance> getJobInstances(String jobName, int start, int count) {
		return jobInstanceDao.getJobInstances(jobName, start, count);
	}

	@Override
	public List<String> getJobNames() {
		return jobInstanceDao.getJobNames();
	}

	@Override
	public long getJobInstanceCount(@Nullable String jobName) throws NoSuchJobException {
		return jobInstanceDao.getJobInstanceCount(jobName);
	}

	@Override
	public long getStepExecutionCount(Set<Long> stepExecutionIds, Set<BatchStatus> matchingBatchStatuses) {
		if (stepExecutionIds.isEmpty() || matchingBatchStatuses.isEmpty()) {
			return 0;
		}
		return stepExecutionDao.countStepExecutions(stepExecutionIds, matchingBatchStatuses);
	}

	/**
	 * @return instance of {@link JobInstanceDao}.
	 * @since 5.1
	 */
	protected JobInstanceDao getJobInstanceDao() {
		return jobInstanceDao;
	}

	/**
	 * @return instance of {@link JobExecutionDao}.
	 * @since 5.1
	 */
	protected JobExecutionDao getJobExecutionDao() {
		return jobExecutionDao;
	}

	/**
	 * @return instance of {@link StepExecutionDao}.
	 * @since 5.1
	 */
	protected StepExecutionDao getStepExecutionDao() {
		return stepExecutionDao;
	}

	/**
	 * @return instance of {@link ExecutionContextDao}.
	 * @since 5.1
	 */
	protected ExecutionContextDao getEcDao() {
		return ecDao;
	}

	/*
	 * Find all dependencies for a JobExecution, including JobInstance (which requires
	 * JobParameters) plus StepExecutions
	 */
	private void getJobExecutionDependencies(JobExecution jobExecution) {
		JobInstance jobInstance = jobInstanceDao.getJobInstance(jobExecution);
		stepExecutionDao.addStepExecutions(jobExecution);
		jobExecution.setJobInstance(jobInstance);
		jobExecution.setExecutionContext(ecDao.getExecutionContext(jobExecution));

	}

	private void getStepExecutionDependencies(StepExecution stepExecution) {
		if (stepExecution != null) {
			stepExecution.setExecutionContext(ecDao.getExecutionContext(stepExecution));
		}
	}

	@Override
	public List<JobInstance> findJobInstancesByJobName(String jobName, int start, int count) {
		return jobInstanceDao.findJobInstancesByName(jobName, start, count);
	}

}
