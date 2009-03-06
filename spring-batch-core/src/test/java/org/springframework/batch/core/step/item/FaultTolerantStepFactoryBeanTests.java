package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.SkipListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.util.StringUtils;

/**
 * Tests for {@link FaultTolerantStepFactoryBean}.
 */
public class FaultTolerantStepFactoryBeanTests {

	protected final Log logger = LogFactory.getLog(getClass());

	private FaultTolerantStepFactoryBean<String, String> factory = new FaultTolerantStepFactoryBean<String, String>();

	@SuppressWarnings("unchecked")
	private Collection<Class<? extends Throwable>> skippableExceptions = new HashSet<Class<? extends Throwable>>(Arrays
			.<Class<? extends Throwable>> asList(SkippableException.class, SkippableRuntimeException.class));

	private SkipReaderStub reader = new SkipReaderStub();

	private SkipWriterStub writer = new SkipWriterStub();

	private JobExecution jobExecution;

	private StepExecution stepExecution;

	private JobRepository repository;

	private List<String> processed = new ArrayList<String>();

	protected int count;

	private Collection<String> NO_FAILURES = Collections.emptyList();

	@Before
	public void setUp() throws Exception {
		factory.setBeanName("stepName");
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setCommitInterval(2);
		factory.setItemReader(reader);
		factory.setItemWriter(writer);
		factory.setSkippableExceptionClasses(skippableExceptions);
		factory.setSkipLimit(2);

		MapJobRepositoryFactoryBean.clear();
		MapJobRepositoryFactoryBean repositoryFactory = new MapJobRepositoryFactoryBean();
		repositoryFactory.setTransactionManager(new ResourcelessTransactionManager());
		repositoryFactory.afterPropertiesSet();
		repository = (JobRepository) repositoryFactory.getObject();
		factory.setJobRepository(repository);

		jobExecution = repository.createJobExecution("skipJob", new JobParameters());
		stepExecution = jobExecution.createStepExecution(factory.getName());
		repository.add(stepExecution);
	}

