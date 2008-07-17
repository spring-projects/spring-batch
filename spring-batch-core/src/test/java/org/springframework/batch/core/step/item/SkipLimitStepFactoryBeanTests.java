package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.NoWorkFoundException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.ResetFailedException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.util.StringUtils;

/**
 * Tests for {@link SkipLimitStepFactoryBean}.
 */
public class SkipLimitStepFactoryBeanTests extends TestCase {

	protected final Log logger = LogFactory.getLog(getClass());

	private SkipLimitStepFactoryBean factory = new SkipLimitStepFactoryBean();

	private Class<?>[] skippableExceptions = new Class[] { SkippableException.class, SkippableRuntimeException.class };

	private final int SKIP_LIMIT = 2;

	private final int COMMIT_INTERVAL = 2;

	private SkipReaderStub reader = new SkipReaderStub();

	private SkipWriterStub writer = new SkipWriterStub();

	private JobExecution jobExecution;

	protected int count;

	protected void setUp() throws Exception {
		factory.setBeanName("stepName");
		factory.setJobRepository(new JobRepositorySupport());
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setCommitInterval(COMMIT_INTERVAL);
		factory.setItemReader(reader);
		factory.setItemWriter(writer);
		factory.setSkippableExceptionClasses(skippableExceptions);
		factory.setSkipLimit(SKIP_LIMIT);

		JobInstance jobInstance = new JobInstance(new Long(1), new JobParameters(), "skipJob");
		jobExecution = new JobExecution(jobInstance);
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	public void testSkip() throws Exception {
		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);

		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(1, stepExecution.getReadSkipCount().intValue());
		assertEquals(1, stepExecution.getWriteSkipCount().intValue());

		// only write exception caused rollback
		assertEquals(1, stepExecution.getRollbackCount().intValue());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertTrue(reader.processed.contains("4"));
		assertFalse(writer.written.contains("4"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,3,5"));
		assertEquals(expectedOutput, writer.written);
		
		assertEquals(4, stepExecution.getItemCount().intValue());

	}

	/**
	 * Check skippable write exception does not cause rollback when included on
	 * transaction attributes as "no rollback for".
	 */
	public void testSkipWithoutRethrow() throws Exception {
		factory.setTransactionAttribute(new DefaultTransactionAttribute() {
			public boolean rollbackOn(Throwable ex) {
				return !(ex instanceof SkippableRuntimeException);
			};
		});
		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);

		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(1, stepExecution.getReadSkipCount().intValue());
		assertEquals(1, stepExecution.getWriteSkipCount().intValue());

		// no rollbacks
		assertEquals(0, stepExecution.getRollbackCount().intValue());
		
		assertEquals(4, stepExecution.getItemCount().intValue());

	}

	/**
	 * Fatal exception should cause immediate termination regardless of other
	 * skip settings (note the fatal exception is also classified as skippable).
	 */
	public void testFatalException() throws Exception {
		factory.setFatalExceptionClasses(new Class[] { FatalRuntimeException.class });
		factory.setItemWriter(new SkipWriterStub() {
			public void write(Object item) {
				throw new FatalRuntimeException("Ouch!");
			}
		});

		AbstractStep step = (AbstractStep) factory.getObject();
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
	public void testSkipOverLimit() throws Exception {

		factory.setSkipLimit(1);

		AbstractStep step = (AbstractStep) factory.getObject();

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
	@SuppressWarnings("unchecked")
	public void testSkipOverLimitOnRead() throws Exception {

		reader = new SkipReaderStub(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"), StringUtils
				.commaDelimitedListToSet("2,3,5"));

		factory.setSkipLimit(3);
		factory.setItemReader(reader);
		factory.setSkippableExceptionClasses(new Class[] { Exception.class });

		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		try {
			step.execute(stepExecution);
			fail("Expected SkipLimitExceededException.");
		}
		catch (SkipLimitExceededException e) {
		}

		assertEquals(3, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getReadSkipCount().intValue());
		assertEquals(1, stepExecution.getWriteSkipCount().intValue());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertFalse(reader.processed.contains("2"));
		assertTrue(reader.processed.contains("4"));

		// failure on "5" tripped the skip limit but "4" failed on write and was skipped and 
		// RepeatSynchronizationManager.setCompleteOnly() was called in the retry policy to
		// aggressively commit after a recovery ("1" was written at that point)
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1"));
		assertEquals(expectedOutput, writer.written);

	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@SuppressWarnings("unchecked")
	public void testSkipOnReadNotDoubleCounted() throws Exception {

		reader = new SkipReaderStub(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"), StringUtils
				.commaDelimitedListToSet("2,3,5"));

		factory.setSkipLimit(4);
		factory.setItemReader(reader);

		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = jobExecution.createStepExecution(step);

		step.execute(stepExecution);
		assertEquals(4, stepExecution.getSkipCount());
		assertEquals(3, stepExecution.getReadSkipCount().intValue());
		assertEquals(1, stepExecution.getWriteSkipCount().intValue());

		// skipped 2,3,4,5
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,6"));
		assertEquals(expectedOutput, writer.written);

	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@SuppressWarnings("unchecked")
	public void testSkipOnWriteNotDoubleCounted() throws Exception {

		reader = new SkipReaderStub(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6,7"), StringUtils
				.commaDelimitedListToSet("2,3"));

		writer = new SkipWriterStub(StringUtils.commaDelimitedListToSet("4,5"));

		factory.setSkipLimit(4);
		factory.setItemReader(reader);
		factory.setItemWriter(writer);

		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = jobExecution.createStepExecution(step);

		step.execute(stepExecution);
		System.err.println(writer.written);
		System.err.println(reader.processed);
		assertEquals(4, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getReadSkipCount().intValue());
		assertEquals(2, stepExecution.getWriteSkipCount().intValue());

		// skipped 2,3,4,5
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,6,7"));
		assertEquals(expectedOutput, writer.written);

	}

	@SuppressWarnings("unchecked")
	public void testDefaultSkipPolicy() throws Exception {
		factory.setSkippableExceptionClasses(new Class[] { Exception.class });
		factory.setSkipLimit(1);
		List<String> items = TransactionAwareProxyFactory.createTransactionalList();
		items.addAll(Arrays.asList(new String[] { "a", "b", "c" }));
		ItemReader provider = new ListItemReader(items) {
			public Object read() {
				Object item = super.read();
				count++;
				if ("b".equals(item)) {
					throw new RuntimeException("Read error - planned failure.");
				}
				return item;
			}
		};
		factory.setItemReader(provider);
		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		// b is processed once and skipped, plus 1, plus c, plus the null at end
		assertEquals(4, count);
	}

	/**
	 * Simple item reader that supports skip functionality.
	 */
	private static class SkipReaderStub implements ItemReader {

		protected final Log logger = LogFactory.getLog(getClass());

		private final String[] items;

		private Collection<String> processed = new ArrayList<String>();

		private int counter = -1;

		private int marked = 0;

		private final Collection<String> failures;

		public SkipReaderStub() {
			this(new String[] { "1", "2", "3", "4", "5" }, Collections.singleton("2"));
		}

		public SkipReaderStub(String[] items, Collection<String> failures) {
			this.items = items;
			this.failures = failures;
		}

		public Object read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {
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

		public void mark() throws MarkFailedException {
			logger.debug("Marked at count=" + counter);
			marked = counter;
		}

		public void reset() throws ResetFailedException {
			counter = marked;
			logger.debug("Reset at count=" + counter);
		}

	}

	/**
	 * Simple item writer that supports skip functionality.
	 */
	private static class SkipWriterStub implements ItemWriter {

		protected final Log logger = LogFactory.getLog(getClass());

		private List<Object> written = new ArrayList<Object>();

		private int flushIndex = -1;

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

		public void clear() throws ClearFailedException {
			for (int i = flushIndex + 1; i < written.size(); i++) {
				written.remove(written.size()-1);
			}
		}

		public void flush() throws FlushFailedException {
			flushIndex = written.size() - 1;
		}

		public void write(Object item) throws Exception {
			if (failures.contains(item)) {
				logger.debug("Throwing write exception on [" + item + "]");
				throw new SkippableRuntimeException("exception in writer");
			}
			written.add(item);
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
