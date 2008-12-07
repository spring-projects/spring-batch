/**
 * 
 */
package org.springframework.batch.core.step.item;

import static org.junit.Assert.*;
import static org.springframework.batch.core.BatchStatus.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.AttributeAccessor;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * Tests for the behavior of TaskletStep in a failure scenario.
 * 
 * @author Lucas Ward
 * 
 */
public class TaskletStepExceptionTests {

	TaskletStep taskletStep;

	StepExecution stepExecution;

	UpdateCountingJobRepository jobRepository;

	static RuntimeException taskletException = new RuntimeException();

	static JobInterruptedException interruptedException = new JobInterruptedException("");

	@Before
	public void init() {
		taskletStep = new TaskletStep();
		taskletStep.setTasklet(new ExceptionTasklet());
		jobRepository = new UpdateCountingJobRepository();
		taskletStep.setJobRepository(jobRepository);
		taskletStep.setTransactionManager(new ResourcelessTransactionManager());

		JobInstance jobInstance = new JobInstance(1L, new JobParameters(), "testJob");
		JobExecution jobExecution = new JobExecution(jobInstance);
		stepExecution = new StepExecution("testStep", jobExecution);
	}

	@Test
	public void testApplicationException() throws Exception {

		taskletStep.execute(stepExecution);
		assertEquals(FAILED, stepExecution.getStatus());
	}

	@Test
	public void testInterrupted() throws Exception {
		taskletStep.setStepExecutionListeners(new StepExecutionListener[] { new InterruptionListener() });
		taskletStep.execute(stepExecution);
		assertEquals(STOPPED, stepExecution.getStatus());
	}

	@Test
	public void testOpenFailure() throws Exception {
		final RuntimeException exception = new RuntimeException();
		taskletStep.setStreams(new ItemStream[] { new ItemStreamSupport() {
			@Override
			public void open(ExecutionContext executionContext) throws ItemStreamException {
				throw exception;
			}
		} });

		taskletStep.execute(stepExecution);
		assertEquals(FAILED, stepExecution.getStatus());
		assertTrue(stepExecution.getFailureExceptions().contains(exception));
		assertEquals(2, jobRepository.getUpdateCount());
	}

	@Test
	public void testBeforeStepFailure() throws Exception {

		final RuntimeException exception = new RuntimeException();
		taskletStep.setStepExecutionListeners(new StepExecutionListenerSupport[] { new StepExecutionListenerSupport() {
			@Override
			public void beforeStep(StepExecution stepExecution) {
				throw exception;
			}
		} });
		taskletStep.execute(stepExecution);
		assertEquals(FAILED, stepExecution.getStatus());
		assertTrue(stepExecution.getFailureExceptions().contains(exception));
		assertEquals(2, jobRepository.getUpdateCount());
	}

	@Test
	public void testAfterStepFailure() throws Exception {

		final RuntimeException exception = new RuntimeException();
		taskletStep.setStepExecutionListeners(new StepExecutionListenerSupport[] { new StepExecutionListenerSupport() {
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				throw exception;
			}
		} });
		taskletStep.setTasklet(new Tasklet() {

			public RepeatStatus execute(StepContribution contribution, AttributeAccessor attributes) throws Exception {
				return RepeatStatus.FINISHED;
			}

		});
		taskletStep.execute(stepExecution);
		assertEquals(COMPLETED, stepExecution.getStatus());
		assertFalse(stepExecution.getFailureExceptions().contains(exception));
		assertEquals(4, jobRepository.getUpdateCount());
	}

	@Test
	/**
	 * Exception in afterStep is ignored (only logged).
	 */
	public void testAfterStepFAilure() throws Exception {

		final RuntimeException exception = new RuntimeException();
		taskletStep.setStepExecutionListeners(new StepExecutionListenerSupport[] { new StepExecutionListenerSupport() {
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				throw exception;
			}
		} });
		taskletStep.execute(stepExecution);
		assertEquals(FAILED, stepExecution.getStatus());
		assertTrue(stepExecution.getFailureExceptions().contains(taskletException));
		assertFalse(stepExecution.getFailureExceptions().contains(exception));
		assertEquals(2, jobRepository.getUpdateCount());
	}

	@Test
	public void testCloseError() throws Exception {

		final RuntimeException exception = new RuntimeException();
		taskletStep.setStreams(new ItemStream[] { new ItemStreamSupport() {
			@Override
			public void close(ExecutionContext executionContext) throws ItemStreamException {
				throw exception;
			}
		} });

		taskletStep.execute(stepExecution);
		assertEquals(FAILED, stepExecution.getStatus());
		assertTrue(stepExecution.getFailureExceptions().contains(taskletException));
		assertTrue(stepExecution.getFailureExceptions().contains(exception));
		assertEquals(2, jobRepository.getUpdateCount());
	}

	@Test
	public void testCommitError() throws Exception {

		final RuntimeException exception = new RuntimeException();
		taskletStep.setTransactionManager(new ResourcelessTransactionManager() {
			@Override
			protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
				throw exception;
			}
		});

		taskletStep.setTasklet(new Tasklet() {

			public RepeatStatus execute(StepContribution contribution, AttributeAccessor attributes) throws Exception {
				return RepeatStatus.FINISHED;
			}

		});

		taskletStep.execute(stepExecution);
		assertEquals(UNKNOWN, stepExecution.getStatus());
		Throwable e = stepExecution.getFailureExceptions().get(0);
		assertEquals(exception, e.getCause());
	}

	@Test
	public void testUpdateError() throws Exception {

		final RuntimeException exception = new RuntimeException();
		taskletStep.setJobRepository(new UpdateCountingJobRepository() {
			boolean firstCall = true;
			@Override
			public void update(StepExecution arg0) {
				if (firstCall) {
					firstCall = false;
					return;
				}
				throw exception;
			}
		});
		
		taskletStep.execute(stepExecution);
		assertEquals(UNKNOWN, stepExecution.getStatus());
		assertTrue(stepExecution.getFailureExceptions().contains(taskletException));
		assertTrue(stepExecution.getFailureExceptions().contains(exception));
	}

	private static class ExceptionTasklet implements Tasklet {

		public RepeatStatus execute(StepContribution contribution, AttributeAccessor attributes) throws Exception {
			throw taskletException;
		}
	}

	private static class InterruptionListener extends StepExecutionListenerSupport {

		@Override
		public void beforeStep(StepExecution stepExecution) {
			stepExecution.setTerminateOnly();
		}
	}

	private static class UpdateCountingJobRepository implements JobRepository {

		private int updateCount = 0;

		public void add(StepExecution stepExecution) {
		}

		public JobExecution createJobExecution(String jobName, JobParameters jobParameters)
				throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
			return null;
		}

		public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
			return null;
		}

		public int getStepExecutionCount(JobInstance jobInstance, String stepName) {
			return 0;
		}

		public boolean isJobInstanceExists(String jobName, JobParameters jobParameters) {
			return false;
		}

		public void update(JobExecution jobExecution) {
		}

		public void update(StepExecution stepExecution) {
			updateCount++;
		}

		public void updateExecutionContext(StepExecution stepExecution) {
		}

		public int getUpdateCount() {
			return updateCount;
		}

		public JobExecution getLastJobExecution(String jobName, JobParameters jobParameters) {
			return null;
		}
	}

}
