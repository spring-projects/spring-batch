/*
 * Copyright 2010-2014 the original author or authors.
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
package org.springframework.batch.core.step.item;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.factory.FaultTolerantStepFactoryBean;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionStatus;

import javax.sql.DataSource;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link FaultTolerantStepFactoryBean} with unexpected rollback.
 */
@ContextConfiguration(locations="classpath:/org/springframework/batch/core/repository/dao/data-source-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class FaultTolerantStepFactoryBeanUnexpectedRollbackTests {

	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private DataSource dataSource;

	@Test
	@Ignore //FIXME
	public void testTransactionException() throws Exception {

		final SkipWriterStub<String> writer = new SkipWriterStub<>();
		FaultTolerantStepFactoryBean<String, String> factory = new FaultTolerantStepFactoryBean<>();
		factory.setItemWriter(writer);

		@SuppressWarnings("serial")
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource) {
			private boolean failed = false;
			@Override
			protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
				if (writer.getWritten().isEmpty() || failed || !isExistingTransaction(status.getTransaction())) {
					super.doCommit(status);
					return;
				}
				failed = true;
				status.setRollbackOnly();
				super.doRollback(status);
				throw new UnexpectedRollbackException("Planned");
			}
		};

		factory.setBeanName("stepName");
		factory.setTransactionManager(transactionManager);
		factory.setCommitInterval(2);

		ItemReader<String> reader = new ListItemReader<>(Arrays.asList("1", "2"));
		factory.setItemReader(reader);

		JobRepositoryFactoryBean repositoryFactory = new JobRepositoryFactoryBean();
		repositoryFactory.setDataSource(dataSource);
		repositoryFactory.setTransactionManager(transactionManager);
		repositoryFactory.afterPropertiesSet();
		JobRepository repository = repositoryFactory.getObject();
		factory.setJobRepository(repository);

		JobExecution jobExecution = repository.createJobExecution("job", new JobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution(factory.getName());
		repository.add(stepExecution);

		Step step = factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());

		assertEquals("[]", writer.getCommitted().toString());
	}

}
