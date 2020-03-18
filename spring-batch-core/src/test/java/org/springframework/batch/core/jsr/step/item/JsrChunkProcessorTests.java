/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.batch.core.jsr.step.item;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.batch.core.jsr.step.builder.JsrSimpleStepBuilder;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.lang.Nullable;

public class JsrChunkProcessorTests {

	private FailingListItemReader reader;
	private FailingCountingItemProcessor processor;
	private StoringItemWriter writer;
	private CountingListener readListener;
	private JsrSimpleStepBuilder<String, String> builder;
	private JobRepository repository;
	private StepExecution stepExecution;

	@Before
	public void setUp() throws Exception {

		List<String> items = new ArrayList<>();

		for (int i = 0; i < 25; i++) {
			items.add("item " + i);
		}

		reader = new FailingListItemReader(items);
		processor = new FailingCountingItemProcessor();
		writer = new StoringItemWriter();
		readListener = new CountingListener();

		builder = new JsrSimpleStepBuilder<>(new StepBuilder("step1"));
		builder.setBatchPropertyContext(new BatchPropertyContext());
		repository = new MapJobRepositoryFactoryBean().getObject();
		builder.repository(repository);
		builder.transactionManager(new ResourcelessTransactionManager());
		stepExecution = null;
	}

	@Test
	public void testNoInputNoListeners() throws Exception{
		reader = new FailingListItemReader(new ArrayList<>());
		Step step = builder.chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) readListener).build();

		runStep(step);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(0, processor.count);
		assertEquals(0, writer.results.size());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(0, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
	}

	@Test
	public void testSimpleScenarioNoListeners() throws Exception{
		Step step = builder.chunk(25).reader(reader).processor(processor).writer(writer).build();

		runStep(step);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(25, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(25, writer.results.size());
		assertEquals(25, processor.count);

		int count = 0;
		for (String curItem : writer.results) {
			assertEquals("item " + count, curItem);
			count++;
		}
	}

	@Test
	public void testSimpleScenarioNoProcessor() throws Exception{
		Step step = builder.chunk(25).reader(reader).writer(writer).listener((ItemReadListener<String>) readListener).build();

		runStep(step);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(25, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(0, readListener.afterProcess);
		assertEquals(25, readListener.afterRead);
		assertEquals(1, readListener.afterWrite);
		assertEquals(0, readListener.beforeProcess);
		assertEquals(26, readListener.beforeRead);
		assertEquals(1, readListener.beforeWriteCount);
		assertEquals(0, readListener.onProcessError);
		assertEquals(0, readListener.onReadError);
		assertEquals(0, readListener.onWriteError);
		assertEquals(0, processor.count);

		int count = 0;
		for (String curItem : writer.results) {
			assertEquals("item " + count, curItem);
			count++;
		}
	}

	@Test
	public void testProcessorFilteringNoListeners() throws Exception{
		processor.filter = true;
		Step step = builder.chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) readListener).build();

		runStep(step);

		int count = 0;
		for (String curItem : writer.results) {
			assertEquals("item " + count, curItem);
			count += 2;
		}

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(13, stepExecution.getWriteCount());
		assertEquals(12, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(25, processor.count);
	}

	@Test
	public void testReadError() throws Exception{
		reader.failCount = 10;

		Step step = builder.chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) readListener).build();

		runStep(step);

		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(9, processor.count);
		assertEquals(0, writer.results.size());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(9, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(1,	stepExecution.getFailureExceptions().size());
		assertEquals("expected at read index 10", stepExecution.getFailureExceptions().get(0).getMessage());
		assertEquals(9, readListener.afterProcess);
		assertEquals(9, readListener.afterRead);
		assertEquals(0, readListener.afterWrite);
		assertEquals(9, readListener.beforeProcess);
		assertEquals(10, readListener.beforeRead);
		assertEquals(0, readListener.beforeWriteCount);
		assertEquals(0, readListener.onProcessError);
		assertEquals(1, readListener.onReadError);
		assertEquals(0, readListener.onWriteError);
	}

	@Test
	public void testProcessError() throws Exception{
		processor.failCount = 10;

		Step step = builder.chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) readListener).build();

		runStep(step);

		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(10, processor.count);
		assertEquals(0, writer.results.size());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(10, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals("expected at process index 10", stepExecution.getFailureExceptions().get(0).getMessage());
		assertEquals(9, readListener.afterProcess);
		assertEquals(10, readListener.afterRead);
		assertEquals(0, readListener.afterWrite);
		assertEquals(10, readListener.beforeProcess);
		assertEquals(10, readListener.beforeRead);
		assertEquals(0, readListener.beforeWriteCount);
		assertEquals(1, readListener.onProcessError);
		assertEquals(0, readListener.onReadError);
		assertEquals(0, readListener.onWriteError);
	}

	@Test
	public void testWriteError() throws Exception{
		writer.fail = true;

		Step step = builder.chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) readListener).build();

		runStep(step);

		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(25, processor.count);
		assertEquals(0, writer.results.size());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals("expected in write", stepExecution.getFailureExceptions().get(0).getMessage());
		assertEquals(25, readListener.afterProcess);
		assertEquals(25, readListener.afterRead);
		assertEquals(0, readListener.afterWrite);
		assertEquals(25, readListener.beforeProcess);
		assertEquals(25, readListener.beforeRead);
		assertEquals(1, readListener.beforeWriteCount);
		assertEquals(0, readListener.onProcessError);
		assertEquals(0, readListener.onReadError);
		assertEquals(1, readListener.onWriteError);
	}

	@Test
	public void testMultipleChunks() throws Exception{

		Step step = builder.chunk(10).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) readListener).build();

		runStep(step);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(25, processor.count);
		assertEquals(25, writer.results.size());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(25, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(25, readListener.afterProcess);
		assertEquals(25, readListener.afterRead);
		assertEquals(3, readListener.afterWrite);
		assertEquals(25, readListener.beforeProcess);
		assertEquals(26, readListener.beforeRead);
		assertEquals(3, readListener.beforeWriteCount);
		assertEquals(0, readListener.onProcessError);
		assertEquals(0, readListener.onReadError);
		assertEquals(0, readListener.onWriteError);
	}

	protected void runStep(Step step)
			throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobInterruptedException {
		JobExecution jobExecution = repository.createJobExecution("job1", new JobParameters());
		stepExecution = new StepExecution("step1", jobExecution);
		repository.add(stepExecution);

		step.execute(stepExecution);
	}

	public static class FailingListItemReader extends ListItemReader<String> {

		protected int failCount = -1;
		protected int count = 0;

		public FailingListItemReader(List<String> list) {
			super(list);
		}

		@Nullable
		@Override
		public String read() {
			count++;

			if(failCount == count) {
				throw new RuntimeException("expected at read index " + failCount);
			} else {
				return super.read();
			}
		}
	}

	public static class FailingCountingItemProcessor implements ItemProcessor<String, String>{
		protected int count = 0;
		protected int failCount = -1;
		protected boolean filter = false;

		@Nullable
		@Override
		public String process(String item) throws Exception {
			count++;

			if(filter && count % 2 == 0) {
				return null;
			} else if(count == failCount){
				throw new RuntimeException("expected at process index " + failCount);
			} else {
				return item;
			}
		}
	}

	public static class StoringItemWriter implements ItemWriter<String>{

		protected List<String> results = new ArrayList<>();
		protected boolean fail = false;

		@Override
		public void write(List<? extends String> items) throws Exception {
			if(fail) {
				throw new RuntimeException("expected in write");
			}

			results.addAll(items);
		}
	}

	public static class CountingListener implements ItemReadListener<String>, ItemProcessListener<String, String>, ItemWriteListener<String> {

		protected int beforeWriteCount = 0;
		protected int afterWrite = 0;
		protected int onWriteError = 0;
		protected int beforeProcess = 0;
		protected int afterProcess = 0;
		protected int onProcessError = 0;
		protected int beforeRead = 0;
		protected int afterRead = 0;
		protected int onReadError = 0;

		@Override
		public void beforeWrite(List<? extends String> items) {
			beforeWriteCount++;
		}

		@Override
		public void afterWrite(List<? extends String> items) {
			afterWrite++;
		}

		@Override
		public void onWriteError(Exception exception,
				List<? extends String> items) {
			onWriteError++;
		}

		@Override
		public void beforeProcess(String item) {
			beforeProcess++;
		}

		@Override
		public void afterProcess(String item, @Nullable String result) {
			afterProcess++;
		}

		@Override
		public void onProcessError(String item, Exception e) {
			onProcessError++;
		}

		@Override
		public void beforeRead() {
			beforeRead++;
		}

		@Override
		public void afterRead(String item) {
			afterRead++;
		}

		@Override
		public void onReadError(Exception ex) {
			onReadError++;
		}
	}
}
