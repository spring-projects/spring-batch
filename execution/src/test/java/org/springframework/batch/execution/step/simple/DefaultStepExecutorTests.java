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

package org.springframework.batch.execution.step.simple;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.TestCase;

import org.springframework.batch.core.configuration.StepConfigurationSupport;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.executor.ExitCodeExceptionClassifier;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.MapJobDao;
import org.springframework.batch.execution.repository.dao.MapStepDao;
import org.springframework.batch.execution.scope.StepSynchronizationManager;
import org.springframework.batch.execution.step.SimpleStepConfiguration;
import org.springframework.batch.execution.tasklet.ItemProviderProcessTasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemProvider;
import org.springframework.batch.item.provider.ListItemProvider;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;

public class DefaultStepExecutorTests extends TestCase {

	ArrayList processed = new ArrayList();

	ItemProcessor processor = new ItemProcessor() {
		public void process(Object data) throws Exception {
			processed.add((String) data);
		}
	};

	private DefaultStepExecutor stepExecutor;

	private StepConfigurationSupport stepConfiguration;

	private ItemProvider getProvider(String[] args) {
		return new ListItemProvider(Arrays.asList(args));
	}

	/**
	 * @param strings
	 * @return
	 */
	private Tasklet getTasklet(String[] strings) {
		ItemProviderProcessTasklet module = new ItemProviderProcessTasklet();
		module.setItemProcessor(processor);
		module.setItemProvider(getProvider(strings));
		return module;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		stepExecutor = new DefaultStepExecutor();
		stepExecutor.setRepository(new JobRepositorySupport());
		stepConfiguration = new SimpleStepConfiguration();
		stepConfiguration.setTasklet(getTasklet(new String[] { "foo", "bar",
				"spam" }));
		// Only process one chunk:
		RepeatTemplate template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		stepExecutor.setStepOperations(template);
		// Only process one item:
		template = new RepeatTemplate();
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		stepExecutor.setChunkOperations(template);
	}

	public void testStepExecutor() throws Exception {

		StepInstance step = new StepInstance(new Long(9));
		SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("FOO");
		JobExecution jobExecutionContext = new JobExecution(new JobInstance(
				jobIdentifier, new Long(3)));
		StepExecution stepExecution = new StepExecution(step,
				jobExecutionContext);

		stepExecutor.process(stepConfiguration, stepExecution);
		assertEquals(1, processed.size());
	}

	public void testChunkExecutor() throws Exception {

		RepeatTemplate template = new RepeatTemplate();

		// Only process one item:
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		stepExecutor.setChunkOperations(template);

		StepInstance step = new StepInstance(new Long(1));
		SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("FOO");
		JobExecution jobExecution = new JobExecution(new JobInstance(
				jobIdentifier, new Long(1)));

		StepExecution stepExecution = new StepExecution(step, jobExecution);
		stepExecutor.processChunk(stepConfiguration, stepExecution);
		assertEquals(1, processed.size());

	}

	public void testStepContextInitialized() throws Exception {

		RepeatTemplate template = new RepeatTemplate();

		// Only process one item:
		template.setCompletionPolicy(new SimpleCompletionPolicy(1));
		stepExecutor.setChunkOperations(template);

		final StepInstance step = new StepInstance(new Long(1));
		SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("FOO");
		final JobExecution jobExecution = new JobExecution(new JobInstance(
				jobIdentifier, new Long(3)));
		final StepExecution stepExecution = new StepExecution(step,
				jobExecution);

		stepConfiguration.setTasklet(new Tasklet() {
			public ExitStatus execute() throws Exception {
				assertEquals(step, stepExecution.getStep());
				assertEquals(1, jobExecution.getChunkContexts().size());
				assertEquals(1, jobExecution.getStepContexts().size());
				assertNotNull(StepSynchronizationManager.getContext()
						.getStepExecution());
				processed.add("foo");
				return ExitStatus.CONTINUABLE;
			}
		});

		stepExecutor.process(stepConfiguration, stepExecution);
		assertEquals(1, processed.size());

	}

