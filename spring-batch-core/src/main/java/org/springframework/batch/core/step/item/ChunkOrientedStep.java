/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.step.item;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import io.micrometer.observation.Observation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.job.JobInterruptedException;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.listener.CompositeChunkListener;
import org.springframework.batch.core.listener.CompositeItemProcessListener;
import org.springframework.batch.core.listener.CompositeItemReadListener;
import org.springframework.batch.core.listener.CompositeItemWriteListener;
import org.springframework.batch.core.listener.CompositeSkipListener;
import org.springframework.batch.core.listener.ItemProcessListener;
import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.observability.BatchMetrics;
import org.springframework.batch.core.observability.jfr.events.step.chunk.ChunkScanEvent;
import org.springframework.batch.core.observability.jfr.events.step.chunk.ChunkTransactionEvent;
import org.springframework.batch.core.observability.jfr.events.step.chunk.ChunkWriteEvent;
import org.springframework.batch.core.observability.jfr.events.step.chunk.ItemProcessEvent;
import org.springframework.batch.core.observability.jfr.events.step.chunk.ItemReadEvent;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.FatalStepExecutionException;
import org.springframework.batch.core.step.StepInterruptionPolicy;
import org.springframework.batch.core.step.ThreadStepInterruptionPolicy;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableProcessException;
import org.springframework.batch.core.step.skip.NonSkippableReadException;
import org.springframework.batch.core.step.skip.NonSkippableWriteException;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.CompositeItemStream;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.core.retry.support.CompositeRetryListener;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import static org.springframework.batch.core.observability.BatchMetrics.METRICS_PREFIX;

/**
 * Step implementation for the chunk-oriented processing model. This class also supports
 * faut-tolerance features (retry and skip) as well as concurrent item processing when a
 * {@link AsyncTaskExecutor} is provided.
 *
 * @param <I> type of input items
 * @param <O> type of output items
 * @author Mahmoud Ben Hassine
 * @author Andrey Litvitski
 * @author xeounxzxu
 * @since 6.0
 */
public class ChunkOrientedStep<I, O> extends AbstractStep {

	private static final Log logger = LogFactory.getLog(ChunkOrientedStep.class.getName());

	/*
	 * Step Input / Output parameters
	 */
	private final ItemReader<I> itemReader;

	private final CompositeItemReadListener<I> compositeItemReadListener = new CompositeItemReadListener<>();

	@SuppressWarnings("unchecked")
	private ItemProcessor<I, O> itemProcessor = item -> (O) item;

	private final CompositeItemProcessListener<I, O> compositeItemProcessListener = new CompositeItemProcessListener<>();

	private final ItemWriter<O> itemWriter;

	private final CompositeItemWriteListener<O> compositeItemWriteListener = new CompositeItemWriteListener<>();

	/*
	 * Step state / interruption parameters
	 */
	private final CompositeItemStream compositeItemStream = new CompositeItemStream();

	private StepInterruptionPolicy interruptionPolicy = new ThreadStepInterruptionPolicy();

	/*
	 * Transaction related parameters
	 */
	private @Nullable PlatformTransactionManager transactionManager;

	@SuppressWarnings("NullAway.Init")
	private TransactionTemplate transactionTemplate;

	private @Nullable TransactionAttribute transactionAttribute;

	/*
	 * Chunk related parameters
	 */
	private final int chunkSize;

	private final ThreadLocal<ChunkTracker<O>> chunkTracker = ThreadLocal.withInitial(ChunkTracker::create);

	private final CompositeChunkListener<I, O> compositeChunkListener = new CompositeChunkListener<>();

	/*
	 * Fault-tolerance parameters
	 */
	private boolean faultTolerant = false;

	private RetryPolicy retryPolicy = throwable -> false;

	private final RetryTemplate retryTemplate = new RetryTemplate();

	private final CompositeRetryListener compositeRetryListener = new CompositeRetryListener();

	private SkipPolicy skipPolicy = new NeverSkipItemSkipPolicy();

	private final CompositeSkipListener<I, O> compositeSkipListener = new CompositeSkipListener<>();

	/*
	 * Concurrency parameters
	 */
	@SuppressWarnings("NullAway.Init")
	private AsyncTaskExecutor taskExecutor;

