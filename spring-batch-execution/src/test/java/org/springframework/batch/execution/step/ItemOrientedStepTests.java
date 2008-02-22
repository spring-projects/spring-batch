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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobSupport;
import org.springframework.batch.core.domain.StepContribution;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.MapJobDao;
import org.springframework.batch.execution.repository.dao.MapStepDao;
import org.springframework.batch.execution.scope.StepScope;
import org.springframework.batch.execution.scope.StepSynchronizationManager;
import org.springframework.batch.execution.step.ItemOrientedStep;
import org.springframework.batch.execution.step.support.JobRepositorySupport;
import org.springframework.batch.execution.step.support.StepInterruptionPolicy;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.exception.ResetFailedException;
import org.springframework.batch.item.exception.StreamException;
import org.springframework.batch.item.reader.ListItemReader;
import org.springframework.batch.item.stream.ItemStreamAdapter;
import org.springframework.batch.item.stream.SimpleStreamManager;
import org.springframework.batch.item.writer.AbstractItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.handler.DefaultExceptionHandler;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.repeat.interceptor.RepeatListenerAdapter;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.TransactionStatus;

public class ItemOrientedStepTests extends TestCase {

	ArrayList processed = new ArrayList();

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

		jobInstance = new JobInstance(new Long(0), new JobParameters());
		jobInstance.setJob(new JobSupport("FOO"));

		SimpleStreamManager streamManager = new SimpleStreamManager(transactionManager);
		streamManager.setUseClassNameAsPrefix(false);
		itemOrientedStep.setStreamManager(streamManager);

	}

	public void testStepExecutor() throws Exception {

		String step = "stepName";
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);

		itemOrientedStep.execute(stepExecution);
		assertEquals(1, processed.size());
		assertEquals(1, stepExecution.getTaskCount().intValue());
	}

	public void testChunkExecutor() throws Exception {

		template = new RepeatTemplate();

		// Only process one item:
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		itemOrientedStep.setChunkOperations(template);

		String step = "stepName";
		JobExecution jobExecution = new JobExecution(jobInstance);

		StepExecution stepExecution = new StepExecution(step, jobExecution);
		StepContribution contribution = stepExecution.createStepContribution();
		itemOrientedStep.processChunk(contribution);
		assertEquals(1, processed.size());
		assertEquals(0, stepExecution.getTaskCount().intValue());
		assertEquals(1, contribution.getTaskCount());

	}

	public void testStepContextInitialized() throws Exception {

		template = new RepeatTemplate();

		// Only process one item:
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		itemOrientedStep.setChunkOperations(template);

		final String step = "stepName";
		final JobExecution jobExecution = new JobExecution(jobInstance);
		final StepExecution stepExecution = new StepExecution(step, jobExecution);

		itemOrientedStep.setItemReader(new ItemReader() {
			public Object read() throws Exception {
				assertEquals(step, stepExecution.getStepName());
				assertNotNull(StepSynchronizationManager.getContext().getStepExecution());
				return "foo";
			}
		});

		itemOrientedStep.execute(stepExecution);
		assertEquals(1, processed.size());

	}

	public void testStepContextInitializedBeforeTasklet() throws Exception {

		template = new RepeatTemplate();

		// Only process one chunk:
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		itemOrientedStep.setStepOperations(template);

		final String step = "stepName";
		final JobExecution jobExecution = new JobExecution(jobInstance);
		jobExecution.setId(new Long(1));
		final StepExecution stepExecution = new StepExecution(step, jobExecution);

		template.setListener(new RepeatListenerAdapter() {
			public void open(RepeatContext context) {
				assertNotNull(StepSynchronizationManager.getContext().getStepExecution());
				assertEquals(stepExecution, StepSynchronizationManager.getContext().getStepExecution());
				// StepScope can obtain id information....
				assertNotNull(StepSynchronizationManager.getContext().getAttribute(StepScope.ID_KEY));
			}
		});

		itemOrientedStep.execute(stepExecution);
		assertEquals(1, processed.size());

	}

	public void testRepository() throws Exception {

		SimpleJobRepository repository = new SimpleJobRepository(new MapJobDao(), new MapJobDao(), new MapStepDao());
		itemOrientedStep.setJobRepository(repository);

		String step = "stepName";
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);

		itemOrientedStep.execute(stepExecution);
		assertEquals(1, processed.size());
	}

	public void testIncrementRollbackCount() {

		ItemReader itemReader = new ItemReader() {

			public Object read() throws Exception {
				int counter = 0;
				counter++;

				if (counter == 1) {
					throw new RuntimeException();
				}

				return ExitStatus.CONTINUABLE;
			}

		};

		String step = "stepName";
		itemOrientedStep.setItemReader(itemReader);
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);

		try {
			itemOrientedStep.execute(stepExecution);
		}
		catch (Exception ex) {
			assertEquals(stepExecution.getRollbackCount(), new Integer(1));
		}

	}

	public void testExitCodeDefaultClassification() throws Exception {

		ItemReader itemReader = new ItemReader() {

			public Object read() throws Exception {
				int counter = 0;
				counter++;

				if (counter == 1) {
					throw new RuntimeException();
				}

				return ExitStatus.CONTINUABLE;
			}

		};

		String step = "stepName";
		itemOrientedStep.setItemReader(itemReader);
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);

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
		String step = "stepName";
		MockRestartableItemReader tasklet = new MockRestartableItemReader();
		itemOrientedStep.setItemReader(tasklet);
		itemOrientedStep.setSaveExecutionContext(true);
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);

		itemOrientedStep.execute(stepExecution);

		assertFalse(tasklet.isRestoreFromCalled());
		assertTrue(tasklet.isGetExecutionAttributesCalled());
	}

	/*
	 * make sure a job that has been executed before, and is therefore being
	 * restarted, is restored.
	 */
