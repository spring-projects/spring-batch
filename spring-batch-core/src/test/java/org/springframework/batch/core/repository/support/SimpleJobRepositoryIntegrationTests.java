/*
 * Copyright 2008-2022 the original author or authors.
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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Repository tests using JDBC DAOs (rather than mocks).
 *
 * @author Robert Kasanicky
 * @author Dimitrios Liapis
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(locations = "/org/springframework/batch/core/repository/dao/sql-dao-test.xml")
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
		builder.addString("stringKey", "stringValue").addLong("longKey", 1L).addDouble("doubleKey", 1.1)
				.addDate("dateKey", new Date(1L));
		JobParameters jobParams = builder.toJobParameters();

		JobExecution firstExecution = jobRepository.createJobExecution(job.getName(), jobParams);
		firstExecution.setStartTime(new Date());
		assertNotNull(firstExecution.getLastUpdated());

		assertEquals(job.getName(), firstExecution.getJobInstance().getJobName());

		jobRepository.update(firstExecution);
		firstExecution.setEndTime(new Date());
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
		firstExecution.setStartTime(new Date(0));
		firstExecution.setEndTime(new Date(1));
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
		firstJobExec.setStartTime(new Date(4));
		firstStepExec.setStartTime(new Date(5));
		firstStepExec.setStatus(BatchStatus.FAILED);
		firstStepExec.setEndTime(new Date(6));
		jobRepository.update(firstStepExec);
		firstJobExec.setStatus(BatchStatus.FAILED);
		firstJobExec.setEndTime(new Date(7));
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
		ExecutionContext ctx = new ExecutionContext() {
			{
				putLong("crashedPosition", 7);
			}
		};
		JobExecution jobExec = jobRepository.createJobExecution(job.getName(), jobParameters);
		jobExec.setStartTime(new Date(0));
		jobExec.setExecutionContext(ctx);
		Step step = new StepSupport("step1");
		StepExecution stepExec = new StepExecution(step.getName(), jobExec);
		stepExec.setExecutionContext(ctx);

		jobRepository.add(stepExec);

		StepExecution retrievedStepExec = jobRepository.getLastStepExecution(jobExec.getJobInstance(), step.getName());
		assertEquals(stepExec, retrievedStepExec);
		assertEquals(ctx, retrievedStepExec.getExecutionContext());

		// JobExecution retrievedJobExec =
		// jobRepository.getLastJobExecution(jobExec.getJobInstance());
		// assertEquals(jobExec, retrievedJobExec);
		// assertEquals(ctx, retrievedJobExec.getExecutionContext());
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
		jobExecution.setStartTime(new Date());
		jobRepository.update(jobExecution);

		assertThrows(JobExecutionAlreadyRunningException.class,
				() -> jobRepository.createJobExecution(job.getName(), jobParameters));
	}

	@Transactional
	@Test
	void testGetLastJobExecution() throws Exception {
		JobExecution jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		jobExecution.setStatus(BatchStatus.FAILED);
		jobExecution.setEndTime(new Date());
		jobRepository.update(jobExecution);
		Thread.sleep(10);
		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		StepExecution stepExecution = new StepExecution("step1", jobExecution);
		jobRepository.add(stepExecution);
		jobExecution.addStepExecutions(Arrays.asList(stepExecution));
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
		jobExecution1.setEndTime(new Date());
		jobRepository.update(jobExecution1);
		JobExecution jobExecution2 = jobRepository.createJobExecution(job.getName(), jobParameters);
		assertNotNull(jobExecution1);
		assertNotNull(jobExecution2);
	}

}
