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

package org.springframework.batch.core.launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.JobParametersInvalidException;
import org.springframework.batch.core.job.parameters.JobParametersValidator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.core.task.TaskRejectedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Lucas Ward
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 *
 */
class TaskExecutorJobLauncherTests {

	private TaskExecutorJobLauncher jobLauncher;

	private JobSupport job = new JobSupport("foo") {
		@Override
		public void execute(JobExecution execution) {
			execution.setExitStatus(ExitStatus.COMPLETED);
		}
	};

	private final JobParameters jobParameters = new JobParameters();

	private JobRepository jobRepository;

	@BeforeEach
	void setUp() {

		jobLauncher = new TaskExecutorJobLauncher();
		jobRepository = mock();
		jobLauncher.setJobRepository(jobRepository);

	}

	@Test
	void testRun() throws Exception {
		run(ExitStatus.COMPLETED);
	}

	@Test
	void testRunWithValidator() throws Exception {

		job.setJobParametersValidator(
				new DefaultJobParametersValidator(new String[] { "missing-and-required" }, new String[0]));

		when(jobRepository.getLastJobExecution(job.getName(), jobParameters)).thenReturn(null);

		jobLauncher.afterPropertiesSet();
		assertThrows(JobParametersInvalidException.class, () -> jobLauncher.run(job, jobParameters));

	}

	@Test
	void testRunRestartableJobInstanceTwice() throws Exception {
		job = new JobSupport("foo") {
			@Override
			public boolean isRestartable() {
				return true;
			}

			@Override
			public void execute(JobExecution execution) {
				execution.setExitStatus(ExitStatus.COMPLETED);
			}
		};

		testRun();
		when(jobRepository.getLastJobExecution(job.getName(), jobParameters))
			.thenReturn(new JobExecution(new JobInstance(1L, job.getName()), jobParameters));
		when(jobRepository.createJobExecution(job.getName(), jobParameters))
			.thenReturn(new JobExecution(new JobInstance(1L, job.getName()), jobParameters));
		jobLauncher.run(job, jobParameters);
	}

	/*
	 * Non-restartable JobInstance can be run only once - attempt to run existing
	 * non-restartable JobInstance causes error.
	 */
	@Test
	void testRunNonRestartableJobInstanceTwice() throws Exception {
		job = new JobSupport("foo") {
			@Override
			public boolean isRestartable() {
				return false;
			}

			@Override
			public void execute(JobExecution execution) {
				execution.setExitStatus(ExitStatus.COMPLETED);
			}
		};

		testRun();
		when(jobRepository.getLastJobExecution(job.getName(), jobParameters))
			.thenReturn(new JobExecution(new JobInstance(1L, job.getName()), jobParameters));
		assertThrows(JobRestartException.class, () -> jobLauncher.run(job, jobParameters));
	}

	@Test
	void testTaskExecutor() throws Exception {
		final List<String> list = new ArrayList<>();
		jobLauncher.setTaskExecutor(task -> {
			list.add("execute");
			task.run();
		});
		testRun();
		assertEquals(1, list.size());
	}

	@Test
	void testTaskExecutorRejects() throws Exception {

		final List<String> list = new ArrayList<>();
		jobLauncher.setTaskExecutor(task -> {
			list.add("execute");
			throw new TaskRejectedException("Planned failure");
		});

		JobExecution jobExecution = new JobExecution((JobInstance) null, (JobParameters) null);

		when(jobRepository.getLastJobExecution(job.getName(), jobParameters)).thenReturn(null);
		when(jobRepository.createJobExecution(job.getName(), jobParameters)).thenReturn(jobExecution);
		jobRepository.update(jobExecution);

		jobLauncher.afterPropertiesSet();
		try {
			jobLauncher.run(job, jobParameters);
		}
		finally {
			assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
			assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		}

		assertEquals(1, list.size());

	}

	@Test
	void testRunWithException() throws Exception {
		job = new JobSupport() {
			@Override
			public void execute(JobExecution execution) {
				execution.setExitStatus(ExitStatus.FAILED);
				throw new RuntimeException("foo");
			}
		};
		Exception exception = assertThrows(RuntimeException.class, () -> run(ExitStatus.FAILED));
		assertEquals("foo", exception.getMessage());
	}

	@Test
	void testRunWithError() {
		job = new JobSupport() {
			@Override
			public void execute(JobExecution execution) {
				execution.setExitStatus(ExitStatus.FAILED);
				throw new Error("foo");
			}
		};
		Error error = assertThrows(Error.class, () -> run(ExitStatus.FAILED));
		assertEquals("foo", error.getMessage());
	}

	@Test
	void testInitialiseWithoutRepository() {
		Exception exception = assertThrows(IllegalStateException.class,
				() -> new TaskExecutorJobLauncher().afterPropertiesSet());
		assertTrue(exception.getMessage().toLowerCase().contains("repository"),
				"Message did not contain repository: " + exception.getMessage());
	}

	@Test
	void testInitialiseWithRepository() throws Exception {
		jobLauncher = new TaskExecutorJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.afterPropertiesSet(); // no error
	}

	private void run(ExitStatus exitStatus) throws Exception {
		JobExecution jobExecution = new JobExecution((JobInstance) null, (JobParameters) null);

		when(jobRepository.getLastJobExecution(job.getName(), jobParameters)).thenReturn(null);
		when(jobRepository.createJobExecution(job.getName(), jobParameters)).thenReturn(jobExecution);

		jobLauncher.afterPropertiesSet();
		try {
			jobLauncher.run(job, jobParameters);
		}
		finally {
			assertEquals(exitStatus, jobExecution.getExitStatus());
		}
	}

	/**
	 * Test to support BATCH-1770 -> throw in parent thread JobRestartException when a
	 * stepExecution is UNKNOWN
	 */
	@Test
	void testRunStepStatusUnknown() {
		assertThrows(JobRestartException.class, () -> testRestartStepExecutionInvalidStatus(BatchStatus.UNKNOWN));
	}

	@Test
	void testRunStepStatusStarting() {
		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> testRestartStepExecutionInvalidStatus(BatchStatus.STARTING));
	}

	@Test
	void testRunStepStatusStarted() {
		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> testRestartStepExecutionInvalidStatus(BatchStatus.STARTED));
	}

	@Test
	void testRunStepStatusStopping() {
		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> testRestartStepExecutionInvalidStatus(BatchStatus.STOPPING));
	}

	private void testRestartStepExecutionInvalidStatus(BatchStatus status) throws Exception {
		String jobName = "test_job";
		JobRepository jobRepository = mock();
		JobParameters parameters = new JobParametersBuilder().addLong("runtime", System.currentTimeMillis())
			.toJobParameters();
		JobExecution jobExecution = mock();
		Job job = mock();
		JobParametersValidator validator = mock();
		StepExecution stepExecution = mock();

		when(job.getName()).thenReturn(jobName);
		when(job.isRestartable()).thenReturn(true);
		when(job.getJobParametersValidator()).thenReturn(validator);
		when(jobRepository.getLastJobExecution(jobName, parameters)).thenReturn(jobExecution);
		when(stepExecution.getStatus()).thenReturn(status);
		when(jobExecution.getStepExecutions()).thenReturn(Arrays.asList(stepExecution));

		// setup launcher
		jobLauncher = new TaskExecutorJobLauncher();
		jobLauncher.setJobRepository(jobRepository);

		// run
		jobLauncher.run(job, parameters);
	}

}
