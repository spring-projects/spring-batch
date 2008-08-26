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
package org.springframework.batch.core.step.handler;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.StepExecutionSynchronizer;
import org.springframework.batch.core.step.StepExecutionSynchronizerFactory;
import org.springframework.batch.core.step.StepInterruptionPolicy;
import org.springframework.batch.core.step.ThreadStepInterruptionPolicy;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemStream;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Simple implementation of executing the step as a set of chunks, each chunk
 * surrounded by a transaction. The structure is therefore that of a loop with
 * transaction boundary inside the loop. The loop is controlled by the step
 * operations ( {@link #setStepOperations(RepeatOperations)}).<br/>
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
public class StepHandlerStep extends AbstractStep {

	private static final Log logger = LogFactory.getLog(StepHandlerStep.class);

	private RepeatOperations stepOperations = new RepeatTemplate();

	// default to checking current thread for interruption.
	private StepInterruptionPolicy interruptionPolicy = new ThreadStepInterruptionPolicy();

	private CompositeItemStream stream = new CompositeItemStream();

	private PlatformTransactionManager transactionManager;

	private TransactionAttribute transactionAttribute = new DefaultTransactionAttribute();

	private StepHandler stepHandler;

	private StepExecutionSynchronizer synchronizer;

	/**
	 * @param name
	 */
	public StepHandlerStep(String name) {
		super(name);
		synchronizer = new StepExecutionSynchronizerFactory().getStepExecutionSynchronizer();
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
	 * @param transactionAttribute the {@link TransactionAttribute} to set
	 */
	public void setTransactionAttribute(TransactionAttribute transactionAttribute) {
		this.transactionAttribute = transactionAttribute;
	}

	/**
	 * Public setter for the {@link StepHandler}.
	 * 
	 * @param stepHandler the {@link StepHandler} to set
	 */
	public void setStepHandler(StepHandler stepHandler) {
		this.stepHandler = stepHandler;
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
	public void setStepExecutionListeners(StepExecutionListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			registerStepExecutionListener(listeners[i]);
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
	 * given an up to date {@link BatchStatus}, and the {@link JobRepository} is
	 * used to store the result. Various reporting information are also added to
	 * the current context (the {@link RepeatContext} governing the step
	 * execution, which would normally be available to the caller somehow
	 * through the step's {@link ExecutionContext}.<br/>
	 * 
	 * @throws JobInterruptedException if the step or a chunk is interrupted
	 * @throws RuntimeException if there is an exception during a chunk
	 * execution
	 * 
	 */
	protected ExitStatus doExecute(final StepExecution stepExecution) throws Exception {
		stream.update(stepExecution.getExecutionContext());
		getJobRepository().updateExecutionContext(stepExecution);

		return stepOperations.iterate(new RepeatCallback() {

			final Queue<AttributeAccessor> attributeQueue = new LinkedBlockingQueue<AttributeAccessor>();

			public ExitStatus doInIteration(RepeatContext context) throws Exception {

				ExceptionHolder fatalException = new ExceptionHolder();

				StepContribution contribution = stepExecution.createStepContribution();

				// Before starting a new transaction, check for
				// interruption.
				interruptionPolicy.checkInterrupted(stepExecution);

				ExitStatus exitStatus = ExitStatus.CONTINUABLE;

				TransactionStatus transaction = transactionManager.getTransaction(transactionAttribute);

				boolean locked = false;

				AttributeAccessor attributes = attributeQueue.poll();
				if (attributes == null) {
					attributes = new BasicAttributeAccessor();
				}

				try {

					try {
						exitStatus = stepHandler.handle(contribution, attributes);
					}
					catch (Error e) {
						if (transactionAttribute.rollbackOn(e)) {
							throw e;
						}
					}
					catch (Exception e) {
						if (transactionAttribute.rollbackOn(e)) {
							throw e;
						}
					}
					finally {
						// Still some stuff to do with the data in this chunk,
						// pass it back
						if (attributes.attributeNames().length > 0) {
							attributeQueue.add(attributes);
						}

						// Apply the contribution to the step
						// even if unsuccessful
						stepExecution.apply(contribution);

					}

					stepExecution.incrementCommitCount();

					// If the step operations are asynchronous then we need
					// to synchronize changes to the step execution (at a
					// minimum).
					try {
						synchronizer.lock(stepExecution);
						locked = true;
					}
					catch (InterruptedException e) {
						stepExecution.setStatus(BatchStatus.STOPPED);
						Thread.currentThread().interrupt();
					}

					try {
						stream.update(stepExecution.getExecutionContext());
					}
					catch (Error e) {
						if (transactionAttribute.rollbackOn(e)) {
							throw e;
						}
					}
					catch (Exception e) {
						if (transactionAttribute.rollbackOn(e)) {
							throw e;
						}
					}

					try {
						getJobRepository().updateExecutionContext(stepExecution);
					}
					catch (Exception e) {
						fatalException.setException(e);
						stepExecution.setStatus(BatchStatus.UNKNOWN);
						throw new FatalException("Fatal error detected during save of step execution context", e);
					}

					try {
						transactionManager.commit(transaction);
					}
					catch (Exception e) {
						fatalException.setException(e);
						stepExecution.setStatus(BatchStatus.UNKNOWN);
						logger.error("Fatal error detected during commit.");
						throw new FatalException("Fatal error detected during commit", e);
					}

					try {
						getJobRepository().update(stepExecution);
					}
					catch (Exception e) {
						fatalException.setException(e);
						stepExecution.setStatus(BatchStatus.UNKNOWN);
						throw new FatalException("Fatal error detected during update of step execution", e);
					}

				}
				catch (Error e) {
					processRollback(stepExecution, fatalException, transaction);
					throw e;
				}
				catch (Exception e) {
					processRollback(stepExecution, fatalException, transaction);
					throw e;
				}
				finally {
					// only release the lock if we acquired it
					if (locked) {
						synchronizer.release(stepExecution);
					}
					locked = false;
				}

				// Check for interruption after transaction as well, so that
				// the interrupted exception is correctly propagated up to
				// caller
				interruptionPolicy.checkInterrupted(stepExecution);

				return exitStatus;
			}

		});

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
			transactionManager.rollback(transaction);
		}
		catch (Exception e) {
			/*
			 * If we already failed to commit, it doesn't help to do this again
			 * - it's better to allow the CommitFailedException to propagate
			 */
			if (!fatalException.hasException()) {
				fatalException.setException(e);
				throw new FatalException("Failed while processing rollback", e);
			}
		}

		if (fatalException.hasException()) {
			// Try and give the system a chance to update the stepExecution with
			// the failure status.
			getJobRepository().update(stepExecution);
		}
	}

	/**
	 * @author Dave Syer
	 *
	 */
	private static final class BasicAttributeAccessor extends AttributeAccessorSupport {
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

	protected void close(ExecutionContext ctx) throws Exception {
		stream.close(ctx);
	}

	protected void open(ExecutionContext ctx) throws Exception {
		stream.open(ctx);
	}
}
