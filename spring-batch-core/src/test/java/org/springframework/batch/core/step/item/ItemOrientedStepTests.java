/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.listener.StepListenerSupport;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.core.step.StepInterruptionPolicy;
import org.springframework.batch.core.step.item.ItemOrientedStep;
import org.springframework.batch.core.step.item.SimpleItemHandler;
import org.springframework.batch.item.AbstractItemReader;
import org.springframework.batch.item.AbstractItemWriter;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.ResetFailedException;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.DefaultTransactionStatus;

public class ItemOrientedStepTests extends TestCase {

	ArrayList processed = new ArrayList();

	private List list = new ArrayList();

	ItemWriter itemWriter = new AbstractItemWriter() {
		public void write(Object data) throws Exception {
			processed.add((String) data);
		}
	};

	private ItemOrientedStep itemOrientedStep;

	private RepeatTemplate template;

	private JobInstance jobInstance;

	private ResourcelessTransactionManager transactionManager;

	private ItemReader getReader(String[] args) {
		return new ListItemReader(Arrays.asList(args));
	}

	private AbstractStep getStep(String[] strings) throws Exception {
		ItemOrientedStep step = new ItemOrientedStep("stepName");
		step.setItemHandler(new SimpleItemHandler(getReader(strings), itemWriter));
		step.setJobRepository(new JobRepositorySupport());
		step.setTransactionManager(transactionManager);
		return step;
	}

	protected void setUp() throws Exception {
		MapJobInstanceDao.clear();
		MapStepExecutionDao.clear();
		MapJobExecutionDao.clear();

		transactionManager = new ResourcelessTransactionManager();

		itemOrientedStep = (ItemOrientedStep) getStep(new String[] { "foo", "bar", "spam" });
		template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		itemOrientedStep.setStepOperations(template);
		// Only process one item:
		template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		itemOrientedStep.setChunkOperations(template);

		jobInstance = new JobInstance(new Long(0), new JobParameters(), new JobSupport("FOO"));

		itemOrientedStep.setTransactionManager(transactionManager);

	}

	public void testStepExecutor() throws Exception {

		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		itemOrientedStep.execute(stepExecution);
		assertEquals(1, processed.size());
		assertEquals(1, stepExecution.getItemCount().intValue());
	}

