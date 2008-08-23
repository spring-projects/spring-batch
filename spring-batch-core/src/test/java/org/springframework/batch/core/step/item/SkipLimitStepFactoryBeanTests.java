package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.SkipListenerSupport;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NoWorkFoundException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.util.StringUtils;

/**
 * Tests for {@link SkipLimitStepFactoryBean}.
 */
public class SkipLimitStepFactoryBeanTests {

	protected final Log logger = LogFactory.getLog(getClass());

	private SkipLimitStepFactoryBean<String, String> factory = new SkipLimitStepFactoryBean<String, String>();

	private Class<?>[] skippableExceptions = new Class[] { SkippableException.class, SkippableRuntimeException.class };

	private SkipReaderStub reader = new SkipReaderStub();

	private SkipWriterStub writer = new SkipWriterStub();

	private JobExecution jobExecution;

	protected int count;

	@Before
	public void setUp() throws Exception {
		factory.setBeanName("stepName");
		factory.setJobRepository(new JobRepositorySupport());
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setCommitInterval(2);
		factory.setItemReader(reader);
		factory.setItemWriter(writer);
		factory.setSkippableExceptionClasses(skippableExceptions);
		factory.setSkipLimit(2);

		JobInstance jobInstance = new JobInstance(new Long(1), new JobParameters(), "skipJob");
		jobExecution = new JobExecution(jobInstance);
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkip() throws Exception {

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);

		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(1, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());

		// only write exception caused rollback, but more than once because it
		// has to go back and split the chunk up to isolate the failed item
		assertEquals(3, stepExecution.getRollbackCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertTrue(reader.processed.contains("4"));
		assertFalse(writer.written.contains("4"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,3,5"));
		assertEquals(expectedOutput, writer.written);

		assertEquals(4, stepExecution.getItemCount());

	}

	/**
	 * Check skippable write exception does not cause rollback when included on
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

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);

		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(1, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());

		// no rollbacks
		assertEquals(0, stepExecution.getRollbackCount());

		assertEquals(4, stepExecution.getItemCount());

	}

	/**
	 * Fatal exception should cause immediate termination regardless of other
	 * skip settings (note the fatal exception is also classified as skippable).
	 */
	@Test
	public void testFatalException() throws Exception {
		factory.setFatalExceptionClasses(new Class[] { FatalRuntimeException.class });
		factory.setItemWriter(new SkipWriterStub() {
			public void write(List<? extends String> items) {
				throw new FatalRuntimeException("Ouch!");
			}
		});

		Step step = (Step) factory.getObject();
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		try {
			step.execute(stepExecution);
			fail();
		}
		catch (FatalRuntimeException expected) {
			assertTrue(expected.getMessage().equals("Ouch!"));
		}
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkipOverLimit() throws Exception {

		factory.setSkipLimit(1);

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		try {
			step.execute(stepExecution);
			fail("Expected SkipLimitExceededException.");
		}
		catch (SkipLimitExceededException e) {
		}

		assertEquals(1, stepExecution.getSkipCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertTrue(reader.processed.contains("4"));
		assertFalse(writer.written.contains("4"));

		// failure on "4" tripped the skip limit so we never got to "5"
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,3"));
		assertEquals(expectedOutput, writer.written);

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
		factory.setSkippableExceptionClasses(new Class[] { Exception.class });

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		try {
			step.execute(stepExecution);
			fail("Expected SkipLimitExceededException.");
		}
		catch (SkipLimitExceededException e) {
		}

		assertEquals(3, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertFalse(reader.processed.contains("2"));
		assertTrue(reader.processed.contains("4"));

		// "1" was sent to writer but never comitted
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray(""));
		assertEquals(expectedOutput, writer.written);

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
		factory.setListeners(new StepListener[] { new SkipListenerSupport() {
			@Override
			public void onSkipInRead(Throwable t) {
				throw new RuntimeException("oops");
			}
		} });
		factory.setSkippableExceptionClasses(new Class[] { Exception.class });

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		try {
			step.execute(stepExecution);
			fail("Expected SkipListenerFailedException.");
		}
		catch (SkipListenerFailedException e) {
			assertEquals("oops", e.getCause().getMessage());
		}

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(1, stepExecution.getReadSkipCount());
		assertEquals(0, stepExecution.getWriteSkipCount());

	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkipListenerFailsOnWrite() throws Exception {

		reader = new SkipReaderStub(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"), Arrays
				.asList(StringUtils.commaDelimitedListToStringArray("2,3,5")));

		factory.setSkipLimit(3);
		factory.setItemReader(reader);
		factory.setListeners(new StepListener[] { new SkipListenerSupport() {
			@Override
			public void onSkipInWrite(Object item, Throwable t) {
				throw new RuntimeException("oops");
			}
		} });
		factory.setSkippableExceptionClasses(new Class[] { Exception.class });

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		try {
			step.execute(stepExecution);
			fail("Expected SkipListenerFailedException.");
		}
		catch (SkipListenerFailedException e) {
			assertEquals("oops", e.getCause().getMessage());
		}

		assertEquals(3, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());

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

		StepExecution stepExecution = jobExecution.createStepExecution(step);

		// TODO: uncomment this! 
//		step.execute(stepExecution);
//		assertEquals(4, stepExecution.getSkipCount());
//		assertEquals(3, stepExecution.getReadSkipCount());
//		assertEquals(1, stepExecution.getWriteSkipCount());
//
//		// skipped 2,3,4,5
//		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,6"));
//		assertEquals(expectedOutput, writer.written);

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

		StepExecution stepExecution = jobExecution.createStepExecution(step);

		// TODO: uncomment this! 
//		step.execute(stepExecution);
//		assertEquals(4, stepExecution.getSkipCount());
//		assertEquals(2, stepExecution.getReadSkipCount());
//		assertEquals(2, stepExecution.getWriteSkipCount());
//
//		// skipped 2,3,4,5
//		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,6,7"));
//		assertEquals(expectedOutput, writer.written);

	}

	@Test
	public void testDefaultSkipPolicy() throws Exception {
		factory.setSkippableExceptionClasses(new Class[] { Exception.class });
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

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		// b is processed once and skipped, plus 1, plus c, plus the null at end
		assertEquals(4, count);
	}

	// TODO: test with transactional reader (e.g. list with tx proxy)

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

		public String read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {
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

		@SuppressWarnings("unchecked")
		public SkipWriterStub() {
			this(StringUtils.commaDelimitedListToSet("4"));
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

}
