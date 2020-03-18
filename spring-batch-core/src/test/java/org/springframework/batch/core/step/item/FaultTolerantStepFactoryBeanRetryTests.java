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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
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
import org.springframework.batch.core.step.factory.FaultTolerantStepFactoryBean;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.lang.Nullable;
import org.springframework.retry.policy.MapRetryContextCache;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 *
 */
public class FaultTolerantStepFactoryBeanRetryTests {

	protected final Log logger = LogFactory.getLog(getClass());

	private FaultTolerantStepFactoryBean<String, String> factory;

	private List<Object> recovered = new ArrayList<>();

	private List<Object> processed = new ArrayList<>();

	private List<Object> provided = new ArrayList<>();

	private List<Object> written = TransactionAwareProxyFactory
			.createTransactionalList();

	int count = 0;

	boolean fail = false;

	private SimpleJobRepository repository = new SimpleJobRepository(
			new MapJobInstanceDao(), new MapJobExecutionDao(),
			new MapStepExecutionDao(), new MapExecutionContextDao());

	JobExecution jobExecution;

	private ItemWriter<String> writer = new ItemWriter<String>() {
		@Override
		public void write(List<? extends String> data) throws Exception {
			processed.addAll(data);
		}
	};

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {

		factory = new FaultTolerantStepFactoryBean<>();
		factory.setBeanName("step");

		factory.setItemReader(new ListItemReader<>(
				new ArrayList<>()));
		factory.setItemWriter(writer);
		factory.setJobRepository(repository);
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setRetryableExceptionClasses(getExceptionMap(Exception.class));
		factory.setCommitInterval(1); // trivial by default

		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));

		JobParameters jobParameters = new JobParametersBuilder().addString(
				"statefulTest", "make_this_unique").toJobParameters();
		jobExecution = repository.createJobExecution("job", jobParameters);
		jobExecution.setEndTime(new Date());

	}

	@Test
	public void testType() throws Exception {
		assertTrue(Step.class.isAssignableFrom(factory.getObjectType()));
	}

	@SuppressWarnings("cast")
	@Test
	public void testDefaultValue() throws Exception {
		assertTrue(factory.getObject() instanceof Step);
	}

	@Test
	public void testProcessAllItemsWhenErrorInWriterTransformationWhenReaderTransactional()
			throws Exception {
		final int RETRY_LIMIT = 3;
		final List<String> ITEM_LIST =  TransactionAwareProxyFactory.createTransactionalList(Arrays.asList("1", "2", "3"));
		FaultTolerantStepFactoryBean<String, Integer> factory = new FaultTolerantStepFactoryBean<>();
		factory.setBeanName("step");

		factory.setJobRepository(repository);
		factory.setTransactionManager(new ResourcelessTransactionManager());
		ItemWriter<Integer> failingWriter = new ItemWriter<Integer>() {
			@Override
			public void write(List<? extends Integer> data) throws Exception {
				int count = 0;
				for (Integer item : data) {
					if (count++ == 2) {
						throw new Exception("Planned failure in writer");
					}
					written.add(item);
				}
			}
		};

		ItemProcessor<String, Integer> processor = new ItemProcessor<String, Integer>() {
			@Nullable
			@Override
			public Integer process(String item) throws Exception {
				processed.add(item);
				return Integer.parseInt(item);
			}
		};
		ItemReader<String> reader = new ListItemReader<>(TransactionAwareProxyFactory.createTransactionalList(ITEM_LIST));
		factory.setCommitInterval(3);
		factory.setRetryLimit(RETRY_LIMIT);
		factory.setSkipLimit(1);
		factory.setIsReaderTransactionalQueue(true);
		@SuppressWarnings("unchecked")
		Map<Class<? extends Throwable>, Boolean> exceptionMap = getExceptionMap(Exception.class);
		factory.setSkippableExceptionClasses(exceptionMap);
		factory.setRetryableExceptionClasses(exceptionMap);
		factory.setItemReader(reader);
		factory.setItemProcessor(processor);
		factory.setItemWriter(failingWriter);
		Step step = factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(),
				jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);
		/*
		 * Each chunk tried up to RETRY_LIMIT, then the scan processes each item
		 * once, identifying the skip as it goes
		 */
		assertEquals((RETRY_LIMIT +1) * ITEM_LIST.size(), processed.size());
	}

	@Test
	public void testProcessAllItemsWhenErrorInWriter() throws Exception {
		final int RETRY_LIMIT = 3;
		final List<String> ITEM_LIST = Arrays.asList("a", "b", "c");
		ItemWriter<String> failingWriter = new ItemWriter<String>() {
			@Override
			public void write(List<? extends String> data) throws Exception {
				int count = 0;
				for (String item : data) {
					if (count++ == 2) {
						throw new Exception("Planned failure in writer");
					}
					written.add(item);
				}
			}
		};

		ItemProcessor<String, String> processor = new ItemProcessor<String, String>() {
			@Nullable
			@Override
			public String process(String item) throws Exception {
				processed.add(item);
				return item;
			}
		};
		ItemReader<String> reader = new ListItemReader<>(ITEM_LIST);
		factory.setCommitInterval(3);
		factory.setRetryLimit(RETRY_LIMIT);
		factory.setSkipLimit(1);
		@SuppressWarnings("unchecked")
		Map<Class<? extends Throwable>, Boolean> exceptionMap = getExceptionMap(Exception.class);
		factory.setSkippableExceptionClasses(exceptionMap);
		factory.setItemReader(reader);
		factory.setItemProcessor(processor);
		factory.setItemWriter(failingWriter);
		Step step = factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(),
				jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals(ExitStatus.COMPLETED.getExitCode(), stepExecution
				.getExitStatus().getExitCode());
		/*
		 * Each chunk tried up to RETRY_LIMIT, then the scan processes each item
		 * once, identifying the skip as it goes
		 */
		assertEquals((RETRY_LIMIT +1) * ITEM_LIST.size(), processed.size());
	}

	@Test
	public void testNoItemsReprocessedWhenErrorInWriterAndProcessorNotTransactional()
			throws Exception {
		ItemWriter<String> failingWriter = new ItemWriter<String>() {
			@Override
			public void write(List<? extends String> data) throws Exception {
				int count = 0;
				for (String item : data) {
					if (count++ == 2) {
						throw new Exception("Planned failure in writer");
					}
					written.add(item);
				}
			}
		};

		ItemProcessor<String, String> processor = new ItemProcessor<String, String>() {
			@Nullable
			@Override
			public String process(String item) throws Exception {
				processed.add(item);
				return item;
			}
		};
		ItemReader<String> reader = new ListItemReader<>(Arrays.asList(
				"a", "b", "c"));
		factory.setProcessorTransactional(false);
		factory.setCommitInterval(3);
		factory.setRetryLimit(3);
		factory.setSkippableExceptionClasses(new HashMap<>());
		factory.setItemReader(reader);
		factory.setItemProcessor(processor);
		factory.setItemWriter(failingWriter);
		Step step = factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(),
				jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals(3, processed.size()); // Initial try only, then cached
	}

	/**
	 * N.B. this doesn't really test retry, since the retry is only on write
	 * failures, but it does test that read errors are re-presented for another
	 * try when the retryLimit is high enough (it is used to build an exception
	 * handler).
	 *
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSuccessfulRetryWithReadFailure() throws Exception {
		ItemReader<String> provider = new ListItemReader<String>(Arrays.asList(
				"a", "b", "c")) {
			@Nullable
			@Override
			public String read() {
				String item = super.read();
				provided.add(item);
				count++;
				if (count == 2) {
					throw new RuntimeException(
							"Temporary error - retry for success.");
				}
				return item;
			}
		};
		factory.setItemReader(provider);
		factory.setRetryLimit(10);
		factory.setSkippableExceptionClasses(getExceptionMap());
		Step step = factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(),
				jobExecution);
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
	public void testRestartAfterFailedWrite() throws Exception {

		factory.setSkipLimit(0);
		factory.setCommitInterval(3);
		AbstractItemCountingItemStreamItemReader<String> reader = new AbstractItemCountingItemStreamItemReader<String>() {

			private ItemReader<String> reader;

			@Override
			protected void doClose() throws Exception {
				reader = null;
			}

			@Override
			protected void doOpen() throws Exception {
				reader = new ListItemReader<>(Arrays.asList("a", "b",
						"c", "d", "e", "f"));
			}

			@Nullable
			@Override
			protected String doRead() throws Exception {
				return reader.read();
			}

		};
		// Need to set name or else reader will fail to open
		reader.setName("foo");
		factory.setItemReader(reader);
		factory.setStreams(new ItemStream[] { reader });
		factory.setItemWriter(new ItemWriter<String>() {
			@Override
			public void write(List<? extends String> items) throws Exception {
				if (fail && items.contains("e")) {
					throw new RuntimeException("Planned failure");
				}
				processed.addAll(items);
			}
		});
		factory.setRetryLimit(0);
		Step step = factory.getObject();

		fail = true;
		StepExecution stepExecution = new StepExecution(step.getName(),
				jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);

		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(4, stepExecution.getWriteCount());
		assertEquals(6, stepExecution.getReadCount());

		fail = false;
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		stepExecution = new StepExecution(step.getName(), jobExecution);
		stepExecution.setExecutionContext(executionContext);
		repository.add(stepExecution);
		step.execute(stepExecution);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getWriteCount());
		assertEquals(2, stepExecution.getReadCount());
	}

	@Test
	public void testSkipAndRetry() throws Exception {

		factory.setSkipLimit(2);
		ItemReader<String> provider = new ListItemReader<String>(Arrays.asList(
				"a", "b", "c", "d", "e", "f")) {
			@Nullable
			@Override
			public String read() {
				String item = super.read();
				count++;
				if ("b".equals(item) || "d".equals(item)) {
					throw new RuntimeException(
							"Read error - planned but skippable.");
				}
				return item;
			}
		};
		factory.setItemReader(provider);
		factory.setRetryLimit(10);
		Step step = factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(),
				jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);

		assertEquals(2, stepExecution.getSkipCount());
		// b is processed once and skipped, plus 1, plus c, plus the null at end
		assertEquals(7, count);
		assertEquals(4, stepExecution.getReadCount());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSkipAndRetryWithWriteFailure() throws Exception {

		factory.setListeners(new StepListener[] { new SkipListenerSupport<String, String>() {
			@Override
			public void onSkipInWrite(String item, Throwable t) {
				recovered.add(item);
				assertTrue(TransactionSynchronizationManager
						.isActualTransactionActive());
			}
		} });
		factory.setSkipLimit(2);
		ItemReader<String> provider = new ListItemReader<String>(Arrays.asList(
				"a", "b", "c", "d", "e", "f")) {
			@Nullable
			@Override
			public String read() {
				String item = super.read();
				logger.debug("Read Called! Item: [" + item + "]");
				provided.add(item);
				count++;
				return item;
			}
		};

		ItemWriter<String> itemWriter = new ItemWriter<String>() {
			@Override
			public void write(List<? extends String> item) throws Exception {
				logger.debug("Write Called! Item: [" + item + "]");
				processed.addAll(item);
				written.addAll(item);
				if (item.contains("b") || item.contains("d")) {
					throw new RuntimeException(
							"Write error - planned but recoverable.");
				}
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		factory.setRetryLimit(5);
		factory.setRetryableExceptionClasses(getExceptionMap(RuntimeException.class));
		AbstractStep step = (AbstractStep) factory.getObject();
		step.setName("mytest");
		StepExecution stepExecution = new StepExecution(step.getName(),
				jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);

		assertEquals(2, recovered.size());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getWriteSkipCount());

		List<String> expectedOutput = Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("a,c,e,f"));
		assertEquals(expectedOutput, written);

		assertEquals("[a, b, c, d, e, f, null]", provided.toString());
		assertEquals("[a, b, b, b, b, b, b, c, d, d, d, d, d, d, e, f]",
				processed.toString());
		assertEquals("[b, d]", recovered.toString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSkipAndRetryWithWriteFailureAndNonTrivialCommitInterval()
			throws Exception {

		factory.setCommitInterval(3);
		factory.setListeners(new StepListener[] { new SkipListenerSupport<String, String>() {
			@Override
			public void onSkipInWrite(String item, Throwable t) {
				recovered.add(item);
				assertTrue(TransactionSynchronizationManager
						.isActualTransactionActive());
			}
		} });
		factory.setSkipLimit(2);
		ItemReader<String> provider = new ListItemReader<String>(Arrays.asList(
				"a", "b", "c", "d", "e", "f")) {
			@Nullable
			@Override
			public String read() {
				String item = super.read();
				logger.debug("Read Called! Item: [" + item + "]");
				provided.add(item);
				count++;
				return item;
			}
		};

		ItemWriter<String> itemWriter = new ItemWriter<String>() {
			@Override
			public void write(List<? extends String> item) throws Exception {
				logger.debug("Write Called! Item: [" + item + "]");
				processed.addAll(item);
				written.addAll(item);
				if (item.contains("b") || item.contains("d")) {
					throw new RuntimeException(
							"Write error - planned but recoverable.");
				}
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		factory.setRetryLimit(5);
		factory.setRetryableExceptionClasses(getExceptionMap(RuntimeException.class));
		AbstractStep step = (AbstractStep) factory.getObject();
		step.setName("mytest");
		StepExecution stepExecution = new StepExecution(step.getName(),
				jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);

		assertEquals(2, recovered.size());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getWriteSkipCount());

		List<String> expectedOutput = Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("a,c,e,f"));
		assertEquals(expectedOutput, written);

		// [a, b, c, d, e, f, null]
		assertEquals(7, provided.size());
		// [a, b, c, a, b, c, a, b, c, a, b, c, a, b, c, a, b, c, d, e, f, d,
		// e, f, d, e, f, d, e, f, d, e, f, d, e, f]
		// System.err.println(processed);
		assertEquals(36, processed.size());
		// [b, d]
		assertEquals(2, recovered.size());
	}

	@Test
	public void testRetryWithNoSkip() throws Exception {

		factory.setRetryLimit(4);
		factory.setSkipLimit(0);
		ItemReader<String> provider = new ListItemReader<String>(
				Arrays.asList("b")) {
			@Nullable
			@Override
			public String read() {
				String item = super.read();
				provided.add(item);
				count++;
				return item;
			}
		};
		ItemWriter<String> itemWriter = new ItemWriter<String>() {
			@Override
			public void write(List<? extends String> item) throws Exception {
				processed.addAll(item);
				written.addAll(item);
				logger.debug("Write Called! Item: [" + item + "]");
				throw new RuntimeException(
						"Write error - planned but retryable.");
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		Step step = factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(),
				jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());

		List<String> expectedOutput = Arrays.asList(StringUtils
				.commaDelimitedListToStringArray(""));
		assertEquals(expectedOutput, written);

		assertEquals(0, stepExecution.getSkipCount());
		// [b]
		assertEquals(1, provided.size());
		// the failed items are tried up to the limit (but only precisely so if
		// the commit interval is 1)
		assertEquals("[b, b, b, b, b]", processed.toString());
		// []
		assertEquals(0, recovered.size());
		assertEquals(1, stepExecution.getReadCount());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNonSkippableException() throws Exception {

		// Very specific skippable exception
		factory.setSkippableExceptionClasses(getExceptionMap(UnsupportedOperationException.class));
		// ...which is not retryable...
		factory.setRetryableExceptionClasses(getExceptionMap());

		factory.setSkipLimit(1);
		ItemReader<String> provider = new ListItemReader<String>(
				Arrays.asList("b")) {
			@Nullable
			@Override
			public String read() {
				String item = super.read();
				provided.add(item);
				count++;
				return item;
			}
		};
		ItemWriter<String> itemWriter = new ItemWriter<String>() {
			@Override
			public void write(List<? extends String> item) throws Exception {
				processed.addAll(item);
				written.addAll(item);
				logger.debug("Write Called! Item: [" + item + "]");
				throw new RuntimeException(
						"Write error - planned but not skippable.");
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		Step step = factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(),
				jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);
		String message = stepExecution.getFailureExceptions().get(0)
				.getMessage();
		assertTrue("Wrong message: " + message,
				message.contains("Write error - planned but not skippable."));

		List<String> expectedOutput = Arrays.asList(StringUtils
				.commaDelimitedListToStringArray(""));
		assertEquals(expectedOutput, written);

		assertEquals(0, stepExecution.getSkipCount());
		// [b]
		assertEquals("[b]", provided.toString());
		// [b]
		assertEquals("[b]", processed.toString());
		// []
		assertEquals(0, recovered.size());
		assertEquals(1, stepExecution.getReadCount());
	}

	@Test
	public void testRetryPolicy() throws Exception {
		factory.setRetryPolicy(new SimpleRetryPolicy(4, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(
						Exception.class, true)));
		factory.setSkipLimit(0);
		ItemReader<String> provider = new ListItemReader<String>(
				Arrays.asList("b")) {
			@Nullable
			@Override
			public String read() {
				String item = super.read();
				provided.add(item);
				count++;
				return item;
			}
		};
		ItemWriter<String> itemWriter = new ItemWriter<String>() {
			@Override
			public void write(List<? extends String> item) throws Exception {
				processed.addAll(item);
				written.addAll(item);
				logger.debug("Write Called! Item: [" + item + "]");
				throw new RuntimeException(
						"Write error - planned but retryable.");
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(),
				jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());

		List<String> expectedOutput = Arrays.asList(StringUtils
				.commaDelimitedListToStringArray(""));
		assertEquals(expectedOutput, written);

		assertEquals(0, stepExecution.getSkipCount());
		// [b]
		assertEquals(1, provided.size());
		assertEquals("[b, b, b, b, b]", processed.toString());
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
			@Nullable
			@Override
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
			@Override
			public void write(List<? extends String> item) throws Exception {
				processed.addAll(item);
				logger.debug("Write Called! Item: [" + item + "]");
				throw new RuntimeException(
						"Write error - planned but retryable.");
			}
		};
		factory.setItemReader(provider);
		factory.setItemWriter(itemWriter);
		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(),
				jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());

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

	@SuppressWarnings("unchecked")
	private Map<Class<? extends Throwable>, Boolean> getExceptionMap(
			Class<? extends Throwable>... args) {
		Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
		for (Class<? extends Throwable> arg : args) {
			map.put(arg, true);
		}
		return map;
	}
}
