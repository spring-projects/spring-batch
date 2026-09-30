/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.batch.core.partition.support;

import org.springframework.batch.infrastructure.support.DatabaseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RemoteStepExecutionAggregatorTests {

	private RemoteStepExecutionAggregator aggregator;

	private StepExecution result;

	private StepExecution stepExecution1;

	private StepExecution stepExecution2;

	private JobRepository jobRepository;

	private JobInstance jobInstance;

	@BeforeEach
	void init() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript(DatabaseType.HSQL.getProductSchemaDrop())
			.addScript(DatabaseType.HSQL.getProductSchema())
			.generateUniqueName(true)
			.build();
		JdbcTransactionManager transactionManager = new JdbcTransactionManager(embeddedDatabase);
		JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(transactionManager);
		factory.afterPropertiesSet();
		jobRepository = factory.getObject();
		aggregator = new RemoteStepExecutionAggregator(jobRepository);
		JobParameters jobParameters = new JobParameters();
		jobInstance = jobRepository.createJobInstance("job", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		result = jobRepository.createStepExecution("aggregate", jobExecution);
		stepExecution1 = jobRepository.createStepExecution("foo:1", jobExecution);
		stepExecution2 = jobRepository.createStepExecution("foo:2", jobExecution);
	}

	@Test
	void testAggregateEmpty() {
		aggregator.aggregate(result, Collections.<StepExecution>emptySet());
	}

	@Test
	void testAggregateNull() {
		aggregator.aggregate(result, null);
	}

	@Test
	void testAggregateStatusSunnyDay() {
		stepExecution1.setStatus(BatchStatus.COMPLETED);
		stepExecution2.setStatus(BatchStatus.COMPLETED);
		aggregator.aggregate(result, Arrays.<StepExecution>asList(stepExecution1, stepExecution2));
		assertNotNull(result);
		assertEquals(BatchStatus.STARTING, result.getStatus());
	}

	/**
	 * A partition that completed in a previous run of the same job instance is not part
	 * of the current job execution, so there is nothing to refresh: it must be aggregated
	 * as is rather than filtered out.
	 */
	@Test
	void testAggregateStepExecutionFromAnotherJobExecution() {
		JobExecution otherJobExecution = jobRepository.createJobExecution(jobInstance, new JobParameters(),
				new ExecutionContext());
		StepExecution completedPartition = jobRepository.createStepExecution("foo:3", otherJobExecution);
		completedPartition.setStatus(BatchStatus.COMPLETED);
		completedPartition.setWriteCount(5);
		completedPartition.setEndTime(LocalDateTime.now());
		jobRepository.update(completedPartition);

		aggregator.aggregate(result, List.of(completedPartition));

		assertEquals(5, result.getWriteCount());
	}

}
