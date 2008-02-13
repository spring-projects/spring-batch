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
package org.springframework.batch.execution.step.simple;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.Chunk;
import org.springframework.batch.core.domain.ChunkResult;
import org.springframework.batch.core.domain.Dechunker;
import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.domain.StepContribution;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.ExitStatusExceptionClassifier;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.scope.SimpleStepContext;
import org.springframework.batch.execution.scope.StepContext;
import org.springframework.batch.execution.scope.StepScope;
import org.springframework.batch.execution.scope.StepSynchronizationManager;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.exception.ResetFailedException;
import org.springframework.batch.item.stream.SimpleStreamManager;
import org.springframework.batch.item.stream.StreamManager;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

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
public class ChunkedStep extends AbstractStep {

	private static final Log logger = LogFactory.getLog(ChunkedStep.class);

	private RepeatOperations stepOperations = new RepeatTemplate();

	private JobRepository jobRepository;

	private ExitStatusExceptionClassifier exceptionClassifier = new SimpleExitStatusExceptionClassifier();

	// default to checking current thread for interruption.
	private StepInterruptionPolicy interruptionPolicy = new ThreadStepInterruptionPolicy();

	private AbstractStep step;

	private StreamManager streamManager;

	private ItemReader itemReader;

	private ItemWriter itemWriter;

	private RetryTemplate retryTemplate = new RetryTemplate();
	
	private int chunkSize;
	
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	/**
	 * Package private constructor so the step can create a the executor.
	 */
	ChunkedStep(AbstractStep abstractStep) {
		this.step = abstractStep;
	}

	/**
	 * Public setter for the {@link StreamManager}. This will be used to create the {@link StepContext}, and hence any
	 * component that is a {@link ItemStream} and in step scope will be registered with the service. The
	 * {@link StepContext} is then a source of aggregate statistics for the step.
	 * 
	 * @param streamManager the {@link StreamManager} to set. Default is a {@link SimpleStreamManager}.
	 */
	public void setStreamManager(StreamManager streamManager) {
		this.streamManager = streamManager;
	}

	/**
	 * Injected strategy for storage and retrieval of persistent step information. Mandatory property.
	 * 
	 * @param jobRepository
	 */
	public void setRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
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
	 * Setter for the {@link StepInterruptionPolicy}. The policy is used to check whether an external request has been
	 * made to interrupt the job execution.
	 * 
	 * @param interruptionPolicy a {@link StepInterruptionPolicy}
	 */
	public void setInterruptionPolicy(StepInterruptionPolicy interruptionPolicy) {
		this.interruptionPolicy = interruptionPolicy;
	}

	/**
	 * Setter for the {@link ExitStatusExceptionClassifier} that will be used to classify any exception that causes a job
	 * to fail.
	 * 
	 * @param exceptionClassifier
	 */
	public void setExceptionClassifier(ExitStatusExceptionClassifier exceptionClassifier) {
		this.exceptionClassifier = exceptionClassifier;
	}

	/**
	 * @param itemReader
	 */
	public void setItemReader(ItemReader itemReader) {
		this.itemReader = itemReader;
	}

	/**
	 * @param itemWriter
	 */
	public void setItemWriter(ItemWriter itemWriter) {
		this.itemWriter = itemWriter;
	}

