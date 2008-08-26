/*
 * Copyright 2006-2007 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.core.repository.dao.MapExecutionContextDao;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.exception.SimpleLimitExceptionHandler;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Tests for {@link SimpleStepFactoryBean}.
 */
public class SimpleStepFactoryBeanTests {

	private List<Exception> listened = new ArrayList<Exception>();

	private SimpleJobRepository repository = new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(),
			new MapStepExecutionDao(), new MapExecutionContextDao());

	private List<String> written = new ArrayList<String>();

	private ItemWriter<String> writer = new ItemWriter<String>() {
		public void write(List<? extends String> data) throws Exception {
			written.addAll(data);
		}
	};

	private ItemReader<String> reader;

	private AbstractJob job = new SimpleJob() {
		{
			setBeanName("simpleJob");
		}
	};

	@Before
	public void setUp() throws Exception {
		job.setJobRepository(repository);
		MapJobInstanceDao.clear();
		MapJobExecutionDao.clear();
		MapStepExecutionDao.clear();
	}

	@Test
	public void testSimpleJob() throws Exception {

		job.setSteps(new ArrayList<Step>());
		AbstractStep step = (AbstractStep) getStepFactory("foo", "bar").getObject();
		step.setName("step1");
		job.addStep(step);
		step = (AbstractStep) getStepFactory("spam").getObject();
		step.setName("step2");
		job.addStep(step);

		JobExecution jobExecution = repository.createJobExecution(job, new JobParameters());

		job.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(3, written.size());
		assertTrue(written.contains("foo"));
	}

	@Test
	public void testSimpleConcurrentJob() throws Exception {

		job.setSteps(new ArrayList<Step>());
		SimpleStepFactoryBean<String,String> factory = getStepFactory("foo", "bar");
		factory.setTaskExecutor(new SimpleAsyncTaskExecutor());
		factory.setThrottleLimit(1);

		AbstractStep step = (AbstractStep) factory.getObject();
		step.setName("step1");
		job.addStep(step);

		JobExecution jobExecution = repository.createJobExecution(job, new JobParameters());

		job.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(2, written.size());
		assertTrue(written.contains("foo"));
	}

	@Test
	public void testSimpleJobWithItemListeners() throws Exception {

		SimpleStepFactoryBean<String,String> factory = getStepFactory(new String[] { "foo", "bar", "spam" });

		factory.setItemWriter(new ItemWriter<String>() {
			public void write(List<? extends String> data) throws Exception {
				throw new RuntimeException("Error!");
			}
		});
		factory.setListeners(new StepListener[] { new ItemListenerSupport<String>() {
			@Override
			public void onReadError(Exception ex) {
				listened.add(ex);
			}

			@Override
			public void onWriteError(Exception ex, List<? extends String> item) {
				listened.add(ex);
			}
		} });

		Step step = (Step) factory.getObject();

		job.setSteps(Collections.singletonList(step));

		JobExecution jobExecution = repository.createJobExecution(job, new JobParameters());
		try {
			job.execute(jobExecution);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			// expected
			assertEquals("Error!", e.getMessage());
		}

		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		assertEquals(0, written.size());
		// provider should be at second item
		assertEquals("bar", reader.read());
		assertEquals(1, listened.size());
	}

	@Test
	public void testExceptionTerminates() throws Exception {
		SimpleStepFactoryBean<String,String> factory = getStepFactory(new String[] { "foo", "bar", "spam" });
		factory.setBeanName("exceptionStep");
		factory.setItemWriter(new ItemWriter<String>() {
			public void write(List<? extends String> data) throws Exception {
				throw new RuntimeException("Foo");
			}
		});
		AbstractStep step = (AbstractStep) factory.getObject();
		job.setSteps(Collections.singletonList((Step) step));

		JobExecution jobExecution = repository.createJobExecution(job, new JobParameters());
		try {
			job.execute(jobExecution);
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Foo", e.getMessage());
			// expected
		}
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
	}

	@Test
	public void testExceptionHandler() throws Exception {
		SimpleStepFactoryBean<String,String> factory = getStepFactory(new String[] { "foo", "bar", "spam" });
		factory.setBeanName("exceptionStep");
		factory.setExceptionHandler(new SimpleLimitExceptionHandler(1));
		factory.setItemWriter(new ItemWriter<String>() {
			int count = 0;

			public void write(List<? extends String> data) throws Exception {
				if (count++ == 0) {
					throw new RuntimeException("Foo");
				}
			}
		});
		AbstractStep step = (AbstractStep) factory.getObject();
		job.setSteps(Collections.singletonList((Step) step));

		JobExecution jobExecution = repository.createJobExecution(job, new JobParameters());
		
		job.execute(jobExecution);
		
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Test
	public void testChunkListeners() throws Exception {
		String[] items = new String[] { "1", "2", "3", "4", "5", "6", "7" };
		int commitInterval = 3;

		SimpleStepFactoryBean<String,String> factory = getStepFactory(items);
		class CountingChunkListener implements ChunkListener {
			int beforeCount = 0;

			int afterCount = 0;

			public void afterChunk() {
				afterCount++;
			}

			public void beforeChunk() {
				beforeCount++;
			}
		}
		CountingChunkListener chunkListener = new CountingChunkListener();
		factory.setListeners(new StepListener[] { chunkListener });
		factory.setCommitInterval(commitInterval);

		AbstractStep step = (AbstractStep) factory.getObject();

		job.setSteps(Collections.singletonList((Step) step));

		JobExecution jobExecution = repository.createJobExecution(job, new JobParameters());
		job.execute(jobExecution);

		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertNull(reader.read());
		assertEquals(items.length, written.size());

		int expectedListenerCallCount = (items.length / commitInterval) + 1;
		assertEquals(expectedListenerCallCount, chunkListener.afterCount);
		assertEquals(expectedListenerCallCount, chunkListener.beforeCount);
	}

	/**
	 * Commit interval specified is not allowed to be zero or negative.
	 * @throws Exception
	 */
	@Test
	public void testCommitIntervalMustBeGreaterThanZero() throws Exception {
		SimpleStepFactoryBean<String,String> factory = getStepFactory("foo");
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
		SimpleStepFactoryBean<String,String> factory = getStepFactory("foo");

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
	
	private SimpleStepFactoryBean<String,String> getStepFactory(String... args) throws Exception {
		SimpleStepFactoryBean<String,String> factory = new SimpleStepFactoryBean<String,String>();

		List<String> items = new ArrayList<String>();
		items.addAll(Arrays.asList(args));
		reader = new ListItemReader<String>(items);

		factory.setItemReader(reader);
		factory.setItemWriter(writer);
		factory.setJobRepository(repository);
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setBeanName("stepName");
		return factory;
	}
	
}
