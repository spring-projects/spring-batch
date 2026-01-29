/*
 * Copyright 2024-present the original author or authors.
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
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link ResourcelessJobRepository}.
 *
 * @author Mahmoud Ben Hassine
 * @author Sanghyuk Jung
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
		assertEquals(1L, jobInstance.getId());
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
		assertEquals(1L, jobExecution.getJobInstance().getId());
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
		assertEquals(1L, jobExecution.getJobInstance().getId());
	}

	@Test
	void getJobInstancesWithDifferentJobParameters() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParametersBuilder().addLong("param", 1L).toJobParameters();
		JobInstance instance = jobRepository.createJobInstance(jobName, jobParameters);
		jobRepository.createJobExecution(instance, jobParameters, new ExecutionContext());

		// when
		JobParameters differentParameters = new JobParametersBuilder().addLong("param", 2L).toJobParameters();
		var jobInstance = jobRepository.getJobInstance(jobName, differentParameters);

		// then
		assertNull(jobInstance);
	}

	@Test
	void getJobInstancesWithDifferentJobName() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		jobRepository.createJobInstance(jobName, jobParameters);

		// when
		var jobInstances = jobRepository.getJobInstances("differentJob", 0, 10);

		// then
		assertTrue(jobInstances.isEmpty());
	}

	@Test
	void getJobInstancesWithCorrectJobName() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		jobRepository.createJobInstance(jobName, jobParameters);

		// when
		var jobInstances = jobRepository.getJobInstances(jobName, 0, 10);

		// then
		assertEquals(1, jobInstances.size());
		assertEquals(jobName, jobInstances.get(0).getJobName());
	}

	@Test
	void findJobInstancesWithDifferentJobName() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		jobRepository.createJobInstance(jobName, jobParameters);

		// when
		var jobInstances = jobRepository.findJobInstances("differentJob");

		// then
		assertTrue(jobInstances.isEmpty());
	}

	@Test
	void findJobInstancesWithCorrectJobName() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		jobRepository.createJobInstance(jobName, jobParameters);

		// when
		var jobInstances = jobRepository.findJobInstances(jobName);

		// then
		assertEquals(1, jobInstances.size());
		assertEquals(jobName, jobInstances.get(0).getJobName());
	}

	@Test
	void getJobInstanceWithDifferentId() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		jobRepository.createJobInstance(jobName, jobParameters);

		// when
		JobInstance jobInstance = jobRepository.getJobInstance(999L);

		// then
		assertNull(jobInstance);
	}

	@Test
	void getJobInstanceWithCorrectId() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		jobRepository.createJobInstance(jobName, jobParameters);

		// when
		JobInstance jobInstance = jobRepository.getJobInstance(1L);

		// then
		assertNotNull(jobInstance);
		assertEquals(jobName, jobInstance.getJobName());
		assertEquals(1L, jobInstance.getId());
	}

	@Test
	void getLastJobInstanceWithDifferentJobName() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		jobRepository.createJobInstance(jobName, jobParameters);

		// when
		JobInstance jobInstance = jobRepository.getLastJobInstance("differentJob");

		// then
		assertNull(jobInstance);
	}

	@Test
	void getLastJobInstanceWithCorrectJobName() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		jobRepository.createJobInstance(jobName, jobParameters);

		// when
		JobInstance jobInstance = jobRepository.getLastJobInstance(jobName);

		// then
		assertNotNull(jobInstance);
		assertEquals(jobName, jobInstance.getJobName());
	}

	@Test
	void getJobInstanceCountWithDifferentJobName() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		jobRepository.createJobInstance(jobName, jobParameters);

		// when
		long count = jobRepository.getJobInstanceCount("differentJob");

		// then
		assertEquals(0L, count);
	}

	@Test
	void getJobInstanceCountWithCorrectJobName() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		jobRepository.createJobInstance(jobName, jobParameters);

		// when
		long count = jobRepository.getJobInstanceCount(jobName);

		// then
		assertEquals(1L, count);
	}

	@Test
	void getJobExecutionWithDifferentId() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		// when
		JobExecution jobExecution = jobRepository.getJobExecution(999L);

		// then
		assertNull(jobExecution);
	}

	@Test
	void getJobExecutionWithCorrectId() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		// when
		JobExecution jobExecution = jobRepository.getJobExecution(1L);

		// then
		assertNotNull(jobExecution);
		assertEquals(1L, jobExecution.getId());
	}

	@Test
	void getLastJobExecutionWithDifferentJobName() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		// when
		JobExecution jobExecution = jobRepository.getLastJobExecution("differentJob", jobParameters);

		// then
		assertNull(jobExecution);
	}

	@Test
	void getLastJobExecutionWithDifferentJobParameters() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParametersBuilder().addLong("param", 1L).toJobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		// when
		JobParameters differentParameters = new JobParametersBuilder().addLong("param", 2L).toJobParameters();
		JobExecution jobExecution = jobRepository.getLastJobExecution(jobName, differentParameters);

		// then
		assertNull(jobExecution);
	}

	@Test
	void getLastJobExecutionByJobInstanceWithDifferentId() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		// when
		JobInstance differentJobInstance = new JobInstance(999L, jobName);
		JobExecution jobExecution = jobRepository.getLastJobExecution(differentJobInstance);

		// then
		assertNull(jobExecution);
	}

	@Test
	void getLastJobExecutionByJobInstanceWithCorrectId() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		// when
		JobExecution jobExecution = jobRepository.getLastJobExecution(jobInstance);

		// then
		assertNotNull(jobExecution);
		assertEquals(1L, jobExecution.getId());
	}

	@Test
	void getJobExecutionsWithDifferentJobInstance() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		// when
		JobInstance differentJobInstance = new JobInstance(999L, jobName);
		var jobExecutions = jobRepository.getJobExecutions(differentJobInstance);

		// then
		assertTrue(jobExecutions.isEmpty());
	}

	@Test
	void getJobExecutionsWithCorrectJobInstance() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		// when
		var jobExecutions = jobRepository.getJobExecutions(jobInstance);

		// then
		assertEquals(1, jobExecutions.size());
		assertEquals(1L, jobExecutions.get(0).getId());
	}

	/* Tests for delete operations */

	@Test
	void deleteJobInstanceWithCorrectId() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());

		// when
		jobRepository.deleteJobInstance(jobInstance);

		// then
		assertTrue(jobRepository.findJobInstances(jobName).isEmpty());
		assertTrue(jobRepository.getJobInstances(jobName, 0, 10).isEmpty());
		assertNull(jobRepository.getJobInstance(1L));
		assertNull(jobRepository.getJobExecution(jobExecution.getId()));
	}

	@Test
	void deleteJobInstanceWithDifferentId() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());

		// when
		JobInstance differentJobInstance = new JobInstance(999L, jobName);
		jobRepository.deleteJobInstance(differentJobInstance);

		// then
		assertEquals(jobInstance, jobRepository.getJobInstance(1L));
		assertNotNull(jobRepository.getJobExecution(1L));
	}

	@Test
	void deleteJobExecutionWithCorrectId() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());

		// when
		jobRepository.deleteJobExecution(jobExecution);

		// then
		assertNull(jobRepository.getJobExecution(jobExecution.getId()));
	}

	@Test
	void deleteJobExecutionWithDifferentId() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());

		// when
		JobExecution differentJobExecution = new JobExecution(999L, jobInstance, jobParameters);
		jobRepository.deleteJobExecution(differentJobExecution);

		// then
		assertEquals(jobExecution, jobRepository.getJobExecution(jobExecution.getId()));
	}

}
