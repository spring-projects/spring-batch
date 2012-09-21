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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.MapExecutionContextDao;
import org.springframework.batch.core.repository.dao.MapJobExecutionDao;
import org.springframework.batch.core.repository.dao.MapJobInstanceDao;
import org.springframework.batch.core.repository.dao.MapStepExecutionDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.ExecutionContext;

/**
 * Tests for DefaultJobLifecycle. MapJobDao and MapStepExecutionDao are used
 * instead of a mock repository to test that status is being stored correctly.
 * 
 * @author Lucas Ward
 */
public class SimpleJobTests {

	private JobRepository jobRepository;

	private JobInstanceDao jobInstanceDao;

	private JobExecutionDao jobExecutionDao;

	private StepExecutionDao stepExecutionDao;

	private ExecutionContextDao ecDao;

	private List<Serializable> list = new ArrayList<Serializable>();

	private JobInstance jobInstance;

	private JobExecution jobExecution;

	private StepExecution stepExecution1;

	private StepExecution stepExecution2;

	private StubStep step1;

	private StubStep step2;

	private JobParameters jobParameters = new JobParameters();

	private SimpleJob job;

	@Before
	public void setUp() throws Exception {

		jobInstanceDao = new MapJobInstanceDao();
		jobExecutionDao = new MapJobExecutionDao();
		stepExecutionDao = new MapStepExecutionDao();
		ecDao = new MapExecutionContextDao();
		jobRepository = new SimpleJobRepository(jobInstanceDao, jobExecutionDao, stepExecutionDao, ecDao);
		job = new SimpleJob();
		job.setJobRepository(jobRepository);

		step1 = new StubStep("TestStep1", jobRepository);
		step1.setCallback(new Runnable() {
			public void run() {
				list.add("default");
			}
		});
		step2 = new StubStep("TestStep2", jobRepository);
		step2.setCallback(new Runnable() {
			public void run() {
				list.add("default");
			}
		});

		List<Step> steps = new ArrayList<Step>();
		steps.add(step1);
		steps.add(step2);
		job.setName("testJob");
		job.setSteps(steps);

		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		jobInstance = jobExecution.getJobInstance();

		stepExecution1 = new StepExecution(step1.getName(), jobExecution);
		stepExecution2 = new StepExecution(step2.getName(), jobExecution);

	}

	/**
	 * Test method for {@link SimpleJob#setSteps(java.util.List)}.
	 */
	@Test
	public void testSetSteps() {
		job.setSteps(Collections.singletonList((Step) new StepSupport("step")));
		job.execute(jobExecution);
		assertEquals(1, jobExecution.getStepExecutions().size());
	}

	/**
	 * Test method for {@link SimpleJob#setSteps(java.util.List)}.
	 */
	@Test
	public void testGetSteps() {
		assertEquals(2, job.getStepNames().size());
	}

	/**
	 * Test method for
	 * {@link SimpleJob#addStep(org.springframework.batch.core.Step)}.
	 */
	@Test
	public void testAddStep() {
		job.setSteps(Collections.<Step> emptyList());
		job.addStep(new StepSupport("step"));
		job.execute(jobExecution);
		assertEquals(1, jobExecution.getStepExecutions().size());
	}

	// Test to ensure the exit status returned by the last step is returned
	@Test
	public void testExitStatusReturned() throws JobExecutionException {

		final ExitStatus customStatus = new ExitStatus("test");

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
		List<Step> steps = new ArrayList<Step>();
		steps.add(testStep);
		job.setSteps(steps);
		job.execute(jobExecution);
		assertEquals(customStatus, jobExecution.getExitStatus());
	}

	@Test
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

	@Test
	public void testRunNormallyWithListener() throws Exception {
		job.setJobExecutionListeners(new JobExecutionListenerSupport[] { new JobExecutionListenerSupport() {
			public void beforeJob(JobExecution jobExecution) {
				list.add("before");
			}

			public void afterJob(JobExecution jobExecution) {
				list.add("after");
			}
		} });
		job.execute(jobExecution);
		assertEquals(4, list.size());
	}

