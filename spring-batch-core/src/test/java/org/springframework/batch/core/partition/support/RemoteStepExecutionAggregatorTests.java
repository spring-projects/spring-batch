/*
 * Copyright 2011-2021 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RemoteStepExecutionAggregatorTests {

	private RemoteStepExecutionAggregator aggregator = new RemoteStepExecutionAggregator();

	private JobExecution jobExecution;

	private StepExecution result;

	private StepExecution stepExecution1;

	private StepExecution stepExecution2;

	@Before
	public void init() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
				.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.build();
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(new DataSourceTransactionManager(embeddedDatabase));
		factory.afterPropertiesSet();
		JobRepository jobRepository = factory.getObject();
		JobExplorerFactoryBean explorerFactoryBean = new JobExplorerFactoryBean();
		explorerFactoryBean.setDataSource(embeddedDatabase);
		explorerFactoryBean.afterPropertiesSet();
		aggregator.setJobExplorer(explorerFactoryBean.getObject());
		jobExecution = jobRepository.createJobExecution("job", new JobParameters());
		result = jobExecution.createStepExecution("aggregate");
		stepExecution1 = jobExecution.createStepExecution("foo:1");
		stepExecution2 = jobExecution.createStepExecution("foo:2");
		jobRepository.add(stepExecution1);
		jobRepository.add(stepExecution2);
	}

	@Test
	public void testAggregateEmpty() {
		aggregator.aggregate(result, Collections.<StepExecution> emptySet());
	}

	@Test
	public void testAggregateNull() {
		aggregator.aggregate(result, null);
	}

	@Test
	public void testAggregateStatusSunnyDay() {
		stepExecution1.setStatus(BatchStatus.COMPLETED);
		stepExecution2.setStatus(BatchStatus.COMPLETED);
		aggregator.aggregate(result, Arrays.<StepExecution> asList(stepExecution1, stepExecution2));
		assertNotNull(result);
		assertEquals(BatchStatus.STARTING, result.getStatus());
	}

	@Test(expected=IllegalStateException.class)
	public void testAggregateStatusMissingExecution() {
		stepExecution2 = jobExecution.createStepExecution("foo:3");
		stepExecution1.setStatus(BatchStatus.COMPLETED);
		stepExecution2.setStatus(BatchStatus.COMPLETED);
		aggregator.aggregate(result, Arrays.<StepExecution> asList(stepExecution1, stepExecution2));
		assertNotNull(result);
		assertEquals(BatchStatus.STARTING, result.getStatus());
	}

}
