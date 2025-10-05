/*
 * Copyright 2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for {@link ResourcelessJobRepository}.
 *
 * @author Mahmoud Ben Hassine
 */
class ResourcelessJobRepositoryTests {

	private final ResourcelessJobRepository jobRepository = new ResourcelessJobRepository();

	@Test
	void isJobInstanceExists() {
		assertFalse(this.jobRepository.isJobInstanceExists("job", new JobParameters()));
	}

	@Test
	void createJobInstance() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();

		// when
		JobInstance jobInstance = this.jobRepository.createJobInstance(jobName, jobParameters);

		// then
		assertNotNull(jobInstance);
		assertEquals(jobName, jobInstance.getJobName());
		assertEquals(1L, jobInstance.getInstanceId());
	}

	@Test
	void createJobExecution() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();

		// when
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());

		// then
		assertNotNull(jobExecution);
		assertEquals(1L, jobExecution.getId());
		assertEquals(jobName, jobExecution.getJobInstance().getJobName());
		assertEquals(1L, jobExecution.getJobInstance().getInstanceId());
	}

	@Test
	void getLastJobExecution() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		// when
		JobExecution jobExecution = this.jobRepository.getLastJobExecution(jobName, jobParameters);

		// then
		assertNotNull(jobExecution);
		assertEquals(1L, jobExecution.getId());
		assertEquals(jobName, jobExecution.getJobInstance().getJobName());
		assertEquals(1L, jobExecution.getJobInstance().getInstanceId());
	}

}