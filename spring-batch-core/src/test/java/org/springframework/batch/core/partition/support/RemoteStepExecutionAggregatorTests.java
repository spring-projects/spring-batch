/*
 * Copyright 2011-2025 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RemoteStepExecutionAggregatorTests {

	private RemoteStepExecutionAggregator aggregator;

	private StepExecution result;

	private StepExecution stepExecution1;

	private StepExecution stepExecution2;

	@BeforeEach
	void init() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.build();
		JdbcTransactionManager transactionManager = new JdbcTransactionManager(embeddedDatabase);
		JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(transactionManager);
		factory.afterPropertiesSet();
		JobRepository jobRepository = factory.getObject();
		aggregator = new RemoteStepExecutionAggregator(jobRepository);
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("job", jobParameters);
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

}
