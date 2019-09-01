/*
 * Copyright 2006-2019 the original author or authors.
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

package org.springframework.batch.core.step.tasklet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.MapExecutionContextDao;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.core.step.StepInterruptionPolicy;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.policy.DefaultResultCompletionPolicy;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.support.DefaultTransactionStatus;

public class TaskletStepTests {

	List<String> processed = new ArrayList<>();

	private List<Serializable> list = new ArrayList<>();

	ItemWriter<String> itemWriter = new ItemWriter<String>() {
		@Override
		public void write(List<? extends String> data) throws Exception {
			processed.addAll(data);
		}
	};

	private TaskletStep step;

	private Job job;

	private JobInstance jobInstance;

	private JobParameters jobParameters;

	private ResourcelessTransactionManager transactionManager;

	@SuppressWarnings("serial")
	private ExecutionContext foobarEc = new ExecutionContext() {
		{
			put("foo", "bar");
		}
	};

	private ItemReader<String> getReader(String[] args) {
		return new ListItemReader<>(Arrays.asList(args));
	}

	private TaskletStep getStep(String[] strings) throws Exception {
		return getStep(strings, 1);
	}

	private TaskletStep getStep(String[] strings, int commitInterval) throws Exception {
		TaskletStep step = new TaskletStep("stepName");
		// Only process one item:
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(commitInterval));
		step.setTasklet(new TestingChunkOrientedTasklet<>(getReader(strings), itemWriter, template));
		step.setJobRepository(new JobRepositorySupport());
		step.setTransactionManager(transactionManager);
		return step;
	}

	@Before
	public void setUp() throws Exception {

		transactionManager = new ResourcelessTransactionManager();

		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));

		step = getStep(new String[] { "foo", "bar", "spam" });
		step.setStepOperations(template);

		job = new JobSupport("FOO");
		jobInstance = new JobInstance(0L, job.getName());
		jobParameters = new JobParameters();

		step.setTransactionManager(transactionManager);

	}

	@Test
	public void testStepExecutor() throws Exception {
		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);
		step.execute(stepExecution);
		assertEquals(1, processed.size());
		assertEquals(1, stepExecution.getReadCount());
		assertEquals(1, stepExecution.getCommitCount());
	}

	@Test
	public void testCommitCount_Even() throws Exception {
		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		step = getStep(new String[] { "foo", "bar", "spam", "eggs" }, 2);
		step.setTransactionManager(transactionManager);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);
		step.execute(stepExecution);
		assertEquals(4, processed.size());
		assertEquals(4, stepExecution.getReadCount());
		assertEquals(4, stepExecution.getWriteCount());
		assertEquals(3, stepExecution.getCommitCount()); //the empty chunk is the 3rd commit
	}

	@Test
	public void testCommitCount_Uneven() throws Exception {
		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		step = getStep(new String[] { "foo", "bar", "spam" }, 2);
		step.setTransactionManager(transactionManager);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);
		step.execute(stepExecution);
		assertEquals(3, processed.size());
		assertEquals(3, stepExecution.getReadCount());
		assertEquals(3, stepExecution.getWriteCount());
		assertEquals(2, stepExecution.getCommitCount());
	}

	@Test
	public void testEmptyReader() throws Exception {
		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);
		step = getStep(new String[0]);
		step.setTasklet(new TestingChunkOrientedTasklet<>(getReader(new String[0]), itemWriter,
				new RepeatTemplate()));
		step.setStepOperations(new RepeatTemplate());
		step.execute(stepExecution);
		assertEquals(0, processed.size());
		assertEquals(0, stepExecution.getReadCount());
		// Commit after end of data detected (this leads to the commit count
		// being one greater than people expect if the commit interval is
		// commensurate with the total number of items).h
		assertEquals(1, stepExecution.getCommitCount());
	}

	/**
	 * StepExecution should be updated after every chunk commit.
	 */
	@Test
	public void testStepExecutionUpdates() throws Exception {

		JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		step.setStepOperations(new RepeatTemplate());

		JobRepositoryStub jobRepository = new JobRepositoryStub();
		step.setJobRepository(jobRepository);

		step.execute(stepExecution);

		assertEquals(3, processed.size());
		assertEquals(3, stepExecution.getReadCount());
		assertTrue(3 <= jobRepository.updateCount);
	}

	/**
	 * Failure to update StepExecution after chunk commit is fatal.
	 */
	@Test
	public void testStepExecutionUpdateFailure() throws Exception {

		JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		JobRepository repository = new JobRepositoryFailedUpdateStub();

		step.setJobRepository(repository);
		step.afterPropertiesSet();

		step.execute(stepExecution);
		assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
	}

	@Test
	public void testRepository() throws Exception {

		SimpleJobRepository repository = new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(),
				new MapStepExecutionDao(), new MapExecutionContextDao());
		step.setJobRepository(repository);

		JobExecution jobExecution = repository.createJobExecution(job.getName(), jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		repository.add(stepExecution);
		step.execute(stepExecution);
		assertEquals(1, processed.size());
	}

	@Test
	public void testIncrementRollbackCount() {

		ItemReader<String> itemReader = new ItemReader<String>() {

			@Nullable
			@Override
			public String read() throws Exception {
				throw new RuntimeException();
			}

		};

		step.setTasklet(new TestingChunkOrientedTasklet<>(itemReader, itemWriter));
		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		try {
			step.execute(stepExecution);
		}
		catch (Exception ex) {
			assertEquals(1, stepExecution.getRollbackCount());
		}

	}

	@Test
	public void testExitCodeDefaultClassification() throws Exception {

		ItemReader<String> itemReader = new ItemReader<String>() {

			@Nullable
			@Override
			public String read() throws Exception {
				throw new RuntimeException();

			}

		};

		step.setTasklet(new TestingChunkOrientedTasklet<>(itemReader, itemWriter));
		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		try {
			step.execute(stepExecution);
		}
		catch (Exception ex) {
			ExitStatus status = stepExecution.getExitStatus();
			assertEquals(ExitStatus.COMPLETED, status);
		}
	}

	@Test
	public void testExitCodeCustomClassification() throws Exception {

		ItemReader<String> itemReader = new ItemReader<String>() {

			@Nullable
			@Override
			public String read() throws Exception {
				throw new RuntimeException();

			}

		};

		step.setTasklet(new TestingChunkOrientedTasklet<>(itemReader, itemWriter));
		step.registerStepExecutionListener(new StepExecutionListenerSupport() {
			@Nullable
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				return ExitStatus.FAILED.addExitDescription("FOO");
			}
		});
		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		try {
			step.execute(stepExecution);
		}
		catch (Exception ex) {
			ExitStatus status = stepExecution.getExitStatus();
			assertEquals(ExitStatus.FAILED.getExitCode(), status.getExitCode());
			String description = status.getExitDescription();
			assertTrue("Description does not include 'FOO': " + description, description.indexOf("FOO") >= 0);
		}
	}

	/*
	 * make sure a job that has never been executed before, but does have
	 * saveExecutionAttributes = true, doesn't have restoreFrom called on it.
	 */
	@Test
	public void testNonRestartedJob() throws Exception {
		MockRestartableItemReader tasklet = new MockRestartableItemReader();
		step.setTasklet(new TestingChunkOrientedTasklet<>(tasklet, itemWriter));
		step.registerStream(tasklet);
		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		step.execute(stepExecution);

		assertFalse(tasklet.isRestoreFromCalled());
		assertTrue(tasklet.isGetExecutionAttributesCalled());
	}

	@Test
	public void testSuccessfulExecutionWithExecutionContext() throws Exception {
		final JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);
		final StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.setJobRepository(new JobRepositorySupport() {
			@Override
			public void updateExecutionContext(StepExecution stepExecution) {
				list.add(stepExecution);
			}
		});
		step.execute(stepExecution);

		// context saved before looping and updated once for every processing
		// loop (once in this case)
		assertEquals(3, list.size());
	}

	@Test
	public void testSuccessfulExecutionWithFailureOnSaveOfExecutionContext() throws Exception {
		final JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);
		final StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.setJobRepository(new JobRepositorySupport() {
			private int counter = 0;

			// initial save before item processing succeeds, later calls fail
			@Override
			public void updateExecutionContext(StepExecution stepExecution) {
				if (counter > 0) {
					throw new RuntimeException("foo");
				}
				counter++;
			}
		});

		step.execute(stepExecution);
		Throwable e = stepExecution.getFailureExceptions().get(0);
		assertEquals("foo", e.getCause().getMessage());
		assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
	}

	/*
	 * Test that a job that is being restarted, but has saveExecutionAttributes
	 * set to false, doesn't have restore or getExecutionAttributes called on
	 * it.
	 */
	@Test
	public void testNoSaveExecutionAttributesRestartableJob() {
		MockRestartableItemReader tasklet = new MockRestartableItemReader();
		step.setTasklet(new TestingChunkOrientedTasklet<>(tasklet, itemWriter));
		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		try {
			step.execute(stepExecution);
		}
		catch (Throwable t) {
			fail();
		}

		assertFalse(tasklet.isRestoreFromCalled());
	}

	/*
	 * Even though the job is restarted, and saveExecutionAttributes is true,
	 * nothing will be restored because the Tasklet does not implement
	 * Restartable.
	 */
	@Test
	public void testRestartJobOnNonRestartableTasklet() throws Exception {
		step.setTasklet(new TestingChunkOrientedTasklet<>(new ItemReader<String>() {
			@Nullable
			@Override
			public String read() throws Exception {
				return "foo";
			}
		}, itemWriter));
		JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		step.execute(stepExecution);
	}

	@Test
	public void testStreamManager() throws Exception {
		MockRestartableItemReader reader = new MockRestartableItemReader() {
			@Nullable
			@Override
			public String read() {
				return "foo";
			}

			@Override
			public void update(ExecutionContext executionContext) {
                                super.update(executionContext);
				executionContext.putString("foo", "bar");
			}
		};
		step.setTasklet(new TestingChunkOrientedTasklet<>(reader, itemWriter));
		step.registerStream(reader);
		JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		assertEquals(false, stepExecution.getExecutionContext().containsKey("foo"));

		step.execute(stepExecution);

		// At least once in that process the statistics service was asked for
		// statistics...
		assertEquals("bar", stepExecution.getExecutionContext().getString("foo"));
	}

	@Test
	public void testDirectlyInjectedItemStream() throws Exception {
		step.setStreams(new ItemStream[] { new ItemStreamSupport() {
			@Override
			public void update(ExecutionContext executionContext) {
                                super.update(executionContext);
				executionContext.putString("foo", "bar");
			}
		} });
		JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		assertEquals(false, stepExecution.getExecutionContext().containsKey("foo"));

		step.execute(stepExecution);

		assertEquals("bar", stepExecution.getExecutionContext().getString("foo"));
	}

	@Test
	public void testDirectlyInjectedListener() throws Exception {
		step.registerStepExecutionListener(new StepExecutionListenerSupport() {
			@Override
			public void beforeStep(StepExecution stepExecution) {
				list.add("foo");
			}

			@Nullable
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				list.add("bar");
				return null;
			}
		});
		JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);
		assertEquals(2, list.size());
	}

	@Test
	public void testListenerCalledBeforeStreamOpened() throws Exception {
		MockRestartableItemReader reader = new MockRestartableItemReader() {
			@Override
			public void beforeStep(StepExecution stepExecution) {
				list.add("foo");
			}

			@Override
			public void open(ExecutionContext executionContext) throws ItemStreamException {
                                super.open(executionContext);
				assertEquals(1, list.size());
			}
		};
		step.setStreams(new ItemStream[] { reader });
		step.registerStepExecutionListener(reader);
		StepExecution stepExecution = new StepExecution(step.getName(), new JobExecution(jobInstance, jobParameters));
		step.execute(stepExecution);
		assertEquals(1, list.size());
	}

	@Test
	public void testAfterStep() throws Exception {

		final ExitStatus customStatus = new ExitStatus("COMPLETED_CUSTOM");

		step.setStepExecutionListeners(new StepExecutionListener[] { new StepExecutionListenerSupport() {
			@Nullable
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				list.add("afterStepCalled");
				return customStatus;
			}
		} });

		RepeatTemplate stepTemplate = new RepeatTemplate();
		stepTemplate.setCompletionPolicy(new SimpleCompletionPolicy(5));
		step.setStepOperations(stepTemplate);

		JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);
		assertEquals(1, list.size());
		ExitStatus returnedStatus = stepExecution.getExitStatus();
		assertEquals(customStatus.getExitCode(), returnedStatus.getExitCode());
		assertEquals(customStatus.getExitDescription(), returnedStatus.getExitDescription());
	}

	@Test
	public void testDirectlyInjectedListenerOnError() throws Exception {
		step.registerStepExecutionListener(new StepExecutionListenerSupport() {
			@Nullable
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				list.add("exception");
				return null;
			}
		});
		step.setTasklet(new TestingChunkOrientedTasklet<>(new MockRestartableItemReader() {
			@Nullable
			@Override
			public String read() throws RuntimeException {
				throw new RuntimeException("FOO");
			}
		}, itemWriter));
		JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);
		step.execute(stepExecution);
		assertEquals("FOO", stepExecution.getFailureExceptions().get(0).getMessage());
		assertEquals(1, list.size());
	}

	@Test
	public void testDirectlyInjectedStreamWhichIsAlsoReader() throws Exception {
		MockRestartableItemReader reader = new MockRestartableItemReader() {
			@Nullable
			@Override
			public String read() {
				return "foo";
			}

			@Override
			public void update(ExecutionContext executionContext) {
                                super.update(executionContext);
				executionContext.putString("foo", "bar");
			}
		};
		step.setTasklet(new TestingChunkOrientedTasklet<>(reader, itemWriter));
		step.setStreams(new ItemStream[] { reader });
		JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecution);

		assertEquals(false, stepExecution.getExecutionContext().containsKey("foo"));

		step.execute(stepExecution);

		// At least once in that process the statistics service was asked for
		// statistics...
		assertEquals("bar", stepExecution.getExecutionContext().getString("foo"));
	}

	@Test
	public void testStatusForInterruptedException() throws Exception {

		StepInterruptionPolicy interruptionPolicy = new StepInterruptionPolicy() {

			@Override
			public void checkInterrupted(StepExecution stepExecution) throws JobInterruptedException {
				throw new JobInterruptedException("interrupted");
			}
		};

		step.setInterruptionPolicy(interruptionPolicy);

		ItemReader<String> itemReader = new ItemReader<String>() {

			@Nullable
			@Override
			public String read() throws Exception {
				throw new RuntimeException();

			}

		};

		step.setTasklet(new TestingChunkOrientedTasklet<>(itemReader, itemWriter));

		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		stepExecution.setExecutionContext(foobarEc);

		step.execute(stepExecution);
		assertEquals(BatchStatus.STOPPED, stepExecution.getStatus());
		String msg = stepExecution.getExitStatus().getExitDescription();
		assertTrue("Message does not contain 'JobInterruptedException': " + msg, msg
				.contains("JobInterruptedException"));
	}

	@Test
	public void testStatusForNormalFailure() throws Exception {

		ItemReader<String> itemReader = new ItemReader<String>() {
			@Nullable
			@Override
			public String read() throws Exception {
				// Trigger a rollback
				throw new RuntimeException("Foo");
			}
		};
		step.setTasklet(new TestingChunkOrientedTasklet<>(itemReader, itemWriter));

		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		stepExecution.setExecutionContext(foobarEc);
		// step.setLastExecution(stepExecution);

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// The original rollback was caused by this one:
		assertEquals("Foo", stepExecution.getFailureExceptions().get(0).getMessage());
	}

	@Test
	public void testStatusForErrorFailure() throws Exception {

		ItemReader<String> itemReader = new ItemReader<String>() {
			@Nullable
			@Override
			public String read() throws Exception {
				// Trigger a rollback
				throw new Error("Foo");
			}
		};
		step.setTasklet(new TestingChunkOrientedTasklet<>(itemReader, itemWriter));

		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		stepExecution.setExecutionContext(foobarEc);
		// step.setLastExecution(stepExecution);

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		// The original rollback was caused by this one:
		assertEquals("Foo", stepExecution.getFailureExceptions().get(0).getMessage());
	}

	@SuppressWarnings("serial")
	@Test
	public void testStatusForResetFailedException() throws Exception {

		ItemReader<String> itemReader = new ItemReader<String>() {
			@Nullable
			@Override
			public String read() throws Exception {
				// Trigger a rollback
				throw new RuntimeException("Foo");
			}
		};
		step.setTasklet(new TestingChunkOrientedTasklet<>(itemReader, itemWriter));
		step.setTransactionManager(new ResourcelessTransactionManager() {
			@Override
			protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
				// Simulate failure on rollback when stream resets
				throw new RuntimeException("Bar");
			}
		});

		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		stepExecution.setExecutionContext(foobarEc);
		// step.setLastExecution(stepExecution);

		step.execute(stepExecution);
		assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
		String msg = stepExecution.getExitStatus().getExitDescription();
		assertTrue("Message does not contain ResetFailedException: " + msg, msg.contains("ResetFailedException"));
		// The original rollback was caused by this one:
		assertEquals("Bar", stepExecution.getFailureExceptions().get(0).getMessage());
	}

	@SuppressWarnings("serial")
	@Test
	public void testStatusForCommitFailedException() throws Exception {

		step.setTransactionManager(new ResourcelessTransactionManager() {
			@Override
			protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
				// Simulate failure on commit
				throw new RuntimeException("Foo");
			}
			@Override
			protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
				throw new RuntimeException("Bar");
			}
		});

		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		stepExecution.setExecutionContext(foobarEc);
		// step.setLastExecution(stepExecution);

		step.execute(stepExecution);
		assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
		Throwable ex = stepExecution.getFailureExceptions().get(0);
		// The original rollback failed because of this one:
		assertEquals("Bar", ex.getMessage());
	}

	@Test
	public void testStatusForFinalUpdateFailedException() throws Exception {

		step.setJobRepository(new JobRepositorySupport());
		step.setStreams(new ItemStream[] { new ItemStreamSupport() {
			@Override
			public void close() throws ItemStreamException {
                                super.close();
				throw new RuntimeException("Bar");
			}
		} });

		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		step.execute(stepExecution);
		// The job actually completed, but the streams couldn't be closed.
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		String msg = stepExecution.getExitStatus().getExitDescription();
		assertEquals("", msg);
		Throwable ex = stepExecution.getFailureExceptions().get(0);

		// The original rollback was caused by this one:
		assertEquals("Bar", ex.getMessage());
	}

	@Test
	public void testStatusForCloseFailedException() throws Exception {

		MockRestartableItemReader itemReader = new MockRestartableItemReader() {
			@Override
			public void close() throws ItemStreamException {
				super.close();
				// Simulate failure on rollback when stream resets
				throw new RuntimeException("Bar");
			}
		};
		step.setTasklet(new TestingChunkOrientedTasklet<>(itemReader, itemWriter));
		step.registerStream(itemReader);

		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		stepExecution.setExecutionContext(foobarEc);
		// step.setLastExecution(stepExecution);

		step.execute(stepExecution);
		// The job actually completed, but the streams couldn't be closed.
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		String msg = stepExecution.getExitStatus().getExitDescription();
		assertEquals("", msg);
		Throwable ex = stepExecution.getFailureExceptions().get(0);
		// The original rollback was caused by this one:
		assertEquals("Bar", ex.getMessage());
	}

	/**
	 * Execution context must not be left empty even if job failed before
	 * committing first chunk - otherwise ItemStreams won't recognize it is
	 * restart scenario on next run.
	 */
	@Test
	public void testRestartAfterFailureInFirstChunk() throws Exception {
		MockRestartableItemReader reader = new MockRestartableItemReader() {
			@Nullable
			@Override
			public String read() throws RuntimeException {
				// fail on the very first item
				throw new RuntimeException("CRASH!");
			}
		};
		step.setTasklet(new TestingChunkOrientedTasklet<>(reader, itemWriter));
		step.registerStream(reader);

		StepExecution stepExecution = new StepExecution(step.getName(), new JobExecution(jobInstance, jobParameters));

		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		Throwable expected = stepExecution.getFailureExceptions().get(0);
		assertEquals("CRASH!", expected.getMessage());
		assertFalse(stepExecution.getExecutionContext().isEmpty());
		assertTrue(stepExecution.getExecutionContext().getString("spam").equals("bucket"));
	}

	@Test
	public void testStepToCompletion() throws Exception {

		RepeatTemplate template = new RepeatTemplate();

		// process all items:
		template.setCompletionPolicy(new DefaultResultCompletionPolicy());
		step.setStepOperations(template);

		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		step.execute(stepExecution);
		assertEquals(3, processed.size());
		assertEquals(3, stepExecution.getReadCount());
	}

	/**
	 * Exception in {@link StepExecutionListener#afterStep(StepExecution)}
	 * doesn't cause step failure.
	 * @throws JobInterruptedException
	 */
	@Test
	public void testStepFailureInAfterStepCallback() throws JobInterruptedException {
		StepExecutionListener listener = new StepExecutionListenerSupport() {
			@Nullable
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				throw new RuntimeException("exception thrown in afterStep to signal failure");
			}
		};
		step.setStepExecutionListeners(new StepExecutionListener[] { listener });
		StepExecution stepExecution = new StepExecution(step.getName(), new JobExecution(jobInstance, jobParameters));

		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

	}

	@Test
	public void testNoRollbackFor() throws Exception {

		step.setTasklet(new Tasklet() {
			@Nullable
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				throw new RuntimeException("Bar");
			}
		});

		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);

		@SuppressWarnings("serial")
		DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute() {
			@Override
			public boolean rollbackOn(Throwable ex) {
				return false;
			}
		};
		step.setTransactionAttribute(transactionAttribute);
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());

	}

	@Test
	public void testTaskletExecuteReturnNull() throws Exception {
		step.setTasklet(new Tasklet() {
			@Nullable
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				return null;
			}
		});
		JobExecution jobExecutionContext = new JobExecution(jobInstance, jobParameters);
		StepExecution stepExecution = new StepExecution(step.getName(), jobExecutionContext);
		step.execute(stepExecution);
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
	}

	private static class JobRepositoryStub extends JobRepositorySupport {

		private int updateCount = 0;

		@Override
		public void update(StepExecution stepExecution) {
			updateCount++;
			if (updateCount <= 3) {
				assertEquals(updateCount, stepExecution.getReadCount() + 1);
			}
		}

	}

	private static class JobRepositoryFailedUpdateStub extends JobRepositorySupport {

		private int called = 0;

		@Override
		public void update(StepExecution stepExecution) {
			called++;
			if (called == 3) {
				throw new DataAccessResourceFailureException("stub exception");
			}
		}
	}

	private class MockRestartableItemReader extends AbstractItemStreamItemReader<String> implements StepExecutionListener {

		private boolean getExecutionAttributesCalled = false;

		private boolean restoreFromCalled = false;

		@Nullable
		@Override
		public String read() {
			return "item";
		}

		@Override
		public void update(ExecutionContext executionContext) {
                        super.update(executionContext);
			getExecutionAttributesCalled = true;
			executionContext.putString("spam", "bucket");
		}

		public boolean isGetExecutionAttributesCalled() {
			return getExecutionAttributesCalled;
		}

		public boolean isRestoreFromCalled() {
			return restoreFromCalled;
		}

		@Nullable
		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			return null;
		}

		@Override
		public void beforeStep(StepExecution stepExecution) {
		}

	}

}
