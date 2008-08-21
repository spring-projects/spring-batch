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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.listener.SkipListenerSupport;
import org.springframework.batch.core.repository.dao.MapExecutionContextDao;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.AbstractItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.policy.RetryCacheCapacityExceededException;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Dave Syer
 * 
 */
public class StatefulRetryStepFactoryBeanTests {

	protected final Log logger = LogFactory.getLog(getClass());

	private SkipLimitStepFactoryBean<String, String> factory = new SkipLimitStepFactoryBean<String, String>();

	private List<Object> recovered = new ArrayList<Object>();

	private List<Object> processed = new ArrayList<Object>();

	private List<Object> provided = new ArrayList<Object>();

	int count = 0;

	private SimpleJobRepository repository = new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(),
			new MapStepExecutionDao(), new MapExecutionContextDao());

	JobExecution jobExecution;

	private ItemWriter<String> processor = new ItemWriter<String>() {
		public void write(List<? extends String> data) throws Exception {
			processed.addAll(data);
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before
	public void setUp() throws Exception {

		MapJobInstanceDao.clear();
		MapJobExecutionDao.clear();
		MapStepExecutionDao.clear();

		factory.setBeanName("step");

		factory.setItemReader(new ListItemReader<String>(new ArrayList<String>()));
		factory.setItemWriter(processor);
		factory.setJobRepository(repository);
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setRetryableExceptionClasses(new Class[] { Exception.class });

		JobSupport job = new JobSupport("jobName");
		job.setRestartable(true);
		JobParameters jobParameters = new JobParametersBuilder().addString("statefulTest", "make_this_unique")
				.toJobParameters();
		jobExecution = repository.createJobExecution(job, jobParameters);
		jobExecution.setEndTime(new Date());

	}

	@Test
	public void testType() throws Exception {
		assertEquals(Step.class, factory.getObjectType());
	}

	@Test
	public void testDefaultValue() throws Exception {
		assertTrue(factory.getObject() instanceof Step);
	}

	/**
	 * N.B. this doesn't really test retry, since the retry is only on write
	 * failures, but it does test that read errors are re-presented for another
	 * try when the retryLimit is high enough (it is used to build an exception
	 * handler).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSuccessfulRetryWithReadFailure() throws Exception {
		List<String> items = Arrays.asList(new String[] { "a", "b", "c" });
		ItemReader<String> provider = new ListItemReader<String>(items) {
			public String read() {
				String item = super.read();
				provided.add(item);
				count++;
				if (count == 2) {
					throw new RuntimeException("Temporary error - retry for success.");
				}
				return item;
			}
		};
		factory.setItemReader(provider);
		factory.setRetryLimit(10);
		factory.setSkippableExceptionClasses(new Class[0]);
		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);

		assertEquals(0, stepExecution.getSkipCount());

		// [a, b, c, null]
		assertEquals(4, provided.size());
		// [a, c]
		assertEquals(2, processed.size());
		// []
		assertEquals(0, recovered.size());
		assertEquals(2, stepExecution.getItemCount());
		assertEquals(0, stepExecution.getReadSkipCount());
	}

	@Test
	public void testSkipAndRetry() throws Exception {
		factory.setSkippableExceptionClasses(new Class[] { Exception.class });
		factory.setSkipLimit(2);
		List<String> items = Arrays.asList(new String[] { "a", "b", "c", "d", "e", "f" });
		ItemReader<String> provider = new ListItemReader<String>(items) {
			public String read() {
				String item = super.read();
				count++;
				if ("b".equals(item) || "d".equals(item)) {
					throw new RuntimeException("Read error - planned but skippable.");
				}
				return item;
			}
		};
		factory.setItemReader(provider);
		factory.setRetryLimit(10);
		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);

		assertEquals(2, stepExecution.getSkipCount());
		// b is processed once and skipped, plus 1, plus c, plus the null at end
		assertEquals(7, count);
		assertEquals(4, stepExecution.getItemCount());
	}

	@Test
	public void testSkipAndRetryWithWriteFailure() throws Exception {

		factory.setSkippableExceptionClasses(new Class[] { RetryException.class });
		factory.setListeners(new StepListener[] { new SkipListenerSupport() {
			public void onSkipInWrite(Object item, Throwable t) {
				recovered.add(item);
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
			}
		} });
		factory.setSkipLimit(2);
		List<String> items = Arrays.asList(new String[] { "a", "b", "c", "d", "e", "f" });
		ItemReader<String> provider = new ListItemReader<String>(items) {
			public String read() {
				String item = super.read();
				logger.debug("Read Called! Item: [" + item + "]");
				provided.add(item);
				count++;
				return item;
			}
		};

		ItemWriter<String> itemWriter = new ItemWriter<String>() {
			public void write(List<? extends String> item) throws Exception {
				logger.debug("Write Called! Item: [" + item + "]");
				processed.addAll(item);
				if (item.contains("b") || item.contains("d")) {
					throw new RuntimeException("Write error - planned but recoverable.");
				}
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		factory.setRetryLimit(5);
		factory.setRetryableExceptionClasses(new Class[] { RuntimeException.class });
		AbstractStep step = (AbstractStep) factory.getObject();
		step.setName("mytest");
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);

		assertEquals(2, recovered.size());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getWriteSkipCount());

		// [a, b, c, d, e, f, null]
		assertEquals(7, provided.size());
		// [a, b, b, b, b, b, c, d, d, d, d, d, e, f]
		assertEquals(14, processed.size());
		// [b, d]
		assertEquals(2, recovered.size());
	}

	@Test
	public void testRetryWithNoSkip() throws Exception {
		factory.setRetryableExceptionClasses(new Class[] { Exception.class });
		factory.setRetryLimit(4);
		factory.setSkipLimit(0);
		List<String> items = Arrays.asList(new String[] { "b" });
		ItemReader<String> provider = new ListItemReader<String>(items) {
			public String read() {
				String item = super.read();
				provided.add(item);
				count++;
				return item;
			}
		};
		ItemWriter<String> itemWriter = new ItemWriter<String>() {
			public void write(List<? extends String> item) throws Exception {
				processed.addAll(item);
				logger.debug("Write Called! Item: [" + item + "]");
				throw new RuntimeException("Write error - planned but retryable.");
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		try {
			step.execute(stepExecution);
			fail("Expected SkipLimitExceededException");
		}
		catch (SkipLimitExceededException e) {
			// expected
		}

		assertEquals(0, stepExecution.getSkipCount());
		// [b]
		assertEquals(1, provided.size());
		// [b, b, b, b]
		assertEquals(4, processed.size());
		// []
		assertEquals(0, recovered.size());
		assertEquals(0, stepExecution.getItemCount());
	}

	@Test
	public void testRetryPolicy() throws Exception {
		factory.setRetryPolicy(new SimpleRetryPolicy(4));
		factory.setSkipLimit(0);
		List<String> items = Arrays.asList(new String[] { "b" });
		ItemReader<String> provider = new ListItemReader<String>(items) {
			public String read() {
				String item = super.read();
				provided.add(item);
				count++;
				return item;
			}
		};
		ItemWriter<String> itemWriter = new ItemWriter<String>() {
			public void write(List<? extends String> item) throws Exception {
				processed.addAll(item);
				logger.debug("Write Called! Item: [" + item + "]");
				throw new RuntimeException("Write error - planned but retryable.");
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		try {
			step.execute(stepExecution);
			fail("Expected SkipLimitExceededException");
		}
		catch (SkipLimitExceededException e) {
			// expected
		}

		assertEquals(0, stepExecution.getSkipCount());
		// [b]
		assertEquals(1, provided.size());
		// [b, b, b, b]
		assertEquals(4, processed.size());
		// []
		assertEquals(0, recovered.size());
		assertEquals(0, stepExecution.getItemCount());
	}

	@Test
	public void testCacheLimitWithRetry() throws Exception {
		factory.setRetryLimit(2);
		factory.setCommitInterval(3);
		// sufficiently high so we never hit it
		factory.setSkipLimit(10);
		// set the cache limit lower than the number of unique un-recovered
		// errors expected
		factory.setCacheCapacity(2);
		ItemReader<String> provider = new AbstractItemReader<String>() {
			public String read() {
				String item = ""+count;
				provided.add(item);
				count++;
				if (count >= 10) {
					// prevent infinite loop in worst case scenario
					return null;
				}
				return item;
			}
		};
		ItemWriter<String> itemWriter = new ItemWriter<String>() {
			public void write(List<? extends String> item) throws Exception {
				processed.addAll(item);
				logger.debug("Write Called! Item: [" + item + "]");
				throw new RuntimeException("Write error - planned but retryable.");
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		try {
			step.execute(stepExecution);
			fail("Expected RetryCacheCapacityExceededException");
		}
		catch (RetryCacheCapacityExceededException e) {
			// expected
		}

		assertEquals(1, stepExecution.getSkipCount());
		// only one item processed but three (the commit interval) were provided
		// [0, 1, 2]
		assertEquals(3, provided.size());
		// TODO: this is a bug: 0 was skipped but it came back in the buffer for the second try
		// [0, 0, 1, 0, 0]
		assertEquals(5, processed.size());
		// []
		assertEquals(0, recovered.size());
	}
}
