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

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Repository tests using JDBC DAOs (rather than mocks).
 *
 * @author Robert Kasanicky
 * @author Dimitrios Liapis
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(locations = "/org/springframework/batch/core/repository/dao/jdbc/sql-dao-test.xml")
class SimpleJobRepositoryIntegrationTests {

	@Autowired
	private SimpleJobRepository jobRepository;

	private final JobSupport job = new JobSupport("SimpleJobRepositoryIntegrationTestsJob");

	private final JobParameters jobParameters = new JobParameters();

	/*
	 * Create two job executions for same job+parameters tuple. Check both executions
	 * belong to the same job instance and job.
	 */
	@Transactional
	@Test
	void testCreateAndFind() throws Exception {

		job.setRestartable(true);

		JobParametersBuilder builder = new JobParametersBuilder();
		builder.addString("stringKey", "stringValue").addLong("longKey", 1L).addDouble("doubleKey", 1.1);
		JobParameters jobParams = builder.toJobParameters();

		JobExecution firstExecution = jobRepository.createJobExecution(job.getName(), jobParams);
		firstExecution.setStartTime(LocalDateTime.now());
		assertNotNull(firstExecution.getLastUpdated());

		assertEquals(job.getName(), firstExecution.getJobInstance().getJobName());

		jobRepository.update(firstExecution);
		firstExecution.setStatus(BatchStatus.FAILED);
		firstExecution.setEndTime(LocalDateTime.now());
		jobRepository.update(firstExecution);
		JobExecution secondExecution = jobRepository.createJobExecution(job.getName(), jobParams);

		assertEquals(firstExecution.getJobInstance(), secondExecution.getJobInstance());
		assertEquals(job.getName(), secondExecution.getJobInstance().getJobName());
	}

	/*
	 * Create two job executions for same job+parameters tuple. Check both executions
	 * belong to the same job instance and job.
	 */
	@Transactional
	@Test
	void testCreateAndFindWithNoStartDate() throws Exception {
		job.setRestartable(true);

		JobExecution firstExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		LocalDateTime now = LocalDateTime.now();
		firstExecution.setStartTime(now);
		firstExecution.setEndTime(now.plus(1, ChronoUnit.SECONDS));
		firstExecution.setStatus(BatchStatus.COMPLETED);
		jobRepository.update(firstExecution);
		JobExecution secondExecution = jobRepository.createJobExecution(job.getName(), jobParameters);

		assertEquals(firstExecution.getJobInstance(), secondExecution.getJobInstance());
		assertEquals(job.getName(), secondExecution.getJobInstance().getJobName());
	}

	/*
	 * Save multiple StepExecutions for the same step and check the returned count and
	 * last execution are correct.
	 */
	@Transactional
	@Test
	void testGetStepExecutionCountAndLastStepExecution() throws Exception {
		job.setRestartable(true);
		StepSupport step = new StepSupport("restartedStep");

		// first execution
		JobExecution firstJobExec = jobRepository.createJobExecution(job.getName(), jobParameters);
		StepExecution firstStepExec = new StepExecution(step.getName(), firstJobExec);
		jobRepository.add(firstStepExec);

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
		JobExecution secondJobExec = jobRepository.createJobExecution(job.getName(), jobParameters);
		StepExecution secondStepExec = new StepExecution(step.getName(), secondJobExec);
		jobRepository.add(secondStepExec);

		assertEquals(2, jobRepository.getStepExecutionCount(secondJobExec.getJobInstance(), step.getName()));
		assertEquals(secondStepExec,
				jobRepository.getLastStepExecution(secondJobExec.getJobInstance(), step.getName()));
	}

