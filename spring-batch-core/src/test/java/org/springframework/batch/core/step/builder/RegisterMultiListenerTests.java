/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.PooledEmbeddedDataSource;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.lang.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for registering a listener class that implements different listeners interfaces
 * just once in java based configuration.
 *
 * @author Tobias Flohre
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
class RegisterMultiListenerTests {

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	@Autowired
	private CallChecker callChecker;

	private GenericApplicationContext context;

	@AfterEach
	void tearDown() {
		jobLauncher = null;
		job = null;
		callChecker = null;

		if (context != null) {
			context.close();
		}
	}

	/*
	 * The times the beforeChunkCalled occurs are: - Before chunk 1 (item1, item2) -
	 * Before the re-attempt of item1 (scanning) - Before the re-attempt of item2
	 * (scanning) - Before the checking that scanning is complete - Before chunk 2 (item3,
	 * item4) - Before chunk 3 (null)
	 */
	@Test
	void testMultiListenerFaultTolerantStep() throws Exception {
		bootstrap(MultiListenerFaultTolerantTestConfiguration.class);

		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, callChecker.beforeStepCalled);
		assertEquals(6, callChecker.beforeChunkCalled);
		assertEquals(2, callChecker.beforeWriteCalled);
		assertEquals(1, callChecker.skipInWriteCalled);
	}

	@Test
	void testMultiListenerSimpleStep() throws Exception {
		bootstrap(MultiListenerTestConfiguration.class);

		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		assertEquals(1, callChecker.beforeStepCalled);
		assertEquals(1, callChecker.beforeChunkCalled);
		assertEquals(1, callChecker.beforeWriteCalled);
		assertEquals(0, callChecker.skipInWriteCalled);
	}

	private void bootstrap(Class<?> configurationClass) {
		context = new AnnotationConfigApplicationContext(configurationClass);
		context.getAutowireCapableBeanFactory()
			.autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
	}

	public static abstract class MultiListenerTestConfigurationSupport {

		@Bean
		public Job testJob(JobRepository jobRepository) {
			return new JobBuilder("testJob", jobRepository).start(step(jobRepository)).build();
		}

		@Bean
		public CallChecker callChecker() {
			return new CallChecker();
		}

		@Bean
		public MultiListener listener() {
			return new MultiListener(callChecker());
		}

		@Bean
		public ItemReader<String> reader() {
			return new ItemReader<>() {

				private int count = 0;

				@Nullable
				@Override
				public String read()
						throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
					count++;

					if (count < 5) {
						return "item" + count;
					}
					else {
						return null;
					}
				}

			};
		}

		@Bean
		public ItemWriter<String> writer() {
			return chunk -> {
				if (chunk.getItems().contains("item2")) {
					throw new MySkippableException();
				}
			};
		}

		public abstract Step step(JobRepository jobRepository);

	}

	@Configuration
	@EnableBatchProcessing
	public static class MultiListenerFaultTolerantTestConfiguration extends MultiListenerTestConfigurationSupport {

		@Bean
		public DataSource dataSource() {
			return new PooledEmbeddedDataSource(new EmbeddedDatabaseBuilder()
				.addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql")
				.setType(EmbeddedDatabaseType.HSQL)
				.generateUniqueName(true)
				.build());
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Override
		@Bean
		public Step step(JobRepository jobRepository) {
			return new StepBuilder("step", jobRepository).listener(listener())
				.<String, String>chunk(2, transactionManager(dataSource()))
				.reader(reader())
				.writer(writer())
				.faultTolerant()
				.skipLimit(1)
				.skip(MySkippableException.class)
				// ChunkListener registered twice for checking BATCH-2149
				.listener((ChunkListener) listener())
				.build();
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class MultiListenerTestConfiguration extends MultiListenerTestConfigurationSupport {

		@Bean
		public DataSource dataSource() {
			return new PooledEmbeddedDataSource(new EmbeddedDatabaseBuilder()
				.addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql")
				.setType(EmbeddedDatabaseType.HSQL)
				.generateUniqueName(true)
				.build());
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Override
		@Bean
		public Step step(JobRepository jobRepository) {
			return new StepBuilder("step", jobRepository).listener(listener())
				.<String, String>chunk(2, transactionManager(dataSource()))
				.reader(reader())
				.writer(writer())
				.build();
		}

	}

	private static class CallChecker {

		int beforeStepCalled = 0;

		int beforeChunkCalled = 0;

		int beforeWriteCalled = 0;

		int skipInWriteCalled = 0;

	}

	private static class MultiListener
			implements StepExecutionListener, ChunkListener, ItemWriteListener<String>, SkipListener<String, String> {

		private final CallChecker callChecker;

		private MultiListener(CallChecker callChecker) {
			super();
			this.callChecker = callChecker;
		}

		@Override
		public void onSkipInRead(Throwable t) {
		}

		@Override
		public void onSkipInWrite(String item, Throwable t) {
			callChecker.skipInWriteCalled++;
		}

		@Override
		public void onSkipInProcess(String item, Throwable t) {
		}

		@Override
		public void beforeWrite(Chunk<? extends String> items) {
			callChecker.beforeWriteCalled++;
		}

		@Override
		public void afterWrite(Chunk<? extends String> items) {
		}

		@Override
		public void onWriteError(Exception exception, Chunk<? extends String> items) {
		}

		@Override
		public void beforeChunk(ChunkContext context) {
			callChecker.beforeChunkCalled++;
		}

		@Override
		public void afterChunk(ChunkContext context) {
		}

		@Override
		public void afterChunkError(ChunkContext context) {
		}

		@Override
		public void beforeStep(StepExecution stepExecution) {
			callChecker.beforeStepCalled++;
		}

		@Nullable
		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			return null;
		}

	}

	private static class MySkippableException extends RuntimeException {

		private static final long serialVersionUID = 1L;

	}

}
