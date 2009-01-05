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

package org.springframework.batch.core.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StartLimitExceededException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Tests for DefaultJobLifecycle. MapJobDao and MapStepExecutionDao are used
 * instead of a mock repository to test that status is being stored correctly.
 * 
 * @author Lucas Ward
 */
public class SimpleJobTests extends TestCase {

	private JobRepository jobRepository;

	private JobInstanceDao jobInstanceDao;

	private JobExecutionDao jobExecutionDao;

	private StepExecutionDao stepExecutionDao;

	private List list = new ArrayList();

	private JobInstance jobInstance;

	private JobExecution jobExecution;

	private StepExecution stepExecution1;

	private StepExecution stepExecution2;

	private StubStep step1;

	private StubStep step2;

	private JobParameters jobParameters = new JobParameters();

	private SimpleJob job;

	private static final ExitStatus customExitStatus = ExitStatus.UNKNOWN.addExitDescription("tweaked");

	protected void setUp() throws Exception {
		super.setUp();

		MapJobInstanceDao.clear();
		MapJobExecutionDao.clear();
		MapStepExecutionDao.clear();
		jobInstanceDao = new MapJobInstanceDao();
		jobExecutionDao = new MapJobExecutionDao();
		stepExecutionDao = new MapStepExecutionDao();
		jobRepository = new SimpleJobRepository(jobInstanceDao, jobExecutionDao, stepExecutionDao);
		job = new SimpleJob();
		job.setJobRepository(jobRepository);

		step1 = new StubStep("TestStep1");
		step1.setCallback(new Runnable() {
			public void run() {
				list.add("default");
			}
		});
		step2 = new StubStep("TestStep2");
		step2.setCallback(new Runnable() {
			public void run() {
				list.add("default");
			}
		});
		step1.setJobRepository(jobRepository);
		step2.setJobRepository(jobRepository);

		List steps = new ArrayList();
		steps.add(step1);
		steps.add(step2);
		job.setName("testJob");
		job.setSteps(steps);

		jobExecution = jobRepository.createJobExecution(job, jobParameters);
		jobInstance = jobExecution.getJobInstance();

		stepExecution1 = new StepExecution(step1.getName(), jobExecution, null);
		stepExecution2 = new StepExecution(step2.getName(), jobExecution, null);

	}

	// Test to ensure the exit status returned by the last step is returned
	public void testExitStatusReturned() throws JobExecutionException {

		final ExitStatus customStatus = new ExitStatus(true, "test");

		Step testStep = new Step() {

			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				stepExecution.setExitStatus(customStatus);
			}

			public String getName() {
				return "test";
			}

			public int getStartLimit() {
				return 1;
			}

			public boolean isAllowStartIfComplete() {
				return false;
			}
		};
		List steps = new ArrayList();
		steps.add(testStep);
		job.setSteps(steps);
		job.execute(jobExecution);
		assertEquals(customStatus, jobExecution.getExitStatus());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRunNormally() throws Exception {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		job.execute(jobExecution);
		assertEquals(2, list.size());
		checkRepository(BatchStatus.COMPLETED);
		assertNotNull(jobExecution.getEndTime());
		assertNotNull(jobExecution.getStartTime());

		assertTrue(step1.passedInJobContext.isEmpty());
		assertFalse(step2.passedInJobContext.isEmpty());
	}

	public void testRunNormallyWithListener() throws Exception {
		job.setJobExecutionListeners(new JobExecutionListenerSupport[] { new JobExecutionListenerSupport() {
			public void beforeJob(JobExecution jobExecution) {
				list.add("before");
			}

			public void afterJob(JobExecution jobExecution) {
				list.add("after");
				jobExecution.setExitStatus(customExitStatus);
			}
		} });
		job.execute(jobExecution);
		assertEquals(4, list.size());
		assertEquals(customExitStatus, jobExecution.getExitStatus());
		checkRepository(BatchStatus.COMPLETED, customExitStatus);
	}

	public void testRunWithSimpleStepExecutor() throws Exception {

		job.setJobRepository(jobRepository);
		// do not set StepExecutorFactory...
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		job.execute(jobExecution);
		assertEquals(2, list.size());
		checkRepository(BatchStatus.COMPLETED, ExitStatus.FINISHED);

	}

	public void testExecutionContextIsSet() throws Exception {
		testRunNormally();
		assertEquals(jobInstance, jobExecution.getJobInstance());
		assertEquals(2, jobExecution.getStepExecutions().size());
		assertEquals(step1.getName(), stepExecution1.getStepName());
		assertEquals(step2.getName(), stepExecution2.getStepName());
	}

