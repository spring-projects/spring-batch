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
package org.springframework.batch.core.job;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

/**
 * Test suite for various failure scenarios during job processing.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class SimpleJobFailureTests {

	private final SimpleJob job = new SimpleJob("job");

	private JobExecution execution;

	@BeforeEach
	void init() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.build();
		JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(new JdbcTransactionManager(embeddedDatabase));
		factory.afterPropertiesSet();
		JobRepository jobRepository = factory.getObject();
		job.setJobRepository(jobRepository);
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("job", jobParameters);
		execution = jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
	}

	@Test
	void testStepFailure() {
		job.setSteps(Arrays.<Step>asList(new StepSupport("step")));
		job.execute(execution);
		assertEquals(BatchStatus.FAILED, execution.getStatus());
	}

	@Test
	void testStepStatusUnknown() {
		job.setSteps(Arrays.<Step>asList(new StepSupport("step1") {
			@Override
			public void execute(StepExecution stepExecution)
					throws JobInterruptedException, UnexpectedJobExecutionException {
				// This is what happens if the repository meta-data cannot be updated
				stepExecution.setStatus(BatchStatus.UNKNOWN);
				stepExecution.setTerminateOnly();
			}
		}, new StepSupport("step2")));
		job.execute(execution);
		assertEquals(BatchStatus.UNKNOWN, execution.getStatus());
		assertEquals(1, execution.getStepExecutions().size());
	}

}
