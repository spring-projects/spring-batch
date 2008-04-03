package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.JobSupport;
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
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.util.StringUtils;

/**
 * Tests for {@link SkipLimitStepFactoryBean}.
 */
public class SkipLimitStepFactoryBeanTests extends TestCase {

	SkipLimitStepFactoryBean factory = new SkipLimitStepFactoryBean();

	Class[] skippableExceptions = new Class[] { SkippableException.class, SkippableRuntimeException.class };

	final int SKIP_LIMIT = 2;

	final int COMMIT_INTERVAL = 2;

	SkipReaderStub reader = new SkipReaderStub();

	SkipWriterStub writer = new SkipWriterStub();

	JobExecution jobExecution;

	protected void setUp() throws Exception {
		factory.setJobRepository(new JobRepositorySupport());
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setCommitInterval(COMMIT_INTERVAL);
		factory.setItemReader(reader);
		factory.setItemWriter(writer);
		factory.setSkippableExceptionClasses(skippableExceptions);
		factory.setSkipLimit(SKIP_LIMIT);

		JobInstance jobInstance = new JobInstance(new Long(1), new JobParameters(), new JobSupport("skipJob"));
		jobExecution = new JobExecution(jobInstance);
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	public void testSkip() throws Exception {
		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step, jobExecution);
		step.execute(stepExecution);

		assertEquals(2, stepExecution.getSkipCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertTrue(reader.processed.contains("4"));
		assertFalse(writer.written.contains("4"));

		List expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,3,5"));
		assertEquals(expectedOutput, writer.written);

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
		StepExecution stepExecution = new StepExecution(step, jobExecution);

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

		StepExecution stepExecution = new StepExecution(step, jobExecution);

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
		List expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,3"));
		assertEquals(expectedOutput, writer.written);

	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	public void testSkipOverLimitOnRead() throws Exception {

		reader = new SkipReaderStub(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"), StringUtils
				.commaDelimitedListToSet("2,3,5"));

		factory.setSkipLimit(3);
		factory.setItemReader(reader);
		factory.setSkippableExceptionClasses(new Class[] { Exception.class });

		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = new StepExecution(step, jobExecution);

		try {
			step.execute(stepExecution);
			fail("Expected SkipLimitExceededException.");
		}
		catch (SkipLimitExceededException e) {
		}

		assertEquals(3, stepExecution.getSkipCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertTrue(reader.processed.contains("4"));

		// failure on "4" tripped the skip limit so we never write anything
		// ("1" was written but rolled back)
		List expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray(""));
		assertEquals(expectedOutput, writer.written);

	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	public void testSkipOnReadNotDoubleCounted() throws Exception {

		reader = new SkipReaderStub(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"), StringUtils
				.commaDelimitedListToSet("2,3,5"));

		factory.setSkipLimit(4);
		factory.setItemReader(reader);

		AbstractStep step = (AbstractStep) factory.getObject();

		StepExecution stepExecution = jobExecution.createStepExecution(step);

		step.execute(stepExecution);
		assertEquals(4, stepExecution.getSkipCount());

		// skipped 2,3,4,5
		List expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,6"));
		assertEquals(expectedOutput, writer.written);

	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
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

		// skipped 2,3,4,5
		List expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,6,7"));
		assertEquals(expectedOutput, writer.written);

	}

	/**
	 * Simple item reader that supports skip functionality.
	 */
	private static class SkipReaderStub implements ItemReader {

		private final String[] items;

		private Collection processed = new ArrayList();

		private int counter = -1;

		private int marked = 0;

		private final Collection failures;

		public SkipReaderStub() {
			this(new String[] { "1", "2", "3", "4", "5" }, Collections.singleton("2"));
		}

		public SkipReaderStub(String[] items, Collection failures) {
			this.items = items;
			this.failures = failures;
		}

		public Object read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {
			counter++;
			if (counter >= items.length) {
				return null;
			}
			String item = items[counter];
			if (failures.contains(item)) {
				throw new SkippableException("exception in reader");
			}
			processed.add(item);
			return item;
		}

		public void mark() throws MarkFailedException {
			marked = counter;
		}

		public void reset() throws ResetFailedException {
			counter = marked;
		}

	}

	/**
	 * Simple item writer that supports skip functionality.
	 */
	private static class SkipWriterStub implements ItemWriter {

		List written = new ArrayList();

		int flushIndex = -1;

		private final Collection failures;

		public SkipWriterStub() {
			this(StringUtils.commaDelimitedListToSet("4"));
		}

		/**
		 * @param commaDelimitedListToSet
		 */
		public SkipWriterStub(Collection failures) {
			this.failures = failures;
		}

		public void clear() throws ClearFailedException {
			for (int i = flushIndex + 1; i < written.size(); i++) {
				written.remove(i);
			}
		}

		public void flush() throws FlushFailedException {
			flushIndex = written.size() - 1;
		}

		public void write(Object item) throws Exception {
			if (failures.contains(item)) {
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
