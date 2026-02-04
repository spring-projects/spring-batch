/*
 * Copyright 2008-2025 the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Abstract repository tests using DAOs (rather than mocks).
 *
 * @author Robert Kasanicky
 * @author Dimitrios Liapis
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 * @author Andrey Litvitski
 */
abstract class AbstractJobRepositoryIntegrationTests {

	@Autowired
	private JobRepository jobRepository;

	private final JobSupport job = new JobSupport("SimpleJobRepositoryIntegrationTestsJob");

	private JobParameters jobParameters = new JobParameters();

	/*
	 * Create two job executions for same job+parameters tuple. Check both executions
	 * belong to the same job instance and job.
	 */
	@Test
	void testCreateAndFind() {

		job.setRestartable(true);

		JobParametersBuilder builder = new JobParametersBuilder();
		builder.addString("stringKey", "stringValue").addLong("longKey", 1L).addDouble("doubleKey", 1.1);
		jobParameters = builder.toJobParameters();

		ExecutionContext executionContext = new ExecutionContext();
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		JobExecution firstExecution = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);
		firstExecution.setStartTime(LocalDateTime.now());
		assertNull(firstExecution.getLastUpdated());

		assertEquals(job.getName(), firstExecution.getJobInstance().getJobName());

		jobRepository.update(firstExecution);
		firstExecution.setStatus(BatchStatus.FAILED);
		firstExecution.setEndTime(LocalDateTime.now());
		jobRepository.update(firstExecution);
		JobExecution secondExecution = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);

		assertEquals(firstExecution.getJobInstance(), secondExecution.getJobInstance());
		assertEquals(job.getName(), secondExecution.getJobInstance().getJobName());
	}

	/*
	 * Create two job executions for same job+parameters tuple. Check both executions
	 * belong to the same job instance and job.
	 */
	@Test
	void testCreateAndFindWithNoStartDate() {
		job.setRestartable(true);

		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		JobExecution firstExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		LocalDateTime now = LocalDateTime.now();
		firstExecution.setStartTime(now);
		firstExecution.setEndTime(now.plus(1, ChronoUnit.SECONDS));
		firstExecution.setStatus(BatchStatus.COMPLETED);
		jobRepository.update(firstExecution);
		JobExecution secondExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());

		assertEquals(firstExecution.getJobInstance(), secondExecution.getJobInstance());
		assertEquals(job.getName(), secondExecution.getJobInstance().getJobName());
	}

	/*
	 * Save multiple StepExecutions for the same step and check the returned count and
	 * last execution are correct.
	 */
	@Test
	void testGetStepExecutionCountAndLastStepExecution() throws Exception {
		job.setRestartable(true);
		StepSupport step = new StepSupport("restartedStep");

		ExecutionContext executionContext = new ExecutionContext();
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		// first execution
		JobExecution firstJobExec = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);
		StepExecution firstStepExec = jobRepository.createStepExecution(step.getName(), firstJobExec);

		assertEquals(1, jobRepository.getStepExecutionCount(firstJobExec.getJobInstance(), step.getName()));
		assertEquals(firstStepExec, jobRepository.getLastStepExecution(firstJobExec.getJobInstance(), step.getName()));

		// first execution failed
		LocalDateTime now = LocalDateTime.now();
		firstJobExec.setStartTime(now);
		firstStepExec.setStartTime(now.plus(1, ChronoUnit.SECONDS));
		firstStepExec.setStatus(BatchStatus.FAILED);
		firstStepExec.setEndTime(now.plus(2, ChronoUnit.SECONDS));
		jobRepository.update(firstStepExec);
		firstJobExec.setStatus(BatchStatus.FAILED);
		firstJobExec.setEndTime(now.plus(3, ChronoUnit.SECONDS));
		jobRepository.update(firstJobExec);

		// second execution
		JobExecution secondJobExec = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);
		StepExecution secondStepExec = jobRepository.createStepExecution(step.getName(), firstJobExec);

		assertEquals(2, jobRepository.getStepExecutionCount(secondJobExec.getJobInstance(), step.getName()));
		assertEquals(secondStepExec,
				jobRepository.getLastStepExecution(secondJobExec.getJobInstance(), step.getName()));
	}

	/*
	 * Save execution context and retrieve it.
	 */
	@Test
	void testSaveExecutionContext() {
		ExecutionContext ctx = new ExecutionContext(Map.of("crashedPosition", 7));
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		JobExecution jobExec = jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
		jobExec.setStartTime(LocalDateTime.now());
		jobExec.setExecutionContext(ctx);
		jobRepository.updateExecutionContext(jobExec);
		Step step = new StepSupport("step1");
		StepExecution stepExec = jobRepository.createStepExecution(step.getName(), jobExec);
		stepExec.setExecutionContext(ctx);
		jobRepository.updateExecutionContext(stepExec);

		StepExecution retrievedStepExec = jobRepository.getLastStepExecution(jobExec.getJobInstance(), step.getName());
		assertEquals(stepExec, retrievedStepExec);
		assertEquals(ctx, retrievedStepExec.getExecutionContext());

		retrievedStepExec = jobRepository.getStepExecution(stepExec.getId());
		assertEquals(stepExec, retrievedStepExec);
		assertEquals(ctx, retrievedStepExec.getExecutionContext());
	}

	/*
	 * If JobExecution is already running, exception will be thrown in attempt to create
	 * new execution.
	 */
	@Test
	@Disabled("JobExecutionAlreadyRunningException is not thrown at repository level")
	void testOnlyOneJobExecutionAllowedRunning() {
		job.setRestartable(true);
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());

		// simulating a running job execution
		jobExecution.setStartTime(LocalDateTime.now());
		jobRepository.update(jobExecution);

		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext()));
	}

	@Test
	void testGetLastJobExecution() throws Exception {
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		jobExecution.setStatus(BatchStatus.FAILED);
		jobExecution.setEndTime(LocalDateTime.now());
		jobRepository.update(jobExecution);
		Thread.sleep(10);
		jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
		StepExecution stepExecution = jobRepository.createStepExecution("step1", jobExecution);
		jobExecution.addStepExecutions(List.of(stepExecution));
		assertEquals(jobExecution, jobRepository.getLastJobExecution(job.getName(), jobParameters));
		assertEquals(stepExecution, jobExecution.getStepExecutions().iterator().next());
	}

	/*
	 * Create two job executions for the same job+parameters tuple. Should ignore
	 * non-identifying job parameters when identifying the job instance.
	 */
	@Test
	void testReExecuteWithSameJobParameters() {
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo", false).toJobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		JobExecution jobExecution1 = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		jobExecution1.setStatus(BatchStatus.COMPLETED);
		jobExecution1.setEndTime(LocalDateTime.now());
		jobRepository.update(jobExecution1);
		JobExecution jobExecution2 = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		assertNotNull(jobExecution1);
		assertNotNull(jobExecution2);
	}

	/*
	 * When a job execution is running, JobExecutionAlreadyRunningException should be
	 * thrown if trying to create any other ones with same job parameters.
	 */
	@Test
	@Disabled("JobExecutionAlreadyRunningException is not thrown at repository level")
	void testReExecuteWithSameJobParametersWhenRunning() {
		JobParameters jobParameters = new JobParametersBuilder().addString("stringKey", "stringValue")
			.toJobParameters();

		// jobExecution with status STARTING
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext()));

		// jobExecution with status STARTED
		jobExecution.setStatus(BatchStatus.STARTED);
		jobExecution.setStartTime(LocalDateTime.now());
		jobRepository.update(jobExecution);
		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext()));

		// jobExecution with status STOPPING
		jobExecution.setStatus(BatchStatus.STOPPING);
		jobRepository.update(jobExecution);
		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext()));
	}

	@Test
	void testDeleteJobInstance() {
		var jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		var jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
		jobRepository.createStepExecution("step", jobExecution);

		jobRepository.deleteJobInstance(jobExecution.getJobInstance());

		assertEquals(0, jobRepository.findJobInstances(job.getName()).size());
		assertNull(jobRepository.getLastJobExecution(job.getName(), jobParameters));
	}

	@Test
	void testUpdateResetsDirtyFlag() {
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		JobExecution jobExec = jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
		jobExec.setStartTime(LocalDateTime.now());

		ExecutionContext ctx = new ExecutionContext();
		ctx.put("crashedPosition", 7);
		jobExec.setExecutionContext(ctx);
		assertTrue(ctx.isDirty());
		jobRepository.updateExecutionContext(jobExec);
		assertFalse(ctx.isDirty());

		Step step = new StepSupport("step1");
		StepExecution stepExec = jobRepository.createStepExecution(step.getName(), jobExec);
		ctx = new ExecutionContext(ctx);
		ctx.put("crashedPosition", 8);
		stepExec.setExecutionContext(ctx);
		assertTrue(ctx.isDirty());
		jobRepository.updateExecutionContext(stepExec);
		assertFalse(ctx.isDirty());
	}

}
