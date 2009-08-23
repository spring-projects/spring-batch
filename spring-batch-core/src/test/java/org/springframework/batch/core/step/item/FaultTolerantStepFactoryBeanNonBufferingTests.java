package org.springframework.batch.core.step.item;

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.util.StringUtils;

public class FaultTolerantStepFactoryBeanNonBufferingTests {

	protected final Log logger = LogFactory.getLog(getClass());

	private FaultTolerantStepFactoryBean<String, String> factory = new FaultTolerantStepFactoryBean<String, String>();

	private List<String> items = Arrays.asList(new String[] { "1", "2", "3", "4", "5" });

	private ListItemReader<String> reader = new ListItemReader<String>(TransactionAwareProxyFactory
			.createTransactionalList(items));

	private SkipWriterStub writer = new SkipWriterStub();

	private JobExecution jobExecution;

	private static final SkippableRuntimeException exception = new SkippableRuntimeException("exception in writer");

	int count = 0;

	@Before
	public void setUp() throws Exception {
		factory.setBeanName("stepName");
		factory.setJobRepository(new JobRepositorySupport());
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setCommitInterval(2);
		factory.setItemReader(reader);
		factory.setItemWriter(writer);
		Map<Class<? extends Throwable>, Boolean> skippableExceptions = new HashMap<Class<? extends Throwable>, Boolean>();
		skippableExceptions.put(SkippableException.class, true);
		skippableExceptions.put(SkippableRuntimeException.class, true);
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
		skipListener.onSkipInWrite("3", exception);
		expectLastCall().once();
		skipListener.onSkipInWrite("4", exception);
		expectLastCall().once();
		replay(skipListener);

		factory.setListeners(new SkipListener[] { skipListener });
		Step step = (Step) factory.getObject();

		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(2, stepExecution.getWriteSkipCount());

		// only one exception caused rollback, and only once in this case
		// because all items in that chunk were skipped immediately
		assertEquals(1, stepExecution.getRollbackCount());

		assertFalse(writer.written.contains("4"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,5"));
		assertEquals(expectedOutput, writer.written);

		// 5 items + 1 rollbacks reading 2 items each time
		assertEquals(7, stepExecution.getReadCount());

		verify(skipListener);
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
			logger.debug("Writing: " + items);
			for (String item : items) {
				if (failures.contains(item)) {
					logger.debug("Throwing write exception on [" + item + "]");
					throw exception;
				}
				written.add(item);
			}
		}

	}

}
