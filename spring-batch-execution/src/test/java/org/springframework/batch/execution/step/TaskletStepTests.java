package org.springframework.batch.execution.step;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobSupport;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.step.TaskletStep;
import org.springframework.batch.execution.step.support.JobRepositorySupport;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.interceptor.RepeatListenerAdapter;

public class TaskletStepTests extends TestCase {

	private StepExecution stepExecution;

	private List list = new ArrayList();

	protected void setUp() throws Exception {
		stepExecution = new StepExecution("stepName", new JobExecution(new JobInstance(
				new Long(0L), new JobParameters(), new JobSupport("testJob")), new Long(12)));
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

	public void testSuccessfulExecution() throws Exception {
		TaskletStep step = new TaskletStep(new StubTasklet(false, false), new JobRepositorySupport());
		step.execute(stepExecution);
		assertNotNull(stepExecution.getStartTime());
		assertEquals(ExitStatus.FINISHED, stepExecution.getExitStatus());
		assertNotNull(stepExecution.getEndTime());
	}

	public void testFailureExecution() throws Exception {
		TaskletStep step = new TaskletStep(new StubTasklet(true, false), new JobRepositorySupport());
		step.execute(stepExecution);
		assertNotNull(stepExecution.getStartTime());
		assertEquals(ExitStatus.FAILED, stepExecution.getExitStatus());
		assertNotNull(stepExecution.getEndTime());
	}

	public void testSuccessfulExecutionWithListener() throws Exception {
		TaskletStep step = new TaskletStep(new StubTasklet(false, false), new JobRepositorySupport());
		step.setListener(new RepeatListenerAdapter() {
			public void open(RepeatContext context) {
				list.add("open");
			}
			public void close(RepeatContext context) {
				list.add("close");
			}
		});
		step.execute(stepExecution);
		System.err.println(list);
		assertEquals(2, list.size());
	}

	public void testExceptionExecution() throws JobInterruptedException, BatchCriticalException {
		TaskletStep step = new TaskletStep(new StubTasklet(false, true), new JobRepositorySupport());
		try {
			step.execute(stepExecution);
			fail();
		}
		catch (BatchCriticalException e) {
			assertNotNull(stepExecution.getStartTime());
			assertEquals(ExitStatus.FAILED, stepExecution.getExitStatus());
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
