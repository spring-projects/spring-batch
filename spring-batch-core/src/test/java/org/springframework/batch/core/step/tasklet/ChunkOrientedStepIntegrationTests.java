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
package org.springframework.batch.core.step.tasklet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Dave Syer
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/core/repository/dao/sql-dao-test.xml")
public class ChunkOrientedStepIntegrationTests {

	private TaskletStep step;

	private Job job;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JobRepository jobRepository;

	private RepeatTemplate chunkOperations;

	private ItemReader<String> getReader(String[] args) {
		return new ListItemReader<String>(Arrays.asList(args));
	}

	@Before
	public void onSetUp() throws Exception {

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
	public void testStatusForCommitFailedException() throws Exception {

		step.setTasklet(new TestingChunkOrientedTasklet<String>(getReader(new String[] { "a", "b", "c" }),
				new ItemWriter<String>() {
					public void write(List<? extends String> data) throws Exception {
						TransactionSynchronizationManager
								.registerSynchronization(new TransactionSynchronizationAdapter() {
									public void beforeCommit(boolean readOnly) {
										throw new RuntimeException("Simulate commit failure");
									}
								});
					}
				}, chunkOperations));

		JobExecution jobExecution = jobRepository.createJobExecution(job.getName(), new JobParameters(Collections
				.singletonMap("run.id", new JobParameter(getClass().getName() + ".1"))));
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		stepExecution.setExecutionContext(new ExecutionContext() {
			{
				put("foo", "bar");
			}
		});

		jobRepository.add(stepExecution);
		step.execute(stepExecution);
		// Exception on commit is not necessarily fatal: it should fail and rollback
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName());
		assertEquals(lastStepExecution, stepExecution);
		assertFalse(lastStepExecution == stepExecution);

		// If the StepExecution is not saved after the failure it will be
		// STARTED instead of FAILED
		assertEquals(BatchStatus.FAILED, lastStepExecution.getStatus());
		// The original rollback was caused by this one:
		assertEquals("Simulate commit failure", stepExecution.getFailureExceptions().get(0).getMessage());

	}

}
