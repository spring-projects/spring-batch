/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.step.item;

import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for the behavior of a multi-threaded TaskletStep.
 *
 * @author Mahmoud Ben Hassine
 */
public class MultiThreadedTaskletStepIntegrationTests {

	@Test
	public void testMultiThreadedTaskletExecutionWhenNoErrors() throws Exception {
		// given
		Class[] configurationClasses = {JobConfiguration.class, TransactionManagerConfiguration.class};
		ApplicationContext context = new AnnotationConfigApplicationContext(configurationClasses);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

		// then
		assertNotNull(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(0, stepExecution.getFailureExceptions().size());
	}

	@Test
	public void testMultiThreadedTaskletExecutionWhenCommitFails() throws Exception {
		// given
		Class[] configurationClasses = {JobConfiguration.class, CommitFailingTransactionManagerConfiguration.class};
		ApplicationContext context = new AnnotationConfigApplicationContext(configurationClasses);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

		// then
		assertNotNull(jobExecution);
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		Throwable e = stepExecution.getFailureExceptions().get(0);
		assertEquals("Planned commit exception!", e.getMessage());
		// No assertions on execution context because it is undefined in this case
	}

	@Test
	public void testMultiThreadedTaskletExecutionWhenRollbackFails() throws Exception {
		// given
		Class[] configurationClasses = {JobConfiguration.class, RollbackFailingTransactionManagerConfiguration.class};
		ApplicationContext context = new AnnotationConfigApplicationContext(configurationClasses);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);

		// when
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

		// then
		assertNotNull(jobExecution);
		assertEquals(BatchStatus.UNKNOWN, jobExecution.getStatus());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
		Throwable e = stepExecution.getFailureExceptions().get(0);
		assertEquals("Planned rollback exception!", e.getMessage());
		// No assertions on execution context because it is undefined in this case
	}

	@Configuration
	@EnableBatchProcessing
	public static class JobConfiguration {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;
		@Autowired
		private StepBuilderFactory stepBuilderFactory;

		@Bean
		public TaskletStep step() {
			return stepBuilderFactory.get("step")
					.<Integer, Integer>chunk(3)
					.reader(itemReader())
					.writer(itemWriter())
					.taskExecutor(taskExecutor())
					.build();
		}

		@Bean
		public Job job(ThreadPoolTaskExecutor taskExecutor) {
			return jobBuilderFactory.get("job")
					.start(step())
					.listener(new JobExecutionListenerSupport() {
						@Override
						public void afterJob(JobExecution jobExecution) {
							taskExecutor.shutdown();
						}
					})
					.build();
		}

		@Bean
		public ThreadPoolTaskExecutor taskExecutor() {
			ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
			taskExecutor.setCorePoolSize(3);
			taskExecutor.setMaxPoolSize(3);
			taskExecutor.setThreadNamePrefix("spring-batch-worker-thread-");
			return taskExecutor;
		}

		@Bean
		public ItemReader<Integer> itemReader() {
			return new ItemReader<Integer>() {
				private AtomicInteger atomicInteger = new AtomicInteger();

				@Override
				public synchronized Integer read() {
					int value = atomicInteger.incrementAndGet();
					return value <= 9 ? value : null;
				}
			};
		}

		@Bean
		public ItemWriter<Integer> itemWriter() {
			return items -> {
			};
		}
	}

	@Configuration
	public static class DataSourceConfiguration {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseType.HSQL)
					.addScript("org/springframework/batch/core/schema-drop-hsqldb.sql")
					.addScript("org/springframework/batch/core/schema-hsqldb.sql")
					.build();
		}

	}

	@Configuration
	@Import(DataSourceConfiguration.class)
	public static class TransactionManagerConfiguration {

		@Bean
		public PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

	}

	@Configuration
	@Import(DataSourceConfiguration.class)
	public static class CommitFailingTransactionManagerConfiguration {

		@Bean
		public PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource) {
				@Override
				protected void doCommit(DefaultTransactionStatus status) {
					super.doCommit(status);
					if (Thread.currentThread().getName().equals("spring-batch-worker-thread-2")) {
						throw new RuntimeException("Planned commit exception!");
					}
				}
			};
		}

	}

	@Configuration
	@Import(DataSourceConfiguration.class)
	public static class RollbackFailingTransactionManagerConfiguration {

		@Bean
		public PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource) {
				@Override
				protected void doCommit(DefaultTransactionStatus status) {
					super.doCommit(status);
					if (Thread.currentThread().getName().equals("spring-batch-worker-thread-2")) {
						throw new RuntimeException("Planned commit exception!");
					}
				}

				@Override
				protected void doRollback(DefaultTransactionStatus status) {
					super.doRollback(status);
					if (Thread.currentThread().getName().equals("spring-batch-worker-thread-2")) {
						throw new RuntimeException("Planned rollback exception!");
					}
				}
			};
		}

	}

}
