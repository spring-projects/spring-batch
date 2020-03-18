/*
 * Copyright 2006-2019 the original author or authors.
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

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
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
 * Implementation of {@link JobExplorer} using the injected DAOs.
 *
 * @author Dave Syer
 * @author Lucas Ward
 * @author Michael Minella
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 *
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
	 * Provide default constructor with low visibility in case user wants to use
	 * use aop:proxy-target-class="true" for AOP interceptor.
	 */
	SimpleJobExplorer() {
	}

	public SimpleJobExplorer(JobInstanceDao jobInstanceDao, JobExecutionDao jobExecutionDao,
			StepExecutionDao stepExecutionDao, ExecutionContextDao ecDao) {
		super();
		this.jobInstanceDao = jobInstanceDao;
		this.jobExecutionDao = jobExecutionDao;
		this.stepExecutionDao = stepExecutionDao;
		this.ecDao = ecDao;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.explore.JobExplorer#findJobExecutions(
	 * org.springframework.batch.core.JobInstance)
	 */
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

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.explore.JobExplorer#getLastJobExecution(
	 * org.springframework.batch.core.JobInstance)
	 */
	@Nullable
	public JobExecution getLastJobExecution(JobInstance jobInstance) {
		return jobExecutionDao.getLastJobExecution(jobInstance);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.explore.JobExplorer#findRunningJobExecutions
	 * (java.lang.String)
	 */
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

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.explore.JobExplorer#getJobExecution(java
	 * .lang.Long)
	 */
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

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.explore.JobExplorer#getStepExecution(java
	 * .lang.Long)
	 */
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

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.explore.JobExplorer#getJobInstance(java
	 * .lang.Long)
	 */
	@Nullable
	@Override
	public JobInstance getJobInstance(@Nullable Long instanceId) {
		return jobInstanceDao.getJobInstance(instanceId);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.explore.JobExplorer#getLastJobInstance(java
	 * .lang.String)
	 */
	@Nullable
	@Override
	public JobInstance getLastJobInstance(String jobName) {
		return jobInstanceDao.getLastJobInstance(jobName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.explore.JobExplorer#getLastJobInstances
	 * (java.lang.String, int)
	 */
	@Override
	public List<JobInstance> getJobInstances(String jobName, int start, int count) {
		return jobInstanceDao.getJobInstances(jobName, start, count);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.explore.JobExplorer#getJobNames()
	 */
	@Override
	public List<String> getJobNames() {
		return jobInstanceDao.getJobNames();
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.explore.JobExplorer#getJobInstanceCount(java.lang.String)
	 */
	@Override
	public int getJobInstanceCount(@Nullable String jobName) throws NoSuchJobException {
		return jobInstanceDao.getJobInstanceCount(jobName);
	}

	/*
	 * Find all dependencies for a JobExecution, including JobInstance (which
	 * requires JobParameters) plus StepExecutions
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
