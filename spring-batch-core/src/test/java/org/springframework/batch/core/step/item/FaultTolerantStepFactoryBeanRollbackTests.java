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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeEditor;
import org.springframework.util.StringUtils;

/**
 * Tests for {@link FaultTolerantStepFactoryBean}.
 */
public class FaultTolerantStepFactoryBeanRollbackTests {

	protected final Log logger = LogFactory.getLog(getClass());

	private FaultTolerantStepFactoryBean<String, String> factory = new FaultTolerantStepFactoryBean<String, String>();

	private static Collection<String> NO_FAILURES = Collections.emptyList();

	private SkipReaderStub reader = new SkipReaderStub();

	private SkipWriterStub writer = new SkipWriterStub();

	private JobExecution jobExecution;

	private StepExecution stepExecution;

	private JobRepository repository;

	private static boolean runtimeException = false;

	@Before
	public void setUp() throws Exception {
		factory.setBeanName("stepName");
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setCommitInterval(2);
		factory.setItemReader(reader);
		factory.setItemWriter(writer);
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

	@Test
	public void testOverrideWithoutChangingRollbackRules() throws Exception {
		TransactionAttributeEditor editor = new TransactionAttributeEditor();
		editor.setAsText("-RuntimeException");
		TransactionAttribute attr = (TransactionAttribute) editor.getValue();
		assertTrue(attr.rollbackOn(new RuntimeException("")));
		assertFalse(attr.rollbackOn(new Exception("")));
	}

	@Test
	public void testChangeRollbackRules() throws Exception {
		TransactionAttributeEditor editor = new TransactionAttributeEditor();
		editor.setAsText("+RuntimeException");
		TransactionAttribute attr = (TransactionAttribute) editor.getValue();
		assertFalse(attr.rollbackOn(new RuntimeException("")));
		assertFalse(attr.rollbackOn(new Exception("")));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNonDefaultRollbackRules() throws Exception {
		TransactionAttributeEditor editor = new TransactionAttributeEditor();
		editor.setAsText("+RuntimeException,+SkippableException");
		RuleBasedTransactionAttribute attr = (RuleBasedTransactionAttribute) editor.getValue();
		attr.getRollbackRules().add(new RollbackRuleAttribute(Exception.class));
		assertTrue(attr.rollbackOn(new Exception("")));
		assertFalse(attr.rollbackOn(new RuntimeException("")));
		assertFalse(attr.rollbackOn(new SkippableException("")));
	}

	/**
	 * Scenario: Exception in reader that should not cause rollback
	 */
	@Test
	public void testReaderDefaultNoRollbackOnCheckedException() throws Exception {
		factory.setItemReader(new SkipReaderStub(new String[] { "1", "2", "3", "4" }, Arrays.asList("2", "3")));

		Step step = (Step) factory.getObject();

		runtimeException = false;
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getRollbackCount());
	}

	/**
	 * Scenario: Exception in reader that should not cause rollback
	 */
	@Test
	public void testReaderAttributesOverrideSkippableNoRollback() throws Exception {
		factory.setItemReader(new SkipReaderStub(new String[] { "1", "2", "3", "4" }, Arrays.asList("2", "3")));

		// No skips by default
		factory.setSkippableExceptionClasses(new HashSet<Class<? extends Throwable>>());
		// But this one is explicit in the tx-attrs so it should be skipped
		factory.setNoRollbackExceptionClasses(getExceptionList(SkippableException.class));

		Step step = (Step) factory.getObject();

		runtimeException = false;
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getRollbackCount());
	}

	/**
	 * Scenario: Exception in processor that should cause rollback because of
	 * checked exception
	 */
	@Test
	public void testProcessorDefaultRollbackOnCheckedException() throws Exception {
		SkipProcessorStub processor = new SkipProcessorStub(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("1,3")));
		factory.setItemProcessor(processor);

		factory.setItemReader(new SkipReaderStub(new String[] { "1", "2", "3", "4" }, NO_FAILURES));
		factory.setItemWriter(new SkipWriterStub(NO_FAILURES));

		Step step = (Step) factory.getObject();