	/**
	 * Create a new {@link ChunkOrientedStep}.
	 * @param name the name of the step
	 * @param chunkSize the size of the chunk to process
	 * @param itemReader the item reader to read items
	 * @param itemWriter the item writer to write items
	 * @param jobRepository the job repository to use for this step
	 */
	public ChunkOrientedStep(String name, int chunkSize, ItemReader<I> itemReader, ItemWriter<O> itemWriter,
			JobRepository jobRepository) {
		super(jobRepository);
		this.chunkSize = chunkSize;
		this.itemReader = itemReader;
		this.itemWriter = itemWriter;
		setName(name);
	}

	/**
	 * Set the item processor to use for processing items.
	 * @param itemProcessor the item processor to set
	 */
	public void setItemProcessor(ItemProcessor<I, O> itemProcessor) {
		Assert.notNull(itemProcessor, "Item processor must not be null");
		this.itemProcessor = itemProcessor;
	}

	/**
	 * Set the step interruption policy to use for checking if the step should be
	 * interrupted. Checked at chunk boundaries. Defaults to
	 * {@link ThreadStepInterruptionPolicy}.
	 */
	public void setInterruptionPolicy(StepInterruptionPolicy interruptionPolicy) {
		Assert.notNull(interruptionPolicy, "Interruption policy must not be null");
		this.interruptionPolicy = interruptionPolicy;
	}

	/**
	 * Register an {@link ItemStream} with this step. The stream will be opened and closed
	 * as part of the step's lifecycle.
	 * @param stream the item stream to register
	 */
	public void registerItemStream(ItemStream stream) {
		Assert.notNull(stream, "Item stream must not be null");
		this.compositeItemStream.register(stream);
	}

	/**
	 * Set the {@link ItemReadListener} to be notified of item read events.
	 * @param itemReadListener the item read listener to set
	 */
	public void registerItemReadListener(ItemReadListener<I> itemReadListener) {
		Assert.notNull(itemReadListener, "Item read listener must not be null");
		this.compositeItemReadListener.register(itemReadListener);
	}

	/**
	 * Set the {@link ItemProcessListener} to be notified of item processing events.
	 * @param itemProcessListener the item process listener to set
	 */
	public void registerItemProcessListener(ItemProcessListener<I, O> itemProcessListener) {
		Assert.notNull(itemProcessListener, "Item process listener must not be null");
		this.compositeItemProcessListener.register(itemProcessListener);
	}

	/**
	 * Set the {@link ItemWriteListener} to be notified of item write events.
	 * @param itemWriteListener the item write listener to set
	 */
	public void registerItemWriteListener(ItemWriteListener<O> itemWriteListener) {
		Assert.notNull(itemWriteListener, "Item write listener must not be null");
		this.compositeItemWriteListener.register(itemWriteListener);
	}

	/**
	 * Set the {@link ChunkListener} to be notified of chunk processing events.
	 * @param chunkListener the chunk listener to set
	 */
	public void registerChunkListener(ChunkListener<I, O> chunkListener) {
		Assert.notNull(chunkListener, "Chunk listener must not be null");
		this.compositeChunkListener.register(chunkListener);
	}

	/**
	 * Set the {@link PlatformTransactionManager} to use for the chunk-oriented tasklet.
	 * Defaults to a {@link ResourcelessTransactionManager}.
	 * @param transactionManager a transaction manager set, must not be null.
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		Assert.notNull(transactionManager, "Transaction manager must not be null");
		this.transactionManager = transactionManager;
	}

	/**
	 * Set the transaction attribute for this step.
	 * @param transactionAttribute the transaction attribute to set
	 */
	public void setTransactionAttribute(TransactionAttribute transactionAttribute) {
		Assert.notNull(transactionAttribute, "Transaction attribute must not be null");
		this.transactionAttribute = transactionAttribute;
	}

	/**
	 * Mark this step as fault-tolerant. When set to true, the step will handle retrying
	 * and skipping items that failed according to the configured retry and skip policies.
	 * If set to false, any exception during item processing will cause the step to fail
	 * immediately.
	 * @param faultTolerant true to enable fault-tolerant processing, false otherwise
	 */
	public void setFaultTolerant(boolean faultTolerant) {
		this.faultTolerant = faultTolerant;
	}

