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
package org.springframework.batch.integration;

import java.util.Collection;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 */
public class JobRepositorySupport implements JobRepository {

	@Override
	public JobExecution createJobExecution(String jobName, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
		return new JobExecution(new JobInstance(0L, jobName), jobParameters);
	}

	@Override
	public JobInstance getJobInstance(String jobName, JobParameters jobParameters) {
		return null;
	}

	@Override
	@Nullable
	public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		return null;
	}

	@Override
	public long getStepExecutionCount(JobInstance jobInstance, String stepName) {
		return 0;
	}

	@Override
	public void update(JobExecution jobExecution) {
	}

	@Override
	public void updateExecutionContext(StepExecution stepExecution) {
	}

	@Override
	public void updateExecutionContext(JobExecution jobExecution) {
	}

	@Override
	public void add(StepExecution stepExecution) {
	}

	@Override
	public void update(StepExecution stepExecution) {
	}

	@SuppressWarnings("removal")
	@Override
	public boolean isJobInstanceExists(String jobName, JobParameters jobParameters) {
		return false;
	}

	@Override
	@Nullable
	public JobExecution getLastJobExecution(String jobName, JobParameters jobParameters) {
		return null;
	}

	@Override
	public void addAll(Collection<StepExecution> stepExecutions) {
		if (stepExecutions != null) {
			for (StepExecution stepExecution : stepExecutions) {
				add(stepExecution);
			}
		}
	}

	@Override
	public JobInstance createJobInstance(String jobName, JobParameters jobParameters) {
		return null;
	}

}
