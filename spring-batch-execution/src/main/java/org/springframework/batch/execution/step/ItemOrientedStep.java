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
package org.springframework.batch.execution.step;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.InfrastructureException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.ExitStatusExceptionClassifier;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.listener.CompositeStepListener;
import org.springframework.batch.execution.step.support.DefaultStepExecutionSynchronizer;
import org.springframework.batch.execution.step.support.SimpleExitStatusExceptionClassifier;
import org.springframework.batch.execution.step.support.StepExecutionSynchronizer;
import org.springframework.batch.execution.step.support.StepInterruptionPolicy;
import org.springframework.batch.execution.step.support.ThreadStepInterruptionPolicy;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.stream.CompositeItemStream;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Simple implementation of executing the step as a set of chunks, each chunk surrounded by a transaction. The structure
 * is therefore that of two nested loops, with transaction boundary around the whole inner loop. The outer loop is
 * controlled by the step operations ({@link #setStepOperations(RepeatOperations)}), and the inner loop by the chunk
 * operations ({@link #setChunkOperations(RepeatOperations)}). The inner loop should always be executed in a single
 * thread, so the chunk operations should not do any concurrent execution. N.B. usually that means that the chunk
 * operations should be a {@link RepeatTemplate} (which is the default).<br/>
 * 
 * Clients can use interceptors in the step operations to intercept or listen to the iteration on a step-wide basis, for
 * instance to get a callback when the step is complete. Those that want callbacks at the level of an individual tasks,
 * can specify interceptors for the chunk operations.
 * 
 * @author Dave Syer
 * @author Lucas Ward
 * @author Ben Hale
 */
public class ItemOrientedStep extends AbstractStep {

	private static final Log logger = LogFactory.getLog(ItemOrientedStep.class);

	private RepeatOperations chunkOperations = new RepeatTemplate();

	private RepeatOperations stepOperations = new RepeatTemplate();

	private ExitStatusExceptionClassifier exceptionClassifier = new SimpleExitStatusExceptionClassifier();

	// default to checking current thread for interruption.
	private StepInterruptionPolicy interruptionPolicy = new ThreadStepInterruptionPolicy();

	private CompositeItemStream stream = new CompositeItemStream();

	private CompositeStepListener listener = new CompositeStepListener();

	private JobRepository jobRepository;

	private PlatformTransactionManager transactionManager;

	private ItemHandler itemHandler;

	private StepExecutionSynchronizer synchronizer = new DefaultStepExecutionSynchronizer();

	/**
	 * @param name
	 */
	public ItemOrientedStep(String name) {
		super(name);
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
	 * Register each of the streams for callbacks at the appropriate time in the step. The {@link ItemReader} and
	 * {@link ItemWriter} are automatically registered, but it doesn't hurt to also register them here. Injected
	 * dependencies of the reader and writer are not automatically registered, so if you implement {@link ItemWriter}
	 * using delegation to another object which itself is a {@link ItemStream}, you need to register the delegate here.
	 * 
	 * @param streams an array of {@link ItemStream} objects.
	 */
	public void setStreams(ItemStream[] streams) {
		for (int i = 0; i < streams.length; i++) {
			registerStream(streams[i]);
		}
	}

	/**
	 * Register a single {@link ItemStream} for callbacks to the stream interface.
	 * 
	 * @param stream
	 */
	public void registerStream(ItemStream stream) {
		this.stream.register(stream);
	}

	/**
	 * Register each of the objects as listeners. If the {@link ItemReader} or {@link ItemWriter} themselves implements
	 * this interface they will be registered automatically, but their injected dependencies will not be. This is a good
	 * way to get access to job parameters and execution context if the tasklet is parameterised.
	 * 
	 * @param listeners an array of listener objects of known types.
	 */
	public void setStepListeners(StepListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			registerStepListener(listeners[i]);
		}
	}

	/**
	 * Register a step listener for callbacks at the appropriate stages in a step execution.
	 * 
	 * @param listener a {@link StepListener}
	 */
	public void registerStepListener(StepListener listener) {
		this.listener.register(listener);
	}

	/**
	 * The {@link RepeatOperations} to use for the outer loop of the batch processing. Should be set up by the caller
	 * through a factory. Defaults to a plain {@link RepeatTemplate}.
	 * 
	 * @param stepOperations a {@link RepeatOperations} instance.
	 */
	public void setStepOperations(RepeatOperations stepOperations) {
		this.stepOperations = stepOperations;
	}

	/**
	 * The {@link RepeatOperations} to use for the inner loop of the batch processing. should be set up by the caller
	 * through a factory. defaults to a plain {@link RepeatTemplate}.
	 * 
	 * @param chunkOperations a {@link RepeatOperations} instance.
	 */
	public void setChunkOperations(RepeatOperations chunkOperations) {
		this.chunkOperations = chunkOperations;
	}

	/**
	 * Setter for the {@link StepInterruptionPolicy}. The policy is used to check whether an external request has been
	 * made to interrupt the job execution.
	 * 
	 * @param interruptionPolicy a {@link StepInterruptionPolicy}
	 */
	public void setInterruptionPolicy(StepInterruptionPolicy interruptionPolicy) {
		this.interruptionPolicy = interruptionPolicy;
	}

	/**
	 * Setter for the {@link ExitStatusExceptionClassifier} that will be used to classify any exception that causes a
	 * job to fail.
	 * 
	 * @param exceptionClassifier
	 */
	public void setExceptionClassifier(ExitStatusExceptionClassifier exceptionClassifier) {
		this.exceptionClassifier = exceptionClassifier;
	}

	/**
	 * Mostly useful for testing, but could be used to remove dependence on backport concurrency utilities. Public
	 * setter for the {@link StepExecutionSynchronizer}.
	 * 
	 * @param synchronizer the {@link StepExecutionSynchronizer} to set
	 */
	public void setSynchronizer(StepExecutionSynchronizer synchronizer) {
		this.synchronizer = synchronizer;
	}

	/**
	 * Process the step and update its context so that progress can be monitored by the caller. The step is broken down
	 * into chunks, each one executing in a transaction. The step and its execution and execution context are all given
	 * an up to date {@link BatchStatus}, and the {@link JobRepository} is used to store the result. Various reporting
	 * information are also added to the current context (the {@link RepeatContext} governing the step execution, which
	 * would normally be available to the caller somehow through the step's {@link JobExecutionContext}.<br/>
	 * 
	 * @throws JobInterruptedException if the step or a chunk is interrupted
	 * @throws RuntimeException if there is an exception during a chunk execution
	 * @see StepExecutor#execute(StepExecution)
	 */
	public void execute(final StepExecution stepExecution) throws InfrastructureException, JobInterruptedException {

		JobInstance jobInstance = stepExecution.getJobExecution().getJobInstance();
		StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobInstance, this);

		boolean isRestart = jobRepository.getStepExecutionCount(jobInstance, this) > 0 ? true : false;

		ExitStatus status = ExitStatus.FAILED;
		final ExceptionHolder fatalException = new ExceptionHolder();

		try {

			stepExecution.setStartTime(new Date(System.currentTimeMillis()));
			// We need to save the step execution right away, before we start
			// using its ID. It would be better to make the creation atomic in
			// the caller.
			fatalException.setException(updateStatus(stepExecution, BatchStatus.STARTED));

			if (isRestart && lastStepExecution != null) {
				stepExecution.setExecutionContext(lastStepExecution.getExecutionContext());
			} else {
				stepExecution.setExecutionContext(new ExecutionContext());
			}

			// Execute step level listeners *after* the execution context is
			// fixed in the step. E.g. ItemStream instances need the the same
			// reference to the ExecutionContext as the step execution.
			listener.beforeStep(stepExecution);
			stream.open(stepExecution.getExecutionContext());

			status = stepOperations.iterate(new RepeatCallback() {

				public ExitStatus doInIteration(final RepeatContext context) throws Exception {

					final StepContribution contribution = stepExecution.createStepContribution();
					contribution.setExecutionContext(stepExecution.getExecutionContext());
					// Before starting a new transaction, check for
					// interruption.
					interruptionPolicy.checkInterrupted(context);

					ExitStatus result = ExitStatus.CONTINUABLE;

					TransactionStatus transaction = transactionManager
					        .getTransaction(new DefaultTransactionDefinition());

					try {

						itemHandler.mark();
						result = processChunk(contribution);
						contribution.incrementCommitCount();

						// If the step operations are asynchronous then we need
						// to synchronize changes to the step execution (at a
						// minimum).
						try {
							synchronizer.lock(stepExecution);
						} catch (InterruptedException e) {
							stepExecution.setStatus(BatchStatus.STOPPED);
							Thread.currentThread().interrupt();
						}

						// Apply the contribution to the step
						// only if chunk was successful
						stepExecution.apply(contribution);

						stream.update(stepExecution.getExecutionContext());
						try {
							jobRepository.saveOrUpdateExecutionContext(stepExecution);
						} catch (Exception e) {
							fatalException.setException(e);
							stepExecution.setStatus(BatchStatus.UNKNOWN);
							throw new CommitFailedException(
							        "Fatal error detected during save of step execution context", e);
						}

						try {
							itemHandler.mark();
							itemHandler.flush();
							transactionManager.commit(transaction);
						} catch (Exception e) {
							fatalException.setException(e);
							stepExecution.setStatus(BatchStatus.UNKNOWN);
							throw new CommitFailedException("Fatal error detected during commit", e);
						}

					} catch (CommitFailedException e) {
						throw e;
					} catch (Throwable t) {
						/*
						 * Any exception thrown within the transaction template will automatically cause the transaction
						 * to rollback. We need to include exceptions during an attempted commit (e.g. Hibernate flush)
						 * so this catch block comes outside the transaction.
						 */
						stepExecution.rollback();

						try {
							itemHandler.reset();
							itemHandler.clear();
							transactionManager.rollback(transaction);
						} catch (Exception e) {
							fatalException.setException(e);
							stepExecution.setStatus(BatchStatus.UNKNOWN);
						}

						if (t instanceof RuntimeException) {
							throw (RuntimeException) t;
						} else {
							throw new RuntimeException(t);
						}

					} finally {
						synchronizer.release(stepExecution);
					}

					// Check for interruption after transaction as well, so that
					// the interrupted exception is correctly propagated up to
					// caller
					interruptionPolicy.checkInterrupted(context);

					return result;

				}
			});

			fatalException.setException(updateStatus(stepExecution, BatchStatus.COMPLETED));
		} catch (CommitFailedException e) {
			logger.error("Fatal error detected during commit.");
			throw e;
		} catch (RuntimeException e) {

			// classify exception so an exit code can be stored.
			status = exceptionClassifier.classifyForExitCode(e);

			if (e.getCause() instanceof JobInterruptedException) {
				updateStatus(stepExecution, BatchStatus.STOPPED);
				throw (JobInterruptedException) e.getCause();
			} else if (!fatalException.hasException()) {
				try {
					status = status.and(listener.onErrorInStep(stepExecution, e));
				} catch (RuntimeException ex) {
					logger.error("Unexpected error in listener on error in step.", ex);
				}
				updateStatus(stepExecution, BatchStatus.FAILED);
				throw e;
			} else {
				logger.error("Fatal error detected during rollback caused by underlying exception: ", e);
				throw e;
			}

		} finally {

			try {
				status = status.and(listener.afterStep(stepExecution));
			} catch (RuntimeException e) {
				logger.error("Unexpected error in listener after step.", e);
			}

			stepExecution.setExitStatus(status);
			stepExecution.setEndTime(new Date(System.currentTimeMillis()));

			try {
				jobRepository.saveOrUpdate(stepExecution);
			} catch (RuntimeException e) {
				String msg = "Fatal error detected during final save of meta data";
				logger.error(msg, e);
				if (!fatalException.hasException()) {
					fatalException.setException(e);
				}
				throw new InfrastructureException(msg, fatalException.getException());
			}

			try {
				stream.close(stepExecution.getExecutionContext());
			} catch (RuntimeException e) {
				String msg = "Fatal error detected during close of streams. "
				        + "The job execution completed (possibly unsuccessfully but with consistent meta-data).";
				logger.error(msg, e);
				if (!fatalException.hasException()) {
					fatalException.setException(e);
				}
				throw new InfrastructureException(msg, fatalException.getException());
			}

			if (fatalException.hasException()) {
				throw new InfrastructureException("Encountered an error saving batch meta data.", fatalException
				        .getException());
			}

		}

	}

	/**
	 * Execute a bunch of identical business logic operations all within a transaction. The transaction is
	 * programmatically started and stopped outside this method, so subclasses that override do not need to create a
	 * transaction.
	 * 
	 * @param step the current step containing the {@link Tasklet} with the business logic.
	 * @return true if there is more data to process.
	 */
	protected ExitStatus processChunk(final StepContribution contribution) {
		ExitStatus result = chunkOperations.iterate(new RepeatCallback() {
			public ExitStatus doInIteration(final RepeatContext context) throws Exception {
				if (contribution.isTerminateOnly()) {
					context.setTerminateOnly();
				}
				// check for interruption before each item as well
				interruptionPolicy.checkInterrupted(context);
				ExitStatus exitStatus = itemHandler.handle(contribution);
				contribution.incrementTaskCount();
				// check for interruption after each item as well
				interruptionPolicy.checkInterrupted(context);
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
		} catch (Exception e) {
			return e;
		}

	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private static class ExceptionHolder {

		private Exception exception;

		public boolean hasException() {
			return exception != null;
		}

		/**
		 * @param exception
		 */
		public void setException(Exception exception) {
			this.exception = exception;
		}

		/**
		 * @return
		 */
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
