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
import org.springframework.batch.core.domain.Chunker;
import org.springframework.batch.core.domain.ChunkingResult;
import org.springframework.batch.core.domain.Dechunker;
import org.springframework.batch.core.domain.DechunkingResult;
import org.springframework.batch.core.domain.SkippedItemHandler;
import org.springframework.batch.core.domain.ItemSkipPolicy;
import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.domain.StepContribution;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.domain.StepSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.ExitStatusExceptionClassifier;
import org.springframework.batch.execution.scope.SimpleStepContext;
import org.springframework.batch.execution.scope.StepContext;
import org.springframework.batch.execution.scope.StepScope;
import org.springframework.batch.execution.scope.StepSynchronizationManager;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.exception.ResetFailedException;
import org.springframework.batch.item.stream.SimpleStreamManager;
import org.springframework.batch.item.stream.StreamManager;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatListener;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

/**
 * <p>Implementation of the {@link Step} interface that deals with input and output as 'chunks'.  Reading is 
 * delegated to a {@link Chunker} that will read in a {@link Chunk} of items for processing.  The number of
 * items per chunks is configurable as the chunk size.  Once the chunk has been read, any errors encountered
 * while reading (usually skipped unless configured not to) will be logged out via the {@link SkippedItemHandler}.
 * The chunk will then be 'dechunked', which in most scenarios will mean delegating to an {@link ItemWriter} 
 * by writing out one chunk at a time.  The transaction boundary is around this process.  If any errors are 
 * encountered, the dechunking process will error out, leaving the decision for retrying the chunk up to
 * a {@link RepeatTemplate}.  This template is configurable, allowing for the number of retries and how long 
 * to wait between retries (backoff) to be set.  Once dechunking has been finished, any errors not fatal to
 * the chunk (usually because the error didn't invalidate the transaction) will also be written out via
 * the {@link SkippedItemHandler}</p>
 * 
 * <p>Clients can use {@link RepeatListener}s in the step operations to intercept or listen to the iteration 
 * on a step-wide basis, for instance to get a callback when the step is complete.  The open and close methods of
 * could easily be done with AOP, however, notifications in between complete chunks (before and after) can
 * be quite useful</p>
 * 
 * <p>Repository Usage: The {@link JobRepository} is used extensively to store metadata about the run such as
 * when the {@link StepExecution} was started, or the commit count.</p>
 * 
 * <p>Interruption: At various times while processing, the step will check to see if it has been interrupted 
 * by calling the {@link StepInterruptionPolicy}.  This policy could check if thread.isInterupted() is true, 
 * or RepeatContext.isTerminateOnly() is set.  It could even be a check to see if a 'stop file' has been added 
 * to a particular directory.  If the step should finish, a {@link JobInterruptedException} is thrown, and the
 * step will clean up, set the status of the {@link StepExecution} to 'STOPPED' and rethrow.</p.
 * 
 * <p>ExitStatusClassification: Any number of fatal errors could be thrown during processing.  In general, the
 * framework must remain fairly dumb as to what error code these exceptions should translate to.  By default
 * it's a fairly generic 'FATAL_EXECUTION'.  However, this may be insufficient for many scenarios.  If an
 * enterprise scheduler is used to kick off a batch job, the exit code is the only means of communication as
 * to what action must be taken.  It may also be the only result that many batch operators see as well.  Therefore,
 * an {@link ExitStatusExceptionClassifier} may be used to classify an exception to a particular exit code.</p>
 * 
 * @author Dave Syer
 * @author Lucas Ward
 * @author Ben Hale
 */
public class ChunkedStep extends StepSupport implements InitializingBean{

	private static final Log logger = LogFactory.getLog(ChunkedStep.class);

	private RepeatOperations stepOperations = new RepeatTemplate();

	private JobRepository jobRepository;

	//default to simple exception classification.
	private ExitStatusExceptionClassifier exceptionClassifier = new SimpleExitStatusExceptionClassifier();

	// default to checking current thread for interruption.
	private StepInterruptionPolicy interruptionPolicy = new ThreadStepInterruptionPolicy();
	
	private SkippedItemHandler failureLog = new DefaultItemFailureLog();

	private StreamManager streamManager;

	private ItemReader itemReader;
	private Chunker chunker;

	private ItemWriter itemWriter;
	private Dechunker dechunker;
	
	private ItemSkipPolicy itemSkipPolicy;

	private RetryTemplate retryTemplate = new RetryTemplate();
	
	private int chunkSize;
	
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
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
	
	public void setFailureLog(SkippedItemHandler failureLog) {
		this.failureLog = failureLog;
	}

