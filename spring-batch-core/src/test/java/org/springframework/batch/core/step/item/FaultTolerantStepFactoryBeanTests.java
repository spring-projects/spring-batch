/*
 * Copyright 2008-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.Joinpoint;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.ItemProcessListener;
import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.factory.FaultTolerantStepFactoryBean;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.Chunk;
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
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.lang.Nullable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FaultTolerantStepFactoryBean}.
 */
public class FaultTolerantStepFactoryBeanTests {

	protected final Log logger = LogFactory.getLog(getClass());

	private FaultTolerantStepFactoryBean<String, String> factory;

	private final SkipReaderStub<String> reader;

	private final SkipProcessorStub<String> processor;

	private final SkipWriterStub<String> writer;

	private JobExecution jobExecution;

	private StepExecution stepExecution;

	private JobRepository repository;

	private boolean opened = false;

	private boolean closed = false;

	public FaultTolerantStepFactoryBeanTests() throws Exception {
		reader = new SkipReaderStub<>();
		processor = new SkipProcessorStub<>();
		writer = new SkipWriterStub<>();
	}

	@SuppressWarnings("unchecked")
	@BeforeEach
	void setUp() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder().generateUniqueName(true)
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/schema-hsqldb-extended.sql")
			.build();
		JdbcTransactionManager transactionManager = new JdbcTransactionManager(embeddedDatabase);

		factory = new FaultTolerantStepFactoryBean<>();

		factory.setBeanName("stepName");
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

		factory
			.setSkippableExceptionClasses(getExceptionMap(SkippableException.class, SkippableRuntimeException.class));

		JdbcJobRepositoryFactoryBean repositoryFactoryBean = new JdbcJobRepositoryFactoryBean();
		repositoryFactoryBean.setDataSource(embeddedDatabase);
		repositoryFactoryBean.setTransactionManager(transactionManager);
		repositoryFactoryBean.setMaxVarCharLength(20000);
		repositoryFactoryBean.afterPropertiesSet();
		repository = repositoryFactoryBean.getObject();
		factory.setJobRepository(repository);

