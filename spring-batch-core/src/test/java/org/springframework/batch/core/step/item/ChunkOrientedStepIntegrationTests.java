/*
 * Copyright 2025-present the original author or authors.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * Integration tests of {@link ChunkOrientedStep}.
 *
 * @author Mahmoud Ben Hassine
 */
public class ChunkOrientedStepIntegrationTests {

	// TODO use parameterized tests for serial and concurrent steps
	// The outcome should be the same for both

	@Test
	void testChunkOrientedStepSuccess() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ChunkOrientedStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().addString("file", "data/persons.csv")
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
		Assertions.assertEquals(5, stepExecution.getReadCount());
		Assertions.assertEquals(5, stepExecution.getWriteCount());
		Assertions.assertEquals(3, stepExecution.getCommitCount());
		Assertions.assertEquals(0, stepExecution.getRollbackCount());
		Assertions.assertEquals(5, JdbcTestUtils.countRowsInTable(jdbcTemplate, "person_target"));
	}

	@Test
	void testConcurrentChunkOrientedStepSuccess() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ConcurrentChunkOrientedStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().addString("file", "data/persons.csv")
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
		Assertions.assertEquals(5, stepExecution.getReadCount());
		Assertions.assertEquals(5, stepExecution.getWriteCount());
		Assertions.assertEquals(3, stepExecution.getCommitCount());
		Assertions.assertEquals(0, stepExecution.getRollbackCount());
		Assertions.assertEquals(5, JdbcTestUtils.countRowsInTable(jdbcTemplate, "person_target"));
	}

	@Test
	void testChunkOrientedStepFailure() throws Exception {
		// given
		System.setProperty("fail", "true");
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ChunkOrientedStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().addString("file", "data/persons.csv")
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		ExitStatus stepExecutionExitStatus = stepExecution.getExitStatus();
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), stepExecutionExitStatus.getExitCode());
		Assertions.assertTrue(stepExecutionExitStatus.getExitDescription()
			.contains("Unable to process item Person[id=1, name=foo1]"));
		Assertions.assertEquals(2, stepExecution.getReadCount());
		Assertions.assertEquals(0, stepExecution.getWriteCount());
		Assertions.assertEquals(0, stepExecution.getCommitCount());
		Assertions.assertEquals(1, stepExecution.getRollbackCount());
		Assertions.assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "person_target"));
		System.clearProperty("fail");
	}

	@Test
	void testConcurrentChunkOrientedStepFailure() throws Exception {
		// given
		System.setProperty("fail", "true");
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class,
				ConcurrentChunkOrientedStepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParametersBuilder().addString("file", "data/persons.csv")
			.toJobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		ExitStatus stepExecutionExitStatus = stepExecution.getExitStatus();
		Assertions.assertEquals(ExitStatus.FAILED.getExitCode(), stepExecutionExitStatus.getExitCode());
		Assertions.assertTrue(stepExecutionExitStatus.getExitDescription()
			.contains("Unable to process item Person[id=1, name=foo1]"));
		Assertions.assertEquals(2, stepExecution.getReadCount());
		Assertions.assertEquals(0, stepExecution.getWriteCount());
		Assertions.assertEquals(0, stepExecution.getCommitCount());
		Assertions.assertEquals(1, stepExecution.getRollbackCount());
		Assertions.assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "person_target"));
		System.clearProperty("fail");
	}

	// Issue: https://github.com/spring-projects/spring-batch/issues/5126
	@Test
	void testChunkOrientedStepReExecution() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(StepConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);

		// when
		jobOperator.start(job, new JobParametersBuilder().addLong("run.id", 1L).toJobParameters());
		jobOperator.start(job, new JobParametersBuilder().addLong("run.id", 2L).toJobParameters());

		// then
		ListItemWriter<String> itemWriter = context.getBean(ListItemWriter.class);
		Assertions.assertEquals(2, itemWriter.getWrittenItems().size());
	}

	@Configuration
	static class ChunkOrientedStepConfiguration {

		@Bean
		public Step chunkOrientedStep(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				ItemReader<Person> itemReader, ItemProcessor<Person, Person> itemProcessor,
				ItemWriter<Person> itemWriter) {
			return new ChunkOrientedStepBuilder<Person, Person>(jobRepository, 2).reader(itemReader)
				.processor(itemProcessor)
				.writer(itemWriter)
				.transactionManager(transactionManager)
				.build();
		}

	}

	@Configuration
	static class ConcurrentChunkOrientedStepConfiguration {

		@Bean
		public Step concurrentChunkOrientedStep(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				ItemReader<Person> itemReader, ItemProcessor<Person, Person> itemProcessor,
				ItemWriter<Person> itemWriter) {
			return new ChunkOrientedStepBuilder<Person, Person>(jobRepository, 2).reader(itemReader)
				.processor(itemProcessor)
				.writer(itemWriter)
				.transactionManager(transactionManager)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.build();
		}

	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	@Import(JdbcInfrastructureConfiguration.class)
	static class StepConfiguration {

		// singleton-scoped item writer acting as a global collector
		// of written items across job executions
		@Bean
		public ListItemWriter<String> itemWriter() {
			return new ListItemWriter<>();
		}

		@Bean
		Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager,
				ListItemWriter<String> itemWriter) {
			ChunkOrientedStep<String, String> step = new StepBuilder("step", jobRepository).<String, String>chunk(1)
				.transactionManager(transactionManager)
				.reader(new SingleItemStreamReader<>("foo"))
				.writer(itemWriter)
				.build();
			return new JobBuilder(jobRepository).start(step).build();
		}

	}

}