	/**
	 * Non-skippable (and non-fatal) exception causes failure immediately.
	 * @throws Exception
	 */
	@Test
	public void testNonSkippableExceptionOnRead() throws Exception {

		// nothing is skippable
		Collection<Class<? extends Throwable>> empty = Collections.emptySet();
		factory.setSkippableExceptionClasses(empty);

		// no exceptions on write
		factory.setItemWriter(new ItemWriter<String>() {
			public void write(List<? extends String> items) throws Exception {
				logger.debug(items);
			}
		});

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		assertTrue(stepExecution.getExitStatus().getExitDescription().contains("Non-skippable exception during read"));

		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	@Test
	public void testNonSkippableException() throws Exception {
		// nothing is skippable
		Collection<Class<? extends Throwable>> empty = Collections.emptySet();
		factory.setSkippableExceptionClasses(empty);
		factory.setCommitInterval(1);

		// no failures on read
		reader = new SkipReaderStub(new String[] { "1", "2", "3", "4", "5" }, new ArrayList<String>());
		factory.setItemReader(reader);
		factory.setItemWriter(new ItemWriter<String>() {

			public void write(List<? extends String> items) throws Exception {
				throw new RuntimeException("non-skippable exception");
			}

		});

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(1, reader.processed.size());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		assertTrue(stepExecution.getExitStatus().getExitDescription().contains("non-skippable exception"));
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testReadSkip() throws Exception {

		writer = new SkipWriterStub(NO_FAILURES);
		factory.setItemWriter(writer);
		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		System.err.println(writer.written);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(1, stepExecution.getReadSkipCount());
		assertEquals(4, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(0, stepExecution.getRollbackCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertTrue(reader.processed.contains("4"));
		assertFalse(reader.processed.contains("2"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,3,4,5"));
		assertEquals(expectedOutput, writer.written);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testProcessSkip() throws Exception {

		reader = new SkipReaderStub(new String[] { "1", "2", "3", "4", "5" }, NO_FAILURES);
		factory.setItemReader(reader);
		writer = new SkipWriterStub(NO_FAILURES);
		factory.setItemWriter(writer);
		SkipProcessorStub processor = new SkipProcessorStub(Arrays.asList(new String[] { "4" }));
		factory.setItemProcessor(processor);
		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(5, stepExecution.getReadCount());
		assertEquals(1, stepExecution.getProcessSkipCount());
		assertEquals(1, stepExecution.getRollbackCount());

		// writer skips "4"
		assertTrue(reader.processed.contains("4"));
		assertFalse(writer.written.contains("4"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,5"));
		assertEquals(expectedOutput, writer.written);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testWriteSkip() throws Exception {

		reader = new SkipReaderStub(new String[] { "1", "2", "3", "4", "5" }, NO_FAILURES);
		factory.setItemReader(reader);
		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(5, stepExecution.getReadCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
		assertEquals(2, stepExecution.getRollbackCount());

		// writer skips "4"
		assertTrue(reader.processed.contains("4"));
		assertFalse(writer.written.contains("4"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,5"));
		assertEquals(expectedOutput, writer.written);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check that rollback write exception does cause rollback when included on
	 * transaction attributes as "no rollback for".
	 */
	@Test
	public void testSkipWithoutRethrow() throws Exception {
		factory.setTransactionAttribute(new DefaultTransactionAttribute() {
			public boolean rollbackOn(Throwable ex) {
				return !(ex instanceof SkippableRuntimeException);
			};
		});
		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(1, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getWriteSkipCount());

		// one rollback for write exception
		assertEquals(1, stepExecution.getRollbackCount());

		assertEquals(4, stepExecution.getReadCount());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Fatal exception should cause immediate termination regardless of other
	 * skip settings (note the fatal exception is also classified as rollback).
	 */
	@Test
	public void testFatalException() throws Exception {
		factory.setFatalExceptionClasses(Collections
				.<Class<? extends Throwable>> singleton(FatalRuntimeException.class));
		factory.setItemWriter(new SkipWriterStub() {
			public void write(List<? extends String> items) {
				throw new FatalRuntimeException("Ouch!");
			}
		});

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertTrue(stepExecution.getFailureExceptions().get(0).getMessage().equals("Ouch!"));
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkipOverLimit() throws Exception {

		factory.setSkipLimit(1);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertTrue(reader.processed.contains("4"));
		assertFalse(writer.written.contains("4"));

		// failure on "4" tripped the skip limit so we never got to "5"
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,3"));
		assertEquals(expectedOutput, writer.written);
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkipOverLimitOnRead() throws Exception {

		reader = new SkipReaderStub(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"), Arrays
				.asList(StringUtils.commaDelimitedListToStringArray("2,3,5")));

		factory.setSkipLimit(3);
		factory.setItemReader(reader);
		factory.setSkippableExceptionClasses(Collections.<Class<? extends Throwable>> singleton(Exception.class));

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());

		assertEquals(3, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertFalse(reader.processed.contains("2"));
		assertTrue(reader.processed.contains("4"));

		// only "1" was ever committed
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1"));
		assertEquals(expectedOutput, writer.written);
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkipListenerFailsOnRead() throws Exception {

		reader = new SkipReaderStub(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"), Arrays
				.asList(StringUtils.commaDelimitedListToStringArray("2,3,5")));

		factory.setSkipLimit(3);
		factory.setItemReader(reader);
		factory.setListeners(new StepListener[] { new SkipListenerSupport<String, String>() {
			@Override
			public void onSkipInRead(Throwable t) {
				throw new RuntimeException("oops");
			}
		} });
		factory.setSkippableExceptionClasses(Collections.<Class<? extends Throwable>> singleton(Exception.class));

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("oops", stepExecution.getFailureExceptions().get(0).getCause().getMessage());

		// listeners are called only once chunk is about to commit, so
		// listener failure does not affect other statistics
		assertEquals(3, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkipListenerFailsOnWrite() throws Exception {

		reader = new SkipReaderStub(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"), Collections
				.<String> emptyList());

		factory.setSkipLimit(3);
		factory.setItemReader(reader);
		factory.setListeners(new StepListener[] { new SkipListenerSupport<String, String>() {
			@Override
			public void onSkipInWrite(String item, Throwable t) {
				throw new RuntimeException("oops");
			}
		} });
		factory.setSkippableExceptionClasses(Collections.<Class<? extends Throwable>> singleton(Exception.class));

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("oops", stepExecution.getFailureExceptions().get(0).getCause().getMessage());
		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkipOnReadNotDoubleCounted() throws Exception {

		reader = new SkipReaderStub(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"), Arrays
				.asList(StringUtils.commaDelimitedListToStringArray("2,3,5")));

		factory.setSkipLimit(4);
		factory.setItemReader(reader);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(4, stepExecution.getSkipCount());
		assertEquals(3, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());

		// skipped 2,3,4,5
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,6"));
		assertEquals(expectedOutput, writer.written);

		// reader exceptions should not cause rollback, 1 writer exception
		// causes 2 rollbacks
		assertEquals(2, stepExecution.getRollbackCount());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkipOnWriteNotDoubleCounted() throws Exception {

		reader = new SkipReaderStub(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6,7"), Arrays
				.asList(StringUtils.commaDelimitedListToStringArray("2,3")));

		writer = new SkipWriterStub(Arrays.asList(StringUtils.commaDelimitedListToStringArray("4,5")));

		factory.setSkipLimit(4);
		factory.setItemReader(reader);
		factory.setItemWriter(writer);
		factory.setCommitInterval(3); // includes all expected skips

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(4, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getReadSkipCount());
		assertEquals(2, stepExecution.getWriteSkipCount());

		// skipped 2,3,4,5
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,6,7"));
		assertEquals(expectedOutput, writer.written);
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	@Test
	public void testDefaultSkipPolicy() throws Exception {
		factory.setSkippableExceptionClasses(Collections.<Class<? extends Throwable>> singleton(Exception.class));
		factory.setSkipLimit(1);
		List<String> items = Arrays.asList(new String[] { "a", "b", "c" });
		ItemReader<String> provider = new ListItemReader<String>(items) {
			public String read() {
				String item = super.read();
				count++;
				if ("b".equals(item)) {
					throw new RuntimeException("Read error - planned failure.");
				}
				return item;
			}
		};
		factory.setItemReader(provider);
		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		// b is processed once and skipped, plus 1, plus c, plus the null at end
		assertEquals(4, count);
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkipOverLimitOnReadWithAllSkipsAtEnd() throws Exception {

		reader = new SkipReaderStub(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15"),
				Arrays.asList(StringUtils.commaDelimitedListToStringArray("6,12,13,14,15")));

		factory.setCommitInterval(5);
		factory.setSkipLimit(3);
		factory.setItemReader(reader);
		factory.setSkippableExceptionClasses(Collections.<Class<? extends Throwable>> singleton(Exception.class));

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("bad skip count", 3, stepExecution.getSkipCount());
		assertEquals("bad read skip count", 2, stepExecution.getReadSkipCount());
		assertEquals("bad write skip count", 1, stepExecution.getWriteSkipCount());

		// writer did not skip "6" as it never made it to writer, only "4" did
		assertFalse(reader.processed.contains("6"));
		assertTrue(reader.processed.contains("4"));

		// only "1" was ever committed
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,5,7,8,9,10,11"));
		assertEquals(expectedOutput, writer.written);
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Scenario: Exception in processor that shouldn't cause rollback
	 */
	@Test
	public void testProcessorNoRollback() throws Exception {
		factory.setTransactionAttribute(new DefaultTransactionAttribute());
		SkipProcessorStub processor = new SkipProcessorStub(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("1,3")));
		factory.setItemProcessor(processor);

		factory.setItemReader(new SkipReaderStub(new String[] { "1", "2", "3", "4" }, NO_FAILURES));
		factory.setItemWriter(new SkipWriterStub(NO_FAILURES));

		Step step = (Step) factory.getObject();

		processor.rollback = false;
		step.execute(stepExecution);
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getRollbackCount());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Scenario: Exception in processor that should cause rollback
	 */
	@Test
	public void testProcessorRollback() throws Exception {
		SkipProcessorStub processor = new SkipProcessorStub(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("1,3")));
		factory.setItemProcessor(processor);

		factory.setItemReader(new SkipReaderStub(new String[] { "1", "2", "3", "4" }, NO_FAILURES));
		factory.setItemWriter(new SkipWriterStub(NO_FAILURES));

		Step step = (Step) factory.getObject();

		processor.rollback = true;
		step.execute(stepExecution);
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getRollbackCount());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	@Test
	public void testReprocessingAfterWriterRollback() throws Exception {
		factory.setItemProcessor(new ItemProcessor<String, String>() {
			public String process(String item) throws Exception {
				processed.add(item);
				return item;
			}
		});
		factory.setItemReader(new SkipReaderStub(new String[] { "1", "2", "3", "4" }, NO_FAILURES));

		Step step = (Step) factory.getObject();
		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getRollbackCount());

		// 1,2,3,4,3,4,3 - two re-processing attempts until the item is
		// identified and skipped
		assertEquals(7, processed.size());
		assertEquals("[1, 2, 3, 4, 3, 4, 3]", processed.toString());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));

	}

	@Test
	public void testAutoRegisterItemListeners() throws Exception {

		final List<Integer> listenerCalls = new ArrayList<Integer>();

		class TestItemListenerWriter implements ItemWriter<String>, ItemReadListener<String>,
				ItemWriteListener<String>, ItemProcessListener<String, String>, SkipListener<String, String>,
				ChunkListener {
			public void write(List<? extends String> items) throws Exception {
				if (items.contains("4")) {
					throw new SkippableException("skippable");
				}
			}

			public void afterRead(String item) {
				listenerCalls.add(1);
			}

			public void beforeRead() {
			}

			public void onReadError(Exception ex) {
			}

			public void afterWrite(List<? extends String> items) {
				listenerCalls.add(2);
			}

			public void beforeWrite(List<? extends String> items) {
			}

			public void onWriteError(Exception exception, List<? extends String> items) {
			}

			public void afterProcess(String item, String result) {
				listenerCalls.add(3);
			}

			public void beforeProcess(String item) {
			}

			public void onProcessError(String item, Exception e) {
			}

			public void afterChunk() {
				listenerCalls.add(4);
			}

			public void beforeChunk() {
			}

			public void onSkipInProcess(String item, Throwable t) {
			}

			public void onSkipInRead(Throwable t) {
				listenerCalls.add(6);
			}

			public void onSkipInWrite(String item, Throwable t) {
				listenerCalls.add(5);
			}

		}

		factory.setItemWriter(new TestItemListenerWriter());

		Step step = (Step) factory.getObject();
		step.execute(stepExecution);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		for (int i = 1; i <= 6; i++) {
			assertTrue("didn't call listener " + i, listenerCalls.contains(i));
		}
	}

	private static class SkipProcessorStub implements ItemProcessor<String, String> {

		private final Collection<String> failures;

		private boolean rollback = false;

		public SkipProcessorStub(Collection<String> failures) {
			this.failures = failures;
		}

		public String process(String item) throws Exception {
			if (failures.contains(item)) {
				if (rollback) {
					throw new SkippableRuntimeException("should cause rollback");
				}
				else {
					throw new SkippableException("shouldn't cause rollback");
				}
			}
			return item;
		}

	}

	/**
	 * Simple item reader that supports skip functionality.
	 */
	private static class SkipReaderStub implements ItemReader<String> {

		protected final Log logger = LogFactory.getLog(getClass());

		private final String[] items;

		private Collection<String> processed = new ArrayList<String>();

		private int counter = -1;

		private final Collection<String> failures;

		public SkipReaderStub() {
			this(new String[] { "1", "2", "3", "4", "5" }, Collections.singleton("2"));
		}

		public SkipReaderStub(String[] items, Collection<String> failures) {
			this.items = items;
			this.failures = failures;
		}

		public String read() throws Exception, UnexpectedInputException, ParseException {
			counter++;
			if (counter >= items.length) {
				logger.debug("Returning null at count=" + counter);
				return null;
			}
			String item = items[counter];
			if (failures.contains(item)) {
				logger.debug("Throwing exception for [" + item + "] at count=" + counter);
				throw new SkippableException("exception in reader");
			}
			processed.add(item);
			logger.debug("Returning [" + item + "] at count=" + counter);
			return item;
		}

	}

	/**
	 * Simple item writer that supports skip functionality.
	 */
	private static class SkipWriterStub implements ItemWriter<String> {

		protected final Log logger = LogFactory.getLog(getClass());

		// simulate transactional output
		private List<Object> written = TransactionAwareProxyFactory.createTransactionalList();

		private final Collection<String> failures;

		public SkipWriterStub() {
			this(Arrays.asList("4"));
		}

		/**
		 * @param failures commaDelimitedListToSet
		 */
		public SkipWriterStub(Collection<String> failures) {
			this.failures = failures;
		}

		public void write(List<? extends String> items) throws Exception {
			for (String item : items) {
				if (failures.contains(item)) {
					logger.debug("Throwing write exception on [" + item + "]");
					throw new SkippableRuntimeException("exception in writer");
				}
				written.add(item);
			}
		}

	}

	private static class SkippableException extends Exception {
		public SkippableException(String message) {
			super(message);
		}
	}

	private static class SkippableRuntimeException extends RuntimeException {
		public SkippableRuntimeException(String message) {
			super(message);
		}
	}

	private static class FatalRuntimeException extends SkippableRuntimeException {
		public FatalRuntimeException(String message) {
			super(message);
		}
	}

	private void assertStepExecutionsAreEqual(StepExecution expected, StepExecution actual) {
		assertEquals(expected.getId(), actual.getId());
		assertEquals(expected.getStartTime(), actual.getStartTime());
		assertEquals(expected.getEndTime(), actual.getEndTime());
		assertEquals(expected.getSkipCount(), actual.getSkipCount());
		assertEquals(expected.getCommitCount(), actual.getCommitCount());
		assertEquals(expected.getReadCount(), actual.getReadCount());
		assertEquals(expected.getWriteCount(), actual.getWriteCount());
		assertEquals(expected.getFilterCount(), actual.getFilterCount());
		assertEquals(expected.getWriteSkipCount(), actual.getWriteSkipCount());
		assertEquals(expected.getReadSkipCount(), actual.getReadSkipCount());
		assertEquals(expected.getProcessSkipCount(), actual.getProcessSkipCount());
		assertEquals(expected.getRollbackCount(), actual.getRollbackCount());
		assertEquals(expected.getExitStatus(), actual.getExitStatus());
		assertEquals(expected.getLastUpdated(), actual.getLastUpdated());
		assertEquals(expected.getExitStatus(), actual.getExitStatus());
		assertEquals(expected.getJobExecutionId(), actual.getJobExecutionId());
	}
}