	/**
	 * Set the {@link AsyncTaskExecutor} to use for processing items asynchronously.
	 * @param asyncTaskExecutor the asynchronous task executor to set
	 */
	public void setTaskExecutor(AsyncTaskExecutor asyncTaskExecutor) {
		Assert.notNull(asyncTaskExecutor, "Task executor must not be null");
		this.taskExecutor = asyncTaskExecutor;
	}

	/**
	 * Set the {@link RetryPolicy} for this step.
	 * @param retryPolicy the retry policy to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		Assert.notNull(retryPolicy, "Retry policy must not be null");
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Register a {@link RetryListener} to be notified of item retry events.
	 * @param retryListener the retry listener to register
	 */
	public void registerRetryListener(RetryListener retryListener) {
		Assert.notNull(retryListener, "Retry listener must not be null");
		this.compositeRetryListener.addListener(retryListener);
	}

	/**
	 * Set the skip policy for this step. The skip policy will be used to determine
	 * whether an item should be skipped or not when an exception occurs during item
	 * processing.
	 * @param skipPolicy the skip policy to set. Defaults to
	 * {@link NeverSkipItemSkipPolicy}.
	 */
	public void setSkipPolicy(SkipPolicy skipPolicy) {
		Assert.notNull(skipPolicy, "Skip policy must not be null");
		this.skipPolicy = skipPolicy;
	}

	/**
	 * register a {@link SkipListener} to be notified of item skip events.
	 * @param skipListener the skip listener to register
	 */
	public void registerSkipListener(SkipListener<I, O> skipListener) {
		Assert.notNull(skipListener, "Skip listener must not be null");
		this.compositeSkipListener.register(skipListener);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		if (this.transactionManager == null) {
			logger.debug("No transaction manager has been set. Defaulting to ResourcelessTransactionManager.");
			this.transactionManager = new ResourcelessTransactionManager();
		}
		if (this.transactionAttribute == null) {
			logger.debug("No transaction attribute has been set. Defaulting to DefaultTransactionAttribute.");
			this.transactionAttribute = new DefaultTransactionAttribute();
		}
		Assert.isTrue(this.chunkSize > 0, "Chunk size must be greater than 0");
		Assert.notNull(this.itemReader, "Item reader must not be null");
		Assert.notNull(this.itemWriter, "Item writer must not be null");
		if (this.itemReader instanceof ItemStream itemStream) {
			this.compositeItemStream.register(itemStream);
		}
		if (this.itemWriter instanceof ItemStream itemStream) {
			this.compositeItemStream.register(itemStream);
		}
		if (this.itemProcessor instanceof ItemStream itemStream) {
			this.compositeItemStream.register(itemStream);
		}
		this.transactionTemplate = new TransactionTemplate(this.transactionManager, this.transactionAttribute);
		if (this.faultTolerant) {
			this.retryTemplate.setRetryPolicy(this.retryPolicy);
			this.retryTemplate.setRetryListener(this.compositeRetryListener);
		}
	}

	@Override
	protected void open(ExecutionContext executionContext) throws Exception {
		this.compositeItemStream.open(executionContext);
		this.chunkTracker.get().init();
	}

	@Override
	protected void close(ExecutionContext executionContext) throws Exception {
		this.chunkTracker.get().reset();
		this.compositeItemStream.close();
	}

	@Override
	protected void doExecute(StepExecution stepExecution) throws Exception {
		stepExecution.getExecutionContext().put(STEP_TYPE_KEY, this.getClass().getName());
		while (this.chunkTracker.get().moreItems() && !interrupted(stepExecution)) {
			// process next chunk in its own transaction
			this.transactionTemplate.executeWithoutResult(transactionStatus -> {
				ChunkTransactionEvent chunkTransactionEvent = new ChunkTransactionEvent(stepExecution.getStepName(),
						stepExecution.getId());
				chunkTransactionEvent.begin();
				StepContribution contribution = stepExecution.createStepContribution();
				processNextChunk(transactionStatus, contribution, stepExecution);

				// Skip update during rollback to avoid OptimisticLockingFailureException
				if (transactionStatus.isRollbackOnly()) {
					chunkTransactionEvent.transactionStatus = BatchMetrics.STATUS_ROLLED_BACK;
					chunkTransactionEvent.commit();
					return;
				}

				this.compositeItemStream.update(stepExecution.getExecutionContext());
				getJobRepository().updateExecutionContext(stepExecution);
				getJobRepository().update(stepExecution);
				chunkTransactionEvent.transactionStatus = BatchMetrics.STATUS_COMMITTED;
				chunkTransactionEvent.commit();
			});
		}
	}

