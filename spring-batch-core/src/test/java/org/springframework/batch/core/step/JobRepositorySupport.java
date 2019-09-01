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
package org.springframework.batch.core.step;

import java.util.Collection;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * @author David Turanski
 *
 */
public class JobRepositorySupport implements JobRepository {

	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.repository.JobRepository#findOrCreateJob(org.springframework.batch.container.common.domain.JobConfiguration)
	 */
	@Override
	public JobExecution createJobExecution(String jobName, JobParameters jobParameters) {
		JobInstance jobInstance = new JobInstance(0L, jobName);
		return new JobExecution(jobInstance, 11L, jobParameters, null);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.repository.JobRepository#saveOrUpdate(org.springframework.batch.container.common.domain.JobExecution)
	 */
	@Override
	public void update(JobExecution jobExecution) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.repository.JobRepository#update(org.springframework.batch.container.common.domain.Job)
	 */
	public void update(JobInstance job) {
	}

	@Nullable
	@Override
	public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		return null;
	}

	@Override
	public int getStepExecutionCount(JobInstance jobInstance, String stepName) {
		return 0;
	}

	public int getJobExecutionCount(JobInstance jobInstance) {
		return 0;
	}

	public JobExecution getLastJobExecution(JobInstance jobInstance) {
		return null;
	}

	@Override
	public void add(StepExecution stepExecution) {
	}

	@Override
	public void update(StepExecution stepExecution) {
	}

	@Override
	public void updateExecutionContext(StepExecution stepExecution) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.repository.JobRepository#isJobInstanceExists(java.lang.String, org.springframework.batch.core.JobParameters)
	 */
	@Override
	public boolean isJobInstanceExists(String jobName, JobParameters jobParameters) {
		return false;
	}

	@Nullable
	@Override
	public JobExecution getLastJobExecution(String jobName, JobParameters jobParameters) {
		return null;
	}

	@Override
	public void updateExecutionContext(JobExecution jobExecution) {
	}

	@Override
	public void addAll(Collection<StepExecution> stepExecutions) {
	}

	@Override
	public JobInstance createJobInstance(String jobName,
			JobParameters jobParameters) {
		return null;
	}

	@Override
	public JobExecution createJobExecution(JobInstance jobInstance,
			JobParameters jobParameters, String jobConfigurationLocation) {
		return null;
	}
}
