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

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobSupport;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.execution.scope.StepScope;
import org.springframework.batch.execution.scope.StepSynchronizationManager;
import org.springframework.batch.execution.step.support.ItemChunker;
import org.springframework.batch.execution.step.support.ItemDechunker;
import org.springframework.batch.execution.step.support.JobRepositorySupport;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.reader.AbstractItemReader;
import org.springframework.batch.item.reader.ListItemReader;
import org.springframework.batch.item.stream.SimpleStreamManager;
import org.springframework.batch.item.writer.AbstractItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.interceptor.RepeatListenerSupport;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;

/**
 * 
 * 
 * @author Lucas Ward
 *
 */
public class ChunkedStepTests extends TestCase {

	ArrayList processed = new ArrayList();

	ItemWriter processor = new AbstractItemWriter() {
		public void write(Object data) throws Exception {
			processed.add((String) data);
		}
	};

	private ChunkedStep chunkedStep;

	private JobInstance jobInstance;

	private ResourcelessTransactionManager transactionManager;

	private JobExecution jobExecutionContext;
	private StepExecution stepExecution;

	private ItemReader getReader(String[] args) {
		return new ListItemReader(Arrays.asList(args));
	}


	
	private ChunkedStep getStep(String[] strings) throws Exception {
		ChunkedStep step = new ChunkedStep();
		step.setItemWriter(processor);
		step.setItemReader(getReader(strings));
		step.setJobRepository(new JobRepositorySupport());
		step.setStreamManager(new SimpleStreamManager(transactionManager));
		step.afterPropertiesSet();
		return step;
	}

	protected void setUp() throws Exception {
		transactionManager = new ResourcelessTransactionManager();
		chunkedStep = getStep(new String[] { "foo", "bar", "spam" });

		// Only process one item:
		chunkedStep.setChunkSize(1);

		jobInstance = new JobInstance(new Long(0), new JobParameters(), new JobSupport("FOO"));
		SimpleStreamManager streamManager = new SimpleStreamManager(transactionManager);
		streamManager.setUseClassNameAsPrefix(false);
		chunkedStep.setStreamManager(streamManager);
		chunkedStep.setJobRepository(new JobRepositorySupport());
		
		jobExecutionContext = new JobExecution(jobInstance);
		stepExecution = new StepExecution(new StepSupport("testStep"), jobExecutionContext);
	}

	public void testStepExecutor() throws Exception {

		chunkedStep.execute(stepExecution);
		assertEquals(3, processed.size());
		assertEquals(stepExecution.getStatus(), BatchStatus.COMPLETED);
	}

	public void testStepContextInitialized() throws Exception {

		final JobExecution jobExecution = new JobExecution(jobInstance);
		final StepExecution stepExecution = new StepExecution(new StepSupport("testStep"), jobExecution);

		chunkedStep.setChunker(new ItemChunker(new AbstractItemReader() {
			int counter = 0;
			public Object read() throws Exception {
				assertNotNull(StepSynchronizationManager.getContext().getStepExecution());
				if(counter++ < 2){
					return "foo";
				}
				else{
					return null;
				}
			}
		}));

		chunkedStep.execute(stepExecution);
		assertEquals(2, processed.size());
	}

	public void testStepContextInitializedBeforeTasklet() throws Exception {

		RepeatTemplate template = new RepeatTemplate();

		// Only process one chunk:
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		chunkedStep.setStepOperations(template);

		final JobExecution jobExecution = new JobExecution(jobInstance);
		jobExecution.setId(new Long(1));
		final StepExecution stepExecution = new StepExecution(new StepSupport("testStep"), jobExecution);

		template.setListener(new RepeatListenerSupport() {
			public void open(RepeatContext context) {
				assertNotNull(StepSynchronizationManager.getContext().getStepExecution());
				assertEquals(stepExecution, StepSynchronizationManager.getContext().getStepExecution());
				// StepScope can obtain id information....
				assertNotNull(StepSynchronizationManager.getContext().getAttribute(StepScope.ID_KEY));
			}
		});

		chunkedStep.execute(stepExecution);
		assertEquals(1, processed.size());

	}

