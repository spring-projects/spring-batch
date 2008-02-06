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

package org.springframework.batch.execution.job.simple;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.domain.StepInterruptedException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.ExitCodeExceptionClassifier;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.JobDao;
import org.springframework.batch.execution.repository.dao.MapJobDao;
import org.springframework.batch.execution.repository.dao.MapStepDao;
import org.springframework.batch.execution.repository.dao.StepDao;
import org.springframework.batch.execution.step.simple.SimpleStep;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Tests for DefaultJobLifecycle. MapJobDao and MapStepDao are used instead of a
 * mock repository to test that status is being stored correctly.
 * 
 * @author Lucas Ward
 */
public class SimpleJobTests extends TestCase {

	private JobRepository jobRepository;

	private JobDao jobDao;

	private StepDao stepDao;

	private List list = new ArrayList();

	private JobInstance jobInstance;

	private JobExecution jobExecution;

	private StepInstance step1;

	private StepInstance step2;

	private StepExecution stepExecution1;

	private StepExecution stepExecution2;

	private StubStep stepConfiguration1;

	private StubStep stepConfiguration2;

	private JobParameters jobParameters = new JobParameters();

	private SimpleJob job;

	protected void setUp() throws Exception {
		super.setUp();

		MapJobDao.clear();
		MapStepDao.clear();
		jobDao = new MapJobDao();
		stepDao = new MapStepDao();
		jobRepository = new SimpleJobRepository(jobDao, stepDao);
		job = new SimpleJob();
		job.setJobRepository(jobRepository);

		stepConfiguration1 = new StubStep("TestStep1");
		stepConfiguration1.setCallback(new Runnable() {
			public void run() {
				list.add("default");
			}
		});
		stepConfiguration2 = new StubStep("TestStep2");
		stepConfiguration2.setCallback(new Runnable() {
			public void run() {
				list.add("default");
			}
		});
		stepConfiguration1.setJobRepository(jobRepository);
		stepConfiguration2.setJobRepository(jobRepository);

		List stepConfigurations = new ArrayList();
		stepConfigurations.add(stepConfiguration1);
		stepConfigurations.add(stepConfiguration2);
		job.setName("testJob");
		job.setSteps(stepConfigurations);

		jobExecution = jobRepository.createJobExecution(job, jobParameters);
		jobInstance = jobExecution.getJobInstance();

		List steps = jobInstance.getStepInstances();
		step1 = (StepInstance) steps.get(0);
		step2 = (StepInstance) steps.get(1);
		stepExecution1 = new StepExecution(step1, jobExecution, null);
		stepExecution2 = new StepExecution(step2, jobExecution, null);

	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRunNormally() throws Exception {
		stepConfiguration1.setStartLimit(5);
		stepConfiguration2.setStartLimit(5);
		job.execute(jobExecution);
		assertEquals(2, list.size());
		checkRepository(BatchStatus.COMPLETED);
	}

	public void testRunWithSimpleStepExecutor() throws Exception {

		job.setJobRepository(jobRepository);
		// do not set StepExecutorFactory...
		stepConfiguration1.setStartLimit(5);
		stepConfiguration1.setTasklet(new Tasklet() {
			public ExitStatus execute() throws Exception {
				list.add("1");
				return ExitStatus.FINISHED;
			}
		});
		stepConfiguration2.setStartLimit(5);
		stepConfiguration2.setTasklet(new Tasklet() {
			public ExitStatus execute() throws Exception {
				list.add("2");
				return ExitStatus.FINISHED;
			}
		});
		job.execute(jobExecution);
		assertEquals(2, list.size());
		checkRepository(BatchStatus.COMPLETED, ExitStatus.FINISHED);

	}

	public void testExecutionContextIsSet() throws Exception {
		testRunNormally();
		assertEquals(jobInstance, jobExecution.getJobInstance());
		assertEquals(2, jobExecution.getStepExecutions().size());
		assertEquals(step1, stepExecution1.getStep());
		assertEquals(step2, stepExecution2.getStep());
	}

	public void testInterrupted() throws Exception {
		stepConfiguration1.setStartLimit(5);
		stepConfiguration2.setStartLimit(5);
		final StepInterruptedException exception = new StepInterruptedException("Interrupt!");
		stepConfiguration1.setProcessException(exception);
		try {
			job.execute(jobExecution);
		}
		catch (BatchCriticalException e) {
			assertEquals(exception, e.getCause());
		}
		assertEquals(0, list.size());
		checkRepository(BatchStatus.STOPPED, new ExitStatus(false, ExitCodeExceptionClassifier.STEP_INTERRUPTED));
	}

	public void testFailed() throws Exception {
		stepConfiguration1.setStartLimit(5);
		stepConfiguration2.setStartLimit(5);
		final RuntimeException exception = new RuntimeException("Foo!");
		stepConfiguration1.setProcessException(exception);
		try {
			job.execute(jobExecution);
		}
		catch (RuntimeException e) {
			assertEquals(exception, e);
		}
		assertEquals(0, list.size());
		checkRepository(BatchStatus.FAILED, new ExitStatus(false, ExitCodeExceptionClassifier.FATAL_EXCEPTION));
	}

	public void testStepShouldNotStart() throws Exception {
		// Start policy will return false, keeping the step from being started.
		stepConfiguration1.setStartLimit(0);

		try {
			job.execute(jobExecution);
			fail("Expected BatchCriticalException");
		}
		catch (BatchCriticalException ex) {
			// expected
			assertTrue("Wrong message in exception: " + ex.getMessage(), ex.getMessage()
					.indexOf("start limit exceeded") >= 0);
		}
	}

	public void testNoSteps() throws Exception {
		job.setSteps(new ArrayList());

		job.execute(jobExecution);
		ExitStatus exitStatus = jobExecution.getExitStatus();
		assertTrue("Wrong message in execution: " + exitStatus, exitStatus.getExitDescription().indexOf(
				"No steps configured") >= 0);
	}

	public void testNoStepsExecuted() throws Exception {
		StepExecution completedExecution = new StepExecution(null, null);
		completedExecution.setStatus(BatchStatus.COMPLETED);
		step1.setLastExecution(completedExecution);
		step2.setLastExecution(completedExecution);

		job.execute(jobExecution);
		ExitStatus exitStatus = jobExecution.getExitStatus();
		assertTrue("Wrong message in execution: " + exitStatus, exitStatus.getExitDescription().indexOf(
				"steps already completed") >= 0);
	}

	/*
	 * Check JobRepository to ensure status is being saved.
	 */
	private void checkRepository(BatchStatus status, ExitStatus exitStatus) {
		assertEquals(jobInstance, jobDao.findJobInstances(jobInstance.getJobName(), jobParameters).get(0));
		// because map dao stores in memory, it can be checked directly
		JobExecution jobExecution = (JobExecution) jobDao.findJobExecutions(jobInstance).get(0);
		assertEquals(jobInstance.getId(), jobExecution.getJobId());
		assertEquals(status, jobExecution.getStatus());
		if (exitStatus != null) {
			assertEquals(jobExecution.getExitStatus().getExitCode(), exitStatus.getExitCode());
		}
	}

	private void checkRepository(BatchStatus status) {
		checkRepository(status, null);
	}

	private class StubStep extends SimpleStep {

		private Runnable runnable;
		private Exception exception;

		/**
		 * @param string
		 */
		public StubStep(String string) {
			super(string);
		}

		/**
		 * @param exception
		 */
		public void setProcessException(Exception exception) {
			this.exception = exception;
		}

		/**
		 * @param runnable
		 */
		public void setCallback(Runnable runnable) {
			this.runnable = runnable;
		}

		public void execute(StepExecution stepExecution) throws StepInterruptedException, BatchCriticalException {
			if (exception instanceof RuntimeException) {
				throw (RuntimeException)exception;
			}
			if (exception instanceof StepInterruptedException) {
				throw (StepInterruptedException)exception;
			}
			if (runnable!=null) {
				runnable.run();
			}
			stepExecution.setExitStatus(ExitStatus.FINISHED);
		}

	}
}
