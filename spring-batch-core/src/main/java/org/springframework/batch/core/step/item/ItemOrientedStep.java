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
package org.springframework.batch.core.step.item;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.launch.support.ExitCodeMapper;
import org.springframework.batch.core.listener.CompositeStepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.NoSuchJobException;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.StepExecutionSynchronizer;
import org.springframework.batch.core.step.StepExecutionSyncronizerFactory;
import org.springframework.batch.core.step.StepInterruptionPolicy;
import org.springframework.batch.core.step.ThreadStepInterruptionPolicy;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemStream;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Simple implementation of executing the step as a set of chunks, each chunk
 * surrounded by a transaction. The structure is therefore that of two nested
 * loops, with transaction boundary around the whole inner loop. The outer loop
 * is controlled by the step operations ({@link #setStepOperations(RepeatOperations)}),
 * and the inner loop by the chunk operations ({@link #setChunkOperations(RepeatOperations)}).
 * The inner loop should always be executed in a single thread, so the chunk
 * operations should not do any concurrent execution. N.B. usually that means
 * that the chunk operations should be a {@link RepeatTemplate} (which is the
 * default).<br/>
 * 
 * Clients can use interceptors in the step operations to intercept or listen to
 * the iteration on a step-wide basis, for instance to get a callback when the
 * step is complete. Those that want callbacks at the level of an individual
 * tasks, can specify interceptors for the chunk operations.
 * 
 * @author Dave Syer
 * @author Lucas Ward
 * @author Ben Hale
 */
public class ItemOrientedStep extends AbstractStep {

	private static final Log logger = LogFactory.getLog(ItemOrientedStep.class);

	/**
	 * Exit code for interrupted status.
	 */
	public static final String JOB_INTERRUPTED = "JOB_INTERRUPTED";

	private RepeatOperations chunkOperations = new RepeatTemplate();

	private RepeatOperations stepOperations = new RepeatTemplate();

	// default to checking current thread for interruption.
	private StepInterruptionPolicy interruptionPolicy = new ThreadStepInterruptionPolicy();

	private CompositeItemStream stream = new CompositeItemStream();

	private CompositeStepExecutionListener listener = new CompositeStepExecutionListener();

	private JobRepository jobRepository;

	private PlatformTransactionManager transactionManager;

	private ItemHandler itemHandler;

	private StepExecutionSynchronizer synchronizer;

	/**
	 * @param name
	 */
	public ItemOrientedStep(String name) {
		super(name);
		synchronizer = new StepExecutionSyncronizerFactory().getStepExecutionSynchronizer();
	}

	/**
	 * Public setter for {@link JobRepository}.
	 * 
	 * @param jobRepository is a mandatory dependence (no default).
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * 
	 * @param transactionManager the transaction manager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Public setter for the {@link ItemHandler}.
	 * 
	 * @param itemHandler the {@link ItemHandler} to set
	 */
	public void setItemHandler(ItemHandler itemHandler) {
		this.itemHandler = itemHandler;
	}

	/**
	 * Register each of the streams for callbacks at the appropriate time in the
	 * step. The {@link ItemReader} and {@link ItemWriter} are automatically
	 * registered, but it doesn't hurt to also register them here. Injected
	 * dependencies of the reader and writer are not automatically registered,
	 * so if you implement {@link ItemWriter} using delegation to another object
	 * which itself is a {@link ItemStream}, you need to register the delegate
	 * here.
	 * 
	 * @param streams an array of {@link ItemStream} objects.
	 */
	public void setStreams(ItemStream[] streams) {
		for (int i = 0; i < streams.length; i++) {
			registerStream(streams[i]);
		}
	}

	/**
	 * Register a single {@link ItemStream} for callbacks to the stream
	 * interface.
	 * 
	 * @param stream
	 */
	public void registerStream(ItemStream stream) {
		this.stream.register(stream);
	}

	/**
	 * Register each of the objects as listeners. If the {@link ItemReader} or
	 * {@link ItemWriter} themselves implements this interface they will be
	 * registered automatically, but their injected dependencies will not be.
	 * This is a good way to get access to job parameters and execution context
	 * if the tasklet is parameterised.
	 * 
	 * @param listeners an array of listener objects of known types.
	 */
	public void setStepListeners(StepExecutionListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			registerStepListener(listeners[i]);
		}
	}

	/**
	 * Register a step listener for callbacks at the appropriate stages in a
	 * step execution.
	 * 
	 * @param listener a {@link StepExecutionListener}
	 */
	public void registerStepListener(StepExecutionListener listener) {
		this.listener.register(listener);
	}

	/**
	 * The {@link RepeatOperations} to use for the outer loop of the batch
	 * processing. Should be set up by the caller through a factory. Defaults to
	 * a plain {@link RepeatTemplate}.
	 * 
	 * @param stepOperations a {@link RepeatOperations} instance.
	 */
	public void setStepOperations(RepeatOperations stepOperations) {
		this.stepOperations = stepOperations;
	}

	/**
	 * The {@link RepeatOperations} to use for the inner loop of the batch
	 * processing. should be set up by the caller through a factory. defaults to
	 * a plain {@link RepeatTemplate}.
	 * 
	 * @param chunkOperations a {@link RepeatOperations} instance.
	 */
	public void setChunkOperations(RepeatOperations chunkOperations) {
		this.chunkOperations = chunkOperations;
	}

	/**
	 * Setter for the {@link StepInterruptionPolicy}. The policy is used to
	 * check whether an external request has been made to interrupt the job
	 * execution.
	 * 
	 * @param interruptionPolicy a {@link StepInterruptionPolicy}
	 */
	public void setInterruptionPolicy(StepInterruptionPolicy interruptionPolicy) {
		this.interruptionPolicy = interruptionPolicy;
	}

	/**
	 * Mostly useful for testing, but could be used to remove dependence on
	 * backport concurrency utilities. Public setter for the
	 * {@link StepExecutionSynchronizer}.
	 * 
	 * @param synchronizer the {@link StepExecutionSynchronizer} to set
	 */
	public void setSynchronizer(StepExecutionSynchronizer synchronizer) {
		this.synchronizer = synchronizer;
	}

	/**
	 * Process the step and update its context so that progress can be monitored
	 * by the caller. The step is broken down into chunks, each one executing in
	 * a transaction. The step and its execution and execution context are all
	 * given an up to date {@link BatchStatus}, and the {@link JobRepository}
	 * is used to store the result. Various reporting information are also added
	 * to the current context (the {@link RepeatContext} governing the step
	 * execution, which would normally be available to the caller somehow
	 * through the step's {@link JobExecutionContext}.<br/>
	 * 
	 * @throws JobInterruptedException if the step or a chunk is interrupted
	 * @throws RuntimeException if there is an exception during a chunk
	 * execution
	 * @see StepExecutor#execute(StepExecution)
	 */
	public void execute(final StepExecution stepExecution) throws UnexpectedJobExecutionException,
			JobInterruptedException {

		ExitStatus status = ExitStatus.FAILED;
		final ExceptionHolder fatalException = new ExceptionHolder();

		try {

			stepExecution.setStartTime(new Date(System.currentTimeMillis()));
			// We need to save the step execution right away, before we start
			// using its ID. It would be better to make the creation atomic in
			// the caller.
			fatalException.setException(updateStatus(stepExecution, BatchStatus.STARTED));

			// Execute step level listeners *after* the execution context is
			// fixed in the step. E.g. ItemStream instances need the the same
			// reference to the ExecutionContext as the step execution.
			listener.beforeStep(stepExecution);
			stream.open(stepExecution.getExecutionContext());
			itemHandler.mark();

			status = stepOperations.iterate(new RepeatCallback() {

				public ExitStatus doInIteration(final RepeatContext context) throws Exception {

					final StepContribution contribution = stepExecution.createStepContribution();
					// Before starting a new transaction, check for
					// interruption.
					if (stepExecution.isTerminateOnly()) {
						context.setTerminateOnly();
					}
					interruptionPolicy.checkInterrupted(stepExecution);

					ExitStatus result = ExitStatus.CONTINUABLE;

					TransactionStatus transaction = transactionManager
							.getTransaction(new DefaultTransactionDefinition());

					try {

						result = processChunk(stepExecution, contribution);
						contribution.incrementCommitCount();

						// If the step operations are asynchronous then we need
						// to synchronize changes to the step execution (at a
						// minimum).
						try {
							synchronizer.lock(stepExecution);
						}
						catch (InterruptedException e) {
							stepExecution.setStatus(BatchStatus.STOPPED);
							Thread.currentThread().interrupt();
						}

						// Apply the contribution to the step
						// only if chunk was successful
						stepExecution.apply(contribution);

						// Attempt to flush before the step execution and stream
						// state are updated
						itemHandler.flush();

						stream.update(stepExecution.getExecutionContext());
						try {
							jobRepository.saveOrUpdateExecutionContext(stepExecution);
						}
						catch (Exception e) {
							fatalException.setException(e);
							stepExecution.setStatus(BatchStatus.UNKNOWN);
							throw new CommitFailedException(
									"Fatal error detected during save of step execution context", e);
						}

						try {
							itemHandler.mark();
							transactionManager.commit(transaction);
						}
						catch (Exception e) {
							fatalException.setException(e);
							stepExecution.setStatus(BatchStatus.UNKNOWN);
							throw new CommitFailedException("Fatal error detected during commit", e);
						}

					}
					catch (Error e) {
						stepExecution.incrementSkipCountBy(contribution.getContributionSkipCount());
						processRollback(stepExecution, fatalException, transaction);
						throw e;
					}
					catch (Exception e) {
						stepExecution.incrementSkipCountBy(contribution.getContributionSkipCount());
						processRollback(stepExecution, fatalException, transaction);
						throw e;
					}
					finally {
						synchronizer.release(stepExecution);
					}

					// Check for interruption after transaction as well, so that
					// the interrupted exception is correctly propagated up to
					// caller
					interruptionPolicy.checkInterrupted(stepExecution);

					return result;

				}
			});

			fatalException.setException(updateStatus(stepExecution, BatchStatus.COMPLETED));
		}
		catch (CommitFailedException e) {
			logger.error("Fatal error detected during commit.");
			throw e;
		}
		catch (RuntimeException e) {
			status = processFailure(stepExecution, fatalException, e);
			if (e.getCause() instanceof JobInterruptedException) {
				updateStatus(stepExecution, BatchStatus.STOPPED);
				throw (JobInterruptedException) e.getCause();
			}
			throw e;
		}
		catch (Error e) {
			status = processFailure(stepExecution, fatalException, e);
			throw e;
		}
		finally {

			try {
				status = status.and(listener.afterStep(stepExecution));
			}
			catch (RuntimeException e) {
				logger.error("Unexpected error in listener after step.", e);
			}

			stepExecution.setExitStatus(status);
			stepExecution.setEndTime(new Date(System.currentTimeMillis()));

			try {
				jobRepository.saveOrUpdate(stepExecution);
			}
			catch (RuntimeException e) {
				String msg = "Fatal error detected during final save of meta data";
				logger.error(msg, e);
				if (!fatalException.hasException()) {
					fatalException.setException(e);
				}
				throw new UnexpectedJobExecutionException(msg, fatalException.getException());
			}

			try {
				stream.close(stepExecution.getExecutionContext());
			}
			catch (RuntimeException e) {
				String msg = "Fatal error detected during close of streams. "
						+ "The job execution completed (possibly unsuccessfully but with consistent meta-data).";
				logger.error(msg, e);
				if (!fatalException.hasException()) {
					fatalException.setException(e);
				}
				throw new UnexpectedJobExecutionException(msg, fatalException.getException());
			}

			if (fatalException.hasException()) {
				throw new UnexpectedJobExecutionException("Encountered an error saving batch meta data.",
						fatalException.getException());
			}

		}

	}

	/**
	 * @param stepExecution the current {@link StepExecution}
	 * @param fatalException the {@link ExceptionHolder} containing information about failures in meta-data
	 * @param e the cause of teh failure
	 * @return an {@link ExitStatus}
	 */
	private ExitStatus processFailure(final StepExecution stepExecution, final ExceptionHolder fatalException,
			Throwable e) {

		// Default classification marks this as a failure and adds the exception
		// type and message
		ExitStatus status = getDefaultExitStatusForFailure(e);

		if (!fatalException.hasException()) {
			try {
				// classify exception so an exit code can be stored.
				status = status.and(listener.onErrorInStep(stepExecution, e));
			}
			catch (RuntimeException ex) {
				logger.error("Unexpected error in listener on error in step.", ex);
			}
			updateStatus(stepExecution, BatchStatus.FAILED);
		}
		else {
			logger.error("Fatal error detected during rollback caused by underlying exception: ", e);
		}
		return status;
	}

	/**
	 * Default mapping from throwable to {@link ExitStatus}. Clients can modify
	 * the exit code using a {@link StepExecutionListener}.
	 * 
	 * @param throwable the cause of teh failure
	 * @return an {@link ExitStatus}
	 */
	private ExitStatus getDefaultExitStatusForFailure(Throwable throwable) {
		ExitStatus exitStatus;
		if (throwable instanceof JobInterruptedException) {
			exitStatus = new ExitStatus(false, JOB_INTERRUPTED, JobInterruptedException.class.getName());
		}
		else if (throwable instanceof NoSuchJobException) {
			exitStatus = new ExitStatus(false, ExitCodeMapper.NO_SUCH_JOB);
		}
		else {
			String message = "";
			if (throwable != null) {
				StringWriter writer = new StringWriter();
				throwable.printStackTrace(new PrintWriter(writer));
				message = writer.toString();
			}
			exitStatus = ExitStatus.FAILED.addExitDescription(message);
		}

		return exitStatus;
	}

	/**
	 * Execute a bunch of identical business logic operations all within a
	 * transaction. The transaction is programmatically started and stopped
	 * outside this method, so subclasses that override do not need to create a
	 * transaction.
	 * @param execution the current {@link StepExecution} which should be
	 * treated as read-only for the purposes of this method.
	 * @param contribution the current {@link StepContribution} which can accept
	 * changes to be aggregated later into the step execution.
	 * 
	 * @return true if there is more data to process.
	 */
	protected ExitStatus processChunk(final StepExecution execution, final StepContribution contribution) {
		ExitStatus result = chunkOperations.iterate(new RepeatCallback() {
			public ExitStatus doInIteration(final RepeatContext context) throws Exception {
				if (execution.isTerminateOnly()) {
					context.setTerminateOnly();
				}
				// check for interruption before each item as well
				interruptionPolicy.checkInterrupted(execution);
				ExitStatus exitStatus = itemHandler.handle(contribution);
				contribution.incrementItemCount();
				// check for interruption after each item as well
				interruptionPolicy.checkInterrupted(execution);
				return exitStatus;
			}
		});
		return result;
	}

	/**
	 * Convenience method to update the status in all relevant places.
	 * 
	 * @param stepInstance the current step
	 * @param stepExecution the current stepExecution
	 * @param status the status to set
	 */
	private Exception updateStatus(StepExecution stepExecution, BatchStatus status) {
		stepExecution.setStatus(status);
		try {
			jobRepository.saveOrUpdate(stepExecution);
			return null;
		}
		catch (Exception e) {
			return e;
		}

	}

	/**
	 * @param stepExecution
	 * @param fatalException
	 * @param transaction
	 */
	private void processRollback(final StepExecution stepExecution, final ExceptionHolder fatalException,
			TransactionStatus transaction) {
		/*
		 * Any exception thrown within the transaction should automatically
		 * cause the transaction to rollback.
		 */
		stepExecution.rollback();

		try {
			itemHandler.reset();
			itemHandler.clear();
			transactionManager.rollback(transaction);
		}
		catch (Exception e) {
			/*
			 * If we already failed to commit, it doesn't help to do this again -
			 * it's better to allow the CommitFailedException to propagate
			 */
			if (!fatalException.hasException()) {
				fatalException.setException(e);
				stepExecution.setStatus(BatchStatus.UNKNOWN);
			}
		}
	}

	private static class ExceptionHolder {

		private Exception exception;

		public boolean hasException() {
			return exception != null;
		}

		public void setException(Exception exception) {
			this.exception = exception;
		}

		public Exception getException() {
			return this.exception;
		}

	}

	private class CommitFailedException extends RuntimeException {

		public CommitFailedException(String string, Exception e) {
			super(string, e);
		}

	}
}