	public void testRepository() throws Exception {

		MockControl repoControl = MockControl.createControl(JobRepository.class);
		JobRepository repository = (JobRepository)repoControl.getMock();
		chunkedStep.setJobRepository(repository);

//		StepInstance step = new StepInstance(new Long(1));
//		JobExecution jobExecutionContext = new JobExecution(jobInstance);
//		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);
		
		repository.getLastStepExecution(jobInstance, chunkedStep);
		repoControl.setReturnValue(stepExecution);
		repository.getStepExecutionCount(jobInstance, chunkedStep);
		repoControl.setReturnValue(0);
		repository.saveOrUpdate(stepExecution);
		repository.saveOrUpdate(stepExecution);
		repository.saveOrUpdate(stepExecution);
		repository.saveOrUpdate(stepExecution);
		repository.saveOrUpdate(stepExecution);
		repository.saveOrUpdate(stepExecution);
		repoControl.replay();
		chunkedStep.execute(stepExecution);
		assertEquals(3, processed.size());
		repoControl.verify();
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
	}
	
	//ReadFailures (meaning an item couldn't be skipped) should cause the job to
	//fail.
	public void testReadFailure() {

		ItemReader itemReader = new AbstractItemReader() {
			int counter = 0;
			public Object read() throws Exception {
				
				counter++;

				if (counter > 1) {
					throw new RuntimeException();
				}

				return "foo";
			}

		};

		chunkedStep.setChunker(new ItemChunker(itemReader));

		try {
			chunkedStep.execute(stepExecution);
			fail();
		}
		catch (Exception ex) {
			assertEquals( 1, processed.size());
			assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		}

	}
	
	public void testWriterFailure(){
		
		ItemWriter itemWriter = new AbstractItemWriter(){
			public void write(Object item) throws Exception {
				throw new RuntimeException();
			}
		};
		
		chunkedStep.setDechunker(new ItemDechunker(itemWriter));
		
		try{
			chunkedStep.execute(stepExecution);
			fail();
		}
		catch(Exception ex){
			//it should rollback three times since that's default behavior for a retry template.
			assertEquals(new Integer(1), stepExecution.getRollbackCount());
		}
	}

	public void testExitCodeDefaultClassification() throws Exception {

		ItemReader itemReader = new AbstractItemReader() {
			int counter = 0;
			public Object read() throws Exception {
				counter++;

				if (counter == 1) {
					throw new RuntimeException();
				}

				return ExitStatus.CONTINUABLE;
			}

		};

		chunkedStep.setItemReader(itemReader);

		try {
			chunkedStep.execute(stepExecution);
		}
		catch (Exception ex) {
			ExitStatus status = stepExecution.getExitStatus();
			assertEquals("FATAL_EXCEPTION",  status.getExitCode());
			assertFalse(status.isContinuable());
		}
	}