		runtimeException = false;
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getRollbackCount());
	}

	/**
	 * Scenario: Exception in processor that should cause rollback
	 */
	@Test
	public void testProcessorDefaultRollbackOnRuntimeException() throws Exception {
		SkipProcessorStub processor = new SkipProcessorStub(Arrays.asList(StringUtils
				.commaDelimitedListToStringArray("1,3")));
		factory.setItemProcessor(processor);

		factory.setItemReader(new SkipReaderStub(new String[] { "1", "2", "3", "4" }, NO_FAILURES));
		factory.setItemWriter(new SkipWriterStub(NO_FAILURES));

		Step step = (Step) factory.getObject();

		runtimeException = true;
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getRollbackCount());
	}

	@Test
	public void testProcessSkipWithNoRollbackForCheckedException() throws Exception {

		reader = new SkipReaderStub(new String[] { "1", "2", "3", "4", "5" }, NO_FAILURES);
		factory.setItemReader(reader);
		factory.setNoRollbackExceptionClasses(getExceptionList(SkippableException.class));
		SkipProcessorStub processor = new SkipProcessorStub(Arrays.asList(new String[] { "4" }));
		factory.setItemProcessor(processor);
		Step step = (Step) factory.getObject();

		runtimeException = false;
		step.execute(stepExecution);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(5, stepExecution.getReadCount());
		assertEquals(1, stepExecution.getProcessSkipCount());
		assertEquals(0, stepExecution.getRollbackCount());

		// skips "4"
		assertTrue(reader.processed.contains("4"));
		assertFalse(writer.written.contains("4"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,5"));
		assertEquals(expectedOutput, writer.written);

	}

	/**
	 * Scenario: Exception in writer that should not cause rollback and scan
	 */
	@Test
	public void testWriterDefaultRollbackOnCheckedException() throws Exception {
		factory.setItemWriter(new SkipWriterStub(Arrays.asList("2", "3")));

		Step step = (Step) factory.getObject();

		runtimeException = false;
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(4, stepExecution.getRollbackCount());
	}

	/**
	 * Scenario: Exception in writer that should not cause rollback and scan
	 */
	@Test
	public void testWriterDefaultRollbackOnRuntimeException() throws Exception {
		factory.setItemWriter(new SkipWriterStub(Arrays.asList("2", "3")));

		Step step = (Step) factory.getObject();

		runtimeException = true;
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(4, stepExecution.getRollbackCount());

	}

	/**
	 * Scenario: Exception in writer that should not cause rollback and scan
	 */
	@Test
	public void testWriterNoRollbackOnRuntimeException() throws Exception {
		factory.setItemWriter(new SkipWriterStub(Arrays.asList("2", "3")));
		factory.setNoRollbackExceptionClasses(getExceptionList(SkippableRuntimeException.class));

		Step step = (Step) factory.getObject();

		runtimeException = true;
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getSkipCount());
		// Two multi-item chunks rolled back. When the item was encountered on
		// its own it can proceed
		assertEquals(2, stepExecution.getRollbackCount());

	}

	/**
	 * Scenario: Exception in writer that should not cause rollback and scan
	 */
	@Test
	public void testWriterNoRollbackOnCheckedException() throws Exception {
		factory.setItemWriter(new SkipWriterStub(Arrays.asList("2", "3")));
		factory.setNoRollbackExceptionClasses(getExceptionList(SkippableException.class));

		Step step = (Step) factory.getObject();

		runtimeException = false;
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getSkipCount());
		// Two multi-item chunks rolled back. When the item was encountered on
		// its own it can proceed
		assertEquals(2, stepExecution.getRollbackCount());

	}

	@SuppressWarnings("unchecked")
	private Collection<Class<? extends Throwable>> getExceptionList(Class<? extends Throwable> args) {
		return Arrays.<Class<? extends Throwable>> asList(args);
	}

	private static class SkipProcessorStub implements ItemProcessor<String, String> {
		private final Collection<String> failures;

		public SkipProcessorStub() {
			this(NO_FAILURES);
		}

		public SkipProcessorStub(Collection<String> failures) {
			this.failures = failures;
		}

		public String process(String item) throws Exception {
			if (failures.contains(item)) {
				if (runtimeException) {
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
			this(new String[] { "1", "2", "3", "4", "5" }, NO_FAILURES);
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
				if (runtimeException) {
					throw new SkippableRuntimeException("should cause rollback in reader");
				}
				else {
					throw new SkippableException("shouldn't cause rollback in reader");
				}
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
			this(NO_FAILURES);
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
					if (runtimeException) {
						throw new SkippableRuntimeException("should cause rollback in writer");
					}
					else {
						throw new SkippableException("shouldn't cause rollback in writer");
					}
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