	private void processNextChunk(TransactionStatus status, StepContribution contribution,
			StepExecution stepExecution) {
		if (isConcurrent()) {
			processChunkConcurrently(status, contribution, stepExecution);
		}
		else {
			processChunkSequentially(status, contribution, stepExecution);
		}
	}

	private void processChunkConcurrently(TransactionStatus status, StepContribution contribution,
			StepExecution stepExecution) {
		List<Future<O>> itemProcessingTasks = new LinkedList<>();
		Chunk<O> processedChunk = new Chunk<>();
		ChunkTracker<O> tracker = this.chunkTracker.get();

		try {
			if (tracker.isScanMode()) {
				logger.info("Executing scan in new transaction after rollback");
				Chunk<O> pendingChunk = tracker.getPendingChunk();
				if (pendingChunk != null) {
					ChunkScanEvent chunkScanEvent = new ChunkScanEvent(stepExecution.getStepName(),
							stepExecution.getId());
					chunkScanEvent.begin();
					scan(pendingChunk, contribution);
					chunkScanEvent.skipCount = contribution.getSkipCount();
					chunkScanEvent.commit();
					logger.info("Chunk scan completed");
					tracker.exitScanMode();
					stepExecution.incrementCommitCount();
				}
				return;
			}

			// read items and submit concurrent item processing tasks
			for (int i = 0; i < this.chunkSize && this.chunkTracker.get().moreItems(); i++) {
				I item = readItem(contribution);
				if (item != null) {
					Future<O> itemProcessingFuture = this.taskExecutor.submit(() -> {
						try {
							StepSynchronizationManager.register(stepExecution);
							return processItem(item, contribution);
						}
						finally {
							StepSynchronizationManager.close();
						}
					});
					itemProcessingTasks.add(itemProcessingFuture);
				}
			}
			// exclude empty chunks (when the total items is a multiple of the chunk size)
			if (itemProcessingTasks.isEmpty()) {
				return;
			}

			// collect processed items
			for (Future<O> future : itemProcessingTasks) {
				O processedItem = future.get();
				if (processedItem != null) {
					processedChunk.add(processedItem);
				}
			}

			// write processed items
			writeChunk(processedChunk, contribution);
			stepExecution.incrementCommitCount();
		}
		catch (Exception e) {
			logger.error("Rolling back chunk transaction", e);
			status.setRollbackOnly();
			stepExecution.incrementRollbackCount();

			if (tracker.isScanMode()) {
				if (e instanceof SkipLimitExceededException || e instanceof NonSkippableWriteException) {
					tracker.exitScanMode();
					throw new FatalStepExecutionException("Unable to process chunk during scan", e);
				}
				logger.info("Rollback complete, scan will execute in next transaction");
				return;
			}

			throw new FatalStepExecutionException("Unable to process chunk", e);
		}
		finally {
			stepExecution.apply(contribution);
		}
	}

	private void processChunkSequentially(TransactionStatus status, StepContribution contribution,
			StepExecution stepExecution) {
		Chunk<I> inputChunk = new Chunk<>();
		Chunk<O> processedChunk = new Chunk<>();
		ChunkTracker<O> tracker = this.chunkTracker.get();

		try {
			if (tracker.isScanMode()) {
				logger.info("Executing scan in new transaction after rollback");
				Chunk<O> pendingChunk = tracker.getPendingChunk();
				if (pendingChunk != null) {
					ChunkScanEvent chunkScanEvent = new ChunkScanEvent(stepExecution.getStepName(),
							stepExecution.getId());
					chunkScanEvent.begin();
					compositeChunkListener.beforeChunk(new Chunk<>());
					scan(pendingChunk, contribution);
					compositeChunkListener.afterChunk(pendingChunk);
					chunkScanEvent.skipCount = contribution.getSkipCount();
					chunkScanEvent.commit();
					logger.info("Chunk scan completed");
					tracker.exitScanMode();
					stepExecution.incrementCommitCount();
				}
				return;
			}

			inputChunk = readChunk(contribution);
			if (inputChunk.isEmpty()) {
				return;
			}
			compositeChunkListener.beforeChunk(inputChunk);
			processedChunk = processChunk(inputChunk, contribution);
			writeChunk(processedChunk, contribution);
			compositeChunkListener.afterChunk(processedChunk);
			stepExecution.incrementCommitCount();
		}
		catch (Exception e) {
			logger.error("Rolling back chunk transaction", e);
			status.setRollbackOnly();
			stepExecution.incrementRollbackCount();

			if (tracker.isScanMode()) {
				if (e instanceof SkipLimitExceededException || e instanceof NonSkippableWriteException) {
					tracker.exitScanMode();
					compositeChunkListener.onChunkError(e, processedChunk);
					throw new FatalStepExecutionException("Unable to process chunk during scan", e);
				}
				logger.info("Rollback complete, scan will execute in next transaction");
				return;
			}

			compositeChunkListener.onChunkError(e, processedChunk);
			throw new FatalStepExecutionException("Unable to process chunk", e);
		}
		finally {
			stepExecution.apply(contribution);
		}
	}

