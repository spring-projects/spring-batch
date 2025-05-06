/*
 * Copyright 2012-2023 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.PostConstruct;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.configuration.support.ApplicationContextFactory;
import org.springframework.batch.core.configuration.support.AutomaticJobRegistrar;
import org.springframework.batch.core.configuration.support.GenericApplicationContextFactory;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class JobLoaderConfigurationTests {

	@Test
	void testJobLoader() throws Exception {
		testJob("test", BatchStatus.COMPLETED, 2, LoaderFactoryConfiguration.class);
	}

	@Test
	void testJobLoaderWithArray() throws Exception {
		testJob("test", BatchStatus.COMPLETED, 2, LoaderRegistrarConfiguration.class);
	}

	private void testJob(String jobName, BatchStatus status, int stepExecutionCount, Class<?>... config)
			throws Exception {

		Class<?>[] configs = new Class<?>[config.length + 1];
		System.arraycopy(config, 0, configs, 1, config.length);
		configs[0] = DataSourceConfiguration.class;
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configs);
		Job job = jobName == null ? context.getBean(Job.class) : context.getBean(JobLocator.class).getJob(jobName);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		JobExecution execution = jobLauncher.run(job,
				new JobParametersBuilder().addLong("run.id", (long) (Math.random() * Long.MAX_VALUE))
					.toJobParameters());
		assertEquals(status, execution.getStatus());
		assertEquals(stepExecutionCount, execution.getStepExecutions().size());
		JobExplorer jobExplorer = context.getBean(JobExplorer.class);
		assertEquals(1, jobExplorer.getJobInstanceCount(jobName));
		context.close();

	}

	@Configuration
	@EnableBatchProcessing(modular = true)
	public static class LoaderFactoryConfiguration {

		@Bean
		public ApplicationContextFactory jobContextFactory() {
			return new GenericApplicationContextFactory(TestConfiguration.class);

		}

		@Bean
		public ApplicationContextFactory vanillaContextFactory() {
			return new GenericApplicationContextFactory(VanillaConfiguration.class);
		}

	}

	@Configuration
	@EnableBatchProcessing(modular = true)
	public static class LoaderRegistrarConfiguration {

		@Autowired
		private AutomaticJobRegistrar registrar;

		@PostConstruct
		public void initialize() {
			registrar.addApplicationContextFactory(new GenericApplicationContextFactory(TestConfiguration.class));
			registrar.addApplicationContextFactory(new GenericApplicationContextFactory(VanillaConfiguration.class));
		}

	}

	@Configuration
	public static class TestConfiguration {

		@Bean
		public Job testJob(JobRepository jobRepository) throws Exception {
			SimpleJobBuilder builder = new JobBuilder("test", jobRepository).start(step1(jobRepository))
				.next(step2(jobRepository));
			return builder.build();
		}

		@Bean
		protected Step step1(JobRepository jobRepository) throws Exception {
			return new StepBuilder("step1", jobRepository).tasklet(tasklet(), new ResourcelessTransactionManager())
				.build();
		}

		@Bean
		protected Step step2(JobRepository jobRepository) throws Exception {
			return new StepBuilder("step2", jobRepository).tasklet(tasklet(), new ResourcelessTransactionManager())
				.build();
		}

		@Bean
		protected Tasklet tasklet() {
			return new Tasklet() {
				@Nullable
				@Override
				public RepeatStatus execute(StepContribution contribution, ChunkContext context) throws Exception {
					return RepeatStatus.FINISHED;
				}
			};
		}

	}

	@Configuration
	public static class VanillaConfiguration {

		@Bean
		public Job vanillaJob(JobRepository jobRepository) throws Exception {
			SimpleJobBuilder builder = new JobBuilder("vanilla", jobRepository).start(step3(jobRepository));
			return builder.build();
		}

		@Bean
		protected Step step3(JobRepository jobRepository) throws Exception {
			return new StepBuilder("step3", jobRepository).tasklet(new Tasklet() {
				@Nullable
				@Override
				public RepeatStatus execute(StepContribution contribution, ChunkContext context) throws Exception {
					return RepeatStatus.FINISHED;
				}
			}, new ResourcelessTransactionManager()).build();
		}

	}

}
