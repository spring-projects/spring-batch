/*
 * Copyright 2015-2025 the original author or authors.
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
package org.springframework.batch.core.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * BATCH-2322
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(classes = { ItemListenerErrorTests.BatchConfiguration.class })
class ItemListenerErrorTests {

	@Autowired
	private FailingListener listener;

	@Autowired
	private FailingItemReader reader;

	@Autowired
	private FailingItemProcessor processor;

	@Autowired
	private FailingItemWriter writer;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job job;

	@BeforeEach
	void setUp() {
		listener.setMethodToThrowExceptionFrom("");
		reader.setGoingToFail(false);
		processor.setGoingToFail(false);
		writer.setGoingToFail(false);
	}

	@Test
	@DirtiesContext
	void testOnWriteError() throws Exception {
		listener.setMethodToThrowExceptionFrom("onWriteError");
		writer.setGoingToFail(true);

		JobExecution execution = jobOperator.start(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Test
	@DirtiesContext
	void testOnReadError() throws Exception {
		listener.setMethodToThrowExceptionFrom("onReadError");
		reader.setGoingToFail(true);

		JobExecution execution = jobOperator.start(job, new JobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		StepExecution stepExecution = execution.getStepExecutions().iterator().next();
		assertEquals(0, stepExecution.getReadCount());
		assertEquals(50, stepExecution.getReadSkipCount());
		List<Throwable> failureExceptions = stepExecution.getFailureExceptions();
		assertEquals(1, failureExceptions.size());
		Throwable failureException = failureExceptions.iterator().next();
		assertEquals("Skip limit of '50' exceeded", failureException.getMessage());
		assertEquals("Error in onReadError.", failureException.getCause().getMessage());
		assertEquals("onReadError caused this Exception", failureException.getCause().getCause().getMessage());
	}

	@Test
	@DirtiesContext
	void testOnProcessError() throws Exception {
		listener.setMethodToThrowExceptionFrom("onProcessError");
		processor.setGoingToFail(true);

		JobExecution execution = jobOperator.start(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	public static class BatchConfiguration {

		@Bean
		public Job testJob(JobRepository jobRepository, Step testStep) {
			return new JobBuilder("testJob", jobRepository).start(testStep).build();
		}

		@Bean
		public Step step1(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				ItemReader<String> fakeItemReader, ItemProcessor<String, String> fakeProcessor,
				ItemWriter<String> fakeItemWriter, ItemProcessListener<String, String> itemProcessListener) {

			return new StepBuilder("testStep", jobRepository).<String, String>chunk(10, transactionManager)
				.reader(fakeItemReader)
				.processor(fakeProcessor)
				.writer(fakeItemWriter)
				.listener(itemProcessListener)
				.faultTolerant()
				.skipLimit(50)
				.skip(RuntimeException.class)
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

		@Bean
		public FailingListener itemListener() {
			return new FailingListener();
		}

		@Bean
		public FailingItemReader fakeReader() {
			return new FailingItemReader();
		}

		@Bean
		public FailingItemProcessor fakeProcessor() {
			return new FailingItemProcessor();
		}

		@Bean
		public FailingItemWriter fakeItemWriter() {
			return new FailingItemWriter();
		}

	}

	public static class FailingItemWriter implements ItemWriter<String> {

		private boolean goingToFail = false;

		@Override
		public void write(Chunk<? extends String> items) throws Exception {
			if (goingToFail) {
				throw new RuntimeException("failure in the writer");
			}
		}

		public void setGoingToFail(boolean goingToFail) {
			this.goingToFail = goingToFail;
		}

	}

	public static class FailingItemProcessor implements ItemProcessor<String, String> {

		private boolean goingToFail = false;

		@Override
		public @Nullable String process(String item) throws Exception {
			if (goingToFail) {
				throw new RuntimeException("failure in the processor");
			}
			else {
				return item;
			}
		}

		public void setGoingToFail(boolean goingToFail) {
			this.goingToFail = goingToFail;
		}

	}

	public static class FailingItemReader implements ItemReader<String> {

		private boolean goingToFail = false;

		private final ItemReader<String> delegate = new ListItemReader<>(Collections.singletonList("1"));

		private int count = 0;

		@Override
		public @Nullable String read() throws Exception {
			count++;
			if (goingToFail) {
				throw new RuntimeException("failure in the reader");
			}
			else {
				return delegate.read();
			}
		}

		public void setGoingToFail(boolean goingToFail) {
			this.goingToFail = goingToFail;
		}

		public int getCount() {
			return count;
		}

	}

	public static class FailingListener extends ItemListenerSupport<String, String> {

		private String methodToThrowExceptionFrom;

		public void setMethodToThrowExceptionFrom(String methodToThrowExceptionFrom) {
			this.methodToThrowExceptionFrom = methodToThrowExceptionFrom;
		}

		@Override
		public void beforeRead() {
			if (methodToThrowExceptionFrom.equals("beforeRead")) {
				throw new RuntimeException("beforeRead caused this Exception");
			}
		}

		@Override
		public void afterRead(String item) {
			if (methodToThrowExceptionFrom.equals("afterRead")) {
				throw new RuntimeException("afterRead caused this Exception");
			}
		}

		@Override
		public void onReadError(Exception ex) {
			if (methodToThrowExceptionFrom.equals("onReadError")) {
				throw new RuntimeException("onReadError caused this Exception");
			}
		}

		@Override
		public void beforeProcess(String item) {
			if (methodToThrowExceptionFrom.equals("beforeProcess")) {
				throw new RuntimeException("beforeProcess caused this Exception");
			}
		}

		@Override
		public void afterProcess(String item, @Nullable String result) {
			if (methodToThrowExceptionFrom.equals("afterProcess")) {
				throw new RuntimeException("afterProcess caused this Exception");
			}
		}

		@Override
		public void onProcessError(String item, Exception ex) {
			if (methodToThrowExceptionFrom.equals("onProcessError")) {
				throw new RuntimeException("onProcessError caused this Exception");
			}
		}

		@Override
		public void beforeWrite(Chunk<? extends String> items) {
			if (methodToThrowExceptionFrom.equals("beforeWrite")) {
				throw new RuntimeException("beforeWrite caused this Exception");
			}
		}

		@Override
		public void afterWrite(Chunk<? extends String> items) {
			if (methodToThrowExceptionFrom.equals("afterWrite")) {
				throw new RuntimeException("afterWrite caused this Exception");
			}
		}

		@Override
		public void onWriteError(Exception ex, Chunk<? extends String> item) {
			if (methodToThrowExceptionFrom.equals("onWriteError")) {
				throw new RuntimeException("onWriteError caused this Exception");
			}
		}

	}

}
