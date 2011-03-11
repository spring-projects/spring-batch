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
package org.springframework.batch.core.step.tasklet;

import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.listener.CompositeChunkListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContextRepeatCallback;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.FatalStepExecutionException;
import org.springframework.batch.core.step.StepInterruptionPolicy;
import org.springframework.batch.core.step.ThreadStepInterruptionPolicy;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemStream;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * Simple implementation of executing the step as a call to a {@link Tasklet},
 * possibly repeated, and each call surrounded by a transaction. The structure
 * is therefore that of a loop with transaction boundary inside the loop. The
 * loop is controlled by the step operations (
 * {@link #setStepOperations(RepeatOperations)}).<br/>
 * <br/>
 * 
 * Clients can use interceptors in the step operations to intercept or listen to
 * the iteration on a step-wide basis, for instance to get a callback when the
 * step is complete. Those that want callbacks at the level of an individual
 * tasks, can specify interceptors for the chunk operations.
 * 
 * @author Dave Syer
 * @author Lucas Ward
 * @author Ben Hale
 * @author Robert Kasanicky
 */
public class TaskletStep extends AbstractStep {

	private static final Log logger = LogFactory.getLog(TaskletStep.class);

	private RepeatOperations stepOperations = new RepeatTemplate();

	private CompositeChunkListener chunkListener = new CompositeChunkListener();

	// default to checking current thread for interruption.
	private StepInterruptionPolicy interruptionPolicy = new ThreadStepInterruptionPolicy();

	private CompositeItemStream stream = new CompositeItemStream();

	private PlatformTransactionManager transactionManager;

	private TransactionAttribute transactionAttribute = new DefaultTransactionAttribute() {

		@Override
		public boolean rollbackOn(Throwable ex) {
			return true;
		}

	};

	private Tasklet tasklet;

	/**
	 * Default constructor.
	 */
	public TaskletStep() {
		this(null);
	}

	/**
	 * @param name
	 */
	public TaskletStep(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.core.step.AbstractStep#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.state(transactionManager != null, "A transaction manager must be provided");
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
	 * Public setter for the {@link TransactionAttribute}.
	 * 
	 * @param transactionAttribute the {@link TransactionAttribute} to set
	 */
	public void setTransactionAttribute(TransactionAttribute transactionAttribute) {
		this.transactionAttribute = transactionAttribute;
	}

	/**
	 * Public setter for the {@link Tasklet}.
	 * 
	 * @param tasklet the {@link Tasklet} to set
	 */
	public void setTasklet(Tasklet tasklet) {
		this.tasklet = tasklet;
		if (tasklet instanceof StepExecutionListener) {
			registerStepExecutionListener((StepExecutionListener) tasklet);
		}
	}

	/**
	 * Register a chunk listener for callbacks at the appropriate stages in a
	 * step execution.
	 * 
	 * @param listener a {@link ChunkListener}
	 */
	public void registerChunkListener(ChunkListener listener) {
		this.chunkListener.register(listener);
	}

	/**
	 * Register each of the objects as listeners.
	 * 
	 * @param listeners an array of listener objects of known types.
	 */
	public void setChunkListeners(ChunkListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			registerChunkListener(listeners[i]);
		}
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
	 * Process the step and update its context so that progress can be monitored
	 * by the caller. The step is broken down into chunks, each one executing in
	 * a transaction. The step and its execution and execution context are all
	 * given an up to date {@link BatchStatus}, and the {@link JobRepository} is
	 * used to store the result. Various reporting information are also added to
	 * the current context governing the step execution, which would normally be
	 * available to the caller through the step's {@link ExecutionContext}.<br/>
	 * 
	 * @throws JobInterruptedException if the step or a chunk is interrupted
	 * @throws RuntimeException if there is an exception during a chunk
	 * execution
	 * 
	 */
	@Override
	protected void doExecute(StepExecution stepExecution) throws Exception {

		stream.update(stepExecution.getExecutionContext());
		getJobRepository().updateExecutionContext(stepExecution);

		// Shared semaphore per step execution, so other step executions can run
		// in parallel without needing the lock
		final Semaphore semaphore = createSemaphore();

		stepOperations.iterate(new StepContextRepeatCallback(stepExecution) {

			@Override
			public RepeatStatus doInChunkContext(RepeatContext repeatContext, ChunkContext chunkContext)
					throws Exception {

				StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();

				// Before starting a new transaction, check for
				// interruption.
				interruptionPolicy.checkInterrupted(stepExecution);

				RepeatStatus result;
				try {
					result = (RepeatStatus) new TransactionTemplate(transactionManager, transactionAttribute)
							.execute(new ChunkTransactionCallback(chunkContext, semaphore));
				}
				catch (UncheckedTransactionException e) {
					// Allow checked exceptions to be thrown inside callback
					throw (Exception) e.getCause();
				}

				chunkListener.afterChunk();

				// Check for interruption after transaction as well, so that
				// the interrupted exception is correctly propagated up to
				// caller
				interruptionPolicy.checkInterrupted(stepExecution);

				return result;
			}

		});

	}

	/**
	 * Extension point mainly for test purposes so that the behaviour of the
	 * lock can be manipulated to simulate various pathologies.
	 * 
	 * @return a semaphore for locking access to the JobRepository
	 */
	protected Semaphore createSemaphore() {
		return new Semaphore(1);
	}

	protected void close(ExecutionContext ctx) throws Exception {
		stream.close();
	}

	protected void open(ExecutionContext ctx) throws Exception {
		stream.open(ctx);
	}

	/**
	 * A callback for the transactional work inside a chunk. Also detects
	 * failures in the transaction commit and rollback, only panicking if the
	 * transaction status is unknown (i.e. if a commit failure leads to a clean
	 * rollback then we assume the state is consistent).
	 * 
	 * @author Dave Syer
	 * 
	 */
	private class ChunkTransactionCallback extends TransactionSynchronizationAdapter implements TransactionCallback {

		private final StepExecution stepExecution;

		private final ChunkContext chunkContext;

		private boolean rolledBack = false;

		private boolean stepExecutionUpdated = false;

		private StepExecution oldVersion;

		private boolean locked = false;

		private final Semaphore semaphore;

		public ChunkTransactionCallback(ChunkContext chunkContext, Semaphore semaphore) {
			this.chunkContext = chunkContext;
			this.stepExecution = chunkContext.getStepContext().getStepExecution();
			this.semaphore = semaphore;
		}

		@Override
		public void afterCompletion(int status) {
			try {
				if (status != TransactionSynchronization.STATUS_COMMITTED) {
					if (stepExecutionUpdated) {
						// Wah! the commit failed. We need to rescue the step
						// execution data.
						logger.info("Commit failed while step execution data was already updated. "
								+ "Reverting to old version.");
						copy(oldVersion, stepExecution);
						if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
							rollback(stepExecution);
						}
					}
				}
				if (status == TransactionSynchronization.STATUS_UNKNOWN) {
					logger.error("Rolling back with transaction in unknown state");
					rollback(stepExecution);
					stepExecution.upgradeStatus(BatchStatus.UNKNOWN);
					stepExecution.setTerminateOnly();
				}
			}
			finally {
				// Only release the lock if we acquired it, and release as late
				// as possible
				if (locked) {
					semaphore.release();
				}
				locked = false;
			}
		}

		public Object doInTransaction(TransactionStatus status) {

			TransactionSynchronizationManager.registerSynchronization(this);

			RepeatStatus result = RepeatStatus.CONTINUABLE;

			StepContribution contribution = stepExecution.createStepContribution();

			chunkListener.beforeChunk();

			// In case we need to push it back to its old value
			// after a commit fails...
			oldVersion = new StepExecution(stepExecution.getStepName(), stepExecution.getJobExecution());
			copy(stepExecution, oldVersion);

			try {

				try {
					try {
						result = tasklet.execute(contribution, chunkContext);
						if (result == null) {
							result = RepeatStatus.FINISHED;
						}
					}
					catch (Exception e) {
						if (transactionAttribute.rollbackOn(e)) {
							throw e;
						}
					}

				}
				finally {

					// If the step operations are asynchronous then we need
					// to synchronize changes to the step execution (at a
					// minimum). Take the lock *before* changing the step
					// execution.
					try {
						semaphore.acquire();
						locked = true;
					}
					catch (InterruptedException e) {
						logger.error("Thread interrupted while locking for repository update");
						stepExecution.setStatus(BatchStatus.STOPPED);
						stepExecution.setTerminateOnly();
						Thread.currentThread().interrupt();
					}

					// Apply the contribution to the step
					// even if unsuccessful
					logger.debug("Applying contribution: " + contribution);
					stepExecution.apply(contribution);

				}

				stepExecutionUpdated = true;

				stream.update(stepExecution.getExecutionContext());

				try {
					// Going to attempt a commit. If it fails this flag will
					// stay false and we can use that later.
					getJobRepository().updateExecutionContext(stepExecution);
					stepExecution.incrementCommitCount();
					logger.debug("Saving step execution before commit: " + stepExecution);
					getJobRepository().update(stepExecution);
				}
				catch (Exception e) {
					// If we get to here there was a problem saving the step
					// execution and we have to fail.
					String msg = "JobRepository failure forcing exit with unknown status";
					logger.error(msg, e);
					stepExecution.upgradeStatus(BatchStatus.UNKNOWN);
					stepExecution.setTerminateOnly();
					throw new FatalStepExecutionException(msg, e);
				}

			}
			catch (Error e) {
				logger.debug("Rollback for Error: " + e.getClass().getName() + ": " + e.getMessage());
				rollback(stepExecution);
				throw e;
			}
			catch (RuntimeException e) {
				logger.debug("Rollback for RuntimeException: " + e.getClass().getName() + ": " + e.getMessage());
				rollback(stepExecution);
				throw e;
			}
			catch (Exception e) {
				logger.debug("Rollback for Exception: " + e.getClass().getName() + ": " + e.getMessage());
				rollback(stepExecution);
				// Allow checked exceptions
				throw new UncheckedTransactionException(e);
			}

			return result;

		}

		private void rollback(StepExecution stepExecution) {
			if (!rolledBack) {
				stepExecution.incrementRollbackCount();
				rolledBack = true;
			}
		}

		private void copy(final StepExecution source, final StepExecution target) {
			target.setVersion(source.getVersion());
			target.setWriteCount(source.getWriteCount());
			target.setFilterCount(source.getFilterCount());
			target.setCommitCount(source.getCommitCount());
			target.setExecutionContext(new ExecutionContext(source.getExecutionContext()));
		}

	}

	/**
	 * Convenience wrapper for a checked exception so that it can cause a
	 * rollback and be extracted afterwards.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private static class UncheckedTransactionException extends RuntimeException {

		public UncheckedTransactionException(Exception e) {
			super(e);
		}

	}

}