	/*
	 * Save execution context and retrieve it.
	 */
	@Transactional
	@Test
	void testSaveExecutionContext() throws Exception {
		ExecutionContext ctx = new ExecutionContext(Map.of("crashedPosition", 7));
		JobExecution jobExec = jobRepository.createJobExecution(job.getName(), jobParameters);
		jobExec.setStartTime(LocalDateTime.now());
		jobExec.setExecutionContext(ctx);
		Step step = new StepSupport("step1");
		StepExecution stepExec = new StepExecution(step.getName(), jobExec);
		stepExec.setExecutionContext(ctx);

		jobRepository.add(stepExec);

		StepExecution retrievedStepExec = jobRepository.getLastStepExecution(jobExec.getJobInstance(), step.getName());
		assertEquals(stepExec, retrievedStepExec);
		assertEquals(ctx, retrievedStepExec.getExecutionContext());
	}

	/*
	 * If JobExecution is already running, exception will be thrown in attempt to create
	 * new execution.
	 */
	@Transactional
	@Test
	void testOnlyOneJobExecutionAllowedRunning() throws Exception {
		job.setRestartable(true);
		JobExecution jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);

		// simulating a running job execution
		jobExecution.setStartTime(LocalDateTime.now());
		jobRepository.update(jobExecution);

		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> jobRepository.createJobExecution(job.getName(), jobParameters));
	}

	@Transactional
	@Test
	void testGetLastJobExecution() throws Exception {
		JobExecution jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		jobExecution.setStatus(BatchStatus.FAILED);
		jobExecution.setEndTime(LocalDateTime.now());
		jobRepository.update(jobExecution);
		Thread.sleep(10);
		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		StepExecution stepExecution = new StepExecution("step1", jobExecution);
		jobRepository.add(stepExecution);
		jobExecution.addStepExecutions(List.of(stepExecution));
		assertEquals(jobExecution, jobRepository.getLastJobExecution(job.getName(), jobParameters));
		assertEquals(stepExecution, jobExecution.getStepExecutions().iterator().next());
	}

	/*
	 * Create two job executions for the same job+parameters tuple. Should ignore
	 * non-identifying job parameters when identifying the job instance.
	 */
	@Transactional
	@Test
	void testReExecuteWithSameJobParameters() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo", false).toJobParameters();
		JobExecution jobExecution1 = jobRepository.createJobExecution(job.getName(), jobParameters);
		jobExecution1.setStatus(BatchStatus.COMPLETED);
		jobExecution1.setEndTime(LocalDateTime.now());
		jobRepository.update(jobExecution1);
		JobExecution jobExecution2 = jobRepository.createJobExecution(job.getName(), jobParameters);
		assertNotNull(jobExecution1);
		assertNotNull(jobExecution2);
	}

	/*
	 * When a job execution is running, JobExecutionAlreadyRunningException should be
	 * thrown if trying to create any other ones with same job parameters.
	 */
	@Transactional
	@Test
	void testReExecuteWithSameJobParametersWhenRunning() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder().addString("stringKey", "stringValue")
			.toJobParameters();

		// jobExecution with status STARTING
		JobExecution jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> jobRepository.createJobExecution(job.getName(), jobParameters));

		// jobExecution with status STARTED
		jobExecution.setStatus(BatchStatus.STARTED);
		jobExecution.setStartTime(LocalDateTime.now());
		jobRepository.update(jobExecution);
		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> jobRepository.createJobExecution(job.getName(), jobParameters));

		// jobExecution with status STOPPING
		jobExecution.setStatus(BatchStatus.STOPPING);
		jobRepository.update(jobExecution);
		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> jobRepository.createJobExecution(job.getName(), jobParameters));
	}

	@Transactional
	@Test
	void testDeleteJobInstance() throws Exception {
		var jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		var jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		var stepExecution = new StepExecution("step", jobExecution);
		jobRepository.add(stepExecution);

		jobRepository.deleteJobInstance(jobExecution.getJobInstance());

		assertEquals(0, jobRepository.getJobInstances(job.getName(), 0, 1).size());
		assertNull(jobRepository.getLastJobExecution(job.getName(), jobParameters));
	}

}