	public void testInterrupted() throws Exception {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		final JobInterruptedException exception = new JobInterruptedException("Interrupt!");
		step1.setProcessException(exception);
		try {
			job.execute(jobExecution);
			fail();
		}
		catch (UnexpectedJobExecutionException e) {
			assertEquals(exception, e.getCause());
		}
		assertEquals(0, list.size());
		checkRepository(BatchStatus.STOPPED, ExitStatus.FAILED);
	}

	public void testFailed() throws Exception {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		final RuntimeException exception = new RuntimeException("Foo!");
		step1.setProcessException(exception);
		try {
			job.execute(jobExecution);
			fail();
		}
		catch (RuntimeException e) {
			assertEquals(exception, e);
		}
		assertEquals(0, list.size());
		checkRepository(BatchStatus.FAILED, ExitStatus.FAILED);
	}

	public void testFailedWithListener() throws Exception {
		job.setJobExecutionListeners(new JobExecutionListenerSupport[] { new JobExecutionListenerSupport() {
			public void onError(JobExecution jobExecution, Throwable t) {
				list.add(t);
				assertEquals(ExitStatus.FAILED, jobExecution.getExitStatus());
				jobExecution.setExitStatus(customExitStatus);
			}
		} });
		final RuntimeException exception = new RuntimeException("Foo!");
		step1.setProcessException(exception);

		try {
			job.execute(jobExecution);
			fail();
		}
		catch (RuntimeException e) {
			assertEquals(exception, e);
		}
		assertEquals(1, list.size());
		assertSame(exception, list.get(0));
		checkRepository(BatchStatus.FAILED, customExitStatus);
	}

	public void testFailedWithError() throws Exception {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		final Error exception = new Error("Foo!");
		step1.setProcessException(exception);
		try {
			job.execute(jobExecution);
			fail();
		}
		catch (Error e) {
			assertEquals(exception, e);
		}
		assertEquals(0, list.size());
		checkRepository(BatchStatus.FAILED, ExitStatus.FAILED);
	}