	/*
	 * Check if the step has been interrupted either internally via user defined policy or
	 * externally via job operator. This will be checked at chunk boundaries.
	 */
	private boolean interrupted(StepExecution stepExecution) {
		// check internal interruption via user defined policy
		try {
			this.interruptionPolicy.checkInterrupted(stepExecution);
		}
		catch (JobInterruptedException exception) {
			return true;
		}
		// check external interruption via job operator
		if (stepExecution.isTerminateOnly()) {
			return true;
		}
		return false;
	}

	private Chunk<I> readChunk(StepContribution contribution) throws Exception {
		Chunk<I> chunk = new Chunk<>();
		for (int i = 0; i < chunkSize && this.chunkTracker.get().moreItems(); i++) {
			I item = readItem(contribution);
			if (item != null) {
				chunk.add(item);
			}
		}
		return chunk;
	}

	private @Nullable I readItem(StepContribution contribution) throws Exception {
		ItemReadEvent itemReadEvent = new ItemReadEvent(contribution.getStepExecution().getStepName(),
				contribution.getStepExecution().getId());
		String fullyQualifiedMetricName = BatchMetrics.METRICS_PREFIX + "item.read";
		Observation observation = Observation.createNotStarted(fullyQualifiedMetricName, this.observationRegistry)
			.lowCardinalityKeyValue(fullyQualifiedMetricName + ".job.name",
					contribution.getStepExecution().getJobExecution().getJobInstance().getJobName())
			.lowCardinalityKeyValue(fullyQualifiedMetricName + ".step.name",
					contribution.getStepExecution().getStepName())
			.start();
		itemReadEvent.begin();
		I item = null;
		try (var scope = observation.openScope()) {
			this.compositeItemReadListener.beforeRead();
			item = doRead();
			if (item == null) {
				this.chunkTracker.get().reset();
			}
			else {
				contribution.incrementReadCount();
				this.compositeItemReadListener.afterRead(item);
			}
			itemReadEvent.itemReadStatus = BatchMetrics.STATUS_SUCCESS;
			observation.lowCardinalityKeyValue(fullyQualifiedMetricName + ".status", BatchMetrics.STATUS_SUCCESS);
		}
		catch (Exception exception) {
			this.compositeItemReadListener.onReadError(exception);
			if (this.faultTolerant && exception instanceof RetryException retryException) {
				doSkipInRead(retryException, contribution);
			}
			else {
				throw exception;
			}
			itemReadEvent.itemReadStatus = BatchMetrics.STATUS_FAILURE;
			observation.lowCardinalityKeyValue(fullyQualifiedMetricName + ".status", BatchMetrics.STATUS_FAILURE);
			observation.error(exception);
		}
		finally {
			itemReadEvent.commit();
			observation.stop();
		}
		return item;
	}

	@SuppressWarnings("NullAway")
	private @Nullable I doRead() throws Exception {
		if (this.faultTolerant) {
			Retryable<I> retryableRead = new Retryable<>() {
				@Override
				public I execute() throws Throwable {
					return itemReader.read();
				}

				@Override
				public String getName() {
					return "Retryable read operation";
				}
			};
			return this.retryTemplate.execute(retryableRead);
		}
		else {
			return this.itemReader.read();
		}
	}

