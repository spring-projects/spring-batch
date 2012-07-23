/**
 * 
 */
package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.batch.core.BatchStatus.COMPLETED;
import static org.springframework.batch.core.BatchStatus.FAILED;
import static org.springframework.batch.core.BatchStatus.STOPPED;
import static org.springframework.batch.core.BatchStatus.UNKNOWN;

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
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tests for the behavior of TaskletStep in a failure scenario.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class TaskletStepExceptionTests {

	TaskletStep taskletStep;

	StepExecution stepExecution;

	UpdateCountingJobRepository jobRepository;

	static RuntimeException taskletException = new RuntimeException("Static planned test exception.");

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
		assertEquals(FAILED.toString(), stepExecution.getExitStatus().getExitCode());
	}

	@Test
	public void testInterrupted() throws Exception {
		taskletStep.setStepExecutionListeners(new StepExecutionListener[] { new InterruptionListener() });
		taskletStep.execute(stepExecution);
		assertEquals(STOPPED, stepExecution.getStatus());
		assertEquals(STOPPED.toString(), stepExecution.getExitStatus().getExitCode());
	}

	@Test
	public void testInterruptedWithCustomStatus() throws Exception {
		taskletStep.setTasklet(new Tasklet() {
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				contribution.setExitStatus(new ExitStatus("FUNNY"));
				throw new JobInterruptedException("Planned");
			}
		});
		taskletStep.execute(stepExecution);
		assertEquals(STOPPED, stepExecution.getStatus());
		assertEquals("FUNNY", stepExecution.getExitStatus().getExitCode());
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
	public void testAfterStepFailureWhenTaskletSucceeds() throws Exception {

		final RuntimeException exception = new RuntimeException();
		taskletStep.setStepExecutionListeners(new StepExecutionListenerSupport[] { new StepExecutionListenerSupport() {
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				throw exception;
			}
		} });
		taskletStep.setTasklet(new Tasklet() {

			public RepeatStatus execute(StepContribution contribution, ChunkContext attributes) throws Exception {
				return RepeatStatus.FINISHED;
			}

		});
		taskletStep.execute(stepExecution);
		assertEquals(COMPLETED, stepExecution.getStatus());
		assertFalse(stepExecution.getFailureExceptions().contains(exception));
		assertEquals(3, jobRepository.getUpdateCount());
	}

	@Test
	/*
	 * Exception in afterStep is ignored (only logged).
	 */
	public void testAfterStepFailureWhenTaskletFails() throws Exception {

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
			public void close() throws ItemStreamException {
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

		taskletStep.setTransactionManager(new ResourcelessTransactionManager() {
			@Override
			protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
				throw new RuntimeException("bar");
			}

			@Override
			protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
				throw new RuntimeException("foo");
			}
		});

		taskletStep.setTasklet(new Tasklet() {

			public RepeatStatus execute(StepContribution contribution, ChunkContext attributes) throws Exception {
				attributes.getStepContext().getStepExecution().getExecutionContext().putString("foo", "bar");
				return RepeatStatus.FINISHED;
			}

		});

		taskletStep.execute(stepExecution);
		assertEquals(UNKNOWN, stepExecution.getStatus());
		Throwable e = stepExecution.getFailureExceptions().get(0);
		assertEquals("foo", e.getMessage());
		assertEquals(0, stepExecution.getCommitCount());
		assertEquals(1, stepExecution.getRollbackCount()); // Failed transaction
															// counts as
															// rollback
		assertEquals(0, stepExecution.getExecutionContext().size());
	}

	@Test
	public void testUnexpectedRollback() throws Exception {

		taskletStep.setTransactionManager(new ResourcelessTransactionManager() {
			@Override
			protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
				super.doRollback(status);
				throw new UnexpectedRollbackException("bar");
			}
		});

		taskletStep.setTasklet(new Tasklet() {

			public RepeatStatus execute(StepContribution contribution, ChunkContext attributes) throws Exception {
				attributes.getStepContext().getStepExecution().getExecutionContext().putString("foo", "bar");
				return RepeatStatus.FINISHED;
			}

		});

		taskletStep.execute(stepExecution);
		assertEquals(FAILED, stepExecution.getStatus());
		Throwable e = stepExecution.getFailureExceptions().get(0);
		assertEquals("bar", e.getMessage());
		assertEquals(0, stepExecution.getCommitCount());
		assertEquals(1, stepExecution.getRollbackCount()); // Failed transaction
															// counts as
															// rollback
		assertEquals(0, stepExecution.getExecutionContext().size());
	}

	@Test
	public void testRepositoryErrorOnExecutionContext() throws Exception {

		taskletStep.setTasklet(new Tasklet() {

			public RepeatStatus execute(StepContribution contribution, ChunkContext attributes) throws Exception {
				return RepeatStatus.FINISHED;
			}

		});

		jobRepository.setFailOnUpdateExecutionContext(true);
		taskletStep.execute(stepExecution);
		assertEquals(UNKNOWN, stepExecution.getStatus());
		Throwable e = stepExecution.getFailureExceptions().get(0);
		assertEquals("Expected exception in step execution context persistence", e.getMessage());

	}

	@Test
	public void testRepositoryErrorOnExecutionContextInTransaction() throws Exception {

		taskletStep.setTasklet(new Tasklet() {

			public RepeatStatus execute(StepContribution contribution, ChunkContext attributes) throws Exception {
				return RepeatStatus.FINISHED;
			}

		});

		jobRepository.setFailOnUpdateExecutionContext(true);
		jobRepository.setFailInTransaction(true);
		taskletStep.execute(stepExecution);
		assertEquals(UNKNOWN, stepExecution.getStatus());
		Throwable e = stepExecution.getFailureExceptions().get(0);
		assertEquals("JobRepository failure forcing exit with unknown status", e.getMessage());

	}

	@Test
	public void testRepositoryErrorOnUpdateStepExecution() throws Exception {

		taskletStep.setTasklet(new Tasklet() {

			public RepeatStatus execute(StepContribution contribution, ChunkContext attributes) throws Exception {
				return RepeatStatus.FINISHED;
			}

		});

		jobRepository.setFailOnUpdateStepExecution(1);
		taskletStep.execute(stepExecution);
		assertEquals(UNKNOWN, stepExecution.getStatus());
		Throwable e = stepExecution.getFailureExceptions().get(0);
		assertEquals("JobRepository failure forcing exit with unknown status", e.getMessage());

	}

	@Test
	public void testRepositoryErrorOnFailure() throws Exception {

		taskletStep.setTasklet(new Tasklet() {

			public RepeatStatus execute(StepContribution contribution, ChunkContext attributes) throws Exception {
				throw new RuntimeException("Tasklet exception");
			}

		});

		jobRepository.setFailOnUpdateExecutionContext(true);
		taskletStep.execute(stepExecution);
		assertEquals(UNKNOWN, stepExecution.getStatus());
		Throwable e = stepExecution.getFailureExceptions().get(0);
		assertEquals("Expected exception in step execution context persistence", e.getMessage());

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
		assertTrue(stepExecution.getFailureExceptions().contains(exception));
		assertTrue(stepExecution.getFailureExceptions().contains(taskletException));
	}

	private static class ExceptionTasklet implements Tasklet {

		public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
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

		private boolean failOnUpdateContext = false;

		private int failOnUpdateExecution = -1;

		private boolean failInTransaction = false;

		public void setFailOnUpdateExecutionContext(boolean failOnUpdate) {
			this.failOnUpdateContext = failOnUpdate;
		}

		public void setFailOnUpdateStepExecution(int failOnUpdate) {
			this.failOnUpdateExecution = failOnUpdate;
		}

		public void setFailInTransaction(boolean failInTransaction) {
			this.failInTransaction = failInTransaction;
		}

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
			if (updateCount == failOnUpdateExecution) {
				throw new RuntimeException("Expected exception in step execution persistence");
			}
			updateCount++;
		}

		public void updateExecutionContext(StepExecution stepExecution) {
			if (failOnUpdateContext) {
				if (!failInTransaction
						|| (failInTransaction && TransactionSynchronizationManager.isActualTransactionActive())) {
					throw new RuntimeException("Expected exception in step execution context persistence");
				}
			}
		}

		public int getUpdateCount() {
			return updateCount;
		}

		public JobExecution getLastJobExecution(String jobName, JobParameters jobParameters) {
			return null;
		}

		public void updateExecutionContext(JobExecution jobExecution) {
		}
	}

}
