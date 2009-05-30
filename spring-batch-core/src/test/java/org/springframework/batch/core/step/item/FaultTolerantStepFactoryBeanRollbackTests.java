package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
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
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
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

	private FaultTolerantStepFactoryBean<String, String> factory;

	private SkipReaderStub<String> reader = new SkipReaderStub<String>();

	private SkipProcessorStub<String> processor = new SkipProcessorStub<String>();

	private SkipWriterStub<String> writer = new SkipWriterStub<String>();

	private JobExecution jobExecution;

	private StepExecution stepExecution;

	private JobRepository repository;

	@Before
	public void setUp() throws Exception {
		factory = new FaultTolerantStepFactoryBean<String, String>();

		factory.setBeanName("stepName");
		factory.setTransactionManager(new ResourcelessTransactionManager());
		factory.setCommitInterval(2);

		reader.clear();
		reader.setItems("1", "2", "3", "4", "5");
		factory.setItemReader(reader);
		processor.clear();
		factory.setItemProcessor(processor);
		writer.clear();
		factory.setItemWriter(writer);

		factory.setSkipLimit(2);

		factory.setSkippableExceptionClasses(getExceptionList(Exception.class));

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
		reader.setItems("1", "2", "3", "4");
		reader.setFailures("2", "3");
		reader.setRuntimeException(false);

		Step step = (Step) factory.getObject();

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
		reader.setFailures("2", "3");
		reader.setItems("1", "2", "3", "4");
		reader.setRuntimeException(false);

		// No skips by default
		factory.setSkippableExceptionClasses(new HashSet<Class<? extends Throwable>>());
		// But this one is explicit in the tx-attrs so it should be skipped
		factory.setNoRollbackExceptionClasses(getExceptionList(SkippableException.class));

		Step step = (Step) factory.getObject();

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
		reader.setItems("1", "2", "3", "4");

		processor.setFailures("1", "3");
		processor.setRuntimeException(false);

		Step step = (Step) factory.getObject();

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
		reader.setItems("1", "2", "3", "4");

		processor.setFailures("1", "3");
		processor.setRuntimeException(true);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getRollbackCount());
	}

	@Test
	public void testProcessSkipWithNoRollbackForCheckedException() throws Exception {
		processor.setFailures("4");
		processor.setRuntimeException(false);

		factory.setNoRollbackExceptionClasses(getExceptionList(SkippableException.class));

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(5, stepExecution.getReadCount());
		assertEquals(1, stepExecution.getProcessSkipCount());
		assertEquals(0, stepExecution.getRollbackCount());

		// skips "4"
		assertTrue(reader.getRead().contains("4"));
		assertFalse(writer.getCommitted().contains("4"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,5"));
		assertEquals(expectedOutput, writer.getCommitted());

	}

	/**
	 * Scenario: Exception in writer that should not cause rollback and scan
	 */
	@Test
	public void testWriterDefaultRollbackOnCheckedException() throws Exception {
		writer.setFailures("2", "3");
		writer.setRuntimeException(false);

		Step step = (Step) factory.getObject();

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
		writer.setFailures("2", "3");
		writer.setRuntimeException(true);

		Step step = (Step) factory.getObject();

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
		writer.setFailures("2", "3");
		writer.setRuntimeException(true);

		factory.setNoRollbackExceptionClasses(getExceptionList(SkippableRuntimeException.class));

		Step step = (Step) factory.getObject();

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
		writer.setFailures("2", "3");
		writer.setRuntimeException(false);

		factory.setNoRollbackExceptionClasses(getExceptionList(SkippableException.class));

		Step step = (Step) factory.getObject();

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

}
