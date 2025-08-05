/*
 * Copyright 2014-2022 the original author or authors.
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
package org.springframework.batch.test;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.lang.Nullable;

import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author mminella
 * @author Mahmoud Ben Hassine
 */
class JobLauncherTestUtilsTests {

	@Test
	void testStepExecutionWithJavaConfig() {
		ApplicationContext context = new AnnotationConfigApplicationContext(TestJobConfiguration.class);

		JobLauncherTestUtils testUtils = context.getBean(JobLauncherTestUtils.class);

		JobExecution execution = testUtils.launchStep("step1");

		assertEquals(ExitStatus.COMPLETED, execution.getExitStatus());
	}

	@Test
	void getUniqueJobParameters_doesNotRepeatJobParameters() {
		ApplicationContext context = new AnnotationConfigApplicationContext(TestJobConfiguration.class);
		JobLauncherTestUtils testUtils = context.getBean(JobLauncherTestUtils.class);
		Set<JobParameters> jobParametersSeen = new HashSet<>();
		for (int i = 0; i < 10_000; i++) {
			JobParameters jobParameters = testUtils.getUniqueJobParameters();
			assertFalse(jobParametersSeen.contains(jobParameters));
			jobParametersSeen.add(jobParameters);
		}
	}

	@Configuration
	@EnableBatchProcessing
	static class TestJobConfiguration {

		@Bean
		public Step step(JobRepository jobRepository) {
			return new StepBuilder("step1", jobRepository).tasklet(new Tasklet() {
				@Nullable
				@Override
				public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
					return null;
				}
			}, transactionManager(dataSource())).build();
		}

		@Bean
		public Job job(JobRepository jobRepository) {
			return new JobBuilder("job", jobRepository).flow(step(jobRepository)).end().build();
		}

		@Bean
		public JobLauncherTestUtils testUtils(Job jobUnderTest, JobRepository jobRepository, JobLauncher jobLauncher) {
			JobLauncherTestUtils jobLauncherTestUtils = new JobLauncherTestUtils();
			jobLauncherTestUtils.setJob(jobUnderTest);
			jobLauncherTestUtils.setJobRepository(jobRepository);
			jobLauncherTestUtils.setJobLauncher(jobLauncher);

			return jobLauncherTestUtils;
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
