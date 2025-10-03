/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.core.step.tasklet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * @author Dave Syer
 *
 */
@SpringJUnitConfig(locations = "/org/springframework/batch/core/repository/dao/jdbc/sql-dao-test.xml")
class ChunkOrientedStepIntegrationTests {

	private TaskletStep step;

	private Job job;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JobRepository jobRepository;

	private RepeatTemplate chunkOperations;

	private ItemReader<String> getReader(String[] args) {
		return new ListItemReader<>(Arrays.asList(args));
	}

	@BeforeEach
	void onSetUp() {

		step = new TaskletStep("stepName");
		step.setJobRepository(jobRepository);
		step.setTransactionManager(transactionManager);

		// Only process one item:
		chunkOperations = new RepeatTemplate();
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(1));

		job = new JobSupport("FOO");

		step.setTransactionManager(transactionManager);

	}

	@Test
	@Disabled
	void testStatusForCommitFailedException() throws Exception {

		step.setTasklet(new TestingChunkOrientedTasklet<>(getReader(new String[] { "a", "b", "c" }),
				data -> TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					@Override
					public void beforeCommit(boolean readOnly) {
						throw new RuntimeException("Simulate commit failure");
					}
				}), chunkOperations));

		JobParameters jobParameters = new JobParameters(
				Set.of(new JobParameter("run.id", getClass().getName() + ".1", Long.class)));
		JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		StepExecution stepExecution = jobRepository.createStepExecution(step.getName(), jobExecution);

		stepExecution.setExecutionContext(new ExecutionContext() {
			{
				put("foo", "bar");
			}
		});

		step.execute(stepExecution);
		// Exception on commit is not necessarily fatal: it should fail and rollback
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobExecution.getJobInstance(),
				step.getName());
		assertEquals(lastStepExecution, stepExecution);
		assertNotSame(lastStepExecution, stepExecution);

		// If the StepExecution is not saved after the failure it will be
		// STARTED instead of FAILED
		assertEquals(BatchStatus.FAILED, lastStepExecution.getStatus());
		// The original rollback was caused by this one:
		assertEquals("Simulate commit failure", stepExecution.getFailureExceptions().get(0).getMessage());

	}

}
