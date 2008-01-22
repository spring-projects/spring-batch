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
package org.springframework.batch.execution.step.simple;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobInstanceProperties;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.repository.JobRepository;

/**
 * @author Dave Syer
 *
 */
public class JobRepositorySupport implements JobRepository {

	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.repository.JobRepository#findOrCreateJob(org.springframework.batch.container.common.domain.JobConfiguration)
	 */
	public JobExecution createJobExecution(Job jobConfiguration, JobInstanceProperties jobInstanceProperties) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.repository.JobRepository#saveOrUpdate(org.springframework.batch.container.common.domain.JobExecution)
	 */
	public void saveOrUpdate(JobExecution jobExecution) {
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

	/* (non-Javadoc)
	 * @see org.springframework.batch.container.common.repository.JobRepository#update(org.springframework.batch.container.common.domain.Step)
	 */
	public void update(StepInstance step) {
	}

}