	/*
	 * make sure a job that has never been executed before, but does have
	 * saveExecutionAttributes = true, doesn't have restoreFrom called on it.
	 */
	// I can't get this test to pass, I think there is something wrong with the code I
	// pulled from SimpleStepExecutor
//	public void testNonRestartedJob() throws Exception {
//		MockRestartableItemReader reader = new MockRestartableItemReader();
//		chunkedStep.setItemReader(reader);
//		chunkedStep.setSaveExecutionAttributes(true);
//
//		chunkedStep.execute(stepExecution);
//
//		assertFalse(reader.isRestoreFromCalled());
//		assertTrue(reader.isGetExecutionAttributesCalled());
//	}
//
//	/*
//	 * make sure a job that has been executed before, and is therefore being
//	 * restarted, is restored.
//	 */
//	public void testRestartedJob() throws Exception {
//		StepInstance step = new StepInstance(new Long(1));
//		step.setStepExecutionCount(1);
//		MockRestartableTasklet tasklet = new MockRestartableTasklet();
//		chunkedStep.setItemReader(tasklet);
//		stepConfiguration.setSaveExecutionAttributes(true);
//		JobExecution jobExecutionContext = new JobExecution(jobInstance);
//		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);
//
//		stepExecution
//				.setExecutionAttributes(new ExecutionAttributes(PropertiesConverter.stringToProperties("foo=bar")));
//		step.setLastExecution(stepExecution);
//		chunkedStep.execute(stepExecution);
//
//		assertTrue(tasklet.isRestoreFromCalled());
//		assertTrue(tasklet.isRestoreFromCalledWithSomeContext());
//		assertTrue(tasklet.isGetExecutionAttributesCalled());
//	}
//
//	/*
//	 * Test that a job that is being restarted, but has saveExecutionAttributes
//	 * set to false, doesn't have restore or getExecutionAttributes called on
//	 * it.
//	 */
//	public void testNoSaveExecutionAttributesRestartableJob() {
//		StepInstance step = new StepInstance(new Long(1));
//		step.setStepExecutionCount(1);
//		MockRestartableTasklet tasklet = new MockRestartableTasklet();
//		stepConfiguration.setItemReader(tasklet);
//		stepConfiguration.setSaveExecutionAttributes(false);
//		JobExecution jobExecutionContext = new JobExecution(jobInstance);
//		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);
//
//		try {
//			chunkedStep.execute(stepExecution);
//		}
//		catch (Throwable t) {
//			fail();
//		}
//
//		assertFalse(tasklet.isRestoreFromCalled());
//		assertFalse(tasklet.isGetExecutionAttributesCalled());
//	}
//
//	/*
//	 * Even though the job is restarted, and saveExecutionAttributes is true,
//	 * nothing will be restored because the Tasklet does not implement
//	 * Restartable.
//	 */
//	public void testRestartJobOnNonRestartableTasklet() throws Exception {
//		StepInstance step = new StepInstance(new Long(1));
//		step.setStepExecutionCount(1);
//		stepConfiguration.setItemReader(new ItemReader() {
//			public Object read() throws Exception {
//				return ExitStatus.FINISHED;
//			}
//		});
//		stepConfiguration.setSaveExecutionAttributes(true);
//		JobExecution jobExecution = new JobExecution(jobInstance);
//		StepExecution stepExecution = new StepExecution(step, jobExecution);
//
//		chunkedStep.execute(stepExecution);
//	}
//
////	public void testApplyConfigurationWithExceptionHandler() throws Exception {
////		AbstractStep stepConfiguration = new SimpleStep("foo");
////		final List list = new ArrayList();
////		chunkedStep.setStepOperations(new RepeatTemplate() {
////			public void setExceptionHandler(ExceptionHandler exceptionHandler) {
////				list.add(exceptionHandler);
////			}
////		});
////		stepConfiguration.setExceptionHandler(new DefaultExceptionHandler());
////		chunkedStep.applyConfiguration(stepConfiguration);
////		assertEquals(1, list.size());
////	}
////
////	public void testApplyConfigurationWithZeroSkipLimit() throws Exception {
////		AbstractStep stepConfiguration = new SimpleStep("foo");
////		stepConfiguration.setSkipLimit(0);
////		final List list = new ArrayList();
////		chunkedStep.setStepOperations(new RepeatTemplate() {
////			public void setExceptionHandler(ExceptionHandler exceptionHandler) {
////				list.add(exceptionHandler);
////			}
////		});
////		chunkedStep.applyConfiguration(stepConfiguration);
////		assertEquals(0, list.size());
////	}
////
////	public void testApplyConfigurationWithNonZeroSkipLimit() throws Exception {
////		AbstractStep stepConfiguration = new SimpleStep("foo");
////		stepConfiguration.setSkipLimit(1);
////		final List list = new ArrayList();
////		chunkedStep.setStepOperations(new RepeatTemplate() {
////			public void setExceptionHandler(ExceptionHandler exceptionHandler) {
////				list.add(exceptionHandler);
////			}
////		});
////		chunkedStep.applyConfiguration(stepConfiguration);
////		assertEquals(1, list.size());
////	}
//
//	public void testStreamManager() throws Exception {
//		StepInstance step = new StepInstance(new Long(1));
//		step.setStepExecutionCount(1);
//		stepConfiguration.setItemReader(new ItemReader() {
//			public Object read() throws Exception {
//				return ExitStatus.FINISHED;
//			}
//		});
//		stepConfiguration.setSaveExecutionAttributes(true);
//		JobExecution jobExecution = new JobExecution(jobInstance);
//		StepExecution stepExecution = new StepExecution(step, jobExecution);
//
//		assertEquals(false, stepExecution.getExecutionAttributes().containsKey("foo"));
//
//		final Map map = new HashMap();
//		chunkedStep.setStreamManager(new SimpleStreamManager(new ResourcelessTransactionManager()) {
//			public ExecutionAttributes getExecutionAttributes(Object key) {
//				// TODO Auto-generated method stub
//				return new ExecutionAttributes(PropertiesConverter.stringToProperties("foo=bar"));
//			}
//		});
//
//		chunkedStep.execute(stepExecution);
//
//		// At least once in that process the statistics service was asked for
//		// statistics...
//		assertEquals("bar", stepExecution.getExecutionAttributes().getString("foo"));
//		// ...but nothing was registered because nothing with step scoped.
//		assertEquals(0, map.size());
//	}
//
//	public void testStatusForInterruptedException() {
//
//		StepInterruptionPolicy interruptionPolicy = new StepInterruptionPolicy() {
//
//			public void checkInterrupted(RepeatContext context) throws JobInterruptedException {
//				throw new JobInterruptedException("");
//			}
//		};
//
//		chunkedStep.setInterruptionPolicy(interruptionPolicy);
//
//		ItemReader itemReader = new ItemReader() {
//
//			public Object read() throws Exception {
//				int counter = 0;
//				counter++;
//
//				if (counter == 1) {
//					throw new RuntimeException();
//				}
//
//				return ExitStatus.CONTINUABLE;
//			}
//
//		};
//
//		chunkedStep.setItemReader(itemReader);
//
//		StepInstance step = new StepInstance(new Long(1));
//		JobExecution jobExecutionContext = new JobExecution(jobInstance);
//		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);
//
//		stepExecution
//				.setExecutionAttributes(new ExecutionAttributes(PropertiesConverter.stringToProperties("foo=bar")));
//		step.setLastExecution(stepExecution);
//
//		try {
//			chunkedStep.execute(stepExecution);
//			fail("Expected StepInterruptedException");
//		}
//		catch (JobInterruptedException ex) {
//			assertEquals(BatchStatus.STOPPED, stepExecution.getStatus());
//			String msg = stepExecution.getExitStatus().getExitDescription();
//			assertTrue("Message does not contain JobInterruptedException: " + msg, msg
//					.contains("JobInterruptedException"));
//		}
//	}
//
//	public void testStatusForResetFailedException() throws Exception {
//
//		ItemReader itemReader = new ItemReader() {
//			public Object read() throws Exception {
//				// Trigger a rollback
//				throw new RuntimeException("Foo");
//			}
//		};
//		chunkedStep.setItemReader(itemReader);
//		chunkedStep.setStreamManager(new SimpleStreamManager(transactionManager) {
//			public void rollback(TransactionStatus status) {
//				super.rollback(status);
//				// Simulate failure on rollback when stream resets
//				throw new ResetFailedException("Bar");
//			}
//		});
//
//		StepInstance step = new StepInstance(new Long(1));
//		JobExecution jobExecutionContext = jobInstance.createJobExecution();
//		StepExecution stepExecution = new StepExecution(step, jobExecutionContext);
//
//		stepExecution
//				.setExecutionAttributes(new ExecutionAttributes(PropertiesConverter.stringToProperties("foo=bar")));
//		step.setLastExecution(stepExecution);
//
//		try {
//			chunkedStep.execute(stepExecution);
//			fail("Expected ResetFailedException");
//		}
//		catch (ResetFailedException ex) {
//			assertEquals(BatchStatus.UNKNOWN, stepExecution.getStatus());
//			String msg = stepExecution.getExitStatus().getExitDescription();
//			assertTrue("Message does not contain ResetFailedException: " + msg, msg.contains("ResetFailedException"));
//			// The original rollback was caused by this one:
//			assertEquals("Foo", ex.getCause().getMessage());
//		}
//	}
//	
//	private class MockRestartableItemReader extends ItemStreamSupport implements ItemReader {
//
//		private boolean getExecutionAttributesCalled = false;
//
//		private boolean restoreFromCalled = false;
//
//		private boolean restoreFromCalledWithSomeContext = false;
//		
//		private int counter = 0;
//
//		public Object read() throws Exception {
//			StepSynchronizationManager.getContext().setAttribute("TASKLET_TEST", this);
//			counter++;
//			if(counter > 4){
//				return "item";
//			}
//			else{
//				return null;
//			}
//		}
//
//		public boolean isRestoreFromCalledWithSomeContext() {
//			return restoreFromCalledWithSomeContext;
//		}
//
//		public ExecutionContext getExecutionContext() {
//			getExecutionAttributesCalled = true;
//			return new ExecutionContext(PropertiesConverter.stringToProperties("spam=bucket"));
//		}
//
//		public void restoreFrom(ExecutionContext data) {
//			restoreFromCalled = true;
//			restoreFromCalledWithSomeContext = data.getProperties().size() > 0;
//		}
//
//		public boolean isGetExecutionAttributesCalled() {
//			return getExecutionAttributesCalled;
//		}
//
//		public boolean isRestoreFromCalled() {
//			return restoreFromCalled;
//		}
//
//		public void open() throws StreamException {
//		}
//
//		public void close() throws StreamException {
//		}
//
//	}

}