		jobExecution = repository.createJobExecution("skipJob", new JobParameters());
		stepExecution = jobExecution.createStepExecution(factory.getName());
		repository.add(stepExecution);
	}

	@Test
	void testMandatoryReader() {
		// given
		factory = new FaultTolerantStepFactoryBean<>();
		factory.setBeanName("test");
		factory.setItemWriter(writer);

		// when
		final Exception expectedException = assertThrows(IllegalStateException.class, factory::getObject);

		// then
		assertEquals("ItemReader must be provided", expectedException.getMessage());
	}

	@Test
	void testMandatoryWriter() {
		// given
		factory = new FaultTolerantStepFactoryBean<>();
		factory.setBeanName("test");
		factory.setItemReader(reader);

		// when
		final Exception expectedException = assertThrows(IllegalStateException.class, factory::getObject);

		// then
		assertEquals("ItemWriter must be provided", expectedException.getMessage());
	}

	/**
	 * Non-skippable (and non-fatal) exception causes failure immediately.
	 */
	@SuppressWarnings("unchecked")
	@Test
	void testNonSkippableExceptionOnRead() throws Exception {
		reader.setFailures("2");

		// nothing is skippable
		factory.setSkippableExceptionClasses(getExceptionMap(NonExistentException.class));

		Step step = factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		assertTrue(stepExecution.getExitStatus().getExitDescription().contains("Non-skippable exception during read"));

		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testNonSkippableException() throws Exception {
		// nothing is skippable
		factory.setSkippableExceptionClasses(getExceptionMap(NonExistentException.class));
		factory.setCommitInterval(1);

		// no failures on read
		reader.setItems("1", "2", "3", "4", "5");
		writer.setFailures("1");

		Step step = factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(1, reader.getRead().size());
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
		assertTrue(stepExecution.getExitStatus().getExitDescription().contains("Intended Failure"));
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	void testReadSkip() throws Exception {
		reader.setFailures("2");

		Step step = factory.getObject();

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
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	void testReadSkipWithPolicy() throws Exception {
		// Should be ignored
		factory.setSkipLimit(0);
		factory.setSkipPolicy(new LimitCheckingItemSkipPolicy(2,
				Collections.<Class<? extends Throwable>, Boolean>singletonMap(Exception.class, true)));
		testReadSkip();
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	void testReadSkipWithPolicyExceptionInReader() throws Exception {

		// Should be ignored
		factory.setSkipLimit(0);

		factory.setSkipPolicy((t, skipCount) -> {
			throw new RuntimeException("Planned exception in SkipPolicy");
		});

		reader.setFailures("2");

		Step step = factory.getObject();

		step.execute(stepExecution);

		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getReadCount());

	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	void testReadSkipWithPolicyExceptionInWriter() throws Exception {

		// Should be ignored
		factory.setSkipLimit(0);

		factory.setSkipPolicy((t, skipCount) -> {
			throw new RuntimeException("Planned exception in SkipPolicy");
		});

		writer.setFailures("2");

		Step step = factory.getObject();

		step.execute(stepExecution);

		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(2, stepExecution.getReadCount());

	}

	/**
	 * Check to make sure that ItemStreamException can be skipped. (see BATCH-915)
	 */
	@Test
	void testReadSkipItemStreamException() throws Exception {
		reader.setFailures("2");
		reader.setExceptionType(ItemStreamException.class);

		Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
		map.put(ItemStreamException.class, true);
		factory.setSkippableExceptionClasses(map);

		Step step = factory.getObject();

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
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	void testProcessSkip() throws Exception {
		processor.setFailures("4");
		writer.setFailures("4");

		Step step = factory.getObject();

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
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	@Test
	void testProcessFilter() throws Exception {
		processor.setFailures("4");
		processor.setFilter(true);
		ItemProcessListenerStub<String, String> listenerStub = new ItemProcessListenerStub<>();
		factory.setListeners(new StepListener[] { listenerStub });
		Step step = factory.getObject();

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
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	void testWriteSkip() throws Exception {
		writer.setFailures("4");

		Step step = factory.getObject();

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
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	/**
	 * Fatal exception should cause immediate termination provided the exception is not
	 * skippable (note the fatal exception is also classified as rollback).
	 */
	@Test
	void testFatalException() throws Exception {
		reader.setFailures("2");

		Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
		map.put(SkippableException.class, true);
		map.put(SkippableRuntimeException.class, true);
		map.put(FatalRuntimeException.class, false);
		factory.setSkippableExceptionClasses(map);
		factory.setItemWriter(items -> {
			throw new FatalRuntimeException("Ouch!");
		});

		Step step = factory.getObject();

		step.execute(stepExecution);
		String message = stepExecution.getFailureExceptions().get(0).getCause().getMessage();
		assertEquals("Ouch!", message, "Wrong message: " + message);
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	void testSkipOverLimit() throws Exception {
		reader.setFailures("2");
		writer.setFailures("4");

		factory.setSkipLimit(1);

		Step step = factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());

		// writer did not skip "2" as it never made it to writer, only "4" did
		assertTrue(reader.getRead().contains("4"));
		assertFalse(writer.getCommitted().contains("4"));

		// failure on "4" tripped the skip limit so we never got to "5"
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,3"));
		assertEquals(expectedOutput, writer.getCommitted());
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@SuppressWarnings("unchecked")
	@Test
	void testSkipOverLimitOnRead() throws Exception {
		reader.setItems(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"));
		reader.setFailures(StringUtils.commaDelimitedListToStringArray("2,3,5"));

		writer.setFailures("4");

		factory.setSkipLimit(3);
		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));

		Step step = factory.getObject();

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
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	void testSkipOverLimitOnReadWithListener() throws Exception {
		reader.setFailures("1", "3", "5");
		writer.setFailures();

		final List<Throwable> listenerCalls = new ArrayList<>();

		factory.setListeners(new StepListener[] { new SkipListener<String, String>() {
			@Override
			public void onSkipInRead(Throwable t) {
				listenerCalls.add(t);
			}
		} });
		factory.setCommitInterval(2);
		factory.setSkipLimit(2);

		Step step = factory.getObject();

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
	void testSkipListenerFailsOnRead() throws Exception {
		reader.setItems(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"));
		reader.setFailures(StringUtils.commaDelimitedListToStringArray("2,3,5"));

		writer.setFailures("4");

		factory.setSkipLimit(3);
		factory.setListeners(new StepListener[] { new SkipListener<String, String>() {
			@Override
			public void onSkipInRead(Throwable t) {
				throw new RuntimeException("oops");
			}
		} });
		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));

		Step step = factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("oops", stepExecution.getFailureExceptions().get(0).getCause().getMessage());

		// listeners are called only once chunk is about to commit, so
		// listener failure does not affect other statistics
		assertEquals(2, stepExecution.getReadSkipCount());
		// but we didn't get as far as the write skip in the scan:
		assertEquals(0, stepExecution.getWriteSkipCount());
		assertEquals(2, stepExecution.getSkipCount());
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@SuppressWarnings("unchecked")
	@Test
	void testSkipListenerFailsOnWrite() throws Exception {
		reader.setItems(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"));

		writer.setFailures("4");

		factory.setSkipLimit(3);
		factory.setListeners(new StepListener[] { new SkipListener<String, String>() {
			@Override
			public void onSkipInWrite(String item, Throwable t) {
				throw new RuntimeException("oops");
			}
		} });
		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));

		Step step = factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals("oops", stepExecution.getFailureExceptions().get(0).getCause().getMessage());
		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(0, stepExecution.getReadSkipCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	void testSkipOnReadNotDoubleCounted() throws Exception {
		reader.setItems(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6"));
		reader.setFailures(StringUtils.commaDelimitedListToStringArray("2,3,5"));

		writer.setFailures("4");

		factory.setSkipLimit(4);

		Step step = factory.getObject();

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
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@Test
	void testSkipOnWriteNotDoubleCounted() throws Exception {
		reader.setItems(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6,7"));
		reader.setFailures(StringUtils.commaDelimitedListToStringArray("2,3"));

		writer.setFailures("4", "5");

		factory.setSkipLimit(4);
		factory.setCommitInterval(3); // includes all expected skips

		Step step = factory.getObject();

		step.execute(stepExecution);
		assertEquals(4, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getReadSkipCount());
		assertEquals(2, stepExecution.getWriteSkipCount());

		// skipped 2,3,4,5
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,6,7"));
		assertEquals(expectedOutput, writer.getCommitted());
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testDefaultSkipPolicy() throws Exception {
		reader.setItems("a", "b", "c");
		reader.setFailures("b");

		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));
		factory.setSkipLimit(1);

		Step step = factory.getObject();

		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals("[a, c]", reader.getRead().toString());
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	/**
	 * Check items causing errors are skipped as expected.
	 */
	@SuppressWarnings("unchecked")
	@Test
	void testSkipOverLimitOnReadWithAllSkipsAtEnd() throws Exception {
		reader.setItems(StringUtils.commaDelimitedListToStringArray("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15"));
		reader.setFailures(StringUtils.commaDelimitedListToStringArray("6,12,13,14,15"));

		writer.setFailures("4");

		factory.setCommitInterval(5);
		factory.setSkipLimit(3);
		factory.setSkippableExceptionClasses(getExceptionMap(Exception.class));

		Step step = factory.getObject();

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(3, stepExecution.getSkipCount(), "bad skip count");
		assertEquals(2, stepExecution.getReadSkipCount(), "bad read skip count");
		assertEquals(1, stepExecution.getWriteSkipCount(), "bad write skip count");

		// writer did not skip "6" as it never made it to writer, only "4" did
		assertFalse(reader.getRead().contains("6"));
		assertTrue(reader.getRead().contains("4"));

		// only "1" was ever committed
		List<String> expectedOutput = Arrays.asList(StringUtils.commaDelimitedListToStringArray("1,2,3,5,7,8,9,10,11"));
		assertEquals(expectedOutput, writer.getCommitted());
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	@Test
	void testReprocessingAfterWriterRollback() throws Exception {
		reader.setItems("1", "2", "3", "4");

		writer.setFailures("4");

		Step step = factory.getObject();
		step.execute(stepExecution);

		assertEquals(1, stepExecution.getSkipCount());
		assertEquals(2, stepExecution.getRollbackCount());

		// 1,2,3,4,3,4 - one scan until the item is
		// identified and finally skipped on the second attempt
		assertEquals("[1, 2, 3, 4, 3, 4]", processor.getProcessed().toString());
		assertStepExecutionsAreEqual(stepExecution,
				repository.getLastStepExecution(jobExecution.getJobInstance(), step.getName()));
	}

	@Test
	void testAutoRegisterItemListeners() throws Exception {
		reader.setFailures("2");

		final List<Integer> listenerCalls = new ArrayList<>();

		class TestItemListenerWriter implements ItemWriter<String>, ItemReadListener<String>, ItemWriteListener<String>,
				ItemProcessListener<String, String>, SkipListener<String, String>, ChunkListener {

			@Override
			public void write(Chunk<? extends String> chunk) throws Exception {
				if (chunk.getItems().contains("4")) {
					throw new SkippableException("skippable");
				}
			}

			@Override
			public void afterRead(String item) {
				listenerCalls.add(1);
			}

			@Override
			public void beforeRead() {
			}

			@Override
			public void onReadError(Exception ex) {
			}

			@Override
			public void afterWrite(Chunk<? extends String> items) {
				listenerCalls.add(2);
			}

			@Override
			public void beforeWrite(Chunk<? extends String> items) {
			}

			@Override
			public void onWriteError(Exception exception, Chunk<? extends String> items) {
			}

			@Override
			public void afterProcess(String item, @Nullable String result) {
				listenerCalls.add(3);
			}

			@Override
			public void beforeProcess(String item) {
			}

			@Override
			public void onProcessError(String item, Exception e) {
			}

			@Override
			public void afterChunk(ChunkContext context) {
				listenerCalls.add(4);
			}

			@Override
			public void beforeChunk(ChunkContext context) {
			}

			@Override
			public void onSkipInProcess(String item, Throwable t) {
			}

			@Override
			public void onSkipInRead(Throwable t) {
				listenerCalls.add(6);
			}

			@Override
			public void onSkipInWrite(String item, Throwable t) {
				listenerCalls.add(5);
			}

			@Override
			public void afterChunkError(ChunkContext context) {
			}

		}

		factory.setItemWriter(new TestItemListenerWriter());

		Step step = factory.getObject();
		step.execute(stepExecution);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		for (int i = 1; i <= 6; i++) {
			assertTrue(listenerCalls.contains(i), "didn't call listener " + i);
		}
	}

	/**
	 * Check ItemStream is opened
	 */
	@Test
	void testItemStreamOpenedEvenWithTaskExecutor() throws Exception {
		writer.setFailures("4");

		ItemReader<String> reader = new AbstractItemStreamItemReader<>() {
			@Override
			public void close() {
				super.close();
				closed = true;
			}

			@Override
			public void open(ExecutionContext executionContext) {
				super.open(executionContext);
				opened = true;
			}

			@Nullable
			@Override
			public String read() {
				return null;
			}
		};

		factory.setItemReader(reader);
		factory.setTaskExecutor(new SyncTaskExecutor());

		Step step = factory.getObject();

		step.execute(stepExecution);

		assertTrue(opened);
		assertTrue(closed);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
	}

	/**
	 * Check ItemStream is opened
	 */
	@Test
	void testNestedItemStreamOpened() throws Exception {
		writer.setFailures("4");

		ItemStreamReader<String> reader = new ItemStreamReader<>() {
			@Override
			public void close() throws ItemStreamException {
			}

			@Override
			public void open(ExecutionContext executionContext) throws ItemStreamException {
			}

			@Override
			public void update(ExecutionContext executionContext) throws ItemStreamException {
			}

			@Nullable
			@Override
			public String read() throws Exception, UnexpectedInputException, ParseException {
				return null;
			}
		};

		ItemStreamReader<String> stream = new ItemStreamReader<>() {
			@Override
			public void close() throws ItemStreamException {
				closed = true;
			}

			@Override
			public void open(ExecutionContext executionContext) throws ItemStreamException {
				opened = true;
			}

			@Override
			public void update(ExecutionContext executionContext) throws ItemStreamException {
			}

			@Nullable
			@Override
			public String read() throws Exception, UnexpectedInputException, ParseException {
				return null;
			}
		};

		factory.setItemReader(reader);
		factory.setStreams(new ItemStream[] { stream, reader });

		Step step = factory.getObject();

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
	void testProxiedItemStreamOpened() throws Exception {
		writer.setFailures("4");

		ItemStreamReader<String> reader = new ItemStreamReader<>() {
			@Override
			public void close() throws ItemStreamException {
				closed = true;
			}

			@Override
			public void open(ExecutionContext executionContext) throws ItemStreamException {
				opened = true;
			}

			@Override
			public void update(ExecutionContext executionContext) throws ItemStreamException {
			}

			@Nullable
			@Override
			public String read() throws Exception, UnexpectedInputException, ParseException {
				return null;
			}
		};

		ProxyFactory proxy = new ProxyFactory();
		proxy.setTarget(reader);
		proxy.setInterfaces(new Class<?>[] { ItemReader.class, ItemStream.class });
		proxy.addAdvice((MethodInterceptor) Joinpoint::proceed);
		Object advised = proxy.getProxy();

		factory.setItemReader((ItemReader<? extends String>) advised);
		factory.setStreams(new ItemStream[] { (ItemStream) advised });

		Step step = factory.getObject();

		step.execute(stepExecution);

		assertTrue(opened);
		assertTrue(closed);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
	}

	private static class ItemProcessListenerStub<T, S> implements ItemProcessListener<T, S> {

		private boolean filterEncountered = false;

		@Override
		public void afterProcess(T item, @Nullable S result) {
			if (result == null) {
				filterEncountered = true;
			}
		}

		@Override
		public void beforeProcess(T item) {

		}

		@Override
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
	 * <p>
	 * expected: false; default classification
	 */
	@Test
	void testSkippableSubset_unclassified() throws Exception {
		assertFalse(getSkippableSubsetSkipPolicy().shouldSkip(new RuntimeException(), 0));
	}

	/**
	 * condition: skippable < fatal; exception is skippable
	 * <p>
	 * expected: true
	 */
	@Test
	void testSkippableSubset_skippable() throws Exception {
		assertTrue(getSkippableSubsetSkipPolicy().shouldSkip(new WriteFailedException(""), 0));
	}

	/**
	 * condition: skippable < fatal; exception is fatal
	 * <p>
	 * expected: false
	 */
	@Test
	void testSkippableSubset_fatal() throws Exception {
		assertFalse(getSkippableSubsetSkipPolicy().shouldSkip(new WriterNotOpenException(""), 0));
	}

	/**
	 * condition: fatal < skippable; exception is unclassified
	 * <p>
	 * expected: false; default classification
	 */
	@Test
	void testFatalSubsetUnclassified() throws Exception {
		assertFalse(getFatalSubsetSkipPolicy().shouldSkip(new RuntimeException(), 0));
	}

	/**
	 * condition: fatal < skippable; exception is skippable
	 * <p>
	 * expected: true
	 */
	@Test
	void testFatalSubsetSkippable() throws Exception {
		assertTrue(getFatalSubsetSkipPolicy().shouldSkip(new WriterNotOpenException(""), 0));
	}

	/**
	 * condition: fatal < skippable; exception is fatal
	 * <p>
	 * expected: false
	 */
	@Test
	void testFatalSubsetFatal() throws Exception {
		assertFalse(getFatalSubsetSkipPolicy().shouldSkip(new WriteFailedException(""), 0));
	}

	private SkipPolicy getSkippableSubsetSkipPolicy() throws Exception {
		Map<Class<? extends Throwable>, Boolean> skippableExceptions = new HashMap<>();
		skippableExceptions.put(WriteFailedException.class, true);
		skippableExceptions.put(ItemWriterException.class, false);
		factory.setSkippableExceptionClasses(skippableExceptions);
		return getSkipPolicy(factory);
	}

	private SkipPolicy getFatalSubsetSkipPolicy() throws Exception {
		Map<Class<? extends Throwable>, Boolean> skippableExceptions = new HashMap<>();
		skippableExceptions.put(ItemWriterException.class, true);
		skippableExceptions.put(WriteFailedException.class, false);
		factory.setSkippableExceptionClasses(skippableExceptions);
		return getSkipPolicy(factory);
	}

	private SkipPolicy getSkipPolicy(FactoryBean<Step> factory) throws Exception {
		Object step = factory.getObject();
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProvider = ReflectionTestUtils.getField(tasklet, "chunkProvider");
		return (SkipPolicy) ReflectionTestUtils.getField(chunkProvider, "skipPolicy");
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends Throwable>, Boolean> getExceptionMap(Class<? extends Throwable>... args) {
		Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
		for (Class<? extends Throwable> arg : args) {
			map.put(arg, true);
		}
		return map;
	}

	public static class NonExistentException extends Exception {

	}

}
