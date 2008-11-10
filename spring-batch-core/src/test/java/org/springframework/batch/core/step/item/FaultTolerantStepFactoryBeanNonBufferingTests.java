package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.easymock.EasyMock.*;

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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.SkipListenerSupport;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.util.StringUtils;

public class FaultTolerantStepFactoryBeanNonBufferingTests {

	protected final Log logger = LogFactory.getLog(getClass());

	private FaultTolerantStepFactoryBean<String, String> factory = new FaultTolerantStepFactoryBean<String, String>();

	@SuppressWarnings("unchecked")
	private Collection<Class<? extends Throwable>> skippableExceptions = new HashSet<Class<? extends Throwable>>(Arrays
			.<Class<? extends Throwable>> asList(SkippableException.class, SkippableRuntimeException.class));

	private List<String> items = Arrays.asList(new String[] { "1", "2", "3", "4", "5" });

	private ListItemReader<String> reader = new ListItemReader<String>(TransactionAwareProxyFactory
			.createTransactionalList(items));

	private SkipWriterStub writer = new SkipWriterStub();

	private JobExecution jobExecution;

	int count = 0;

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
		factory.setIsReaderTransactionalQueue(true);

		JobInstance jobInstance = new JobInstance(new Long(1), new JobParameters(), "skipJob");
		jobExecution = new JobExecution(jobInstance);
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkip() throws Exception {
		@SuppressWarnings("unchecked")
		SkipListener<Integer, String> skipListener = createStrictMock(SkipListener.class);
		skipListener.onSkipInWrite("4", SkipWriterStub.exception);
		expectLastCall().once();
		replay(skipListener);
		
		factory.setListeners(new SkipListener[] { skipListener });
		factory.setSkipLimit(1);
		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());

		// only one exception caused rollback, but more than once because it
		// has to go back and split the chunk up to isolate the failed item
		assertEquals(2, stepExecution.getRollbackCount());

		assertFalse(writer.written.contains("4"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,5"));
		assertEquals(expectedOutput, writer.written);

		// 5 items + 2 rollbacks re-reading 2 items each time
		assertEquals(9, stepExecution.getReadCount());

		verify(skipListener);
	}

	@Test
	public void testSkipOverLimit() throws Exception {
		SkipProcessorStub processor = new SkipProcessorStub(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("3")));
		processor.rollback = false;

		factory.setItemProcessor(processor);

		factory.setSkipLimit(1);

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());

		assertFalse(writer.written.contains("4"));

		// failure on "4" tripped the skip limit so only first chunk was written
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2"));
		assertEquals(expectedOutput, writer.written);

	}

	/**
	 * Exception in listener causes failure regardless of skip limit.
	 * @throws Exception
	 */
	@Test
	public void testSkipListenerFailsOnWrite() throws Exception {

		factory.setSkipLimit(7); // some high limit
		factory.setItemReader(reader);
		factory.setListeners(new StepListener[] { new SkipListenerSupport<String, String>() {
			@Override
			public void onSkipInWrite(String item, Throwable t) {
				throw new RuntimeException("oops");
			}
		} });
		factory.setSkippableExceptionClasses(Collections.<Class<? extends Throwable>> singleton(Exception.class));

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("oops", stepExecution.getFailureExceptions().get(0).getCause().getMessage());
		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());

	}

	@Test
	public void testSkipOnWriteNotDoubleCounted() throws Exception {

		writer = new SkipWriterStub(Arrays.asList(StringUtils.commaDelimitedListToStringArray("4,5")));

		factory.setSkipLimit(4);
		factory.setItemReader(reader);
		factory.setItemWriter(writer);
		factory.setCommitInterval(3); // includes all expected skips

		Step step = (Step) factory.getObject();

		StepExecution stepExecution = jobExecution.createStepExecution(step.getName());

		step.execute(stepExecution);
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(2, stepExecution.getWriteSkipCount());

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3"));
		assertEquals(expectedOutput, writer.written);

	}

	@Test
	public void testDefaultSkipPolicy() throws Exception {
		factory.setSkippableExceptionClasses(Collections.<Class<? extends Throwable>> singleton(Exception.class));
		factory.setSkipLimit(1);
		List<String> items = Arrays.asList(new String[] { "a", "b", "c" });
		ItemReader<String> provider = new ListItemReader<String>(TransactionAwareProxyFactory
				.createTransactionalList(items)) {
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

	/**
	 * Scenario: Exception in processor that shouldn't cause rollback
	 */
	@Test
	public void testProcessorNoRollback() throws Exception {

		factory.setTransactionAttribute(new DefaultTransactionAttribute());
		SkipProcessorStub processor = new SkipProcessorStub(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("1,3")));
		factory.setItemProcessor(processor);

		final Collection<String> NO_FAILURES = Collections.emptyList();
		factory.setItemWriter(new SkipWriterStub(NO_FAILURES));

		Step step = (Step) factory.getObject();
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		processor.rollback = false;
		step.execute(stepExecution);
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getRollbackCount());

	}

	/**
	 * Scenario: Exception in processor that should cause rollback
	 */
	@Test
	public void testProcessorRollback() throws Exception {
		SkipProcessorStub processor = new SkipProcessorStub(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("1,3")));
		factory.setItemProcessor(processor);

		final Collection<String> NO_FAILURES = Collections.emptyList();
		factory.setItemWriter(new SkipWriterStub(NO_FAILURES));

		Step step = (Step) factory.getObject();
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		processor.rollback = true;
		step.execute(stepExecution);
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getRollbackCount());
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
	 * Simple item writer that supports skip functionality.
	 */
	private static class SkipWriterStub implements ItemWriter<String> {

		protected final Log logger = LogFactory.getLog(getClass());

		private static final SkippableRuntimeException exception = new SkippableRuntimeException("exception in writer");

		// simulate transactional output
		private List<Object> written = TransactionAwareProxyFactory.createTransactionalList();

		private final Collection<String> failures;

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
					throw exception;
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

}
