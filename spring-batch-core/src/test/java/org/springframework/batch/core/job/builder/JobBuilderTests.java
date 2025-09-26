/*
 * Copyright 2020-2025 the original author or authors.
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
package org.springframework.batch.core.job.builder;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.JobParameters;

import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mahmoud Ben Hassine
 * @author Minkuk Jo
 */
class JobBuilderTests {

	@Test
	void testListeners() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(MyJobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		assertEquals(1, AnnotationBasedJobExecutionListener.beforeJobCount);
		assertEquals(1, AnnotationBasedJobExecutionListener.afterJobCount);
		assertEquals(1, InterfaceBasedJobExecutionListener.beforeJobCount);
		assertEquals(1, InterfaceBasedJobExecutionListener.afterJobCount);

	}

	@Test
	void testJobDescription() {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(MyJobConfiguration.class);
		JobRepository jobRepository = context.getBean(JobRepository.class);
		PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
		Step step = new StepBuilder("step", jobRepository)
			.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
			.build();

		// when
		Job job = new JobBuilder("job", jobRepository).description("This is a test job").start(step).build();

		// then
		assertThat(job).isInstanceOf(SimpleJob.class);
		assertThat(((SimpleJob) job).getDescription()).isEqualTo("This is a test job");
	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class MyJobConfiguration {

		@Bean
		public Job job(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new JobBuilder("job", jobRepository).listener(new InterfaceBasedJobExecutionListener())
				.listener(new AnnotationBasedJobExecutionListener())
				.start(new StepBuilder("step", jobRepository)
					.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
					.build())
				.build();
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

	static class InterfaceBasedJobExecutionListener implements JobExecutionListener {

		public static int beforeJobCount = 0;

		public static int afterJobCount = 0;

		@Override
		public void beforeJob(JobExecution jobExecution) {
			beforeJobCount++;
		}

		@Override
		public void afterJob(JobExecution jobExecution) {
			afterJobCount++;
		}

	}

	static class AnnotationBasedJobExecutionListener {

		public static int beforeJobCount = 0;

		public static int afterJobCount = 0;

		@BeforeJob
		public void beforeJob(JobExecution jobExecution) {
			beforeJobCount++;
		}

		@AfterJob
		public void afterJob(JobExecution jobExecution) {
			afterJobCount++;
		}

	}

}