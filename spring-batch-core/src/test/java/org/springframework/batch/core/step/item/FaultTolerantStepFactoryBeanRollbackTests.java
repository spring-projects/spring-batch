package org.springframework.batch.core.step.item;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.batch.core.BatchStatus.FAILED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.FatalStepExecutionException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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

	private SkipReaderStub<String> reader;

	private SkipProcessorStub<String> processor;

	private SkipWriterStub<String> writer;

	private JobExecution jobExecution;

	private StepExecution stepExecution;

	private JobRepository repository;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		reader = new SkipReaderStub<String>();
		processor = new SkipProcessorStub<String>();
		writer = new SkipWriterStub<String>();

		factory = new FaultTolerantStepFactoryBean<String, String>();

		factory.setBeanName("stepName");
		ResourcelessTransactionManager transactionManager = new ResourcelessTransactionManager();
		factory.setTransactionManager(transactionManager);
		factory.setCommitInterval(2);

		reader.clear();
		reader.setItems("1", "2", "3", "4", "5");
		factory.setItemReader(reader);
		processor.clear();
		factory.setItemProcessor(processor);
		writer.clear();
		factory.setItemWriter(writer);

		factory.setSkipLimit(2);

		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));

		MapJobRepositoryFactoryBean repositoryFactory = new MapJobRepositoryFactoryBean();
		repositoryFactory.setTransactionManager(transactionManager);
		repositoryFactory.afterPropertiesSet();
		repository = (JobRepository) repositoryFactory.getObject();
		factory.setJobRepository(repository);

		jobExecution = repository.createJobExecution("skipJob", new JobParameters());
		stepExecution = jobExecution.createStepExecution(factory.getName());
		repository.add(stepExecution);
	}

	@After
	public void tearDown() throws Exception {
		reader = null;
		processor = null;
		writer = null;
		factory = null;
	}
	
	@Test
	public void testBeforeChunkListenerException() throws Exception{
		factory.setListeners(new StepListener []{new ExceptionThrowingChunkListener(true)});
		Step step = (Step) factory.getObject();
		step.execute(stepExecution);
		assertEquals(FAILED, stepExecution.getStatus());
		assertEquals(FAILED.toString(), stepExecution.getExitStatus().getExitCode());	
		assertTrue(stepExecution.getCommitCount() == 0);//Make sure exception was thrown in after, not before
		Throwable e = stepExecution.getFailureExceptions().get(0);
		assertThat(e, instanceOf(FatalStepExecutionException.class));
		assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
	}
	
	@Test
	public void testAfterChunkListenerException() throws Exception{
		factory.setListeners(new StepListener []{new ExceptionThrowingChunkListener(false)});
		Step step = (Step) factory.getObject();
		step.execute(stepExecution);
		assertEquals(FAILED, stepExecution.getStatus());
		assertEquals(FAILED.toString(), stepExecution.getExitStatus().getExitCode());	
		assertTrue(stepExecution.getCommitCount() > 0);//Make sure exception was thrown in after, not before
		Throwable e = stepExecution.getFailureExceptions().get(0);
		assertThat(e, instanceOf(FatalStepExecutionException.class));
		assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
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
		reader.setExceptionType(SkippableException.class);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getRollbackCount());
	}

	/**
	 * Scenario: Exception in reader that should not cause rollback
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testReaderAttributesOverrideSkippableNoRollback() throws Exception {
		reader.setFailures("2", "3");
		reader.setItems("1", "2", "3", "4");
		reader.setExceptionType(SkippableException.class);

		// No skips by default
		factory.setSkippableExceptionClasses(getExceptionMap(RuntimeException.class));
		// But this one is explicit in the tx-attrs so it should be skipped
		factory.setNoRollbackExceptionClasses(getExceptionList(SkippableException.class));

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(0, stepExecution.getSkipCount());
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
		processor.setExceptionType(SkippableException.class);

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
		processor.setExceptionType(SkippableRuntimeException.class);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getRollbackCount());
	}

	@Test
	public void testNoRollbackInProcessorWhenSkipExceeded() throws Throwable {

		jobExecution = repository.createJobExecution("noRollbackJob", new JobParameters());

		factory.setSkipLimit(0);

		reader.clear();
		reader.setItems("1", "2", "3", "4", "5");
		factory.setItemReader(reader);
		writer.clear();
		factory.setItemWriter(writer);
		processor.clear();
		factory.setItemProcessor(processor);

		@SuppressWarnings("unchecked")
		List<Class<? extends Throwable>> exceptions = Arrays.<Class<? extends Throwable>>asList(Exception.class);
		factory.setNoRollbackExceptionClasses(exceptions);
		@SuppressWarnings("unchecked")
		Map<Class<? extends Throwable>, Boolean> skippable = getExceptionMap(Exception.class);
		factory.setSkippableExceptionClasses(skippable);

		processor.setFailures("2");

		Step step = (Step) factory.getObject();

		stepExecution = jobExecution.createStepExecution(factory.getName());
		repository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals("[1, 3, 4, 5]", writer.getCommitted().toString());
		// No rollback on 2 so processor has side effect
		assertEquals("[1, 2, 3, 4, 5]", processor.getCommitted().toString());
		List<String> processed = new ArrayList<String>(processor.getProcessed());
		Collections.sort(processed);
		assertEquals("[1, 2, 3, 4, 5]", processed.toString());
		assertEquals(0, stepExecution.getSkipCount());

	}

	@Test
	public void testProcessSkipWithNoRollbackForCheckedException() throws Exception {
		processor.setFailures("4");
		processor.setExceptionType(SkippableException.class);

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
		writer.setExceptionType(SkippableException.class);

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
	public void testWriterDefaultRollbackOnError() throws Exception {
		writer.setFailures("2", "3");
		writer.setExceptionType(AssertionError.class);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(1, stepExecution.getRollbackCount());
	}

	/**
	 * Scenario: Exception in writer that should not cause rollback and scan
	 */
	@Test
	public void testWriterDefaultRollbackOnRuntimeException() throws Exception {
		writer.setFailures("2", "3");
		writer.setExceptionType(SkippableRuntimeException.class);

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
		writer.setExceptionType(SkippableRuntimeException.class);

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
		writer.setExceptionType(SkippableException.class);

		factory.setNoRollbackExceptionClasses(getExceptionList(SkippableException.class));

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getSkipCount());
		// Two multi-item chunks rolled back. When the item was encountered on
		// its own it can proceed
		assertEquals(2, stepExecution.getRollbackCount());
	}

	@Test
	public void testSkipInProcessor() throws Exception {
		processor.setFailures("4");
		factory.setCommitInterval(30);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals("[1, 2, 3, 4, 1, 2, 3, 5]", processor.getProcessed().toString());
		assertEquals("[1, 2, 3, 5]", processor.getCommitted().toString());
		assertEquals("[1, 2, 3, 5]", writer.getWritten().toString());
		assertEquals("[1, 2, 3, 5]", writer.getCommitted().toString());
	}

	@Test
	public void testMultipleSkipsInProcessor() throws Exception {
		processor.setFailures("2", "4");
		factory.setCommitInterval(30);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals("[1, 3, 5]", processor.getCommitted().toString());
		assertEquals("[1, 3, 5]", writer.getWritten().toString());
		assertEquals("[1, 3, 5]", writer.getCommitted().toString());
		assertEquals("[1, 2, 1, 3, 4, 1, 3, 5]", processor.getProcessed().toString());
	}

	@Test
	public void testMultipleSkipsInNonTransactionalProcessor() throws Exception {
		processor.setFailures("2", "4");
		factory.setCommitInterval(30);
		factory.setProcessorTransactional(false);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals("[1, 3, 5]", writer.getWritten().toString());
		assertEquals("[1, 3, 5]", writer.getCommitted().toString());
		// If non-transactional, we should only process each item once
		assertEquals("[1, 2, 3, 4, 5]", processor.getProcessed().toString());
	}

	@Test
	public void testFilterInProcessor() throws Exception {
		processor.setFailures("4");
		processor.setFilter(true);
		factory.setCommitInterval(30);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals("[1, 2, 3, 4, 5]", processor.getProcessed().toString());
		assertEquals("[1, 2, 3, 4, 5]", processor.getCommitted().toString());
		assertEquals("[1, 2, 3, 5]", writer.getWritten().toString());
		assertEquals("[1, 2, 3, 5]", writer.getCommitted().toString());
	}

	@Test
	public void testSkipInWriter() throws Exception {
		writer.setFailures("4");
		factory.setCommitInterval(30);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals("[1, 2, 3, 5]", processor.getCommitted().toString());
		assertEquals("[1, 2, 3, 5]", writer.getCommitted().toString());
		assertEquals("[1, 2, 3, 4, 1, 2, 3, 4, 5]", writer.getWritten().toString());
		assertEquals("[1, 2, 3, 4, 5, 1, 2, 3, 4, 5]", processor.getProcessed().toString());

		assertEquals(1, stepExecution.getWriteSkipCount());
		assertEquals(5, stepExecution.getReadCount());
		assertEquals(4, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
	}

	@Test
	public void testSkipInWriterNonTransactionalProcessor() throws Exception {
		writer.setFailures("4");
		factory.setCommitInterval(30);
		factory.setProcessorTransactional(false);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals("[1, 2, 3, 5]", writer.getCommitted().toString());
		assertEquals("[1, 2, 3, 4, 1, 2, 3, 4, 5]", writer.getWritten().toString());
		assertEquals("[1, 2, 3, 4, 5]", processor.getProcessed().toString());
	}

	@Test
	public void testSkipInWriterTransactionalReader() throws Exception {
		writer.setFailures("4");
		ItemReader<String> reader = new ListItemReader<String>(TransactionAwareProxyFactory.createTransactionalList(Arrays.asList("1", "2", "3", "4", "5")));
		factory.setItemReader(reader);
		factory.setCommitInterval(30);
		factory.setSkipLimit(10);
		factory.setIsReaderTransactionalQueue(true);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals("[]", writer.getCommitted().toString());
		assertEquals("[1, 2, 3, 4]", writer.getWritten().toString());
		assertEquals("[1, 2, 3, 4, 5, 1, 2, 3, 4, 5]", processor.getProcessed().toString());
	}

	@Test
	public void testMultithreadedSkipInWriter() throws Exception {
		writer.setFailures("1", "2", "3", "4", "5");
		factory.setCommitInterval(3);
		factory.setSkipLimit(10);
		factory.setTaskExecutor(new SimpleAsyncTaskExecutor());

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals("[]", writer.getCommitted().toString());
		assertEquals("[]", processor.getCommitted().toString());
		assertEquals(5, stepExecution.getSkipCount());
	}

	@Test
	public void testMultipleSkipsInWriter() throws Exception {
		writer.setFailures("2", "4");
		factory.setCommitInterval(30);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals("[1, 3, 5]", writer.getCommitted().toString());
		assertEquals("[1, 2, 1, 2, 3, 4, 5]", writer.getWritten().toString());
		assertEquals("[1, 3, 5]", processor.getCommitted().toString());
		assertEquals("[1, 2, 3, 4, 5, 1, 2, 3, 4, 5]", processor.getProcessed().toString());

		assertEquals(2, stepExecution.getWriteSkipCount());
		assertEquals(5, stepExecution.getReadCount());
		assertEquals(3, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
	}

	@Test
	public void testMultipleSkipsInWriterNonTransactionalProcessor() throws Exception {
		writer.setFailures("2", "4");
		factory.setCommitInterval(30);
		factory.setProcessorTransactional(false);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

		assertEquals("[1, 3, 5]", writer.getCommitted().toString());
		assertEquals("[1, 2, 1, 2, 3, 4, 5]", writer.getWritten().toString());
		assertEquals("[1, 2, 3, 4, 5]", processor.getProcessed().toString());
	}

	@SuppressWarnings("unchecked")
	private Collection<Class<? extends Throwable>> getExceptionList(Class<? extends Throwable> arg) {
		return Arrays.<Class<? extends Throwable>> asList(arg);
	}

	private Map<Class<? extends Throwable>, Boolean> getExceptionMap(Class<? extends Throwable>... args) {
		Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>();
		for (Class<? extends Throwable> arg : args) {
			map.put(arg, true);
		}
		return map;
	}
	
	class ExceptionThrowingChunkListener implements ChunkListener{

		private boolean throwBefore = true;

		public ExceptionThrowingChunkListener(boolean throwBefore) {
			this.throwBefore  = throwBefore;
		}
		
		public void beforeChunk() {
			if(throwBefore){
				throw new IllegalArgumentException("Planned exception");
			}
		}

		public void afterChunk() {
			throw new IllegalArgumentException("Planned exception");
			
		}
	}

}
