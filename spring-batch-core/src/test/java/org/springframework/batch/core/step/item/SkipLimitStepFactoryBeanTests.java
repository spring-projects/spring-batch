package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.step.JobRepositorySupport;
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

/**
 * Tests for {@link SkipLimitStepFactoryBean}.
 */
public class SkipLimitStepFactoryBeanTests extends TestCase {

	SkipLimitStepFactoryBean tested = new SkipLimitStepFactoryBean();

	Class[] skippableExceptions = new Class[] { SkippableException.class, SkippableRuntimeException.class };

	final int SKIP_LIMIT = 2;

	final int COMMIT_INTERVAL = 2;

	SkipReaderStub reader = new SkipReaderStub();

	SkipWriterStub writer = new SkipWriterStub();

	JobExecution jobExecution;

	protected void setUp() throws Exception {
		tested.setJobRepository(new JobRepositorySupport());
		tested.setTransactionManager(new ResourcelessTransactionManager());
		tested.setCommitInterval(COMMIT_INTERVAL);
		tested.setItemReader(reader);
		tested.setItemWriter(writer);
		tested.setSkippableExceptionClasses(skippableExceptions);
		tested.setSkipLimit(SKIP_LIMIT);

		JobInstance jobInstance = new JobInstance(new Long(1), new JobParameters(), new JobSupport("skipJob"));
		jobExecution = new JobExecution(jobInstance);
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	public void testSkip() throws Exception {
		ItemOrientedStep step = (ItemOrientedStep) tested.getObject();

		StepExecution stepExecution = new StepExecution(step, jobExecution);
		step.execute(stepExecution);

		assertEquals(2, stepExecution.getSkipCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertTrue(reader.processed.contains("4"));
		assertFalse(writer.written.contains("4"));

		String[] expectedOutput = { "1", "3", "5" };

		for (int i = 0; i < expectedOutput.length; i++) {
			assertTrue("Output should contain \"" + expectedOutput[i] + "\"", writer.written
					.contains(expectedOutput[i]));
		}
		assertTrue(writer.written.size() == expectedOutput.length);

	}

	/**
	 * Fatal exception should cause immediate termination regardless of other
	 * skip settings (note the fatal exception is also classified as skippable).
	 */
	public void testFatalException() throws Exception {
		tested.setFatalExceptionClasses(new Class[] { FatalRuntimeException.class });
		tested.setItemWriter(new SkipWriterStub() {
			public void write(Object item) {
				throw new FatalRuntimeException("Ouch!");
			}
		});

		ItemOrientedStep step = (ItemOrientedStep) tested.getObject();
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
	 * Simple item reader that supports skip functionality.
	 */
	private static class SkipReaderStub implements ItemReader {

		final String[] items = { "1", "2", "3", "4", "5", null };

		Collection processed = new ArrayList();

		int counter = -1;

		int marked = 0;

		public Object read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {
			counter++;
			if ("2".equals(items[counter])) {
				throw new SkippableException("exception in reader");
			}
			processed.add(items[counter]);
			return items[counter];
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

		public void clear() throws ClearFailedException {
			for (int i = flushIndex + 1; i < written.size(); i++) {
				written.remove(i);
			}
		}

		public void flush() throws FlushFailedException {
			flushIndex = written.size() - 1;
		}

		public void write(Object item) throws Exception {
			written.add(item);
			if (item.equals("4")) {
				throw new SkippableRuntimeException("exception in writer");
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
