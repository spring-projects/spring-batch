/*
 * Copyright 2026 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TaskExecutorJobLauncher}.
 *
 * @author Jeongwook Ko
 */
@SuppressWarnings("removal")
class TaskExecutorJobLauncherTests {

	private final JobRepository jobRepository = mock();

	private TaskExecutorJobLauncher jobLauncher;

	@BeforeEach
	void setUp() throws Exception {
		jobLauncher = new TaskExecutorJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.afterPropertiesSet();
	}

	@Test
	void testRunWithRunningJobExecution() {
		// given
		String jobName = "job";
		JobParameters jobParameters = new JobParameters();
		Job job = mock();
		Mockito.when(job.getName()).thenReturn(jobName);
		JobInstance jobInstance = new JobInstance(1L, jobName);
		JobExecution runningJobExecution = new JobExecution(2L, jobInstance, jobParameters);
		runningJobExecution.setStatus(BatchStatus.STARTED);
		Mockito.when(jobRepository.getJobInstance(jobName, jobParameters)).thenReturn(jobInstance);
		Mockito.when(jobRepository.getJobExecutions(jobInstance)).thenReturn(List.of(runningJobExecution));

		// when
		JobExecutionAlreadyRunningException exception = Assertions
			.assertThrows(JobExecutionAlreadyRunningException.class, () -> jobLauncher.run(job, jobParameters));

		// then
		Assertions.assertEquals("A job execution for this job is already running: " + runningJobExecution,
				exception.getMessage());
		// the message should report the running job execution, not the job instance
		Assertions.assertTrue(exception.getMessage().contains("status=" + BatchStatus.STARTED));
	}

}
