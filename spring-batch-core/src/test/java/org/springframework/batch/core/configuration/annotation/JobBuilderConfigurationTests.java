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

package org.springframework.batch.core.configuration.annotation;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.lang.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class JobBuilderConfigurationTests {

	public static boolean fail = false;

	@Test
	void testVanillaBatchConfiguration() throws Exception {
		testJob(BatchStatus.COMPLETED, 2, TestConfiguration.class);
	}

	@Test
	void testConfigurerAsBean() throws Exception {
		testJob(BatchStatus.COMPLETED, 1, BeansConfigurer.class);
	}

	@Test
	void testTwoConfigurations() throws Exception {
		testJob("testJob", BatchStatus.COMPLETED, 2, TestConfiguration.class, AnotherConfiguration.class);
	}

	@Test
	void testTwoConfigurationsAndBeansConfigurer() throws Exception {
		testJob("testJob", BatchStatus.COMPLETED, 2, TestConfiguration.class, BeansConfigurer.class);
	}

	private void testJob(BatchStatus status, int stepExecutionCount, Class<?> config) throws Exception {
		testJob(null, status, stepExecutionCount, config);
	}

	private void testJob(String jobName, BatchStatus status, int stepExecutionCount, Class<?>... config)
			throws Exception {

		Class<?>[] configs = new Class<?>[config.length + 1];
		System.arraycopy(config, 0, configs, 1, config.length);
		configs[0] = DataSourceConfiguration.class;
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configs);
		Job job = jobName == null ? context.getBean(Job.class) : context.getBean(jobName, Job.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		JobExecution execution = jobOperator.start(job,
				new JobParametersBuilder().addLong("run.id", (long) (Math.random() * Long.MAX_VALUE))
					.toJobParameters());
		assertEquals(status, execution.getStatus());
		assertEquals(stepExecutionCount, execution.getStepExecutions().size());
		context.close();

	}

	@Configuration
	@EnableBatchProcessing
	@Import(DataSourceConfiguration.class)
	public static class TestConfiguration {

		@Autowired
		private JobRepository jobRepository;

		@Autowired
		private JdbcTransactionManager transactionManager;

		@Bean
		public Job testJob() throws Exception {
			SimpleJobBuilder builder = new JobBuilder("test", this.jobRepository).start(step1()).next(step2());
			return builder.build();
		}

		@Bean
		protected Step step1() throws Exception {
			return new StepBuilder("step1", jobRepository).tasklet(tasklet(), this.transactionManager).build();
		}

		@Bean
		protected Step step2() throws Exception {
			return new StepBuilder("step2", jobRepository).tasklet(tasklet(), this.transactionManager).build();
		}

		@Bean
		protected Tasklet tasklet() {
			return new Tasklet() {
				@Nullable
				@Override
				public RepeatStatus execute(StepContribution contribution, ChunkContext context) throws Exception {
					if (fail) {
						throw new RuntimeException("Planned!");
					}
					return RepeatStatus.FINISHED;
				}
			};
		}

	}

	@Configuration
	@EnableBatchProcessing
	@Import(DataSourceConfiguration.class)
	public static class AnotherConfiguration {

		@Autowired
		private JdbcTransactionManager transactionManager;

		@Autowired
		private Tasklet tasklet;

		@Bean
		public Job anotherJob(JobRepository jobRepository) throws Exception {
			SimpleJobBuilder builder = new JobBuilder("another", jobRepository).start(step3(jobRepository));
			return builder.build();
		}

		@Bean
		protected Step step3(JobRepository jobRepository) throws Exception {
			return new StepBuilder("step3", jobRepository).tasklet(tasklet, this.transactionManager).build();
		}

	}

	@Configuration
	@EnableBatchProcessing
	@Import(DataSourceConfiguration.class)
	public static class BeansConfigurer {

		@Autowired
		private JdbcTransactionManager transactionManager;

		@Bean
		public Job beansConfigurerJob(JobRepository jobRepository) throws Exception {
			SimpleJobBuilder builder = new JobBuilder("beans", jobRepository).start(step1(jobRepository));
			return builder.build();
		}

		@Bean
		protected Step step1(JobRepository jobRepository) throws Exception {
			return new StepBuilder("step1", jobRepository).tasklet(new Tasklet() {

				@Nullable
				@Override
				public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
					return null;
				}
			}, this.transactionManager).build();
		}

	}

	@Configuration
	static class DataSourceConfiguration {

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
