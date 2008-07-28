/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.step;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;

/**
 * @author Dave Syer
 *
 */
public class JobRepositorySupport implements JobRepository {

	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.repository.JobRepository#findOrCreateJob(org.springframework.batch.container.common.domain.JobConfiguration)
	 */
	public JobExecution createJobExecution(Job jobConfiguration, JobParameters jobParameters) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.repository.JobRepository#saveOrUpdate(org.springframework.batch.container.common.domain.JobExecution)
	 */
	public void updateJobExecution(JobExecution jobExecution) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.repository.JobRepository#saveOrUpdate(org.springframework.batch.container.common.domain.StepExecution)
	 */
	public void saveOrUpdate(StepExecution stepExecution) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.repository.JobRepository#update(org.springframework.batch.container.common.domain.Job)
	 */
	public void update(JobInstance job) {
	}

	public StepExecution getLastStepExecution(JobInstance jobInstance, Step step) {
		return null;
	}

	public int getStepExecutionCount(JobInstance jobInstance, Step step) {
		return 0;
	}

	public int getJobExecutionCount(JobInstance jobInstance) {
		return 0;
	}

	public JobExecution getLastJobExecution(JobInstance jobInstance) {
		return null;
	}

	public void save(StepExecution stepExecution) {
	}

	public void update(StepExecution stepExecution) {
	}

	public void saveOrUpdateExecutionContext(StepExecution stepExecution) {
	}

}
