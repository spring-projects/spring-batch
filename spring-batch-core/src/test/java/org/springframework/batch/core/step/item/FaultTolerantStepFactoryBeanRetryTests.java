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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.SkipListenerSupport;
import org.springframework.batch.core.repository.dao.MapExecutionContextDao;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.retry.policy.MapRetryContextCache;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 * 
 */
public class FaultTolerantStepFactoryBeanRetryTests {

	protected final Log logger = LogFactory.getLog(getClass());

	private FaultTolerantStepFactoryBean<String, String> factory = new FaultTolerantStepFactoryBean<String, String>();

	private List<Object> recovered = new ArrayList<Object>();

	private List<Object> processed = new ArrayList<Object>();

	private List<Object> provided = new ArrayList<Object>();

	private List<Object> written = TransactionAwareProxyFactory.createTransactionalList();

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
		factory.setRetryableExceptionClasses(new HashSet<Class<? extends Throwable>>() {
			{
				add(Exception.class);
			}
		});
		factory.setCommitInterval(1); // trivial by default

		JobParameters jobParameters = new JobParametersBuilder().addString("statefulTest", "make_this_unique")
				.toJobParameters();
		jobExecution = repository.createJobExecution("job", jobParameters);
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
		ItemReader<String> provider = new ListItemReader<String>(Arrays.asList("a", "b", "c")) {
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
		factory.setSkippableExceptionClasses(new HashSet<Class<? extends Throwable>>());
		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);

		assertEquals(0, stepExecution.getSkipCount());

