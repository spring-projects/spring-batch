package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
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
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ItemWriterException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.WriteFailedException;
import org.springframework.batch.item.WriterNotOpenException;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

/**
 * Tests for {@link FaultTolerantStepFactoryBean}.
 */
public class FaultTolerantStepFactoryBeanTests {

	protected final Log logger = LogFactory.getLog(getClass());

	private FaultTolerantStepFactoryBean<String, String> factory;

	private SkipReaderStub<String> reader;

	private SkipProcessorStub<String> processor;

	private SkipWriterStub<String> writer;

	private JobExecution jobExecution;

	private StepExecution stepExecution;

	private JobRepository repository;

	private boolean opened = false;

	private boolean closed = false;

	public FaultTolerantStepFactoryBeanTests() throws Exception {
		reader = new SkipReaderStub<String>();
		processor = new SkipProcessorStub<String>();
		writer = new SkipWriterStub<String>();
	}

	@SuppressWarnings("unchecked")
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

		factory
				.setSkippableExceptionClasses(getExceptionMap(SkippableException.class, SkippableRuntimeException.class));

		MapJobRepositoryFactoryBean repositoryFactory = new MapJobRepositoryFactoryBean();
		repositoryFactory.afterPropertiesSet();
		repository = (JobRepository) repositoryFactory.getObject();
		factory.setJobRepository(repository);