	public void testRepository() throws Exception {

		SimpleJobRepository repository = new SimpleJobRepository(
				new MapJobDao(), new MapStepDao());
		stepExecutor.setRepository(repository);

		StepInstance step = new StepInstance(new Long(1));
		SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("FOO");
		JobExecution jobExecutionContext = new JobExecution(new JobInstance(
				jobIdentifier, new Long(3)));
		StepExecution stepExecution = new StepExecution(step,
				jobExecutionContext);

		stepExecutor.process(stepConfiguration, stepExecution);
		assertEquals(1, processed.size());
	}

	public void testIncrementRollbackCount() {

		Tasklet tasklet = new Tasklet() {

			public ExitStatus execute() throws Exception {
				int counter = 0;
				counter++;

				if (counter == 1) {
					throw new Exception();
				}

				return ExitStatus.CONTINUABLE;
			}

		};

		StepInstance step = new StepInstance(new Long(1));
		stepConfiguration.setTasklet(tasklet);
		SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("FOO");
		JobExecution jobExecutionContext = new JobExecution(new JobInstance(
				jobIdentifier, new Long(3)));
		StepExecution stepExecution = new StepExecution(step,
				jobExecutionContext);

		try {
			stepExecutor.process(stepConfiguration, stepExecution);
		} catch (Exception ex) {
			assertEquals(stepExecution.getRollbackCount(), new Integer(1));
		}

	}

	public void testExitCodeDefaultClassification() {

		Tasklet tasklet = new Tasklet() {

			public ExitStatus execute() throws Exception {
				int counter = 0;
				counter++;

				if (counter == 1) {
					throw new RuntimeException();
				}

				return ExitStatus.CONTINUABLE;
			}

		};

		StepInstance step = new StepInstance(new Long(1));
		stepConfiguration.setTasklet(tasklet);
		SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("FOO");
		JobExecution jobExecutionContext = new JobExecution(new JobInstance(
				jobIdentifier, new Long(3)));
		StepExecution stepExecution = new StepExecution(step,
				jobExecutionContext);

		try {
			stepExecutor.process(stepConfiguration, stepExecution);
		} catch (Exception ex) {
			assertEquals(new ExitStatus(false,
					ExitCodeExceptionClassifier.FATAL_EXCEPTION,
					"java.lang.RuntimeException"), stepExecution
					.getExitStatus());
		}
	}

	/*
	 * make sure a job that has never been executed before, but does have
	 * saveRestartData = true, doesn't have restoreFrom called on it.
	 */
	public void testNonRestartedJob() {
		StepInstance step = new StepInstance(new Long(1));
		MockRestartableTasklet tasklet = new MockRestartableTasklet();
		stepConfiguration.setTasklet(tasklet);
		stepConfiguration.setSaveRestartData(true);
		SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("FOO");
		JobExecution jobExecutionContext = new JobExecution(new JobInstance(
				jobIdentifier, new Long(3)));
		StepExecution stepExecution = new StepExecution(step,
				jobExecutionContext);

		try {
			stepExecutor.process(stepConfiguration, stepExecution);
		} catch (Throwable t) {
			fail();
		}

		assertFalse(tasklet.isRestoreFromCalled());
		assertTrue(tasklet.isGetRestartDataCalled());
	}

	/*
	 * make sure a job that has been executed before, and is therefore being
	 * restarted, is restored.
	 */
	public void testRestartedJob() {
		StepInstance step = new StepInstance(new Long(1));
		step.setStepExecutionCount(1);
		MockRestartableTasklet tasklet = new MockRestartableTasklet();
		stepConfiguration.setTasklet(tasklet);
		stepConfiguration.setSaveRestartData(true);
		SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("FOO");
		JobExecution jobExecutionContext = new JobExecution(new JobInstance(
				jobIdentifier, new Long(3)));
		StepExecution stepExecution = new StepExecution(step,
				jobExecutionContext);

		try {
			stepExecutor.process(stepConfiguration, stepExecution);
		} catch (Throwable t) {
			fail();
		}

		assertTrue(tasklet.isRestoreFromCalled());
		assertTrue(tasklet.isGetRestartDataCalled());
	}

