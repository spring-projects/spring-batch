/*
 * Copyright 2015-2019 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * BATCH-2322
 *
 * @author Michael Minella
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ItemListenerErrorTests.BatchConfiguration.class})
public class ItemListenerErrorTests {

	@Autowired
	private FailingListener listener;

	@Autowired
	private FailingItemReader reader;

	@Autowired
	private FailingItemProcessor processor;

	@Autowired
	private FailingItemWriter writer;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	@Before
	public void setUp() {
		listener.setMethodToThrowExceptionFrom("");
		reader.setGoingToFail(false);
		processor.setGoingToFail(false);
		writer.setGoingToFail(false);
	}

	@Test
	@DirtiesContext
	public void testOnWriteError() throws Exception {
		listener.setMethodToThrowExceptionFrom("onWriteError");
		writer.setGoingToFail(true);

		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Ignore
	@Test
	@DirtiesContext
	public void testOnReadError() throws Exception {
		listener.setMethodToThrowExceptionFrom("onReadError");
		reader.setGoingToFail(true);

		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Test
	@DirtiesContext
	public void testOnProcessError() throws Exception {
		listener.setMethodToThrowExceptionFrom("onProcessError");
		processor.setGoingToFail(true);

		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Configuration
	@EnableBatchProcessing
	public static class BatchConfiguration {

		@Bean
		public Job testJob(JobBuilderFactory jobs, Step testStep) {
			return jobs.get("testJob")
					.incrementer(new RunIdIncrementer())
					.start(testStep)
					.build();
		}

		@Bean
		public Step step1(StepBuilderFactory stepBuilderFactory,
				ItemReader<String> fakeItemReader,
				ItemProcessor<String, String> fakeProcessor,
				ItemWriter<String> fakeItemWriter,
				ItemProcessListener<String, String> itemProcessListener) {

			return stepBuilderFactory.get("testStep").<String, String>chunk(10)
					.reader(fakeItemReader)
					.processor(fakeProcessor)
					.writer(fakeItemWriter)
					.listener(itemProcessListener)
					.faultTolerant().skipLimit(50).skip(RuntimeException.class)
					.build();
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
		public void write(List<? extends String> items) throws Exception {
			if(goingToFail) {
				throw new RuntimeException("failure in the writer");
			}
			else {
				for (String item : items) {
					System.out.println(item);
				}
			}
		}

		public void setGoingToFail(boolean goingToFail) {
			this.goingToFail = goingToFail;
		}
	}

	public static class FailingItemProcessor implements ItemProcessor<String, String> {

		private boolean goingToFail = false;

		@Nullable
		@Override
		public String process(String item) throws Exception {
			if(goingToFail) {
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

		private ItemReader<String> delegate = new ListItemReader<>(Collections.singletonList("1"));

		private int count = 0;

		@Nullable
		@Override
		public String read() throws Exception {
			count++;
			if(goingToFail) {
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
		public void beforeWrite(List<? extends String> items) {
			if (methodToThrowExceptionFrom.equals("beforeWrite")) {
				throw new RuntimeException("beforeWrite caused this Exception");
			}
		}

		@Override
		public void afterWrite(List<? extends String> items) {
			if (methodToThrowExceptionFrom.equals("afterWrite")) {
				throw new RuntimeException("afterWrite caused this Exception");
			}
		}

		@Override
		public void onWriteError(Exception ex, List<? extends String> item) {
			if (methodToThrowExceptionFrom.equals("onWriteError")) {
				throw new RuntimeException("onWriteError caused this Exception");
			}
		}
	}
}