	public void testChunkExecutor() throws Exception {

		template = new RepeatTemplate();

		// Only process one item:
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		itemOrientedStep.setChunkOperations(template);

		JobExecution jobExecution = new JobExecution(jobInstance);

		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);
		StepContribution contribution = stepExecution.createStepContribution();
		itemOrientedStep.processChunk(stepExecution, contribution);
		assertEquals(1, processed.size());
		assertEquals(0, stepExecution.getItemCount().intValue());
		assertEquals(1, contribution.getTaskCount());

	}

	public void testRepository() throws Exception {

		SimpleJobRepository repository = new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(),
		        new MapStepExecutionDao());
		itemOrientedStep.setJobRepository(repository);

		JobExecution jobExecution = repository.createJobExecution(jobInstance.getJob(), jobInstance.getJobParameters());
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);

		itemOrientedStep.execute(stepExecution);
		assertEquals(1, processed.size());
	}

	public void testIncrementRollbackCount() {

		ItemReader itemReader = new AbstractItemReader() {

			public Object read() throws Exception {
				int counter = 0;
				counter++;

				if (counter == 1) {
					throw new RuntimeException();
				}

				return ExitStatus.CONTINUABLE;
			}

		};

		itemOrientedStep.setItemHandler(new SimpleItemHandler(itemReader, itemWriter));
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		try {
			itemOrientedStep.execute(stepExecution);
		} catch (Exception ex) {
			assertEquals(stepExecution.getRollbackCount(), new Integer(1));
		}

	}

	public void testExitCodeDefaultClassification() throws Exception {

		ItemReader itemReader = new AbstractItemReader() {

			public Object read() throws Exception {
				int counter = 0;
				counter++;

				if (counter == 1) {
					throw new RuntimeException();
				}

				return ExitStatus.CONTINUABLE;
			}

		};

		itemOrientedStep.setItemHandler(new SimpleItemHandler(itemReader, itemWriter));
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		try {
			itemOrientedStep.execute(stepExecution);
		} catch (Exception ex) {
			ExitStatus status = stepExecution.getExitStatus();
			assertFalse(status.isContinuable());
		}
	}

	/*
	 * make sure a job that has never been executed before, but does have saveExecutionAttributes = true, doesn't have
	 * restoreFrom called on it.
	 */
	public void testNonRestartedJob() throws Exception {
		MockRestartableItemReader tasklet = new MockRestartableItemReader();
		itemOrientedStep.setItemHandler(new SimpleItemHandler(tasklet, itemWriter));
		itemOrientedStep.registerStream(tasklet);
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		itemOrientedStep.execute(stepExecution);

		assertFalse(tasklet.isRestoreFromCalled());
		assertTrue(tasklet.isGetExecutionAttributesCalled());
	}

	public void testSuccessfulExecutionWithExecutionContext() throws Exception {
		final JobExecution jobExecution = new JobExecution(jobInstance);
		final StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);
		itemOrientedStep.setJobRepository(new JobRepositorySupport() {
			public void saveOrUpdateExecutionContext(StepExecution stepExecution) {
				list.add(stepExecution);
			}
		});
		itemOrientedStep.execute(stepExecution);
		assertEquals(1, list.size());
	}

	public void testSuccessfulExecutionWithFailureOnSaveOfExecutionContext() throws Exception {
		final JobExecution jobExecution = new JobExecution(jobInstance);
		final StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);
		itemOrientedStep.setJobRepository(new JobRepositorySupport() {
			public void saveOrUpdateExecutionContext(StepExecution stepExecution) {
				throw new RuntimeException("foo");
			}
		});
		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected BatchCriticalException");
		} catch (UnexpectedJobExecutionException e) {
			assertEquals("foo", e.getCause().getMessage());
		}
		assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
	}

	/*
	 * make sure a job that has been executed before, and is therefore being restarted, is restored.
	 */
	// public void testRestartedJob() throws Exception {
	// String step = "stepName";
	// // step.setStepExecutionCount(1);
	// MockRestartableItemReader tasklet = new MockRestartableItemReader();
	// stepExecutor.setItemReader(tasklet);
	// stepConfiguration.setSaveExecutionContext(true);
	// JobExecution jobExecution = new JobExecution(jobInstance);
	// StepExecution stepExecution = new StepExecution(step, jobExecution);
	//
	// stepExecution
	// .setExecutionContext(new
	// ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
	// // step.setLastExecution(stepExecution);
	// stepExecutor.execute(stepExecution);
	//
	// assertTrue(tasklet.isRestoreFromCalled());
	// assertTrue(tasklet.isRestoreFromCalledWithSomeContext());
	// assertTrue(tasklet.isGetExecutionAttributesCalled());
	// }
	/*
	 * Test that a job that is being restarted, but has saveExecutionAttributes set to false, doesn't have restore or
	 * getExecutionAttributes called on it.
	 */
	public void testNoSaveExecutionAttributesRestartableJob() {
		MockRestartableItemReader tasklet = new MockRestartableItemReader();
		itemOrientedStep.setItemHandler(new SimpleItemHandler(tasklet, itemWriter));
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		try {
			itemOrientedStep.execute(stepExecution);
		} catch (Throwable t) {
			fail();
		}

		assertFalse(tasklet.isRestoreFromCalled());
	}

	/*
	 * Even though the job is restarted, and saveExecutionAttributes is true, nothing will be restored because the
	 * Tasklet does not implement Restartable.
	 */
	public void testRestartJobOnNonRestartableTasklet() throws Exception {
		itemOrientedStep.setItemHandler(new SimpleItemHandler(new AbstractItemReader() {
			public Object read() throws Exception {
				return "foo";
			}
		}, itemWriter));
		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);

		itemOrientedStep.execute(stepExecution);
	}

	public void testStreamManager() throws Exception {
		MockRestartableItemReader reader = new MockRestartableItemReader() {
			public Object read() throws Exception {
				return "foo";
			}

			public void update(ExecutionContext executionContext) {
				// TODO Auto-generated method stub
				executionContext.putString("foo", "bar");
			}
		};
		itemOrientedStep.setItemHandler(new SimpleItemHandler(reader, itemWriter));
		itemOrientedStep.registerStream(reader);
		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);

		assertEquals(false, stepExecution.getExecutionContext().containsKey("foo"));

		itemOrientedStep.execute(stepExecution);

		// At least once in that process the statistics service was asked for
		// statistics...
		assertEquals("bar", stepExecution.getExecutionContext().getString("foo"));
	}

	public void testDirectlyInjectedItemStream() throws Exception {
		itemOrientedStep.setStreams(new ItemStream[] { new ItemStreamSupport() {
			public void update(ExecutionContext executionContext) {
				executionContext.putString("foo", "bar");
			}
		} });
		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);

		assertEquals(false, stepExecution.getExecutionContext().containsKey("foo"));

		itemOrientedStep.execute(stepExecution);

		assertEquals("bar", stepExecution.getExecutionContext().getString("foo"));
	}

	public void testDirectlyInjectedListener() throws Exception {
		itemOrientedStep.registerStepListener(new StepListenerSupport() {
			public void beforeStep(StepExecution stepExecution) {
				list.add("foo");
			}

			public ExitStatus afterStep(StepExecution stepExecution) {
				list.add("bar");
				return null;
			}
		});
		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);
		itemOrientedStep.execute(stepExecution);
		assertEquals(2, list.size());
	}

	public void testListenerCalledBeforeStreamOpened() throws Exception {
		MockRestartableItemReader reader = new MockRestartableItemReader() {
			public void beforeStep(StepExecution stepExecution) {
				list.add("foo");
			}

			public void open(ExecutionContext executionContext) throws ItemStreamException {
				assertEquals(1, list.size());
			}
		};
		itemOrientedStep.setStreams(new ItemStream[] { reader });
		itemOrientedStep.registerStepListener(reader);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, new JobExecution(jobInstance));
		itemOrientedStep.execute(stepExecution);
		assertEquals(1, list.size());
	}

	public void testAfterStep() throws Exception {

		final ExitStatus customStatus = new ExitStatus(false, "custom code");

		itemOrientedStep.setStepListeners(new StepListener[] { new StepListenerSupport() {
			public ExitStatus afterStep(StepExecution stepExecution) {
				list.add("afterStepCalled");
				return customStatus;
			}
		} });

		RepeatTemplate stepTemplate = new RepeatTemplate();
		stepTemplate.setCompletionPolicy(new SimpleCompletionPolicy(5));
		itemOrientedStep.setStepOperations(stepTemplate);

		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);
		itemOrientedStep.execute(stepExecution);
		assertEquals(1, list.size());
		ExitStatus returnedStatus = stepExecution.getExitStatus();
		assertEquals(customStatus.getExitCode(), returnedStatus.getExitCode());
		assertEquals(customStatus.getExitDescription(), returnedStatus.getExitDescription());
	}

	public void testDirectlyInjectedListenerOnError() throws Exception {
		itemOrientedStep.registerStepListener(new StepListenerSupport() {
			public ExitStatus onErrorInStep(StepExecution stepExecution, Throwable e) {
				list.add(e);
				return null;
			}
		});
		itemOrientedStep.setItemHandler(new SimpleItemHandler(new MockRestartableItemReader() {
			public Object read() throws Exception {
				throw new RuntimeException("FOO");
			}
		}, itemWriter));
		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);
		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("FOO", e.getMessage());
		}
		assertEquals(1, list.size());
	}

	public void testDirectlyInjectedStreamWhichIsAlsoReader() throws Exception {
		MockRestartableItemReader reader = new MockRestartableItemReader() {
			public Object read() throws Exception {
				return "foo";
			}

			public void update(ExecutionContext executionContext) {
				// TODO Auto-generated method stub
				executionContext.putString("foo", "bar");
			}
		};
		itemOrientedStep.setItemHandler(new SimpleItemHandler(reader, itemWriter));
		itemOrientedStep.setStreams(new ItemStream[] { reader });
		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);

		assertEquals(false, stepExecution.getExecutionContext().containsKey("foo"));

		itemOrientedStep.execute(stepExecution);

		// At least once in that process the statistics service was asked for
		// statistics...
		assertEquals("bar", stepExecution.getExecutionContext().getString("foo"));
	}

	public void testStatusForInterruptedException() {

		StepInterruptionPolicy interruptionPolicy = new StepInterruptionPolicy() {

			public void checkInterrupted(StepExecution stepExecution) throws JobInterruptedException {
				throw new JobInterruptedException("interrupted");
			}
		};

		itemOrientedStep.setInterruptionPolicy(interruptionPolicy);

		ItemReader itemReader = new AbstractItemReader() {

			public Object read() throws Exception {
				int counter = 0;
				counter++;

				if (counter == 1) {
					throw new RuntimeException();
				}

				return ExitStatus.CONTINUABLE;
			}

		};

		itemOrientedStep.setItemHandler(new SimpleItemHandler(itemReader, itemWriter));

		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
		// step.setLastExecution(stepExecution);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected JobInterruptedException");
		} catch (JobInterruptedException ex) {
			assertEquals(BatchStatus.STOPPED, stepExecution.getStatus());
			String msg = stepExecution.getExitStatus().getExitDescription();
			assertTrue("Message does not contain 'interrupted': " + msg, contains(msg,
			        "interrupted"));
		}
	}

	public void testStatusForNormalFailure() throws Exception {

		ItemReader itemReader = new AbstractItemReader() {
			public Object read() throws Exception {
				// Trigger a rollback
				throw new RuntimeException("Foo");
			}
		};
		itemOrientedStep.setItemHandler(new SimpleItemHandler(itemReader, itemWriter));

		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
		// step.setLastExecution(stepExecution);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected RuntimeException");
		} catch (RuntimeException ex) {
			assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
			// The original rollback was caused by this one:
			assertEquals("Foo", ex.getMessage());
		}
	}

	public void testStatusForErrorFailure() throws Exception {

		ItemReader itemReader = new AbstractItemReader() {
			public Object read() throws Exception {
				// Trigger a rollback
				throw new Error("Foo");
			}
		};
		itemOrientedStep.setItemHandler(new SimpleItemHandler(itemReader, itemWriter));

		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
		// step.setLastExecution(stepExecution);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected Error");
		} catch (Error ex) {
			assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
			// The original rollback was caused by this one:
			assertEquals("Foo", ex.getMessage());
		}
	}

	public void testStatusForResetFailedException() throws Exception {

		ItemReader itemReader = new AbstractItemReader() {
			public Object read() throws Exception {
				// Trigger a rollback
				throw new RuntimeException("Foo");
			}
		};
		itemOrientedStep.setItemHandler(new SimpleItemHandler(itemReader, itemWriter));
		itemOrientedStep.setTransactionManager(new ResourcelessTransactionManager() {
			protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
				// Simulate failure on rollback when stream resets
				throw new ResetFailedException("Bar");
			}
		});

		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
		// step.setLastExecution(stepExecution);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected BatchCriticalException");
		} catch (UnexpectedJobExecutionException ex) {
			assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
			String msg = stepExecution.getExitStatus().getExitDescription();
			assertTrue("Message does not contain ResetFailedException: " + msg, contains(msg, "ResetFailedException"));
			// The original rollback was caused by this one:
			assertEquals("Bar", ex.getCause().getMessage());
		}
	}

	public void testStatusForCommitFailedException() throws Exception {

		itemOrientedStep.setTransactionManager(new ResourcelessTransactionManager() {
			protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
				// Simulate failure on commit
				throw new RuntimeException("Bar");
			}
		});

		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
		// step.setLastExecution(stepExecution);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected BatchCriticalException");
		} catch (UnexpectedJobExecutionException ex) {
			assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
			String msg = stepExecution.getExitStatus().getExitDescription();
			assertEquals("", msg);
			msg = ex.getMessage();
			assertTrue("Message does not contain 'saving': " + msg, contains(msg, "saving"));
			// The original rollback was caused by this one:
			assertEquals("Bar", ex.getCause().getMessage());
		}
	}

	public void testStatusForFinalUpdateFailedException() throws Exception {

		itemOrientedStep.setJobRepository(new JobRepositorySupport() {
			public void saveOrUpdate(StepExecution stepExecution) {
				if (stepExecution.getEndTime() != null) {
					throw new RuntimeException("Bar");
				}
				super.saveOrUpdate(stepExecution);
			}
		});

		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected RuntimeException");
		} catch (RuntimeException ex) {
			// The job actually completeed, but teh streams couldn't be closed.
			assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
			String msg = stepExecution.getExitStatus().getExitDescription();
			assertEquals("", msg);
			msg = ex.getMessage();
			assertTrue("Message does not contain 'final': " + msg, contains(msg, "final"));
			// The original rollback was caused by this one:
			assertEquals("Bar", ex.getCause().getMessage());
		}
	}

	public void testStatusForCloseFailedException() throws Exception {

		MockRestartableItemReader itemReader = new MockRestartableItemReader() {
			public void close(ExecutionContext executionContext) throws ItemStreamException {
				super.close(executionContext);
				// Simulate failure on rollback when stream resets
				throw new RuntimeException("Bar");
			}
		};
		itemOrientedStep.setItemHandler(new SimpleItemHandler(itemReader, itemWriter));
		itemOrientedStep.registerStream(itemReader);

		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
		// step.setLastExecution(stepExecution);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected InfrastructureException");
		} catch (UnexpectedJobExecutionException ex) {
			// The job actually completed, but the streams couldn't be closed.
			assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
			String msg = stepExecution.getExitStatus().getExitDescription();
			assertEquals("", msg);
			msg = ex.getMessage();
			assertTrue("Message does not contain 'close': " + msg, contains(msg, "close"));
			// The original rollback was caused by this one:
			assertEquals("Bar", ex.getCause().getMessage());
		}
	}

	private boolean contains(String str, String searchStr) {
		return str.indexOf(searchStr) != -1;
	}

	private class MockRestartableItemReader extends ItemStreamSupport implements ItemReader, StepListener {

		private boolean getExecutionAttributesCalled = false;

		private boolean restoreFromCalled = false;

		private boolean restoreFromCalledWithSomeContext = false;

		public Object read() throws Exception {
			return "item";
		}

		public boolean isRestoreFromCalledWithSomeContext() {
			return restoreFromCalledWithSomeContext;
		}

		public void update(ExecutionContext executionContext) {
			getExecutionAttributesCalled = true;
			executionContext.putString("spam", "bucket");
		}

		public boolean isGetExecutionAttributesCalled() {
			return getExecutionAttributesCalled;
		}

		public boolean isRestoreFromCalled() {
			return restoreFromCalled;
		}

		public void mark() throws MarkFailedException {
		}

		public void reset() throws ResetFailedException {
		}

		public ExitStatus afterStep(StepExecution stepExecution) {
			return null;
		}

		public void beforeStep(StepExecution stepExecution) {
		}

		public ExitStatus onErrorInStep(StepExecution stepExecution, Throwable e) {
			return null;
		}

	}

}