	/*
	 * Test that a job that is being restarted, but has saveRestartData set to
	 * false, doesn't have restore or getRestartData called on it.
	 */
	public void testNoSaveRestartDataRestartableJob() {
		StepInstance step = new StepInstance(new Long(1));
		step.setStepExecutionCount(1);
		MockRestartableTasklet tasklet = new MockRestartableTasklet();
		stepConfiguration.setTasklet(tasklet);
		stepConfiguration.setSaveRestartData(false);
		SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("FOO");
		JobExecution jobExecutionContext = new JobExecution(new JobInstance(
				jobIdentifier, new Long(3)));
		StepExecution stepExecution = new StepExecution(step,
				jobExecutionContext);

		try {
			stepExecutor.process(stepConfiguration, stepExecution);
		} catch (Throwable t) {
			fail();
		}

		assertFalse(tasklet.isRestoreFromCalled());
		assertFalse(tasklet.isGetRestartDataCalled());
	}

	/*
	 * Even though the job is restarted, and saveRestartData is true, nothing
	 * will be restored because the Tasklet does not implement Restartable.
	 */
	public void testRestartJobOnNonRestartableTasklet() {
		StepInstance step = new StepInstance(new Long(1));
		step.setStepExecutionCount(1);
		stepConfiguration.setTasklet(new Tasklet() {
			public ExitStatus execute() throws Exception {
				return ExitStatus.FINISHED;
			}
		});
		stepConfiguration.setSaveRestartData(true);
		SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("FOO");
		JobExecution jobExecution = new JobExecution(new JobInstance(
				jobIdentifier, new Long(3)));
		StepExecution stepExecution = new StepExecution(step, jobExecution);

		try {
			stepExecutor.process(stepConfiguration, stepExecution);
		} catch (Throwable t) {
			fail();
		}
	}

	private class MockRestartableTasklet implements Tasklet, Restartable {

		private boolean getRestartDataCalled = false;
		private boolean restoreFromCalled = false;

		public ExitStatus execute() throws Exception {
			return ExitStatus.FINISHED;
		}

		public RestartData getRestartData() {
			getRestartDataCalled = true;
			return null;
		}

		public void restoreFrom(RestartData data) {
			restoreFromCalled = true;
		}

		public boolean isGetRestartDataCalled() {
			return getRestartDataCalled;
		}

		public boolean isRestoreFromCalled() {
			return restoreFromCalled;
		}
	}

	/*
	 * StepExecutor will never pass StepInterruptedException to the
	 * exceptionClassifier. This may or may not stay the same, so the test will
	 * remain commented out for reference purposes.
	 */
	/*
	 * public void testExitCodeInterruptedClassification(){
	 * 
	 * StepInterruptionPolicy interruptionPolicy = new StepInterruptionPolicy(){
	 * 
	 * public void checkInterrupted(RepeatContext context) throws
	 * StepInterruptedException { throw new StepInterruptedException(""); } };
	 * 
	 * stepExecutor.setInterruptionPolicy(interruptionPolicy);
	 * 
	 * Tasklet tasklet = new Tasklet(){
	 * 
	 * public ExitStatus execute() throws Exception { int counter = 0;
	 * counter++;
	 * 
	 * if(counter == 1){ throw new StepInterruptedException(""); }
	 * 
	 * return ExitStatus.CONTINUABLE; } };
	 * 
	 * StepInstance step = new StepInstance(new Long(1));
	 * stepConfiguration.setTasklet(tasklet); JobExecutionContext
	 * jobExecutionContext = new JobExecutionContext(new
	 * SimpleJobIdentifier("FOO"), new JobInstance(new Long(3))); StepExecution
	 * stepExecution = new StepExecution(step, jobExecutionContext);
	 * 
	 * try{ stepExecutor.process(stepConfiguration, stepExecution); }
	 * catch(Exception ex){
	 * assertEquals(ExitCodeExceptionClassifier.STEP_INTERRUPTED,
	 * step.getStepExecution().getExitCode() );
	 * assertEquals(step.getStepExecution().getExitDescription(),
	 * "java.lang.RuntimeException"); } }
	 */
}
