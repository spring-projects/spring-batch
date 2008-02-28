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

package org.springframework.batch.execution.step;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepContribution;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.interceptor.StepListenerSupport;
import org.springframework.batch.execution.job.JobSupport;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.MapJobExecutionDao;
import org.springframework.batch.execution.repository.dao.MapJobInstanceDao;
import org.springframework.batch.execution.repository.dao.MapStepExecutionDao;
import org.springframework.batch.execution.step.support.JobRepositorySupport;
import org.springframework.batch.execution.step.support.StepInterruptionPolicy;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.exception.MarkFailedException;
import org.springframework.batch.item.exception.ResetFailedException;
import org.springframework.batch.item.exception.StreamException;
import org.springframework.batch.item.reader.AbstractItemReader;
import org.springframework.batch.item.reader.ListItemReader;
import org.springframework.batch.item.stream.ItemStreamSupport;
import org.springframework.batch.item.writer.AbstractItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.handler.DefaultExceptionHandler;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.DefaultTransactionStatus;

public class ItemOrientedStepTests extends TestCase {

	ArrayList processed = new ArrayList();

	private List list = new ArrayList();

	ItemWriter processor = new AbstractItemWriter() {
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
		ItemOrientedStep step = new ItemOrientedStep();
		step.setName("stepName");
		step.setItemWriter(processor);
		step.setItemReader(getReader(strings));
		step.setJobRepository(new JobRepositorySupport());
		step.setTransactionManager(transactionManager);
		step.afterPropertiesSet();
		return step;
	}

	protected void setUp() throws Exception {
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
		assertEquals(1, stepExecution.getTaskCount().intValue());
	}

