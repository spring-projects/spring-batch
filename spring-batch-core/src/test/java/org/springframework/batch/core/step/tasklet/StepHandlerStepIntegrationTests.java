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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.tasklet.StepHandlerStep;
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
public class StepHandlerStepIntegrationTests {

	private StepHandlerStep step;

	private Job job;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private DataSource dataSource;

	private JobRepository jobRepository;

	private RepeatTemplate chunkOperations;

	private ItemReader<String> getReader(String[] args) {
		return new ListItemReader<String>(Arrays.asList(args));
	}

	@Before
	public void onSetUp() throws Exception {
		MapJobInstanceDao.clear();
		MapStepExecutionDao.clear();
		MapJobExecutionDao.clear();

		JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
		jobRepositoryFactoryBean.setDatabaseType("hsql");
		jobRepositoryFactoryBean.setDataSource(dataSource);
		jobRepositoryFactoryBean.setTransactionManager(transactionManager);
		jobRepositoryFactoryBean.afterPropertiesSet();
		jobRepository = (JobRepository) jobRepositoryFactoryBean.getObject();

		step = new StepHandlerStep("stepName");
		step.setJobRepository(jobRepository);
		step.setTransactionManager(transactionManager);
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		step.setStepOperations(template);

		// Only process one item:
		chunkOperations = new RepeatTemplate();
		chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(1));

		job = new JobSupport("FOO");

		step.setTransactionManager(transactionManager);

	}

	@Test
	public void testStatusForCommitFailedException() throws Exception {

		step.setStepHandler(new SimpleStepHandler<String>(getReader(new String[] { "a", "b", "c" }),
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

		JobExecution jobExecution = jobRepository.createJobExecution(job, new JobParameters());
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		stepExecution.setExecutionContext(new ExecutionContext() {
			{
				put("foo", "bar");
			}
		});
		// step.setLastExecution(stepExecution);

		try {
			step.execute(stepExecution);
			fail("Expected BatchCriticalException");
		}
		catch (RuntimeException e) {

			assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
			StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobExecution.getJobInstance(), step);
			assertEquals(lastStepExecution, stepExecution);
			assertFalse(lastStepExecution == stepExecution);

			// If the StepExecution is not saved after the failure it will be
			// STARTED instead of UNKNOWN
			assertEquals(BatchStatus.UNKNOWN, lastStepExecution.getStatus());

			String msg = stepExecution.getExitStatus().getExitDescription();
			assertTrue(msg.contains("Fatal error detected during commit"));
			// The original rollback was caused by this one:
			assertEquals("Simulate commit failure", e.getCause().getMessage());

		}
	}

}