	private void doSkipInRead(RetryException retryException, StepContribution contribution) {
		Throwable cause = retryException.getCause();
		if (this.skipPolicy.shouldSkip(cause, contribution.getStepSkipCount())) {
			this.compositeSkipListener.onSkipInRead(cause);
			contribution.incrementReadSkipCount();
		}
		else {
			throw new NonSkippableReadException("Skip policy rejected skipping item", cause);
		}
	}

	private Chunk<O> processChunk(Chunk<I> chunk, StepContribution contribution) throws Exception {
		Chunk<O> processedChunk = new Chunk<>();
		for (I item : chunk) {
			O processedItem = processItem(item, contribution);
			if (processedItem != null) {
				processedChunk.add(processedItem);
			}
		}
		return processedChunk;
	}

	private @Nullable O processItem(I item, StepContribution contribution) throws Exception {
		ItemProcessEvent itemProcessEvent = new ItemProcessEvent(contribution.getStepExecution().getStepName(),
				contribution.getStepExecution().getId());
		String fullyQualifiedMetricName = METRICS_PREFIX + "item.process";
		Observation observation = Observation.createNotStarted(fullyQualifiedMetricName, this.observationRegistry)
			.lowCardinalityKeyValue(fullyQualifiedMetricName + ".job.name",
					contribution.getStepExecution().getJobExecution().getJobInstance().getJobName())
			.lowCardinalityKeyValue(fullyQualifiedMetricName + ".step.name",
					contribution.getStepExecution().getStepName())
			.start();
		itemProcessEvent.begin();
		O processedItem = null;
		try (var scope = observation.openScope()) {
			this.compositeItemProcessListener.beforeProcess(item);
			processedItem = doProcess(item);
			if (processedItem == null) {
				contribution.incrementFilterCount();
			}
			this.compositeItemProcessListener.afterProcess(item, processedItem);
			itemProcessEvent.itemProcessStatus = BatchMetrics.STATUS_SUCCESS;
			observation.lowCardinalityKeyValue(fullyQualifiedMetricName + ".status", BatchMetrics.STATUS_SUCCESS);
		}
		catch (Exception exception) {
			this.compositeItemProcessListener.onProcessError(item, exception);
			if (this.faultTolerant && exception instanceof RetryException retryException) {
				doSkipInProcess(item, retryException, contribution);
			}
			else {
				throw exception;
			}
			itemProcessEvent.itemProcessStatus = BatchMetrics.STATUS_FAILURE;
			observation.lowCardinalityKeyValue(fullyQualifiedMetricName + ".status", BatchMetrics.STATUS_FAILURE);
			observation.error(exception);
		}
		finally {
			itemProcessEvent.commit();
			observation.stop();
		}
		return processedItem;
	}

	@SuppressWarnings("NullAway")
	private @Nullable O doProcess(I item) throws Exception {
		if (this.faultTolerant) {
			Retryable<O> retryableProcess = new Retryable<>() {
				@Override
				public O execute() throws Throwable {
					StepContext context = StepSynchronizationManager.getContext();
					final StepExecution stepExecution = context == null ? null : context.getStepExecution();
					if (isConcurrent() && stepExecution != null) {
						StepSynchronizationManager.register(stepExecution);
					}
					try {
						return itemProcessor.process(item);
					}
					finally {
						if (isConcurrent() && stepExecution != null) {
							StepSynchronizationManager.close();
						}
					}
				}

				@Override
				public String getName() {
					return "Retryable process operation";
				}
			};
			return this.retryTemplate.execute(retryableProcess);
		}
		else {
			return this.itemProcessor.process(item);
		}
	}

	private void doSkipInProcess(I item, RetryException retryException, StepContribution contribution) {
		Throwable cause = retryException.getCause();
		if (this.skipPolicy.shouldSkip(cause, contribution.getStepSkipCount())) {
			this.compositeSkipListener.onSkipInProcess(item, retryException.getCause());
			contribution.incrementProcessSkipCount();
		}
		else {
			throw new NonSkippableProcessException("Skip policy rejected skipping item", cause);
		}
	}