	@Test
	public void testRunWithSimpleStepExecutor() throws Exception {

		job.setJobRepository(jobRepository);
		// do not set StepExecutorFactory...
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		job.execute(jobExecution);
		assertEquals(2, list.size());
		checkRepository(BatchStatus.COMPLETED, ExitStatus.COMPLETED);

	}

	@Test
	public void testExecutionContextIsSet() throws Exception {
		testRunNormally();
		assertEquals(jobInstance, jobExecution.getJobInstance());
		assertEquals(2, jobExecution.getStepExecutions().size());
		assertEquals(step1.getName(), stepExecution1.getStepName());
		assertEquals(step2.getName(), stepExecution2.getStepName());
	}

	@Test
	public void testInterrupted() throws Exception {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		final JobInterruptedException exception = new JobInterruptedException("Interrupt!");
		step1.setProcessException(exception);
		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(exception, jobExecution.getStepExecutions().iterator().next().getFailureExceptions().get(0));
		assertEquals(0, list.size());
		checkRepository(BatchStatus.STOPPED, ExitStatus.STOPPED);
	}

	@Test
	public void testInterruptedAfterUnknownStatus() throws Exception {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		final JobInterruptedException exception = new JobInterruptedException("Interrupt!", BatchStatus.UNKNOWN);
		step1.setProcessException(exception);
		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(exception, jobExecution.getStepExecutions().iterator().next().getFailureExceptions().get(0));
		assertEquals(0, list.size());
		checkRepository(BatchStatus.UNKNOWN, ExitStatus.STOPPED);
	}

	@Test
	public void testFailed() throws Exception {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		final RuntimeException exception = new RuntimeException("Foo!");
		step1.setProcessException(exception);

		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(exception, jobExecution.getAllFailureExceptions().get(0));
		assertEquals(0, list.size());
		assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
		checkRepository(BatchStatus.FAILED, ExitStatus.FAILED);
	}

	@Test
	public void testFailedWithListener() throws Exception {
		job.setJobExecutionListeners(new JobExecutionListenerSupport[] { new JobExecutionListenerSupport() {
			public void afterJob(JobExecution jobExecution) {
				list.add("afterJob");
			}
		} });
		final RuntimeException exception = new RuntimeException("Foo!");
		step1.setProcessException(exception);

		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(exception, jobExecution.getAllFailureExceptions().get(0));
		assertEquals(1, list.size());
		checkRepository(BatchStatus.FAILED, ExitStatus.FAILED);
	}

	@Test
	public void testFailedWithError() throws Exception {
		step1.setStartLimit(5);
		step2.setStartLimit(5);
		final Error exception = new Error("Foo!");
		step1.setProcessException(exception);

		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		assertEquals(exception, jobExecution.getAllFailureExceptions().get(0));
		assertEquals(0, list.size());
		checkRepository(BatchStatus.FAILED, ExitStatus.FAILED);
	}

	@Test
	public void testStepShouldNotStart() throws Exception {
		// Start policy will return false, keeping the step from being started.
		step1.setStartLimit(0);

		job.execute(jobExecution);

		assertEquals(1, jobExecution.getFailureExceptions().size());
		Throwable ex = jobExecution.getFailureExceptions().get(0);
		assertTrue("Wrong message in exception: " + ex.getMessage(),
				ex.getMessage().indexOf("start limit exceeded") >= 0);
	}

	@Test
	public void testStepAlreadyComplete() throws Exception {
		stepExecution1.setStatus(BatchStatus.COMPLETED);
		jobRepository.add(stepExecution1);
		jobExecution.setEndTime(new Date());
		jobRepository.update(jobExecution);
		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		job.execute(jobExecution);
		assertEquals(0, jobExecution.getFailureExceptions().size());
		assertEquals(1, jobExecution.getStepExecutions().size());
		assertEquals(stepExecution2.getStepName(), jobExecution.getStepExecutions().iterator().next().getStepName());
	}

