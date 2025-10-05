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
package org.springframework.batch.core.launch.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * @author Dave Syer
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @author Jinwoo Bae
 * @author Yejeong Ham
 */
class TaskExecutorJobOperatorTests {

	private TaskExecutorJobOperator jobOperator;

	private Job job;

	private JobRepository jobRepository;

	private JobRegistry jobRegistry;

	@BeforeEach
	void setUp() throws Exception {
		EmbeddedDatabase database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.addScript("/org/springframework/batch/core/schema-drop-h2.sql")
			.addScript("/org/springframework/batch/core/schema-h2.sql")
			.build();
		JdbcTransactionManager transactionManager = new JdbcTransactionManager(database);

		JdbcJobRepositoryFactoryBean jobRepositoryFactoryBean = new JdbcJobRepositoryFactoryBean();
		jobRepositoryFactoryBean.setDataSource(database);
		jobRepositoryFactoryBean.setTransactionManager(transactionManager);
		jobRepositoryFactoryBean.afterPropertiesSet();
		jobRepository = jobRepositoryFactoryBean.getObject();

		job = new JobBuilder("job", jobRepository)
			.start(new StepBuilder("step", jobRepository).tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
				.build())
			.build();

		jobRegistry = new MapJobRegistry();
		jobRegistry.register(job);

		jobOperator = new TaskExecutorJobOperator();
		jobOperator.setJobRepository(jobRepository);
		jobOperator.setJobRegistry(jobRegistry);
		jobOperator.afterPropertiesSet();
	}

	@Test
	void testStart() throws JobInstanceAlreadyCompleteException, NoSuchJobException,
			JobExecutionAlreadyRunningException, InvalidJobParametersException, JobRestartException {
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		Assertions.assertNotNull(jobExecution);
		Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Test
	void testRestart() throws Exception {
		Tasklet tasklet = new Tasklet() {
			boolean executed = false;

			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				if (!executed) {
					executed = true;
					throw new RuntimeException("Planned failure");
				}
				return RepeatStatus.FINISHED;
			}
		};
		job = new JobBuilder("job", jobRepository)
			.start(new StepBuilder("step", jobRepository).tasklet(tasklet).build())
			.build();

		JobParameters jobParameters = new JobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		Assertions.assertNotNull(jobExecution);
		Assertions.assertEquals(BatchStatus.FAILED, jobExecution.getStatus());

		jobExecution = jobOperator.restart(jobExecution);

		Assertions.assertNotNull(jobExecution);
		Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

}