	/**
	 * Injected strategy for storage and retrieval of persistent step information. Mandatory property.
	 * 
	 * @param jobRepository
	 */
	public void setJobRepository(JobRepository jobRepository) {
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
	
	public void setChunker(Chunker chunker) {
		this.chunker = chunker;
	}
	
	public void setDechunker(Dechunker dechunker) {
		this.dechunker = dechunker;
	}
	
	/**
	 * Set the skip policy.  If set, it will be used for both reading
	 * and writing.
	 * 
	 * @param itemSkipPolicy
	 */
	public void setItemSkipPolicy(ItemSkipPolicy itemSkipPolicy) {
		this.itemSkipPolicy = itemSkipPolicy;
	}

	/**
	 * Check mandatory properties (reader and writer).
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		//This is currently a little bit funky, I don't want to require a chunker or
		//dechunker to be wired in, since the developer should really only be wiring in a
		//ItemReader and ItemWriter, a namespace should take care of the issue though.
		if(chunker == null){
			chunker = new ItemChunker(itemReader);
			if(itemSkipPolicy != null){
				((ItemChunker)chunker).setItemSkipPolicy(itemSkipPolicy);
			}
		}
		
		if(dechunker == null){
			dechunker = new ItemDechunker(itemWriter);
			if(itemSkipPolicy != null){
				((ItemChunker)dechunker).setItemSkipPolicy(itemSkipPolicy);
			}
		}
		
		Assert.notNull(jobRepository, "JobRepository must not be null");
	}

	/**
	 * Process the step and update its context so that progress can be monitored by the caller. The step is broken down
	 * into chunks, each one executing in a transaction. The step and its execution and execution context are all given
	 * an up to date {@link BatchStatus}, and the {@link JobRepository} is used to store the result. Various reporting
	 * information are also added to the current context (the {@link RepeatContext} governing the step execution, which
	 * would normally be available to the caller somehow through the step's {@link StepContext}.<br/>
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

		final boolean saveExecutionContext = isSaveExecutionContext();

		if (saveExecutionContext && isRestart && stepInstance.getLastExecution() != null) {
			stepExecution.setExecutionContext(stepInstance.getLastExecution().getExecutionContext());
			stepContext.restoreFrom(stepExecution.getExecutionContext());
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
					
					ChunkingResult chunkingResult = chunker.chunk(chunkSize, stepExecution);
					
					if(chunkingResult == null){
						return ExitStatus.FINISHED;
					}
					
					final Chunk chunk = chunkingResult.getChunk();
					failureLog.handle(chunkingResult.getExceptions());

					retryTemplate.execute(new RetryCallback(){

						public Object doWithRetry(RetryContext context)
								throws Throwable {
							processChunk(chunk, stepExecution, stepContext);
							return null;
						}});

					// Check for interruption after transaction as well, so that
					// the interrupted exception is correctly propagated up to
					// caller
					interruptionPolicy.checkInterrupted(context);

					return ExitStatus.CONTINUABLE;

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
	 * Execute a bunch of identical business logic operations all within a transaction.
	 * 
	 * @param stepExecution the current execution in which to process the chunk in.
	 * @param chunk to be processed.
	 * @param stepContext the current step context.
	 * @return true if there is more data to process.
	 */
	void processChunk(Chunk chunk, final StepExecution stepExecution, StepContext stepContext) {
		
		TransactionStatus transaction = streamManager.getTransaction(stepContext);

		final StepContribution contribution = stepExecution.createStepContribution();
		
		try {
			
			DechunkingResult chunkResult = dechunker.dechunk(chunk, stepExecution);
			failureLog.handle(chunkResult.getExceptions());

			// TODO: check that stepExecution can
			// aggregate these contributions if they
			// come in asynchronously.
			ExecutionContext statistics = stepContext.getExecutionContext();
			contribution.setExecutionContext(statistics);
			contribution.incrementCommitCount();

			// If the step operations are asynchronous then we need
			// to synchronize changes to the step execution (at a
			// minimum).
			synchronized (stepExecution) {

				// Apply the contribution to the step
				// only if chunk was successful
				stepExecution.apply(contribution);

				if (isSaveExecutionContext()) {
					stepExecution.setExecutionContext(stepContext.getExecutionContext());
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
		
	}

	/*
	 * Convenience method to update the status in all relevant places.
	 * 
	 * @param stepInstance the current step
	 * @param stepExecution the current stepExecution
	 * @param status the status to set
	 */
	private void updateStatus(StepExecution stepExecution, BatchStatus status) {
		stepExecution.setStatus(status);
		jobRepository.saveOrUpdate(stepExecution);
	}
}