		jobExecution = repository.createJobExecution("skipJob", new JobParameters());
		stepExecution = jobExecution.createStepExecution(factory.getName());
		repository.add(stepExecution);
	}

	/**
	 * Non-skippable (and non-fatal) exception causes failure immediately.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testNonSkippableExceptionOnRead() throws Exception {
		reader.setFailures("2");

		// nothing is skippable
		factory.setSkippableExceptionClasses(getExceptionMap(NonExistentException.class));

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		assertTrue(stepExecution.getExitStatus().getExitDescription().contains("Non-skippable exception during read"));

		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNonSkippableException() throws Exception {
		// nothing is skippable
		factory.setSkippableExceptionClasses(getExceptionMap(NonExistentException.class));
		factory.setCommitInterval(1);

		// no failures on read
		reader.setItems("1", "2", "3", "4", "5");
		writer.setFailures("1");

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(1, reader.getRead().size());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		assertTrue(stepExecution.getExitStatus().getExitDescription().contains("Intended Failure"));
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testReadSkip() throws Exception {
		reader.setFailures("2");

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(1, stepExecution.getReadSkipCount());
		assertEquals(4, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(0, stepExecution.getRollbackCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertTrue(reader.getRead().contains("4"));
		assertFalse(reader.getRead().contains("2"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,3,4,5"));
		assertEquals(expectedOutput, writer.getWritten());

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testReadSkipWithPolicy() throws Exception {
		// Should be ignored
		factory.setSkipLimit(0);
		factory.setSkipPolicy(new LimitCheckingItemSkipPolicy(2, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true)));
		testReadSkip();
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testReadSkipWithPolicyExceptionInReader() throws Exception {

		// Should be ignored
		factory.setSkipLimit(0);

		factory.setSkipPolicy(new SkipPolicy() {
			public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
				throw new  RuntimeException("Planned exception in SkipPolicy");
			}
		});

		reader.setFailures("2");

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getReadCount());

	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testReadSkipWithPolicyExceptionInWriter() throws Exception {

		// Should be ignored
		factory.setSkipLimit(0);

		factory.setSkipPolicy(new SkipPolicy() {
			public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
				throw new  RuntimeException("Planned exception in SkipPolicy");
			}
		});

		writer.setFailures("2");

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(2, stepExecution.getReadCount());

	}

	/**
	 * Check to make sure that ItemStreamException can be skipped. (see
	 * BATCH-915)
	 */
	@Test
	public void testReadSkipItemStreamException() throws Exception {
		reader.setFailures("2");
		reader.setExceptionType(ItemStreamException.class);

		Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>();
		map.put(ItemStreamException.class, true);
		factory.setSkippableExceptionClasses(map);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(1, stepExecution.getReadSkipCount());
		assertEquals(4, stepExecution.getReadCount());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(0, stepExecution.getRollbackCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertTrue(reader.getRead().contains("4"));
		assertFalse(reader.getRead().contains("2"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,3,4,5"));
		assertEquals(expectedOutput, writer.getWritten());

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testProcessSkip() throws Exception {
		processor.setFailures("4");
		writer.setFailures("4");

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(5, stepExecution.getReadCount());
		assertEquals(1, stepExecution.getProcessSkipCount());
		assertEquals(1, stepExecution.getRollbackCount());

		// writer skips "4"
		assertTrue(reader.getRead().contains("4"));
		assertFalse(writer.getWritten().contains("4"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,5"));
		assertEquals(expectedOutput, writer.getWritten());

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	@Test
	public void testProcessFilter() throws Exception {
		processor.setFailures("4");
		processor.setFilter(true);
		ItemProcessListenerStub<String, String> listenerStub = new ItemProcessListenerStub<String, String>();
		factory.setListeners(new StepListener[] { listenerStub });
		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(5, stepExecution.getReadCount());
		assertEquals(1, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getRollbackCount());
		assertTrue(listenerStub.isFilterEncountered());

		// writer skips "4"
		assertTrue(reader.getRead().contains("4"));
		assertFalse(writer.getWritten().contains("4"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,5"));
		assertEquals(expectedOutput, writer.getWritten());

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	@Test
	public void testNullWriter() throws Exception {

		factory.setItemWriter(null);
		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(0, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(5, stepExecution.getReadCount());
		// Write count is incremented even if nothing happens
		assertEquals(5, stepExecution.getWriteCount());
		assertEquals(0, stepExecution.getFilterCount());
		assertEquals(0, stepExecution.getRollbackCount());

		// writer skips "4"
		assertTrue(reader.getRead().contains("4"));

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testWriteSkip() throws Exception {
		writer.setFailures("4");

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(5, stepExecution.getReadCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
		assertEquals(2, stepExecution.getRollbackCount());

		// writer skips "4"
		assertTrue(reader.getRead().contains("4"));
		assertFalse(writer.getCommitted().contains("4"));

		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,5"));
		assertEquals(expectedOutput, writer.getCommitted());

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Fatal exception should cause immediate termination provided the exception
	 * is not skippable (note the fatal exception is also classified as
	 * rollback).
	 */
	@Test
	public void testFatalException() throws Exception {
		reader.setFailures("2");

		Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>();
		map.put(SkippableException.class, true);
		map.put(SkippableRuntimeException.class, true);
		map.put(FatalRuntimeException.class, false);
		factory.setSkippableExceptionClasses(map);
		factory.setItemWriter(new ItemWriter<String>() {
			public void write(List<? extends String> items) {
				throw new FatalRuntimeException("Ouch!");
			}
		});

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		String message = stepExecution.getFailureExceptions().get(0).getCause().getMessage();
		assertEquals("Wrong message: ", "Ouch!", message);
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkipOverLimit() throws Exception {
		reader.setFailures("2");
		writer.setFailures("4");

		factory.setSkipLimit(1);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertTrue(reader.getRead().contains("4"));
		assertFalse(writer.getCommitted().contains("4"));

		// failure on "4" tripped the skip limit so we never got to "5"
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,3"));
		assertEquals(expectedOutput, writer.getCommitted());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSkipOverLimitOnRead() throws Exception {
		reader.setItems(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"));
		reader.setFailures(StringUtils.commaDelimitedListToStringArray("2,3,5"));

		writer.setFailures("4");

		factory.setSkipLimit(3);
		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());

		assertEquals(3, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
		assertEquals(2, stepExecution.getReadCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertFalse(reader.getRead().contains("2"));
		assertTrue(reader.getRead().contains("4"));

		// only "1" was ever committed
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1"));
		assertEquals(expectedOutput, writer.getCommitted());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	public void testSkipOverLimitOnReadWithListener() throws Exception {
		reader.setFailures("1", "3", "5");
		writer.setFailures();

		final List<Throwable> listenerCalls = new ArrayList<Throwable>();

		factory.setListeners(new StepListener[] { new SkipListenerSupport<String, String>() {
			@Override
			public void onSkipInRead(Throwable t) {
				listenerCalls.add(t);
			}
		} });
		factory.setCommitInterval(2);
		factory.setSkipLimit(2);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		// 1,3 skipped inside a committed chunk. 5 tripped the skip
		// limit but it was skipped in a chunk that rolled back, so
		// it will re-appear on a restart and the listener is not called.
		assertEquals(2, listenerCalls.size());
		assertEquals(2, stepExecution.getReadSkipCount());

		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());

	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSkipListenerFailsOnRead() throws Exception {
		reader.setItems(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"));
		reader.setFailures(StringUtils.commaDelimitedListToStringArray("2,3,5"));

		writer.setFailures("4");

		factory.setSkipLimit(3);
		factory.setListeners(new StepListener[] { new SkipListenerSupport<String, String>() {
			@Override
			public void onSkipInRead(Throwable t) {
				throw new RuntimeException("oops");
			}
		} });
		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("oops", stepExecution.getFailureExceptions().get(0).getCause().getMessage());

		// listeners are called only once chunk is about to commit, so
		// listener failure does not affect other statistics
		assertEquals(2, stepExecution.getReadSkipCount());
		// but we didn't get as far as the write skip in the scan:
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(2, stepExecution.getSkipCount());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSkipListenerFailsOnWrite() throws Exception {
		reader.setItems(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"));

		writer.setFailures("4");

		factory.setSkipLimit(3);
		factory.setListeners(new StepListener[] { new SkipListenerSupport<String, String>() {
			@Override
			public void onSkipInWrite(String item, Throwable t) {
				throw new RuntimeException("oops");
			}
		} });
		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));

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
		reader.setItems(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"));
		reader.setFailures(StringUtils.commaDelimitedListToStringArray("2,3,5"));

		writer.setFailures("4");

		factory.setSkipLimit(4);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(4, stepExecution.getSkipCount());
		assertEquals(3, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());

		// skipped 2,3,4,5
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,6"));
		assertEquals(expectedOutput, writer.getCommitted());

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
		reader.setItems(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6,7"));
		reader.setFailures(StringUtils.commaDelimitedListToStringArray("2,3"));

		writer.setFailures("4", "5");

		factory.setSkipLimit(4);
		factory.setCommitInterval(3); // includes all expected skips

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(4, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getReadSkipCount());
		assertEquals(2, stepExecution.getWriteSkipCount());

		// skipped 2,3,4,5
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,6,7"));
		assertEquals(expectedOutput, writer.getCommitted());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testDefaultSkipPolicy() throws Exception {
		reader.setItems("a", "b", "c");
		reader.setFailures("b");

		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));
		factory.setSkipLimit(1);

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals("[a, c]", reader.getRead().toString());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testSkipOverLimitOnReadWithAllSkipsAtEnd() throws Exception {
		reader.setItems(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15"));
		reader.setFailures(StringUtils.commaDelimitedListToStringArray("6,12,13,14,15"));

		writer.setFailures("4");

		factory.setCommitInterval(5);
		factory.setSkipLimit(3);
		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("bad skip count", 3, stepExecution.getSkipCount());
		assertEquals("bad read skip count", 2, stepExecution.getReadSkipCount());
		assertEquals("bad write skip count", 1, stepExecution.getWriteSkipCount());

		// writer did not skip "6" as it never made it to writer, only "4" did
		assertFalse(reader.getRead().contains("6"));
		assertTrue(reader.getRead().contains("4"));

		// only "1" was ever committed
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,5,7,8,9,10,11"));
		assertEquals(expectedOutput, writer.getCommitted());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	@Test
	public void testReprocessingAfterWriterRollback() throws Exception {
		reader.setItems("1", "2", "3", "4");

		writer.setFailures("4");

		Step step = (Step) factory.getObject();
		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getRollbackCount());

		// 1,2,3,4,3,4 - one scan until the item is
		// identified and finally skipped on the second attempt
		assertEquals("[1, 2, 3, 4, 3, 4]", processor.getProcessed().toString());
		assertStepExecutionsAreEqual(stepExecution, repository.getLastStepExecution(jobExecution.getJobInstance(), step
				.getName()));
	}

	@Test
	public void testAutoRegisterItemListeners() throws Exception {
		reader.setFailures("2");

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

	/**
	 * Check ItemStream is opened
	 */
	@Test
	public void testItemStreamOpenedEvenWithTaskExecutor() throws Exception {
		writer.setFailures("4");

		ItemStreamReader<String> reader = new ItemStreamReader<String>() {
			public void close() throws ItemStreamException {
				closed = true;
			}

			public void open(ExecutionContext executionContext) throws ItemStreamException {
				opened = true;
			}

			public void update(ExecutionContext executionContext) throws ItemStreamException {
			}

			public String read() throws Exception, UnexpectedInputException, ParseException {
				return null;
			}
		};

		factory.setItemReader(reader);
		factory.setTaskExecutor(new ConcurrentTaskExecutor());

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertTrue(opened);
		assertTrue(closed);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
	}

	/**
	 * Check ItemStream is opened
	 */
	@Test
	public void testNestedItemStreamOpened() throws Exception {
		writer.setFailures("4");

		ItemStreamReader<String> reader = new ItemStreamReader<String>() {
			public void close() throws ItemStreamException {
			}

			public void open(ExecutionContext executionContext) throws ItemStreamException {
			}

			public void update(ExecutionContext executionContext) throws ItemStreamException {
			}

			public String read() throws Exception, UnexpectedInputException, ParseException {
				return null;
			}
		};

		ItemStreamReader<String> stream = new ItemStreamReader<String>() {
			public void close() throws ItemStreamException {
				closed = true;
			}

			public void open(ExecutionContext executionContext) throws ItemStreamException {
				opened = true;
			}

			public void update(ExecutionContext executionContext) throws ItemStreamException {
			}

			public String read() throws Exception, UnexpectedInputException, ParseException {
				return null;
			}
		};

		factory.setItemReader(reader);
		factory.setStreams(new ItemStream[] { stream, reader });

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertTrue(opened);
		assertTrue(closed);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
	}

	/**
	 * Check ItemStream is opened
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testProxiedItemStreamOpened() throws Exception {
		writer.setFailures("4");

		ItemStreamReader<String> reader = new ItemStreamReader<String>() {
			public void close() throws ItemStreamException {
				closed = true;
			}

			public void open(ExecutionContext executionContext) throws ItemStreamException {
				opened = true;
			}

			public void update(ExecutionContext executionContext) throws ItemStreamException {
			}

			public String read() throws Exception, UnexpectedInputException, ParseException {
				return null;
			}
		};

		ProxyFactory proxy = new ProxyFactory();
		proxy.setTarget(reader);
		proxy.setInterfaces(new Class<?>[] { ItemReader.class, ItemStream.class });
		proxy.addAdvice(new MethodInterceptor() {
			public Object invoke(MethodInvocation invocation) throws Throwable {
				return invocation.proceed();
			}
		});
		Object advised = proxy.getProxy();

		factory.setItemReader((ItemReader<? extends String>) advised);
		factory.setStreams(new ItemStream[] { (ItemStream) advised });

		Step step = (Step) factory.getObject();

		step.execute(stepExecution);

		assertTrue(opened);
		assertTrue(closed);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
	}

	private static class ItemProcessListenerStub<T, S> implements ItemProcessListener<T, S> {

		private boolean filterEncountered = false;

		public void afterProcess(T item, S result) {
			if (result == null) {
				filterEncountered = true;
			}
		}

		public void beforeProcess(T item) {

		}

		public void onProcessError(T item, Exception e) {

		}

		public boolean isFilterEncountered() {
			return filterEncountered;
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

	/**
	 * condition: skippable < fatal; exception is unclassified
	 * 
	 * expected: false; default classification
	 */
	@Test
	public void testSkippableSubset_unclassified() throws Exception {
		assertFalse(getSkippableSubsetSkipPolicy().shouldSkip(new RuntimeException(), 0));
	}

	/**
	 * condition: skippable < fatal; exception is skippable
	 * 
	 * expected: true
	 */
	@Test
	public void testSkippableSubset_skippable() throws Exception {
		assertTrue(getSkippableSubsetSkipPolicy().shouldSkip(new WriteFailedException(""), 0));
	}

	/**
	 * condition: skippable < fatal; exception is fatal
	 * 
	 * expected: false
	 */
	@Test
	public void testSkippableSubset_fatal() throws Exception {
		assertFalse(getSkippableSubsetSkipPolicy().shouldSkip(new WriterNotOpenException(""), 0));
	}

	/**
	 * condition: fatal < skippable; exception is unclassified
	 * 
	 * expected: false; default classification
	 */
	@Test
	public void testFatalSubsetUnclassified() throws Exception {
		assertFalse(getFatalSubsetSkipPolicy().shouldSkip(new RuntimeException(), 0));
	}

	/**
	 * condition: fatal < skippable; exception is skippable
	 * 
	 * expected: true
	 */
	@Test
	public void testFatalSubsetSkippable() throws Exception {
		assertTrue(getFatalSubsetSkipPolicy().shouldSkip(new WriterNotOpenException(""), 0));
	}

	/**
	 * condition: fatal < skippable; exception is fatal
	 * 
	 * expected: false
	 */
	@Test
	public void testFatalSubsetFatal() throws Exception {
		assertFalse(getFatalSubsetSkipPolicy().shouldSkip(new WriteFailedException(""), 0));
	}

	private SkipPolicy getSkippableSubsetSkipPolicy() throws Exception {
		Map<Class<? extends Throwable>, Boolean> skippableExceptions = new HashMap<Class<? extends Throwable>, Boolean>();
		skippableExceptions.put(WriteFailedException.class, true);
		skippableExceptions.put(ItemWriterException.class, false);
		factory.setSkippableExceptionClasses(skippableExceptions);
		return getSkipPolicy(factory);
	}

	private SkipPolicy getFatalSubsetSkipPolicy() throws Exception {
		Map<Class<? extends Throwable>, Boolean> skippableExceptions = new HashMap<Class<? extends Throwable>, Boolean>();
		skippableExceptions.put(ItemWriterException.class, true);
		skippableExceptions.put(WriteFailedException.class, false);
		factory.setSkippableExceptionClasses(skippableExceptions);
		return getSkipPolicy(factory);
	}

	private SkipPolicy getSkipPolicy(FactoryBean factory) throws Exception {
		Object step = factory.getObject();
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProvider = ReflectionTestUtils.getField(tasklet, "chunkProvider");
		return (SkipPolicy) ReflectionTestUtils.getField(chunkProvider, "skipPolicy");
	}

	private Map<Class<? extends Throwable>, Boolean> getExceptionMap(Class<? extends Throwable>... args) {
		Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>();
		for (Class<? extends Throwable> arg : args) {
			map.put(arg, true);
		}
		return map;
	}
	
	public static class NonExistentException extends Exception {
		
	}

}