	/**
	 * Check mandatory properties (reader and writer).
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(itemReader, "ItemReader must be provided");
		Assert.notNull(itemWriter, "ItemWriter must be provided");
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
	public void execute(final StepExecution stepExecution) throws BatchCriticalException, JobInterruptedException {

		final StepInstance stepInstance = stepExecution.getStep();
		Assert.notNull(stepInstance);
		boolean isRestart = stepInstance.getStepExecutionCount() > 0 ? true : false;

		ExitStatus status = ExitStatus.FAILED;

		final int chunkSize = this.chunkSize;
		StepContext parentStepContext = StepSynchronizationManager.getContext();
		final StepContext stepContext = new SimpleStepContext(stepExecution, parentStepContext, streamManager);
		StepSynchronizationManager.register(stepContext);
		// Add the job identifier so that it can be used to identify
		// the conversation in StepScope
		stepContext.setAttribute(StepScope.ID_KEY, stepExecution.getJobExecution().getId());

		final boolean saveExecutionAttributes = step.isSaveExecutionAttributes();

		if (saveExecutionAttributes && isRestart && stepInstance.getLastExecution() != null) {
			stepExecution.setExecutionAttributes(stepInstance.getLastExecution().getExecutionAttributes());
			stepContext.restoreFrom(stepExecution.getExecutionAttributes());
		}

		try {

			stepExecution.setStartTime(new Date(System.currentTimeMillis()));
			stepInstance.setLastExecution(stepExecution);
			updateStatus(stepExecution, BatchStatus.STARTED);

			status = stepOperations.iterate(new RepeatCallback() {

				public ExitStatus doInIteration(final RepeatContext context) throws Exception {


					// Before starting a new transaction, check for
					// interruption.
					interruptionPolicy.checkInterrupted(context);
					
					//shouldn't have to create a chunker each time, I'll refactor the interface later
					Chunker chunker = new ItemChunker(itemReader, stepExecution);
					final Chunk chunk = chunker.chunk(chunkSize);

					ExitStatus result = (ExitStatus)retryTemplate.execute(new RetryCallback(){

						public Object doWithRetry(RetryContext context)
								throws Throwable {
							return processChunk(chunk, stepExecution, stepContext);
						}});

					// Check for interruption after transaction as well, so that
					// the interrupted exception is correctly propagated up to
					// caller
					interruptionPolicy.checkInterrupted(context);

					return result;

				}
			});

			updateStatus(stepExecution, BatchStatus.COMPLETED);
		} catch (RuntimeException e) {

			// classify exception so an exit code can be stored.
			status = exceptionClassifier.classifyForExitCode(e);
			if (e.getCause() instanceof JobInterruptedException) {
				updateStatus(stepExecution, BatchStatus.STOPPED);
				throw (JobInterruptedException) e.getCause();
			} else if (e instanceof ResetFailedException) {
				updateStatus(stepExecution, BatchStatus.UNKNOWN);
				throw (ResetFailedException) e;
			} else {
				updateStatus(stepExecution, BatchStatus.FAILED);
				throw e;
			}

		} finally {
			stepExecution.setExitStatus(status);
			stepExecution.setEndTime(new Date(System.currentTimeMillis()));
			try {
				jobRepository.saveOrUpdate(stepExecution);
			} finally {
				// clear any registered synchronizations
				StepSynchronizationManager.close();
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
	ExitStatus processChunk(Chunk chunk, final StepExecution stepExecution, StepContext stepContext) {
		
		TransactionStatus transaction = streamManager.getTransaction(stepContext);

		final StepContribution contribution = stepExecution.createStepContribution();
		
		try {

			Dechunker dechunker = new ItemDechunker(itemWriter, stepExecution);
			
			ChunkResult chunkResult = dechunker.dechunk(chunk);

			// TODO: check that stepExecution can
			// aggregate these contributions if they
			// come in asynchronously.
			ExecutionAttributes statistics = stepContext.getExecutionAttributes();
			contribution.setExecutionAttributes(statistics);
			contribution.incrementCommitCount();

			// If the step operations are asynchronous then we need
			// to synchronize changes to the step execution (at a
			// minimum).
			synchronized (stepExecution) {

				// Apply the contribution to the step
				// only if chunk was successful
				stepExecution.apply(contribution);

				if (step.isSaveExecutionAttributes()) {
					stepExecution.setExecutionAttributes(stepContext.getExecutionAttributes());
				}
				jobRepository.saveOrUpdate(stepExecution);

			}

			streamManager.commit(transaction);

		} catch (Throwable t) {
			/*
			 * Any exception thrown within the transaction template will automatically cause the transaction
			 * to rollback. We need to include exceptions during an attempted commit (e.g. Hibernate flush)
			 * so this catch block comes outside the transaction.
			 */
			synchronized (stepExecution) {
				stepExecution.rollback();
			}
			try {
				streamManager.rollback(transaction);
			} catch (ResetFailedException e) {
				// The original Throwable cause is in danger of
				// being lost here, so we log the reset
				// failure and re-throw with cause of the rollback.
				logger.error("Encountered reset error on rollback: "
				        + "one of the streams may be in an inconsistent state, "
				        + "so this step should not proceed", e);
				throw new ResetFailedException("Encountered reset error on rollback.  "
				        + "Consult logs for the cause of the reet failure.  "
				        + "The cause of the original rollback is incuded here.", t);
			}
			if (t instanceof RuntimeException) {
				throw (RuntimeException) t;
			} else {
				throw new RuntimeException(t);
			}
		}
		
		return null;

	}

	/**
	 * Convenience method to update the status in all relevant places.
	 * 
	 * @param step the current step
	 * @param stepExecution the current stepExecution
	 * @param status the status to set
	 */
	private void updateStatus(StepExecution stepExecution, BatchStatus status) {
		StepInstance step = stepExecution.getStep();
		stepExecution.setStatus(status);
		jobRepository.update(step);
		jobRepository.saveOrUpdate(stepExecution);
	}
}
