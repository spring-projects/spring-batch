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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.AbstractItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.ExceptionHandler;
import org.springframework.batch.repeat.exception.SimpleLimitExceptionHandler;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Tests for {@link SimpleStepFactoryBean}.
 */
public class SimpleStepFactoryBeanTests extends TestCase {

	private List<Exception> recovered = new ArrayList<Exception>();

	private SimpleJobRepository repository = new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(),
			new MapStepExecutionDao());

	private List<String> written = new ArrayList<String>();

	private ItemWriter<String> writer = new AbstractItemWriter<String>() {
		public void write(String data) throws Exception {
			written.add(data);
		}
	};

	private ItemReader<String> reader;

	private AbstractJob job = new SimpleJob() {
		{
			setBeanName("simpleJob");
		}
	};

	protected void setUp() throws Exception {
		super.setUp();
		job.setJobRepository(repository);
		MapJobInstanceDao.clear();
		MapJobExecutionDao.clear();
		MapStepExecutionDao.clear();
	}

	private SimpleStepFactoryBean<String> getStepFactory(String arg) throws Exception {
		return getStepFactory(new String[] { arg });
	}

	private SimpleStepFactoryBean<String> getStepFactory(String arg0, String arg1) throws Exception {
		return getStepFactory(new String[] { arg0, arg1 });
	}

	private SimpleStepFactoryBean<String> getStepFactory(String[] args) throws Exception {
		SimpleStepFactoryBean<String> factory = new SimpleStepFactoryBean<String>();

		List<String> items = TransactionAwareProxyFactory.createTransactionalList();
		items.addAll(Arrays.asList(args));
		reader = new ListItemReader<String>(items);

		factory.setItemReader(reader);
		factory.setItemWriter(writer);
		factory.setJobRepository(repository);
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setBeanName("stepName");
		return factory;
	}

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

	public void testSimpleConcurrentJob() throws Exception {

		job.setSteps(new ArrayList<Step>());
		SimpleStepFactoryBean<String> factory = getStepFactory("foo", "bar");
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

	public void testSimpleJobWithItemListeners() throws Exception {

		final List<Throwable> throwables = new ArrayList<Throwable>();

		RepeatTemplate chunkOperations = new RepeatTemplate();
		// Always handle the exception a check it is the right one...
		chunkOperations.setExceptionHandler(new ExceptionHandler() {
			public void handleException(RepeatContext context, Throwable throwable) throws RuntimeException {
				throwables.add(throwable);
				assertEquals("Error!", throwable.getMessage());
			}
		});

		/*
		 * Each message fails once and the chunk (size=1) "rolls back"; then it
		 * is recovered ("skipped") on the second attempt (see retry policy
		 * definition above)...
		 */
		SimpleStepFactoryBean<String> factory = getStepFactory(new String[] { "foo", "bar", "spam" });

		factory.setItemWriter(new AbstractItemWriter<String>() {
			public void write(String data) throws Exception {
				throw new RuntimeException("Error!");
			}
		});
		factory.setListeners(new StepListener[] { new ItemListenerSupport() {
			public void onReadError(Exception ex) {
				recovered.add(ex);
			}

			public void onWriteError(Exception ex, Object item) {
				recovered.add(ex);
			}
		} });

		ItemOrientedStep step = (ItemOrientedStep) factory.getObject();
		step.setChunkOperations(chunkOperations);

		job.setSteps(Collections.singletonList((Step) step));

		JobExecution jobExecution = repository.createJobExecution(job, new JobParameters());
		job.execute(jobExecution);

		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(0, written.size());
		// provider should be exhausted
		assertEquals(null, reader.read());
		assertEquals(3, recovered.size());
	}

	public void testExceptionTerminates() throws Exception {
		SimpleStepFactoryBean<String> factory = getStepFactory(new String[] { "foo", "bar", "spam" });
		factory.setBeanName("exceptionStep");
		factory.setItemWriter(new AbstractItemWriter<String>() {
			public void write(String data) throws Exception {
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

	public void testExceptionHandler() throws Exception {
		SimpleStepFactoryBean<String> factory = getStepFactory(new String[] { "foo", "bar", "spam" });
		factory.setBeanName("exceptionStep");
		factory.setExceptionHandler(new SimpleLimitExceptionHandler(1));
		factory.setItemWriter(new AbstractItemWriter<String>() {
			int count = 0;

			public void write(String data) throws Exception {
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

	public void testChunkListeners() throws Exception {
		String[] items = new String[] { "1", "2", "3", "4", "5", "6", "7" };
		int commitInterval = 3;

		SimpleStepFactoryBean<String> factory = getStepFactory(items);
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
	public void testCommitIntervalMustBeGreaterThanZero() throws Exception {
		SimpleStepFactoryBean<String> factory = getStepFactory("foo");
		// nothing wrong here
		factory.getObject();

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
	public void testCommitIntervalAndCompletionPolicyBothSet() throws Exception {
		SimpleStepFactoryBean<String> factory = getStepFactory("foo");

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
}