	@Test
	public void testStepAlreadyCompleteInSameExecution() throws Exception {
		List<Step> steps = new ArrayList<Step>();
		steps.add(step1);
		steps.add(step2);
		// Two steps with the same name should both be executed, since
		// the user might actually want it to happen twice.  On a restart
		// it would be executed twice again, even if it failed on the
		// second execution.  This seems reasonable.
		steps.add(step2);
		job.setSteps(steps);
		job.execute(jobExecution);
		assertEquals(0, jobExecution.getFailureExceptions().size());
		assertEquals(3, jobExecution.getStepExecutions().size());
		assertEquals(stepExecution1.getStepName(), jobExecution.getStepExecutions().iterator().next().getStepName());
	}

	@Test
	public void testNoSteps() throws Exception {
		job.setSteps(new ArrayList<Step>());

		job.execute(jobExecution);
		ExitStatus exitStatus = jobExecution.getExitStatus();
		assertTrue("Wrong message in execution: " + exitStatus, exitStatus.getExitDescription().indexOf(
				"no steps configured") >= 0);
	}

	@Test
	public void testNotExecutedIfAlreadyStopped() throws Exception {
		jobExecution.stop();
		job.execute(jobExecution);

		assertEquals(0, list.size());
		checkRepository(BatchStatus.STOPPED, ExitStatus.NOOP);
		ExitStatus exitStatus = jobExecution.getExitStatus();
		assertEquals(ExitStatus.NOOP.getExitCode(), exitStatus.getExitCode());
	}

	@Test
	public void testRestart() throws Exception {
		step1.setAllowStartIfComplete(true);
		final RuntimeException exception = new RuntimeException("Foo!");
		step2.setProcessException(exception);

		job.execute(jobExecution);
		Throwable e = jobExecution.getAllFailureExceptions().get(0);
		assertSame(exception, e);

		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		job.execute(jobExecution);
		e = jobExecution.getAllFailureExceptions().get(0);
		assertSame(exception, e);
		assertTrue(step1.passedInStepContext.isEmpty());
		assertFalse(step2.passedInStepContext.isEmpty());
	}

