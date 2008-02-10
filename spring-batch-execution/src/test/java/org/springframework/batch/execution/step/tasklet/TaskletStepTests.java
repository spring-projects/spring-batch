package org.springframework.batch.execution.step.tasklet;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.domain.StepInterruptedException;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.step.simple.JobRepositorySupport;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;

public class TaskletStepTests extends TestCase {

	private StepExecution stepExecution;

	protected void setUp() throws Exception {
		stepExecution = new StepExecution(new StepInstance(new Long(11)), new JobExecution(new JobInstance(
				new Long(0L), new JobParameters()), new Long(12)));
	}

	public void testTaskletMandatory() throws Exception {
		TaskletStep step = new TaskletStep();
		step.setJobRepository(new JobRepositorySupport());
		try {
			step.afterPropertiesSet();
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message should contain 'tasklet': " + message, message.toLowerCase().contains("tasklet"));
		}
	}

	public void testRepositoryMandatory() throws Exception {
		TaskletStep step = new TaskletStep();
		try {
			step.afterPropertiesSet();
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message should contain 'tasklet': " + message, message.toLowerCase().contains("tasklet"));
		}
	}

	public void testSuccessfulExecution() throws StepInterruptedException, BatchCriticalException {
		TaskletStep step = new TaskletStep(new StubTasklet(false, false), new JobRepositorySupport());
		step.execute(stepExecution);
		assertNotNull(stepExecution.getStartTime());
		assertSame(ExitStatus.FINISHED, stepExecution.getExitStatus());
		assertNotNull(stepExecution.getEndTime());
	}

	public void testFailureExecution() throws StepInterruptedException, BatchCriticalException {
		TaskletStep step = new TaskletStep(new StubTasklet(true, false), new JobRepositorySupport());
		step.execute(stepExecution);
		assertNotNull(stepExecution.getStartTime());
		assertSame(ExitStatus.FAILED, stepExecution.getExitStatus());
		assertNotNull(stepExecution.getEndTime());
	}

	public void testExceptionExecution() throws StepInterruptedException, BatchCriticalException {
		TaskletStep step = new TaskletStep(new StubTasklet(false, true), new JobRepositorySupport());
		try {
			step.execute(stepExecution);
			fail();
		}
		catch (BatchCriticalException e) {
			assertNotNull(stepExecution.getStartTime());
			assertSame(ExitStatus.FAILED, stepExecution.getExitStatus());
			assertNotNull(stepExecution.getEndTime());
		}
	}

	private class StubTasklet implements Tasklet {

		private final boolean exitFailure;

		private final boolean throwException;

		public StubTasklet(boolean exitFailure, boolean throwException) {
			this.exitFailure = exitFailure;
			this.throwException = throwException;
		}

		public ExitStatus execute() throws Exception {
			if (throwException) {
				throw new Exception();
			}

			if (exitFailure) {
				return ExitStatus.FAILED;
			}

			return ExitStatus.FINISHED;
		}

	}

}
