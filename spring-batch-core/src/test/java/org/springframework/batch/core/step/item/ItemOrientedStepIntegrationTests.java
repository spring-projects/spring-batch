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
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.AbstractJobDaoTests;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.AbstractItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 * 
 */
public class ItemOrientedStepIntegrationTests extends AbstractDependencyInjectionSpringContextTests {

	private List processed = new ArrayList();

	private ItemOrientedStep step;

	private Job job;

	private PlatformTransactionManager transactionManager;

	private DataSource dataSource;

	private JobRepository jobRepository;

	/**
	 * Public setter for the PlatformTransactionManager.
	 * @param transactionManager the transactionManager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Public setter for the DataSource.
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	private ItemReader getReader(String[] args) {
		return new ListItemReader(Arrays.asList(args));
	}

	protected void onSetUp() throws Exception {
		MapJobInstanceDao.clear();
		MapStepExecutionDao.clear();
		MapJobExecutionDao.clear();

		JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
		jobRepositoryFactoryBean.setDatabaseType("hsql");
		jobRepositoryFactoryBean.setDataSource(dataSource);
		jobRepositoryFactoryBean.setTransactionManager(transactionManager);
		jobRepositoryFactoryBean.afterPropertiesSet();
		jobRepository = (JobRepository) jobRepositoryFactoryBean.getObject();
		RepeatTemplate template;

		step = new ItemOrientedStep("stepName");
		step.setJobRepository(jobRepository);
		step.setTransactionManager(transactionManager);
		template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		step.setStepOperations(template);

		// Only process one item:
		template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		step.setChunkOperations(template);

		job = new JobSupport("FOO");

		step.setTransactionManager(transactionManager);

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.AbstractSingleSpringContextTests#getConfigLocations()
	 */
	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(AbstractJobDaoTests.class, "sql-dao-test.xml") };
	}

	public void testStatusForCommitFailedException() throws Exception {

		step.setItemHandler(new SimpleItemHandler(getReader(new String[] { "a", "b", "c" }), new AbstractItemWriter() {
			public void write(Object data) throws Exception {
				processed.add((String) data);
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
					public void beforeCommit(boolean readOnly) {
						throw new RuntimeException("Simulate commit failure");
					}
				});
			}
		}));

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