		// [a, b with error]
		assertEquals(2, provided.size());
		// [a]
		assertEquals(1, processed.size());
		// []
		assertEquals(0, recovered.size());
		assertEquals(1, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getReadSkipCount());
	}

	@Test
	public void testSkipAndRetry() throws Exception {
		
		factory.setSkipLimit(2);
		ItemReader<String> provider = new ListItemReader<String>(Arrays.asList("a", "b", "c", "d", "e", "f")) {
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
		repository.add(stepExecution);
		step.execute(stepExecution);

		assertEquals(2, stepExecution.getSkipCount());
		// b is processed once and skipped, plus 1, plus c, plus the null at end
		assertEquals(7, count);
		assertEquals(4, stepExecution.getReadCount());
	}

	@Test
	public void testSkipAndRetryWithWriteFailure() throws Exception {

		factory.setListeners(new StepListener[] { new SkipListenerSupport<String,String>() {
			public void onSkipInWrite(String item, Throwable t) {
				recovered.add(item);
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
			}
		} });
		factory.setSkipLimit(2);
		ItemReader<String> provider = new ListItemReader<String>(Arrays.asList("a", "b", "c", "d", "e", "f")) {
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
				written.addAll(item);
				if (item.contains("b") || item.contains("d")) {
					throw new RuntimeException("Write error - planned but recoverable.");
				}
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		factory.setRetryLimit(5);
		factory.setRetryableExceptionClasses(new HashSet<Class<? extends Throwable>>() {
			{
				add(RuntimeException.class);
			}
		});
		AbstractStep step = (AbstractStep) factory.getObject();
		step.setName("mytest");
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);

		assertEquals(2, recovered.size());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getWriteSkipCount());

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,c,e,f"));
		assertEquals(expectedOutput, written);

		// [a, b, c, d, e, f, null]
		assertEquals(7, provided.size());
		// [a, b, b, b, b, b, c, d, d, d, d, d, e, f]
		assertEquals(14, processed.size());
		// [b, d]
		assertEquals(2, recovered.size());
	}

	@Test
	public void testSkipAndRetryWithWriteFailureAndNonTrivialCommitInterval() throws Exception {

		factory.setCommitInterval(3);
		factory.setListeners(new StepListener[] { new SkipListenerSupport<String,String>() {
			public void onSkipInWrite(String item, Throwable t) {
				recovered.add(item);
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
			}
		} });
		factory.setSkipLimit(2);
		ItemReader<String> provider = new ListItemReader<String>(Arrays.asList("a", "b", "c", "d", "e", "f")) {
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
				written.addAll(item);
				if (item.contains("b") || item.contains("d")) {
					throw new RuntimeException("Write error - planned but recoverable.");
				}
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		factory.setRetryLimit(5);
		factory.setRetryableExceptionClasses(new HashSet<Class<? extends Throwable>>() {
			{
				add(RuntimeException.class);
			}
		});
		AbstractStep step = (AbstractStep) factory.getObject();
		step.setName("mytest");
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);

		assertEquals(2, recovered.size());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getWriteSkipCount());

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("a,c,e,f"));
		assertEquals(expectedOutput, written);

		// [a, b, c, d, e, f, null]
		assertEquals(7, provided.size());
		// [a, b, c, a, b, c, a, b, c, a, b, c, a, b, c, a, b, a, c, d, e, f, d,
		// e, f, d, e, f, d, e, f, d, e, f, d, e, f]
		assertEquals(37, processed.size());
		// [b, d]
		assertEquals(2, recovered.size());
	}

	@Test
	public void testRetryWithNoSkip() throws Exception {
		
		factory.setRetryLimit(4);
		factory.setSkipLimit(0);
		ItemReader<String> provider = new ListItemReader<String>(Arrays.asList("b")) {
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
				written.addAll(item);
				logger.debug("Write Called! Item: [" + item + "]");
				throw new RuntimeException("Write error - planned but retryable.");
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals(BatchStatus.INCOMPLETE, stepExecution.getStatus());	
			
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray(""));
		assertEquals(expectedOutput, written);

		assertEquals(0, stepExecution.getSkipCount());
		// [b]
		assertEquals(1, provided.size());
		// the failed items are tried up to the limit (but only precisely so if
		// the commit interval is 1)
		// [b, b, b, b]
		assertEquals(4, processed.size());
		// []
		assertEquals(0, recovered.size());
		assertEquals(1, stepExecution.getReadCount());
	}

	@Test
	public void testNonSkippableException() throws Exception {

		// Very specific skippable exception
		factory.setSkippableExceptionClasses(new HashSet<Class<? extends Throwable>>() {
			{
				add(UnsupportedOperationException.class);
			}
		});
		// ...which is not retryable...
		factory.setRetryableExceptionClasses(new HashSet<Class<? extends Throwable>>());

		factory.setSkipLimit(1);
		ItemReader<String> provider = new ListItemReader<String>(Arrays.asList("b")) {
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
				written.addAll(item);
				logger.debug("Write Called! Item: [" + item + "]");
				throw new RuntimeException("Write error - planned but not skippable.");
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);
		String message = stepExecution.getFailureExceptions().get(0).getMessage();
		assertTrue("Wrong message: " + message, message.contains("Write error - planned but not skippable."));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray(""));
		assertEquals(expectedOutput, written);

		assertEquals(0, stepExecution.getSkipCount());
		// [b]
		assertEquals(1, provided.size());
		// [b]
		assertEquals(1, processed.size());
		// []
		assertEquals(0, recovered.size());
		assertEquals(1, stepExecution.getReadCount());
	}

	@Test
	public void testRetryPolicy() throws Exception {
		factory.setRetryPolicy(new SimpleRetryPolicy(4));
		factory.setSkipLimit(0);
		ItemReader<String> provider = new ListItemReader<String>(Arrays.asList("b")) {
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
				written.addAll(item);
				logger.debug("Write Called! Item: [" + item + "]");
				throw new RuntimeException("Write error - planned but retryable.");
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals(BatchStatus.INCOMPLETE, stepExecution.getStatus());
		
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray(""));
		assertEquals(expectedOutput, written);

		assertEquals(0, stepExecution.getSkipCount());
		// [b]
		assertEquals(1, provided.size());
		// [b, b, b, b]
		assertEquals(4, processed.size());
		// []
		assertEquals(0, recovered.size());
		assertEquals(1, stepExecution.getReadCount());
	}

	@Test
	public void testCacheLimitWithRetry() throws Exception {
		factory.setRetryLimit(2);
		factory.setCommitInterval(3);
		// sufficiently high so we never hit it
		factory.setSkipLimit(10);
		// set the cache limit stupidly low
		factory.setRetryContextCache(new MapRetryContextCache(0));
		ItemReader<String> provider = new ItemReader<String>() {
			public String read() {
				String item = "" + count;
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
		repository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals(BatchStatus.INCOMPLETE, stepExecution.getStatus());
		
		// We added a bogus cache so no items are actually skipped
		// because they aren't recognised as eligible
		assertEquals(0, stepExecution.getSkipCount());
		// [0, 1, 2]
		assertEquals(3, provided.size());
		// [0, 1, 2]
		assertEquals(3, processed.size());
		// []
		assertEquals(0, recovered.size());
	}
}