	private void writeChunk(Chunk<O> chunk, StepContribution contribution) throws Exception {
		ChunkWriteEvent chunkWriteEvent = new ChunkWriteEvent(contribution.getStepExecution().getStepName(),
				contribution.getStepExecution().getId(), chunk.size());
		String fullyQualifiedMetricName = METRICS_PREFIX + "chunk.write";
		Observation observation = Observation.createNotStarted(fullyQualifiedMetricName, this.observationRegistry)
			.lowCardinalityKeyValue(fullyQualifiedMetricName + ".job.name",
					contribution.getStepExecution().getJobExecution().getJobInstance().getJobName())
			.lowCardinalityKeyValue(fullyQualifiedMetricName + ".step.name",
					contribution.getStepExecution().getStepName())
			.start();
		chunkWriteEvent.begin();
		try (var scope = observation.openScope()) {
			this.compositeItemWriteListener.beforeWrite(chunk);
			doWrite(chunk);
			contribution.incrementWriteCount(chunk.size());
			this.compositeItemWriteListener.afterWrite(chunk);
			chunkWriteEvent.chunkWriteStatus = BatchMetrics.STATUS_SUCCESS;
			observation.lowCardinalityKeyValue(fullyQualifiedMetricName + ".status", BatchMetrics.STATUS_SUCCESS);
		}
		catch (Exception exception) {
			this.compositeItemWriteListener.onWriteError(exception, chunk);
			chunkWriteEvent.chunkWriteStatus = BatchMetrics.STATUS_FAILURE;
			observation.lowCardinalityKeyValue(fullyQualifiedMetricName + ".status", BatchMetrics.STATUS_FAILURE);
			observation.error(exception);

			if (this.faultTolerant && exception instanceof RetryException retryException
					&& this.skipPolicy.shouldSkip(retryException.getCause(), -1)) {
				logger.info("Retry exhausted, entering scan mode for next transaction", retryException);
				this.chunkTracker.get().enterScanMode(chunk);
			}
			else {
				logger.error("Retry exhausted after last attempt in recovery path, but exception is not skippable");
			}
			throw exception;
		}
		finally {
			chunkWriteEvent.commit();
			observation.stop();
		}
	}

	private void doWrite(Chunk<O> chunk) throws Exception {
		if (this.faultTolerant) {
			Retryable<Void> retryableWrite = new Retryable<>() {
				@Override
				public Void execute() throws Throwable {
					itemWriter.write(chunk);
					return null;
				}

				@Override
				public String getName() {
					return "Retryable write operation";
				}
			};
			this.retryTemplate.execute(retryableWrite);
		}
		else {
			this.itemWriter.write(chunk);
		}
	}

	private void scan(Chunk<O> chunk, StepContribution contribution) {
		for (O item : chunk) {
			Chunk<O> singleItemChunk = new Chunk<>(item);
			try {
				this.compositeItemWriteListener.beforeWrite(singleItemChunk);
				this.itemWriter.write(singleItemChunk);
				contribution.incrementWriteCount(singleItemChunk.size());
				this.compositeItemWriteListener.afterWrite(singleItemChunk);
			}
			catch (Exception exception) {
				if (this.skipPolicy.shouldSkip(exception, contribution.getStepSkipCount())) {
					this.compositeSkipListener.onSkipInWrite(item, exception);
					contribution.incrementWriteSkipCount();
					contribution.getStepExecution().incrementRollbackCount();
				}
				else {
					logger.error("Failed to write item: " + item, exception);
					this.compositeItemWriteListener.onWriteError(exception, singleItemChunk);
					throw new NonSkippableWriteException("Skip policy rejected skipping item", exception);
				}
			}
		}
	}

	private boolean isConcurrent() {
		return this.taskExecutor != null;
	}

	private static class ChunkTracker<O> {

		static <T> ChunkTracker<T> create() {
			return new ChunkTracker<>();
		}

		private boolean moreItems;

		private boolean scanMode;

		@Nullable private Chunk<O> pendingChunk;

		void init() {
			this.moreItems = true;
			this.scanMode = false;
			this.pendingChunk = null;
		}

		void reset() {
			this.moreItems = false;
		}

		boolean moreItems() {
			return this.moreItems || this.scanMode;
		}

		void enterScanMode(Chunk<O> chunk) {
			this.scanMode = true;
			this.pendingChunk = new Chunk<>(chunk.getItems());
		}

		boolean isScanMode() {
			return this.scanMode;
		}

		@Nullable Chunk<O> getPendingChunk() {
			return this.pendingChunk;
		}

		void exitScanMode() {
			this.scanMode = false;
			this.pendingChunk = null;
		}

	}

}
