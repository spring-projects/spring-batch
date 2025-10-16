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
package org.springframework.batch.core.repository;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.dao.JacksonExecutionContextStringSerializer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JacksonExecutionContextStringSerializerIntegrationTests {

	@Test
	void testExecutionContextSerializationDeserializationRoundTrip() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JobParameters jobParameters = new JobParameters();

		// when
		JobExecution jobExecution = jobOperator.start(job, jobParameters);
		JobExecution restartedExecution = jobOperator.restart(jobExecution);

		// then
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		assertEquals(true, jobExecution.getStepExecutions().iterator().next().getExecutionContext().get("failed"));
		assertEquals(BatchStatus.COMPLETED, restartedExecution.getStatus());
		assertEquals(false,
				restartedExecution.getStepExecutions().iterator().next().getExecutionContext().get("failed"));

	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository(executionContextSerializerRef = "serializer")
	static class JobConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, Tasklet tasklet) {
			return new StepBuilder("step", jobRepository).tasklet(tasklet).build();
		}

		@Bean
		public Tasklet tasklet() {
			return new Tasklet() {
				private boolean shouldFail = true;

				@Override
				public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
					if (shouldFail) {
						shouldFail = false;
						contribution.getStepExecution().getExecutionContext().put("failed", true);
						throw new Exception("Expected failure");
					}
					System.out.println("Hello world!");
					contribution.getStepExecution().getExecutionContext().put("failed", false);
					return RepeatStatus.FINISHED;
				}
			};
		}

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder("job", jobRepository).start(step).build();
		}

		@Bean
		public JacksonExecutionContextStringSerializer serializer() {
			return new JacksonExecutionContextStringSerializer();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}
