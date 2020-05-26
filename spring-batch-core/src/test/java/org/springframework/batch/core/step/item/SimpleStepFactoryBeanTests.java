/*
 * Copyright 2006-2019 the original author or authors.
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

package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.core.listener.StepListenerSupport;
import org.springframework.batch.core.repository.dao.MapExecutionContextDao;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.factory.SimpleStepFactoryBean;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.exception.SimpleLimitExceptionHandler;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.Nullable;

/**
 * Tests for {@link SimpleStepFactoryBean}.
 */
public class SimpleStepFactoryBeanTests {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private List<Exception> listened = new ArrayList<>();

	private SimpleJobRepository repository = new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(),
			new MapStepExecutionDao(), new MapExecutionContextDao());

	private List<String> written = new ArrayList<>();

	private ItemWriter<String> writer = new ItemWriter<String>() {
		@Override
		public void write(List<? extends String> data) throws Exception {
			written.addAll(data);
		}
	};

	private ItemReader<String> reader = new ListItemReader<>(Arrays.asList("a", "b", "c"));

	private SimpleJob job = new SimpleJob();

	@Before
	public void setUp() throws Exception {
		job.setJobRepository(repository);
		job.setBeanName("simpleJob");
	}

	@Test(expected = IllegalStateException.class)
	public void testMandatoryProperties() throws Exception {
		new SimpleStepFactoryBean<String, String>().getObject();
	}

	@Test
	public void testMandatoryReader() throws Exception {
		SimpleStepFactoryBean<String, String> factory = new SimpleStepFactoryBean<>();
		factory.setItemWriter(writer);

		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage("ItemReader must be provided");

		factory.getObject();
	}

	@Test
	public void testMandatoryWriter() throws Exception {
		SimpleStepFactoryBean<String, String> factory = new SimpleStepFactoryBean<>();
		factory.setItemReader(reader);

		expectedException.expect(IllegalStateException.class);
		expectedException.expectMessage("ItemWriter must be provided");

		factory.getObject();
	}

	@Test
	public void testSimpleJob() throws Exception {

		job.setSteps(new ArrayList<>());
		AbstractStep step = (AbstractStep) getStepFactory("foo", "bar").getObject();
		step.setName("step1");
		job.addStep(step);
		step = (AbstractStep) getStepFactory("spam").getObject();
		step.setName("step2");
		job.addStep(step);

		JobExecution jobExecution = repository.createJobExecution(job.getName(), new JobParameters());

		job.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(3, written.size());
		assertTrue(written.contains("foo"));
	}

	@Test
	public void testSimpleConcurrentJob() throws Exception {

		SimpleStepFactoryBean<String, String> factory = getStepFactory("foo", "bar");
		factory.setTaskExecutor(new SimpleAsyncTaskExecutor());
		factory.setThrottleLimit(1);

		AbstractStep step = (AbstractStep) factory.getObject();
		step.setName("step1");

		JobExecution jobExecution = repository.createJobExecution(job.getName(), new JobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution(step.getName());
		repository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, written.size());
		assertTrue(written.contains("foo"));
	}

	@Test
	public void testSimpleJobWithItemListeners() throws Exception {

		SimpleStepFactoryBean<String, String> factory = getStepFactory(new String[] { "foo", "bar", "spam" });

		factory.setItemWriter(new ItemWriter<String>() {
			@Override
			public void write(List<? extends String> data) throws Exception {
				throw new RuntimeException("Error!");
			}
		});
		factory.setListeners(new StepListener[] { new ItemListenerSupport<String, String>() {
			@Override
			public void onReadError(Exception ex) {
				listened.add(ex);
			}

			@Override
			public void onWriteError(Exception ex, List<? extends String> item) {
				listened.add(ex);
			}
		} });

		Step step = factory.getObject();

		job.setSteps(Collections.singletonList(step));

		JobExecution jobExecution = repository.createJobExecution(job.getName(), new JobParameters());

		job.execute(jobExecution);
		assertEquals("Error!", jobExecution.getAllFailureExceptions().get(0).getMessage());

		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		assertEquals(0, written.size());
		// provider should be at second item
		assertEquals("bar", reader.read());
		assertEquals(1, listened.size());
	}

	@Test
	public void testExceptionTerminates() throws Exception {
		SimpleStepFactoryBean<String, String> factory = getStepFactory(new String[] { "foo", "bar", "spam" });
		factory.setBeanName("exceptionStep");
		factory.setItemWriter(new ItemWriter<String>() {
			@Override
			public void write(List<? extends String> data) throws Exception {
				throw new RuntimeException("Foo");
			}
		});
		AbstractStep step = (AbstractStep) factory.getObject();
		job.setSteps(Collections.singletonList((Step) step));

		JobExecution jobExecution = repository.createJobExecution(job.getName(), new JobParameters());

		job.execute(jobExecution);
		assertEquals("Foo", jobExecution.getAllFailureExceptions().get(0).getMessage());
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
	}

	@Test
	public void testExceptionHandler() throws Exception {
		SimpleStepFactoryBean<String, String> factory = getStepFactory(new String[] { "foo", "bar", "spam" });
		factory.setBeanName("exceptionStep");
		SimpleLimitExceptionHandler exceptionHandler = new SimpleLimitExceptionHandler(1);
		exceptionHandler.afterPropertiesSet();
		factory.setExceptionHandler(exceptionHandler);
		factory.setItemWriter(new ItemWriter<String>() {
			int count = 0;

			@Override
			public void write(List<? extends String> data) throws Exception {
				if (count++ == 0) {
					throw new RuntimeException("Foo");
				}
			}
		});
		AbstractStep step = (AbstractStep) factory.getObject();
		job.setSteps(Collections.singletonList((Step) step));

		JobExecution jobExecution = repository.createJobExecution(job.getName(), new JobParameters());

		job.execute(jobExecution);

		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Test
	public void testChunkListeners() throws Exception {
		String[] items = new String[] { "1", "2", "3", "4", "5", "6", "7", "error" };
		int commitInterval = 3;

		SimpleStepFactoryBean<String, String> factory = getStepFactory(items);
		class AssertingWriteListener extends StepListenerSupport<Object, Object> {

			String trail = "";

			@Override
			public void beforeWrite(List<? extends Object> items) {
				if(items.contains("error")) {
					throw new RuntimeException("rollback the last chunk");
				}

				trail = trail + "2";
			}

			@Override
			public void afterWrite(List<? extends Object> items) {
				trail = trail + "3";
			}

		}
		class CountingChunkListener implements ChunkListener {
			int beforeCount = 0;

			int afterCount = 0;

			int failedCount = 0;

			private AssertingWriteListener writeListener;

			public CountingChunkListener(AssertingWriteListener writeListener) {
				super();
				this.writeListener = writeListener;
			}

			@Override
			public void afterChunk(ChunkContext context) {
				writeListener.trail = writeListener.trail + "4";
				afterCount++;
			}

			@Override
			public void beforeChunk(ChunkContext context) {
				writeListener.trail = writeListener.trail + "1";
				beforeCount++;
			}

			@Override
			public void afterChunkError(ChunkContext context) {
				writeListener.trail = writeListener.trail + "5";
				failedCount++;
			}
		}
		AssertingWriteListener writeListener = new AssertingWriteListener();
		CountingChunkListener chunkListener = new CountingChunkListener(writeListener);
		factory.setListeners(new StepListener[] { chunkListener, writeListener });
		factory.setCommitInterval(commitInterval);

		AbstractStep step = (AbstractStep) factory.getObject();

		job.setSteps(Collections.singletonList((Step) step));

		JobExecution jobExecution = repository.createJobExecution(job.getName(), new JobParameters());
		job.execute(jobExecution);

		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		assertNull(reader.read());
		assertEquals(6, written.size());

		int expectedListenerCallCount = (items.length / commitInterval) + 1;
		assertEquals(expectedListenerCallCount - 1, chunkListener.afterCount);
		assertEquals(expectedListenerCallCount, chunkListener.beforeCount);
		assertEquals(1, chunkListener.failedCount);
		assertEquals("1234123415", writeListener.trail);
		assertTrue("Listener order not as expected: " + writeListener.trail, writeListener.trail.startsWith("1234"));
	}

	/**
	 * Commit interval specified is not allowed to be zero or negative.
	 * @throws Exception
	 */
	@Test
	public void testCommitIntervalMustBeGreaterThanZero() throws Exception {
		SimpleStepFactoryBean<String, String> factory = getStepFactory("foo");
		// nothing wrong here
		factory.getObject();

		factory = getStepFactory("foo");
		// but exception expected after setting commit interval to value < 0
		factory.setCommitInterval(-1);
		try {
			factory.getObject();
			fail();
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	/**
	 * Commit interval specified is not allowed to be zero or negative.
	 * @throws Exception
	 */
	@Test
	public void testCommitIntervalAndCompletionPolicyBothSet() throws Exception {
		SimpleStepFactoryBean<String, String> factory = getStepFactory("foo");

		// but exception expected after setting commit interval and completion
		// policy
		factory.setCommitInterval(1);
		factory.setChunkCompletionPolicy(new SimpleCompletionPolicy(2));
		try {
			factory.getObject();
			fail();
		}
		catch (IllegalStateException e) {
			// expected
		}

	}

	@Test
	public void testAutoRegisterItemListeners() throws Exception {

		SimpleStepFactoryBean<String, String> factory = getStepFactory(new String[] { "foo", "bar", "spam" });

		final List<String> listenerCalls = new ArrayList<>();

		class TestItemListenerWriter implements ItemWriter<String>, ItemProcessor<String, String>,
		ItemReadListener<String>, ItemWriteListener<String>, ItemProcessListener<String, String>, ChunkListener {
			@Override
			public void write(List<? extends String> items) throws Exception {
			}

			@Nullable
			@Override
			public String process(String item) throws Exception {
				return item;
			}

			@Override
			public void afterRead(String item) {
				listenerCalls.add("read");
			}

			@Override
			public void beforeRead() {
			}

			@Override
			public void onReadError(Exception ex) {
			}

			@Override
			public void afterWrite(List<? extends String> items) {
				listenerCalls.add("write");
			}

			@Override
			public void beforeWrite(List<? extends String> items) {
			}

			@Override
			public void onWriteError(Exception exception, List<? extends String> items) {
			}

			@Override
			public void afterProcess(String item, @Nullable String result) {
				listenerCalls.add("process");
			}

			@Override
			public void beforeProcess(String item) {
			}

			@Override
			public void onProcessError(String item, Exception e) {
			}

			@Override
			public void afterChunk(ChunkContext context) {
				listenerCalls.add("chunk");
			}

			@Override
			public void beforeChunk(ChunkContext context) {
			}

			@Override
			public void afterChunkError(ChunkContext context) {
			}

		}

		TestItemListenerWriter itemWriter = new TestItemListenerWriter();
		factory.setItemWriter(itemWriter);
		factory.setItemProcessor(itemWriter);

		Step step = factory.getObject();

		job.setSteps(Collections.singletonList(step));

		JobExecution jobExecution = repository.createJobExecution(job.getName(), new JobParameters());

		job.execute(jobExecution);

		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		for (String type : new String[] { "read", "write", "process", "chunk" }) {
			assertTrue("Missing listener call: " + type + " from " + listenerCalls, listenerCalls.contains(type));
		}
	}

	@Test
	public void testAutoRegisterItemListenersNoDoubleCounting() throws Exception {

		SimpleStepFactoryBean<String, String> factory = getStepFactory(new String[] { "foo", "bar", "spam" });

		final List<String> listenerCalls = new ArrayList<>();

		class TestItemListenerWriter implements ItemWriter<String>, ItemWriteListener<String> {
			@Override
			public void write(List<? extends String> items) throws Exception {
			}

			@Override
			public void afterWrite(List<? extends String> items) {
				listenerCalls.add("write");
			}

			@Override
			public void beforeWrite(List<? extends String> items) {
			}

			@Override
			public void onWriteError(Exception exception, List<? extends String> items) {
			}

		}

		TestItemListenerWriter itemWriter = new TestItemListenerWriter();
		factory.setListeners(new StepListener[] { itemWriter });
		factory.setItemWriter(itemWriter);

		Step step = factory.getObject();

		job.setSteps(Collections.singletonList(step));

		JobExecution jobExecution = repository.createJobExecution(job.getName(), new JobParameters());

		job.execute(jobExecution);

		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals("[write, write, write]", listenerCalls.toString());

	}

	private SimpleStepFactoryBean<String, String> getStepFactory(String... args) throws Exception {
		SimpleStepFactoryBean<String, String> factory = new SimpleStepFactoryBean<>();

		List<String> items = new ArrayList<>();
		items.addAll(Arrays.asList(args));
		reader = new ListItemReader<>(items);

		factory.setItemReader(reader);
		factory.setItemWriter(writer);
		factory.setJobRepository(repository);
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setBeanName("stepName");
		return factory;
	}

}