	public void testStepShouldNotStart() throws Exception {
		// Start policy will return false, keeping the step from being started.
		step1.setStartLimit(0);

		try {
			job.execute(jobExecution);
			fail("Expected BatchCriticalException");
		}
		catch (StartLimitExceededException ex) {
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

	// public void testNoStepsExecuted() throws Exception {
	// StepExecution completedExecution = new
	// StepExecution("completedExecution", jobExecution);
	// completedExecution.setStatus(BatchStatus.COMPLETED);
	//
	// job.execute(jobExecution);
	// ExitStatus exitStatus = jobExecution.getExitStatus();
	// assertEquals(ExitStatus.NOOP.getExitCode(), exitStatus.getExitCode());
	// assertTrue("Wrong message in execution: " + exitStatus,
	// exitStatus.getExitDescription().contains(
	// "steps already completed"));
	// }

	public void testNotExecutedIfAlreadyStopped() throws Exception {
		jobExecution.stop();
		try {
			job.execute(jobExecution);
			fail();
		}
		catch (UnexpectedJobExecutionException e) {
			assertTrue(e.getCause() instanceof JobInterruptedException);
		}
		assertEquals(0, list.size());
		checkRepository(BatchStatus.STOPPED, ExitStatus.NOOP);
		ExitStatus exitStatus = jobExecution.getExitStatus();
		assertEquals(ExitStatus.NOOP.getExitCode(), exitStatus.getExitCode());
	}

	public void testRestart() throws Exception {
		step1.setAllowStartIfComplete(true);
		final RuntimeException exception = new RuntimeException("Foo!");
		step2.setProcessException(exception);

		try {
			job.execute(jobExecution);
			fail();
		}
		catch (RuntimeException e) {
			assertSame(exception, e);
		}

		try {
			job.execute(jobExecution);
			fail();
		}
		catch (RuntimeException e) {
			assertSame(exception, e);
		}
		assertTrue(step1.passedInStepContext.isEmpty());
		assertFalse(step2.passedInStepContext.isEmpty());

	}

	public void testInterruptWithListener() throws Exception {
		step1.setProcessException(new JobInterruptedException("job interrupted!"));

		MockControl control = MockControl.createStrictControl(JobExecutionListener.class);
		JobExecutionListener listener = (JobExecutionListener) control.getMock();
		listener.beforeJob(jobExecution);
		control.setVoidCallable();
		listener.onInterrupt(jobExecution);
		control.setVoidCallable();
		control.replay();

		job.setJobExecutionListeners(new JobExecutionListener[] { listener });

		try {
			job.execute(jobExecution);
			fail();
		}
		catch (UnexpectedJobExecutionException e) {
			// expected
		}

		control.verify();
	}

	/**
	 * Execution context should be restored on restart.
	 */
	public void testRestartScenario() throws Exception {

		job.setRestartable(true);

		step1.setAllowStartIfComplete(true);
		final RuntimeException exception = new RuntimeException("Foo!");
		step2.setProcessException(exception);

		try {
			job.execute(jobExecution);
			fail();
		}
		catch (RuntimeException e) {
			assertSame(exception, e);
		}

		assertTrue(step1.passedInJobContext.isEmpty());
		assertFalse(step2.passedInJobContext.isEmpty());

		assertFalse(jobExecution.getExecutionContext().isEmpty());

		jobExecution = jobRepository.createJobExecution(job, jobParameters);

		try {
			job.execute(jobExecution);
			fail();
		}
		catch (RuntimeException e) {
			assertSame(exception, e);
		}
		assertFalse(step1.passedInJobContext.isEmpty());
		assertFalse(step2.passedInJobContext.isEmpty());
	}

	public void testInterruptJob() throws Exception {

		step1 = new StubStep("interruptStep") {

			public void execute(StepExecution stepExecution) throws JobInterruptedException,
					UnexpectedJobExecutionException {
				stepExecution.getJobExecution().stop();
			}

		};

		job.setSteps(Arrays.asList(new Step[] { step1, step2 }));

		try {
			job.execute(jobExecution);
			fail();
		}
		catch (UnexpectedJobExecutionException expected) {
			assertTrue(expected.getCause() instanceof JobInterruptedException);
			assertEquals("JobExecution interrupted.", expected.getCause().getMessage());
		}

		assertNull("Second step was not executed", step2.passedInStepContext);
	}

	/*
	 * Check JobRepository to ensure status is being saved.
	 */
	private void checkRepository(BatchStatus status, ExitStatus exitStatus) {
		assertEquals(jobInstance, jobInstanceDao.getJobInstance(job, jobParameters));
		// because map dao stores in memory, it can be checked directly
		JobExecution jobExecution = (JobExecution) jobExecutionDao.findJobExecutions(jobInstance).get(0);
		assertEquals(jobInstance.getId(), jobExecution.getJobId());
		assertEquals(status, jobExecution.getStatus());
		if (exitStatus != null) {
			assertEquals(jobExecution.getExitStatus().getExitCode(), exitStatus.getExitCode());
		}
	}

	private void checkRepository(BatchStatus status) {
		checkRepository(status, null);
	}

	private static class StubStep extends StepSupport {

		private Runnable runnable;

		private Throwable exception;

		private JobRepository jobRepository;

		private ExecutionContext passedInStepContext;

		private ExecutionContext passedInJobContext;

		/**
		 * @param string
		 */
		public StubStep(String string) {
			super(string);
		}

		/**
		 * @param exception
		 */
		public void setProcessException(Throwable exception) {
			this.exception = exception;
		}

		/**
		 * @param runnable
		 */
		public void setCallback(Runnable runnable) {
			this.runnable = runnable;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @seeorg.springframework.batch.core.step.StepSupport#execute(org.
		 * springframework.batch.core.StepExecution)
		 */
		public void execute(StepExecution stepExecution) throws JobInterruptedException,
				UnexpectedJobExecutionException {

			passedInJobContext = new ExecutionContext(stepExecution.getJobExecution().getExecutionContext());
			passedInStepContext = new ExecutionContext(stepExecution.getExecutionContext());
			stepExecution.getExecutionContext().putString("stepKey", "stepValue");
			stepExecution.getJobExecution().getExecutionContext().putString("jobKey", "jobValue");
			jobRepository.saveOrUpdate(stepExecution);
			jobRepository.saveOrUpdateExecutionContext(stepExecution);

			if (exception instanceof RuntimeException) {
				stepExecution.setExitStatus(ExitStatus.FAILED);
				stepExecution.setStatus(BatchStatus.FAILED);
				throw (RuntimeException) exception;
			}
			if (exception instanceof Error) {
				stepExecution.setExitStatus(ExitStatus.FAILED);
				stepExecution.setStatus(BatchStatus.FAILED);
				throw (Error) exception;
			}
			if (exception instanceof JobInterruptedException) {
				stepExecution.setExitStatus(ExitStatus.FAILED);
				stepExecution.setStatus(BatchStatus.FAILED);
				throw (JobInterruptedException) exception;
			}
			if (runnable != null) {
				runnable.run();
			}
			stepExecution.setExitStatus(ExitStatus.FINISHED);
			stepExecution.setStatus(BatchStatus.COMPLETED);
			jobRepository.saveOrUpdate(stepExecution);
			jobRepository.saveOrUpdateExecutionContext(stepExecution);

		}

		/**
		 * Public setter for {@link JobRepository}.
		 * 
		 * @param jobRepository is a mandatory dependence (no default).
		 */
		public void setJobRepository(JobRepository jobRepository) {
			this.jobRepository = jobRepository;
		}

	}
}
