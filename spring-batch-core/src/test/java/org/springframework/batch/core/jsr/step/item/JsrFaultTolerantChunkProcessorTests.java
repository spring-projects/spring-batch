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
import static org.junit.Assert.assertNotNull;

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
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.batch.core.jsr.step.builder.JsrFaultTolerantStepBuilder;
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

public class JsrFaultTolerantChunkProcessorTests {

	private FailingListItemReader reader;
	private FailingCountingItemProcessor processor;
	private StoringItemWriter writer;
	private CountingListener listener;
	private JsrFaultTolerantStepBuilder<String, String> builder;
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
		listener = new CountingListener();

		builder = new JsrFaultTolerantStepBuilder<>(new StepBuilder("step1"));
		builder.setBatchPropertyContext(new BatchPropertyContext());
		repository = new MapJobRepositoryFactoryBean().getObject();
		builder.repository(repository);
		builder.transactionManager(new ResourcelessTransactionManager());
		stepExecution = null;
	}

	@Test
	public void testNoInputNoListeners() throws Exception{
		reader = new FailingListItemReader(new ArrayList<>());
		Step step = builder.chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) listener).build();

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
		Step step = builder.chunk(25).reader(reader).writer(writer).listener((ItemReadListener<String>) listener).build();

		runStep(step);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(25, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(0, listener.afterProcess);
		assertEquals(25, listener.afterRead);
		assertEquals(1, listener.afterWrite);
		assertEquals(0, listener.beforeProcess);
		assertEquals(26, listener.beforeRead);
		assertEquals(1, listener.beforeWriteCount);
		assertEquals(0, listener.onProcessError);
		assertEquals(0, listener.onReadError);
		assertEquals(0, listener.onWriteError);
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
		Step step = builder.chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) listener).build();

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
	public void testSkipReadError() throws Exception{
		reader.failCount = 10;

		Step step = builder.faultTolerant().skip(RuntimeException.class).skipLimit(20).chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) listener).build();

		runStep(step);

		assertNotNull(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(25, processor.count);
		assertEquals(25, writer.results.size());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(1, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(25, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(0,	stepExecution.getFailureExceptions().size());
		assertEquals(25, listener.afterProcess);
		assertEquals(25, listener.afterRead);
		assertEquals(1, listener.afterWrite);
		assertEquals(25, listener.beforeProcess);
		assertEquals(27, listener.beforeRead);
		assertEquals(1, listener.beforeWriteCount);
		assertEquals(0, listener.onProcessError);
		assertEquals(1, listener.onReadError);
		assertEquals(0, listener.onWriteError);
	}

	@Test
	public void testRetryReadError() throws Exception{
		reader.failCount = 10;

		Step step = builder.faultTolerant().retry(RuntimeException.class).retryLimit(20).chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) listener).build();

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
		assertEquals(0,	stepExecution.getFailureExceptions().size());
		assertEquals(25, listener.afterProcess);
		assertEquals(25, listener.afterRead);
		assertEquals(1, listener.afterWrite);
		assertEquals(25, listener.beforeProcess);
		assertEquals(27, listener.beforeRead);
		assertEquals(1, listener.beforeWriteCount);
		assertEquals(0, listener.onProcessError);
		assertEquals(1, listener.onReadError);
		assertEquals(0, listener.onWriteError);
	}

	@Test
	public void testReadError() throws Exception{
		reader.failCount = 10;

		Step step = builder.chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) listener).build();

		runStep(step);

		assertNotNull(stepExecution);
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
		assertEquals("expected at read index 10", stepExecution.getFailureExceptions().get(0).getCause().getMessage());
		assertEquals(9, listener.afterProcess);
		assertEquals(9, listener.afterRead);
		assertEquals(0, listener.afterWrite);
		assertEquals(9, listener.beforeProcess);
		assertEquals(10, listener.beforeRead);
		assertEquals(0, listener.beforeWriteCount);
		assertEquals(0, listener.onProcessError);
		assertEquals(1, listener.onReadError);
		assertEquals(0, listener.onWriteError);
	}

	@Test
	public void testProcessError() throws Exception{
		processor.failCount = 10;

		Step step = builder.chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) listener).build();

		runStep(step);

		assertEquals(10, processor.count);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(0, writer.results.size());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(10, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals("expected at process index 10", stepExecution.getFailureExceptions().get(0).getCause().getMessage());
		assertEquals(9, listener.afterProcess);
		assertEquals(10, listener.afterRead);
		assertEquals(0, listener.afterWrite);
		assertEquals(10, listener.beforeProcess);
		assertEquals(10, listener.beforeRead);
		assertEquals(0, listener.beforeWriteCount);
		assertEquals(1, listener.onProcessError);
		assertEquals(0, listener.onReadError);
		assertEquals(0, listener.onWriteError);
	}

	@Test
	public void testSkipProcessError() throws Exception{
		processor.failCount = 10;

		Step step = builder.faultTolerant().skip(RuntimeException.class).skipLimit(20).chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) listener).build();

		runStep(step);

		assertNotNull(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(25, processor.count);
		assertEquals(24, writer.results.size());
		assertEquals(1, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(24, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(0,	stepExecution.getFailureExceptions().size());
		assertEquals(24, listener.afterProcess);
		assertEquals(25, listener.afterRead);
		assertEquals(1, listener.afterWrite);
		assertEquals(25, listener.beforeProcess);
		assertEquals(26, listener.beforeRead);
		assertEquals(1, listener.beforeWriteCount);
		assertEquals(1, listener.onProcessError);
		assertEquals(0, listener.onReadError);
		assertEquals(0, listener.onWriteError);
	}

	@Test
	public void testRetryProcessError() throws Exception{
		processor.failCount = 10;

		Step step = builder.faultTolerant().retry(RuntimeException.class).retryLimit(20).chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) listener).build();

		runStep(step);

		assertNotNull(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(26, processor.count);
		assertEquals(25, writer.results.size());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(25, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(0,	stepExecution.getFailureExceptions().size());
		assertEquals(25, listener.afterProcess);
		assertEquals(25, listener.afterRead);
		assertEquals(1, listener.afterWrite);
		assertEquals(26, listener.beforeProcess);
		assertEquals(26, listener.beforeRead);
		assertEquals(1, listener.beforeWriteCount);
		assertEquals(1, listener.onProcessError);
		assertEquals(0, listener.onReadError);
		assertEquals(0, listener.onWriteError);
	}

	@Test
	public void testWriteError() throws Exception{
		writer.fail = true;

		Step step = builder.chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) listener).build();

		runStep(step);

		assertEquals(25, processor.count);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(0, writer.results.size());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(25, listener.afterProcess);
		assertEquals(25, listener.afterRead);
		assertEquals(0, listener.afterWrite);
		assertEquals(25, listener.beforeProcess);
		assertEquals(25, listener.beforeRead);
		assertEquals(1, listener.beforeWriteCount);
		assertEquals(0, listener.onProcessError);
		assertEquals(0, listener.onReadError);
		assertEquals(1, listener.onWriteError);
	}

	@Test
	public void testRetryWriteError() throws Exception{
		writer.fail = true;

		Step step = builder.faultTolerant().retry(RuntimeException.class).retryLimit(25).chunk(25).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) listener).build();

		runStep(step);

		assertEquals(25, processor.count);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(25, writer.results.size());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(25, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(25, listener.afterProcess);
		assertEquals(25, listener.afterRead);
		assertEquals(1, listener.afterWrite);
		assertEquals(25, listener.beforeProcess);
		assertEquals(26, listener.beforeRead);
		assertEquals(2, listener.beforeWriteCount);
		assertEquals(0, listener.onProcessError);
		assertEquals(0, listener.onReadError);
		assertEquals(1, listener.onWriteError);
	}

	@Test
	public void testSkipWriteError() throws Exception{
		writer.fail = true;

		Step step = builder.faultTolerant().skip(RuntimeException.class).skipLimit(25).chunk(7).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) listener).build();

		runStep(step);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(25, processor.count);
		assertEquals(18, writer.results.size());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(18, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
		assertEquals(25, listener.afterProcess);
		assertEquals(25, listener.afterRead);
		assertEquals(3, listener.afterWrite);
		assertEquals(25, listener.beforeProcess);
		assertEquals(26, listener.beforeRead);
		assertEquals(4, listener.beforeWriteCount);
		assertEquals(0, listener.onProcessError);
		assertEquals(0, listener.onReadError);
		assertEquals(1, listener.onWriteError);
		assertEquals(0, listener.onSkipInRead);
		assertEquals(0, listener.onSkipInProcess);
		assertEquals(1, listener.onSkipInWrite);
	}
	
	@Test
	public void testMultipleChunks() throws Exception{

		Step step = builder.chunk(10).reader(reader).processor(processor).writer(writer).listener((ItemReadListener<String>) listener).build();

		runStep(step);

		assertEquals(25, processor.count);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(25, writer.results.size());
		assertEquals(0, stepExecution.getProcessSkipCount());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(25, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(25, listener.afterProcess);
		assertEquals(25, listener.afterRead);
		assertEquals(3, listener.afterWrite);
		assertEquals(25, listener.beforeProcess);
		assertEquals(26, listener.beforeRead);
		assertEquals(3, listener.beforeWriteCount);
		assertEquals(0, listener.onProcessError);
		assertEquals(0, listener.onReadError);
		assertEquals(0, listener.onWriteError);
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
				fail = false;
				throw new RuntimeException("expected in write");
			}

			results.addAll(items);
		}
	}

	public static class CountingListener implements ItemReadListener<String>, ItemProcessListener<String, String>, ItemWriteListener<String>, SkipListener<String, List<Object>> {

		protected int beforeWriteCount = 0;
		protected int afterWrite = 0;
		protected int onWriteError = 0;
		protected int beforeProcess = 0;
		protected int afterProcess = 0;
		protected int onProcessError = 0;
		protected int beforeRead = 0;
		protected int afterRead = 0;
		protected int onReadError = 0;
		protected int onSkipInRead = 0;
		protected int onSkipInProcess = 0;
		protected int onSkipInWrite = 0;

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

		@Override
		public void onSkipInRead(Throwable t) {
			onSkipInRead++;
		}

		@Override
		public void onSkipInWrite(List<Object> items, Throwable t) {
			onSkipInWrite++;			
		}

		@Override
		public void onSkipInProcess(String item, Throwable t) {
			onSkipInProcess++;
		}
	}
}