//	public void testRestartedJob() throws Exception {
//		String step = "stepName";
////		step.setStepExecutionCount(1);
//		MockRestartableItemReader tasklet = new MockRestartableItemReader();
//		stepExecutor.setItemReader(tasklet);
//		stepConfiguration.setSaveExecutionContext(true);
//		JobExecution jobExecution = new JobExecution(jobInstance);
//		StepExecution stepExecution = new StepExecution(step, jobExecution);
//
//		stepExecution
//				.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
////		step.setLastExecution(stepExecution);
//		stepExecutor.execute(stepExecution);
//
//		assertTrue(tasklet.isRestoreFromCalled());
//		assertTrue(tasklet.isRestoreFromCalledWithSomeContext());
//		assertTrue(tasklet.isGetExecutionAttributesCalled());
//	}

	/*
	 * Test that a job that is being restarted, but has saveExecutionAttributes
	 * set to false, doesn't have restore or getExecutionAttributes called on
	 * it.
	 */
	public void testNoSaveExecutionAttributesRestartableJob() {
		String step = "stepName";
//		step.setStepExecutionCount(1);
		MockRestartableItemReader tasklet = new MockRestartableItemReader();
		itemOrientedStep.setItemReader(tasklet);
		itemOrientedStep.setSaveExecutionContext(false);
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);

		try {
			itemOrientedStep.execute(stepExecution);
		}
		catch (Throwable t) {
			fail();
		}

		assertFalse(tasklet.isRestoreFromCalled());
		assertFalse(tasklet.isGetExecutionAttributesCalled());
	}

	/*
	 * Even though the job is restarted, and saveExecutionAttributes is true,
	 * nothing will be restored because the Tasklet does not implement
	 * Restartable.
	 */
	public void testRestartJobOnNonRestartableTasklet() throws Exception {
		String step = "stepName";
//		step.setStepExecutionCount(1);
		itemOrientedStep.setItemReader(new ItemReader() {
			public Object read() throws Exception {
				return "foo";
			}
		});
		itemOrientedStep.setSaveExecutionContext(true);
		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(step, jobExecution);

		itemOrientedStep.execute(stepExecution);
	}

	public void testApplyConfigurationWithExceptionHandler() throws Exception {
		AbstractStep stepConfiguration = new StubStep("foo");
		final List list = new ArrayList();
		itemOrientedStep.setStepOperations(new RepeatTemplate() {
			public void setExceptionHandler(ExceptionHandler exceptionHandler) {
				list.add(exceptionHandler);
			}
		});
		stepConfiguration.setExceptionHandler(new DefaultExceptionHandler());
		itemOrientedStep.applyConfiguration(stepConfiguration);
		assertEquals(1, list.size());
	}

	public void testApplyConfigurationWithZeroSkipLimit() throws Exception {
		AbstractStep stepConfiguration = new StubStep("foo");
		stepConfiguration.setSkipLimit(0);
		final List list = new ArrayList();
		itemOrientedStep.setStepOperations(new RepeatTemplate() {
			public void setExceptionHandler(ExceptionHandler exceptionHandler) {
				list.add(exceptionHandler);
			}
		});
		itemOrientedStep.applyConfiguration(stepConfiguration);
		assertEquals(0, list.size());
	}

	public void testApplyConfigurationWithNonZeroSkipLimit() throws Exception {
		AbstractStep stepConfiguration = new StubStep("foo");
		stepConfiguration.setSkipLimit(1);
		final List list = new ArrayList();
		itemOrientedStep.setStepOperations(new RepeatTemplate() {
			public void setExceptionHandler(ExceptionHandler exceptionHandler) {
				list.add(exceptionHandler);
			}
		});
		itemOrientedStep.applyConfiguration(stepConfiguration);
		assertEquals(1, list.size());
	}

	public void testStreamManager() throws Exception {
		String step = "stepName";
//		step.setStepExecutionCount(1);
		itemOrientedStep.setItemReader(new ItemReader() {
			public Object read() throws Exception {
				return "foo";
			}
		});
		itemOrientedStep.setSaveExecutionContext(true);
		JobExecution jobExecution = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(step, jobExecution);

		assertEquals(false, stepExecution.getExecutionContext().containsKey("foo"));

		final Map map = new HashMap();
		itemOrientedStep.setStreamManager(new SimpleStreamManager(new ResourcelessTransactionManager()) {
			public ExecutionContext getExecutionContext(Object key) {
				// TODO Auto-generated method stub
				return new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar"));
			}
		});

		itemOrientedStep.execute(stepExecution);

		// At least once in that process the statistics service was asked for
		// statistics...
		assertEquals("bar", stepExecution.getExecutionContext().getString("foo"));
		// ...but nothing was registered because nothing with step scoped.
		assertEquals(0, map.size());
	}

	private class MockRestartableItemReader extends ItemStreamAdapter implements ItemReader {

		private boolean getExecutionAttributesCalled = false;

		private boolean restoreFromCalled = false;

		private boolean restoreFromCalledWithSomeContext = false;

		public Object read() throws Exception {
			StepSynchronizationManager.getContext().setAttribute("TASKLET_TEST", this);
			return "item";
		}

		public boolean isRestoreFromCalledWithSomeContext() {
			return restoreFromCalledWithSomeContext;
		}

		public ExecutionContext getExecutionContext() {
			getExecutionAttributesCalled = true;
			return new ExecutionContext(PropertiesConverter.stringToProperties("spam=bucket"));
		}

		public void restoreFrom(ExecutionContext data) {
			restoreFromCalled = true;
			restoreFromCalledWithSomeContext = data.getProperties().size() > 0;
		}

		public boolean isGetExecutionAttributesCalled() {
			return getExecutionAttributesCalled;
		}

		public boolean isRestoreFromCalled() {
			return restoreFromCalled;
		}

		public void open() throws StreamException {
		}

		public void close() throws StreamException {
		}

	}

	public void testStatusForInterruptedException() {

		StepInterruptionPolicy interruptionPolicy = new StepInterruptionPolicy() {

			public void checkInterrupted(RepeatContext context) throws JobInterruptedException {
				throw new JobInterruptedException("");
			}
		};

		itemOrientedStep.setInterruptionPolicy(interruptionPolicy);

		ItemReader itemReader = new ItemReader() {

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

		String step = "stepName";
		JobExecution jobExecutionContext = new JobExecution(jobInstance);
		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);

		stepExecution
				.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
//		step.setLastExecution(stepExecution);

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

	public void testStatusForResetFailedException() throws Exception {

		ItemReader itemReader = new ItemReader() {
			public Object read() throws Exception {
				// Trigger a rollback
				throw new RuntimeException("Foo");
			}
		};
		itemOrientedStep.setItemReader(itemReader);
		itemOrientedStep.setStreamManager(new SimpleStreamManager(transactionManager) {
			public void rollback(TransactionStatus status) {
				super.rollback(status);
				// Simulate failure on rollback when stream resets
				throw new ResetFailedException("Bar");
			}
		});

		String step = "stepName";
		JobExecution jobExecutionContext = jobInstance.createJobExecution();
		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);

		stepExecution
				.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("foo=bar")));
//		step.setLastExecution(stepExecution);

		try {
			itemOrientedStep.execute(stepExecution);
			fail("Expected ResetFailedException");
		}
		catch (ResetFailedException ex) {
			assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
			String msg = stepExecution.getExitStatus().getExitDescription();
			assertTrue("Message does not contain ResetFailedException: " + msg, msg.contains("ResetFailedException"));
			// The original rollback was caused by this one:
			assertEquals("Foo", ex.getCause().getMessage());
		}
	}
	
	private class StubStep extends AbstractStep{

		public StubStep(String name) {
			super(name);
		}
		
		public void execute(StepExecution stepExecution)
				throws JobInterruptedException, BatchCriticalException {
		}
		
	}

}