	public void testChunkExecutor() throws Exception {

		template = new RepeatTemplate();

		// Only process one item:
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		itemOrientedStep.setChunkOperations(template);

		JobExecution jobExecution = new JobExecution(jobInstance);

		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);
		StepContribution contribution = stepExecution.createStepContribution();
		itemOrientedStep.processChunk(contribution);
		assertEquals(1, processed.size());
		assertEquals(0, stepExecution.getTaskCount().intValue());
		assertEquals(1, contribution.getTaskCount());

	}

	public void testRepository() throws Exception {

		SimpleJobRepository repository = new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(), new MapStepExecutionDao());
		itemOrientedStep.setJobRepository(repository);

		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

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

		itemOrientedStep.setItemReader(itemReader);
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		try {
			itemOrientedStep.execute(stepExecution);
		}
		catch (Exception ex) {
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

		itemOrientedStep.setItemReader(itemReader);
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		try {
			itemOrientedStep.execute(stepExecution);
		}
		catch (Exception ex) {
			ExitStatus status = stepExecution.getExitStatus();
			assertFalse(status.isContinuable());
		}
	}

	/*
	 * make sure a job that has never been executed before, but does have
	 * saveExecutionAttributes = true, doesn't have restoreFrom called on it.
	 */
	public void testNonRestartedJob() throws Exception {
		MockRestartableItemReader tasklet = new MockRestartableItemReader();
		itemOrientedStep.setItemReader(tasklet);
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
		}
		catch (BatchCriticalException e) {
			assertEquals("foo", e.getCause().getMessage());
		}
		assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
	}

	/*
	 * make sure a job that has been executed before, and is therefore being
	 * restarted, is restored.
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
	 * Test that a job that is being restarted, but has saveExecutionAttributes
	 * set to false, doesn't have restore or getExecutionAttributes called on
	 * it.
	 */
	public void testNoSaveExecutionAttributesRestartableJob() {
		MockRestartableItemReader tasklet = new MockRestartableItemReader();
		itemOrientedStep.setItemReader(tasklet);
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		try {
			itemOrientedStep.execute(stepExecution);
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
	public void testRestartJobOnNonRestartableTasklet() throws Exception {
		itemOrientedStep.setItemReader(new AbstractItemReader() {
			public Object read() throws Exception {
				return "foo";
			}
		});
		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);

		itemOrientedStep.execute(stepExecution);
	}

	public void testApplyConfigurationWithExceptionHandler() throws Exception {
		final List list = new ArrayList();
		itemOrientedStep.setStepOperations(new RepeatTemplate() {
			public void setExceptionHandler(ExceptionHandler exceptionHandler) {
				list.add(exceptionHandler);
			}
		});
		itemOrientedStep.setExceptionHandler(new DefaultExceptionHandler());
		itemOrientedStep.applyConfiguration();
		assertEquals(1, list.size());
	}

	public void testStreamManager() throws Exception {
		itemOrientedStep.setItemReader(new MockRestartableItemReader() {
			public Object read() throws Exception {
				return "foo";
			}
			public void update(ExecutionContext executionContext) {
				// TODO Auto-generated method stub
				executionContext.putString("foo", "bar");
			}
		});
		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);

		assertEquals(false, stepExecution.getExecutionContext().containsKey("foo"));

		itemOrientedStep.execute(stepExecution);

		// At least once in that process the statistics service was asked for
		// statistics...
		assertEquals("bar", stepExecution.getExecutionContext().getString("foo"));
	}

	public void testDirectlyInjectedItemStream() throws Exception {
		itemOrientedStep.setListeners(new Object[] {new ItemStreamSupport() {
			public void update(ExecutionContext executionContext) {
				executionContext.putString("foo", "bar");
			}
		}});
		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);

		assertEquals(false, stepExecution.getExecutionContext().containsKey("foo"));

		itemOrientedStep.execute(stepExecution);

		assertEquals("bar", stepExecution.getExecutionContext().getString("foo"));
	}

	public void testDirectlyInjectedListener() throws Exception {
		itemOrientedStep.setListeners(new Object[] {new StepListenerSupport() {
			public void beforeStep(StepExecution stepExecution) {
				list.add("foo");
			}
			public ExitStatus afterStep() {
				list.add("bar");
				return null;
			}
		}});
		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecution);
		itemOrientedStep.execute(stepExecution);
		assertEquals(2, list.size());
	}

	public void testDirectlyInjectedListenerOnError() throws Exception {
		itemOrientedStep.setListeners(new Object[] {new StepListenerSupport() {
			public ExitStatus onErrorInStep(Throwable e) {
				list.add(e);
				return null;
			}
		}});
		itemOrientedStep.setItemReader(new MockRestartableItemReader() {
			public Object read() throws Exception {
				throw new RuntimeException("FOO");
			}
		});
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
		itemOrientedStep.setItemReader(reader);
		itemOrientedStep.setListeners(new Object[] {reader});
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

			public void checkInterrupted(RepeatContext context) throws JobInterruptedException {
				throw new JobInterruptedException("");
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

		itemOrientedStep.setItemReader(itemReader);

		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
		// step.setLastExecution(stepExecution);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected StepInterruptedException");
		}
		catch (JobInterruptedException ex) {
			assertEquals(BatchStatus.STOPPED, stepExecution.getStatus());
			String msg = stepExecution.getExitStatus().getExitDescription();
			assertTrue("Message does not contain JobInterruptedException: " + msg, msg
					.contains("JobInterruptedException"));
		}
	}

	public void testStatusForNormalFailure() throws Exception {

		ItemReader itemReader = new AbstractItemReader() {
			public Object read() throws Exception {
				// Trigger a rollback
				throw new RuntimeException("Foo");
			}
		};
		itemOrientedStep.setItemReader(itemReader);

		JobExecution jobExecutionContext = jobInstance.createJobExecution();
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
		// step.setLastExecution(stepExecution);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected RuntimeException");
		}
		catch (RuntimeException ex) {
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
		itemOrientedStep.setItemReader(itemReader);
		itemOrientedStep.setTransactionManager(new ResourcelessTransactionManager() {
			protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
				// Simulate failure on rollback when stream resets
				throw new ResetFailedException("Bar");
			}
		});

		JobExecution jobExecutionContext = jobInstance.createJobExecution();
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
		// step.setLastExecution(stepExecution);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected BatchCriticalException");
		}
		catch (BatchCriticalException ex) {
			assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
			String msg = stepExecution.getExitStatus().getExitDescription();
			assertTrue("Message does not contain ResetFailedException: " + msg, msg.contains("ResetFailedException"));
			// The original rollback was caused by this one:
			assertEquals("Bar", ex.getCause().getMessage());
		}
	}

	public void testStatusForCommitFailedException() throws Exception {

		itemOrientedStep.setTransactionManager(new ResourcelessTransactionManager() {
			protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
				// Simulate failure on rollback when stream resets
				throw new RuntimeException("Bar");
			}
		});

		JobExecution jobExecutionContext = jobInstance.createJobExecution();
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
		// step.setLastExecution(stepExecution);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected BatchCriticalException");
		}
		catch (BatchCriticalException ex) {
			assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
			String msg = stepExecution.getExitStatus().getExitDescription();
			assertEquals("", msg);
			msg = ex.getMessage();
			assertTrue("Message does not contain 'saving': " + msg, msg.contains("saving"));
			// The original rollback was caused by this one:
			assertEquals("Bar", ex.getCause().getMessage());
		}
	}

	public void testStatusForFinalUpdateFailedException() throws Exception {

		itemOrientedStep.setJobRepository(new JobRepositorySupport() {
			public void saveOrUpdate(StepExecution stepExecution) {
				if (stepExecution.getEndTime()!=null) {
					throw new RuntimeException("Bar");
				}
				super.saveOrUpdate(stepExecution);
			}
		});

		JobExecution jobExecutionContext = jobInstance.createJobExecution();
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected RuntimeException");
		}
		catch (RuntimeException ex) {
			// The job actually completeed, but teh streams couldn't be closed.
			assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
			String msg = stepExecution.getExitStatus().getExitDescription();
			assertEquals("", msg);
			msg = ex.getMessage();
			assertTrue("Message does not contain 'final': " + msg, msg.contains("final"));
			// The original rollback was caused by this one:
			assertEquals("Bar", ex.getCause().getMessage());
		}
	}

	public void testStatusForCloseFailedException() throws Exception {

		itemOrientedStep.setItemReader(new MockRestartableItemReader() {
			public void close(ExecutionContext executionContext) throws StreamException {
				super.close(executionContext);
				// Simulate failure on rollback when stream resets
				throw new RuntimeException("Bar");
			}
		});

		JobExecution jobExecutionContext = jobInstance.createJobExecution();
		StepExecution stepExecution = new StepExecution(itemOrientedStep, jobExecutionContext);

		stepExecution.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
		// step.setLastExecution(stepExecution);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected BatchCriticalException");
		}
		catch (BatchCriticalException ex) {
			// The job actually completeed, but teh streams couldn't be closed.
			assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
			String msg = stepExecution.getExitStatus().getExitDescription();
			assertEquals("", msg);
			msg = ex.getMessage();
			assertTrue("Message does not contain 'close': " + msg, msg.contains("close"));
			// The original rollback was caused by this one:
			assertEquals("Bar", ex.getCause().getMessage());
		}
	}

	private class MockRestartableItemReader extends ItemStreamSupport implements ItemReader {

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

		public void open(ExecutionContext executionContext) throws StreamException {
		}

		public void close(ExecutionContext executionContext) throws StreamException {
		}

		public void mark() throws MarkFailedException {
		}

		public void reset() throws ResetFailedException {
		}

	}

}