	@Test
	public void testRestartWithNullParameter() throws Exception {

		JobParameters jobParameters = new JobParametersBuilder().addString("foo", null).toJobParameters();
		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		jobInstance = jobExecution.getJobInstance();

		step1.setAllowStartIfComplete(true);
		final RuntimeException exception = new RuntimeException("Foo!");
		step2.setProcessException(exception);

		job.execute(jobExecution);
		Throwable e = jobExecution.getAllFailureExceptions().get(0);
		assertSame(exception, e);

		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);
		job.execute(jobExecution);
		e = jobExecution.getAllFailureExceptions().get(0);
		assertSame(exception, e);
		assertTrue(step1.passedInStepContext.isEmpty());
		assertFalse(step2.passedInStepContext.isEmpty());
	}

	@Test
	public void testInterruptWithListener() throws Exception {
		step1.setProcessException(new JobInterruptedException("job interrupted!"));

		JobExecutionListener listener = createMock(JobExecutionListener.class);
		listener.beforeJob(jobExecution);
		listener.afterJob(jobExecution);
		replay(listener);

		job.setJobExecutionListeners(new JobExecutionListener[] { listener });

		job.execute(jobExecution);
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());

		verify(listener);
	}

	/**
	 * Execution context should be restored on restart.
	 */
	@Test
	public void testRestartAndExecutionContextRestored() throws Exception {

		job.setRestartable(true);

		step1.setAllowStartIfComplete(true);
		final RuntimeException exception = new RuntimeException("Foo!");
		step2.setProcessException(exception);

		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		Throwable e = jobExecution.getAllFailureExceptions().get(0);
		assertSame(exception, e);

		assertTrue(step1.passedInJobContext.isEmpty());
		assertFalse(step2.passedInJobContext.isEmpty());

		assertFalse(jobExecution.getExecutionContext().isEmpty());

		jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);

		job.execute(jobExecution);
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		e = jobExecution.getAllFailureExceptions().get(0);
		assertSame(exception, e);
		assertFalse(step1.passedInJobContext.isEmpty());
		assertFalse(step2.passedInJobContext.isEmpty());
	}

	@Test
	public void testInterruptJob() throws Exception {

		step1 = new StubStep("interruptStep", jobRepository) {

			public void execute(StepExecution stepExecution) throws JobInterruptedException,
					UnexpectedJobExecutionException {
				stepExecution.getJobExecution().stop();
				super.execute(stepExecution);
			}

		};

		job.setSteps(Arrays.asList(new Step[] { step1, step2 }));
		job.execute(jobExecution);
		assertEquals(BatchStatus.STOPPED, jobExecution.getStatus());
		assertEquals(1, jobExecution.getAllFailureExceptions().size());
		Throwable expected = jobExecution.getAllFailureExceptions().get(0);
		assertTrue("Wrong exception " + expected, expected instanceof JobInterruptedException);
		assertEquals("JobExecution interrupted.", expected.getMessage());

		assertNull("Second step was not supposed to be executed", step2.passedInStepContext);
	}

	@Test
	public void testGetStepExists() {
		step1 = new StubStep("step1", jobRepository);
		step2 = new StubStep("step2", jobRepository);
		job.setSteps(Arrays.asList(new Step[] { step1, step2 }));
		Step step = job.getStep("step2");
		assertNotNull(step);
		assertEquals("step2", step.getName());
	}

	@Test
	public void testGetStepNotExists() {
		step1 = new StubStep("step1", jobRepository);
		step2 = new StubStep("step2", jobRepository);
		job.setSteps(Arrays.asList(new Step[] { step1, step2 }));
		Step step = job.getStep("foo");
		assertNull(step);
	}

	/*
	 * Check JobRepository to ensure status is being saved.
	 */
	private void checkRepository(BatchStatus status, ExitStatus exitStatus) {
		assertEquals(jobInstance, jobInstanceDao.getJobInstance(job.getName(), jobParameters));
		// because map dao stores in memory, it can be checked directly
		JobExecution jobExecution = jobExecutionDao.findJobExecutions(jobInstance).get(0);
		assertEquals(jobInstance.getId(), jobExecution.getJobId());
		assertEquals(status, jobExecution.getStatus());
		if (exitStatus != null) {
			assertEquals(exitStatus.getExitCode(), jobExecution.getExitStatus().getExitCode());
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
		public StubStep(String string, JobRepository jobRepository) {
			super(string);
			this.jobRepository = jobRepository;
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
			jobRepository.update(stepExecution);
			jobRepository.updateExecutionContext(stepExecution);

			if (exception instanceof JobInterruptedException) {
				stepExecution.setExitStatus(ExitStatus.FAILED);
				stepExecution.setStatus(((JobInterruptedException) exception).getStatus());
				stepExecution.addFailureException(exception);
				throw (JobInterruptedException) exception;
			}
			if (exception instanceof RuntimeException) {
				stepExecution.setExitStatus(ExitStatus.FAILED);
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.addFailureException(exception);
				return;
			}
			if (exception instanceof Error) {
				stepExecution.setExitStatus(ExitStatus.FAILED);
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.addFailureException(exception);
				return;
			}
			if (exception instanceof JobInterruptedException) {
				stepExecution.setExitStatus(ExitStatus.FAILED);
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.addFailureException(exception);
				return;
			}
			if (runnable != null) {
				runnable.run();
			}
			stepExecution.setExitStatus(ExitStatus.COMPLETED);
			stepExecution.setStatus(BatchStatus.COMPLETED);
			jobRepository.update(stepExecution);
			jobRepository.updateExecutionContext(stepExecution);

		}

	}
}
