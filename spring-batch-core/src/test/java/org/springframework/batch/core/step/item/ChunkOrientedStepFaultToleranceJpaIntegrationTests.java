/*
 * Copyright 2026-present the original author or authors.
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

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * Integration tests for the fault-tolerance features of {@link ChunkOrientedStep} with a
 * JPA setup.
 *
 * @author Mahmoud Ben Hassine
 */
public class ChunkOrientedStepFaultToleranceJpaIntegrationTests {

	@Test
	void testFaultTolerantChunkOrientedStep() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		ExitStatus stepExecutionExitStatus = stepExecution.getExitStatus();
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), stepExecutionExitStatus.getExitCode());
		Assertions.assertEquals(6, stepExecution.getReadCount());
		Assertions.assertEquals(4, stepExecution.getWriteCount());
		Assertions.assertEquals(0, stepExecution.getReadSkipCount());
		Assertions.assertEquals(2, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(2, stepExecution.getSkipCount());
		Assertions.assertEquals(4, JdbcTestUtils.countRowsInTable(jdbcTemplate, "person_target"));
	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	@Import(JpaInfrastructureConfiguration.class)
	static class JobConfiguration {

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder(jobRepository).start(step).build();
		}

		@Bean
		public Step step(JobRepository jobRepository, JpaTransactionManager transactionManager,
				EntityManagerFactory entityManagerFactory) {
			// @formatter:off
			List<Person> items = List.of(
					new Person(1, "foo1"),
					new Person(2, "foooo2"), // this item will cause a write failure due to the name length constraint
					new Person(3, "foo3"),
					new Person(4, "foo4"),
					new Person(5, "foooo5"), // this item will cause a write failure due to the name length constraint
					new Person(6, "foo6"));
			// @formatter:on
			return new ChunkOrientedStepBuilder<Person, PersonEntity>(jobRepository, 3)
				.reader(new ListItemReader<>(items))
				.processor(item -> new PersonEntity(item.id(), item.name()))
				.writer(new JpaItemWriter<>(entityManagerFactory))
				.transactionManager(transactionManager)
				.faultTolerant()
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.skipLimit(10)
				.build();
		}

	}

}