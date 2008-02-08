package org.springframework.batch.execution.step.tasklet;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.domain.StepInterruptedException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;

public class TaskletStepTest extends TestCase {

	private StepExecution stepExecution;

	protected void setUp() throws Exception {
		stepExecution = new StepExecution(new StepInstance(new Long(11)), new JobExecution(new JobInstance(
		        new Long(0L), new JobParameters()), new Long(12)));
	}

	public void testSuccessfulExecution() throws StepInterruptedException, BatchCriticalException {
		TaskletStep step = new TaskletStep(new StubTasklet(false, false), new StubJobRepository());
		step.execute(stepExecution);
		assertNotNull(stepExecution.getStartTime());
		assertSame(ExitStatus.FINISHED, stepExecution.getExitStatus());
		assertNotNull(stepExecution.getEndTime());
	}

	public void testFailureExecution() throws StepInterruptedException, BatchCriticalException {
		TaskletStep step = new TaskletStep(new StubTasklet(true, false), new StubJobRepository());
		step.execute(stepExecution);
		assertNotNull(stepExecution.getStartTime());
		assertSame(ExitStatus.FAILED, stepExecution.getExitStatus());
		assertNotNull(stepExecution.getEndTime());
	}

	public void testExceptionExecution() throws StepInterruptedException, BatchCriticalException {
		TaskletStep step = new TaskletStep(new StubTasklet(false, true), new StubJobRepository());
		try {
			step.execute(stepExecution);
			fail();
		} catch (BatchCriticalException e) {
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

	private class StubJobRepository implements JobRepository {

		public JobExecution createJobExecution(Job job, JobParameters jobParameters)
		        throws JobExecutionAlreadyRunningException {
			// TODO Auto-generated method stub
			return null;
		}

		public void saveOrUpdate(JobExecution jobExecution) {
			// TODO Auto-generated method stub
		}

		public void saveOrUpdate(StepExecution stepExecution) {
			// TODO Auto-generated method stub
		}

		public void update(JobInstance jobInstance) {
			// TODO Auto-generated method stub
		}

		public void update(StepInstance stepInstance) {
			// TODO Auto-generated method stub
		}

	}
}
