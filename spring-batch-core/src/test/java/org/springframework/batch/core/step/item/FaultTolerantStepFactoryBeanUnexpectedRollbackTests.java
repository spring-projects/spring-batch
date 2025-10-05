/*
 * Copyright 2010-2023 the original author or authors.
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
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.factory.FaultTolerantStepFactoryBean;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionStatus;

import javax.sql.DataSource;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link FaultTolerantStepFactoryBean} with unexpected rollback.
 */
@SpringJUnitConfig(locations = "classpath:data-source-context.xml")
class FaultTolerantStepFactoryBeanUnexpectedRollbackTests {

	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private DataSource dataSource;

	@Test
	void testTransactionException() throws Exception {

		final SkipWriterStub<String> writer = new SkipWriterStub<>();
		FaultTolerantStepFactoryBean<String, String> factory = new FaultTolerantStepFactoryBean<>();
		factory.setItemWriter(writer);

		@SuppressWarnings("serial")
		JdbcTransactionManager transactionManager = new JdbcTransactionManager(dataSource) {
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

		JdbcJobRepositoryFactoryBean repositoryFactory = new JdbcJobRepositoryFactoryBean();
		repositoryFactory.setDataSource(dataSource);
		repositoryFactory.setTransactionManager(transactionManager);
		repositoryFactory.afterPropertiesSet();
		JobRepository repository = repositoryFactory.getObject();
		factory.setJobRepository(repository);

		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = repository.createJobInstance("job", jobParameters);
		JobExecution jobExecution = repository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
		StepExecution stepExecution = repository.createStepExecution(factory.getName(), jobExecution);

		Step step = factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());

		assertEquals("[]", writer.getCommitted().toString());
	}

}
