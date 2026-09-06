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

import java.util.ArrayList;
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
 * @author Minchul Son
 * @author Yanming Zhou
 * @author Taeik Lim
 * @since 6.0
 */
public class ChunkOrientedStep<I, O> extends AbstractStep {

	private static final Log logger = LogFactory.getLog(ChunkOrientedStep.class.getName());

	/*
	 * Step Input / Output parameters
	 */
	private final ItemReader<? extends I> itemReader;

	private final CompositeItemReadListener<I> compositeItemReadListener = new CompositeItemReadListener<>();

	@SuppressWarnings("unchecked")
	private ItemProcessor<? super I, ? extends O> itemProcessor = item -> (O) item;

	private final CompositeItemProcessListener<I, O> compositeItemProcessListener = new CompositeItemProcessListener<>();

	private final ItemWriter<? super O> itemWriter;

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

	private final ThreadLocal<ChunkTracker<I, O>> chunkTracker = ThreadLocal.withInitial(ChunkTracker::create);

	private final CompositeChunkListener<I, O> compositeChunkListener = new CompositeChunkListener<>();

	/*
	 * Fault-tolerance parameters
	 */
	private boolean faultTolerant = false;

	private boolean processorTransactional = true;

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
	public ChunkOrientedStep(String name, int chunkSize, ItemReader<? extends I> itemReader,
			ItemWriter<? super O> itemWriter, JobRepository jobRepository) {
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
	public void setItemProcessor(ItemProcessor<? super I, ? extends O> itemProcessor) {
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
	 * Set whether the {@link ItemProcessor} is transactional. Defaults to {@code true},
	 * which means that the processor is re-invoked for every item that is re-attempted
	 * during chunk scanning (ie after a write failure has rolled back the chunk
	 * transaction). Set this flag to {@code false} to cache the results of item
	 * processing and re-use them during chunk scanning instead.
	 * <p>
	 * Only set this flag to {@code false} if the output of the processor is safe to write
	 * again after a failed, rolled back write. This is typically not the case for items
	 * that the writer mutates, like JPA entities: a failed flush can leave the entity in
	 * a state that makes the next write attempt fail with an unrelated exception.
	 * @param processorTransactional {@code true} to re-invoke the processor during chunk
	 * scanning (the default), {@code false} to re-use the cached processor output
	 * @since 6.0.6
	 */
	public void setProcessorTransactional(boolean processorTransactional) {
		this.processorTransactional = processorTransactional;
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
	public void registerSkipListener(SkipListener<? super I, ? super O> skipListener) {
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
					// Explicitly mark as locally rollback-only to prevent
					// UnexpectedRollbackException when the transaction manager
					// (eg JpaTransactionManager) has marked it as globally rollback-only
					// (eg after a JPA flush failure) but not locally rollback-only.
					transactionStatus.setRollbackOnly();
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
		List<Future<O>> itemProcessingTasks = new ArrayList<>();
		List<I> inputItems = new ArrayList<>();
		List<ScanItem<I, O>> scanItems = new ArrayList<>();
		Chunk<O> processedChunk = new Chunk<>();
		ChunkTracker<I, O> tracker = this.chunkTracker.get();
		boolean scanning = tracker.isScanMode();

		try {
			if (scanning) {
				logger.info("Executing scan in new transaction after rollback");
				ScanItem<I, O> scanItem = tracker.pollNextScanItem();
				if (scanItem != null) {
					ChunkScanEvent chunkScanEvent = new ChunkScanEvent(stepExecution.getStepName(),
							stepExecution.getId());
					chunkScanEvent.begin();
					// no chunk listener callbacks here: ChunkListener is not called in
					// concurrent steps
					scan(scanItem, contribution, status);
					chunkScanEvent.skipCount = contribution.getSkipCount();
					chunkScanEvent.commit();
				}
				if (!tracker.hasPendingScanItems()) {
					logger.info("Chunk scan completed");
					tracker.exitScanMode();
					if (!status.isRollbackOnly()) {
						stepExecution.incrementCommitCount();
					}
				}
				return;
			}

			// read items and submit concurrent item processing tasks
			for (int i = 0; i < this.chunkSize && this.chunkTracker.get().moreItems(); i++) {
				I item = readItem(contribution);
				if (item != null) {
					inputItems.add(item);
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

			// collect processed items, keeping track of the input item each output item
			// was produced from, so that it can be re-processed during a chunk scan
			for (int i = 0; i < itemProcessingTasks.size(); i++) {
				O processedItem = itemProcessingTasks.get(i).get();
				if (processedItem != null) {
					processedChunk.add(processedItem);
					scanItems.add(new ScanItem<>(inputItems.get(i), processedItem));
				}
			}

			// write processed items
			writeChunk(processedChunk, scanItems, contribution, status);
			stepExecution.incrementCommitCount();
		}
		catch (Exception e) {
			logger.error("Rolling back chunk transaction", e);
			status.setRollbackOnly();
			stepExecution.incrementRollbackCount();

			// the write of this chunk has just failed with a skippable exception and the
			// chunk has been queued for scanning: roll back and start the scan in the
			// next transaction
			if (!scanning && tracker.isScanMode()) {
				logger.info("Rollback complete, scan will execute in next transaction");
				return;
			}

			// a scan attempt itself failed: the pending item has already been polled off
			// the scan queue, so carrying on would silently lose it. Fail the step
			// instead, leaving the job restartable.
			if (scanning) {
				tracker.exitScanMode();
				throw new FatalStepExecutionException("Unable to process chunk during scan", e);
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
		List<ScanItem<I, O>> scanItems = new ArrayList<>();
		ChunkTracker<I, O> tracker = this.chunkTracker.get();
		boolean scanning = tracker.isScanMode();

		try {
			if (scanning) {
				logger.info("Executing scan in new transaction after rollback");
				ScanItem<I, O> scanItem = tracker.pollNextScanItem();
				if (scanItem != null) {
					ChunkScanEvent chunkScanEvent = new ChunkScanEvent(stepExecution.getStepName(),
							stepExecution.getId());
					chunkScanEvent.begin();
					compositeChunkListener.beforeChunk(new Chunk<>(scanItem.input()));
					Chunk<O> singleItemChunk = scan(scanItem, contribution, status);
					if (!status.isRollbackOnly()) {
						compositeChunkListener.afterChunk(singleItemChunk);
					}
					chunkScanEvent.skipCount = contribution.getSkipCount();
					chunkScanEvent.commit();
				}
				if (!tracker.hasPendingScanItems()) {
					logger.info("Chunk scan completed");
					tracker.exitScanMode();
					if (!status.isRollbackOnly()) {
						stepExecution.incrementCommitCount();
					}
				}
				return;
			}

			inputChunk = readChunk(contribution);
			if (inputChunk.isEmpty()) {
				return;
			}
			compositeChunkListener.beforeChunk(inputChunk);
			processedChunk = processChunk(inputChunk, contribution, scanItems);
			writeChunk(processedChunk, scanItems, contribution, status);
			compositeChunkListener.afterChunk(processedChunk);
			stepExecution.incrementCommitCount();
		}
		catch (Exception e) {
			logger.error("Rolling back chunk transaction", e);
			status.setRollbackOnly();
			stepExecution.incrementRollbackCount();

			// the write of this chunk has just failed with a skippable exception and the
			// chunk has been queued for scanning: roll back and start the scan in the
			// next transaction
			if (!scanning && tracker.isScanMode()) {
				notifyChunkError(e, processedChunk);
				logger.info("Rollback complete, scan will execute in next transaction");
				return;
			}

			// a scan attempt itself failed: the pending item has already been polled off
			// the scan queue, so carrying on would silently lose it. Fail the step
			// instead, leaving the job restartable. Failures of the scanned item itself
			// have already been reported to the chunk listener by scan().
			if (scanning) {
				tracker.exitScanMode();
				throw new FatalStepExecutionException("Unable to process chunk during scan", e);
			}

			notifyChunkError(e, processedChunk);
			throw new FatalStepExecutionException("Unable to process chunk", e);
		}
		finally {
			stepExecution.apply(contribution);
		}
	}

	/*
	 * Report a chunk failure to the chunk listener. The listener is never called with an
	 * empty chunk: a failure that happens before any item was processed (a read failure,
	 * for example) has no processed chunk to report and is signalled by the read or
	 * process listeners instead. The listener is not called in concurrent steps either.
	 */
	private void notifyChunkError(Exception exception, Chunk<O> processedChunk) {
		if (!isConcurrent() && !processedChunk.isEmpty()) {
			this.compositeChunkListener.onChunkError(exception, processedChunk);
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

	private Chunk<O> processChunk(Chunk<I> chunk, StepContribution contribution, List<ScanItem<I, O>> scanItems)
			throws Exception {
		Chunk<O> processedChunk = new Chunk<>();
		for (I item : chunk) {
			O processedItem = processItem(item, contribution);
			if (processedItem != null) {
				processedChunk.add(processedItem);
				// keep track of the input item each output item was produced from, so
				// that it can be re-processed during a chunk scan
				scanItems.add(new ScanItem<>(item, processedItem));
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

	private void writeChunk(Chunk<O> chunk, List<ScanItem<I, O>> scanItems, StepContribution contribution,
			TransactionStatus status) throws Exception {
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
			// the write may return normally (eg because a retried write attempt no
			// longer throws) even though a participant (eg a JPA transaction manager,
			// after a resource-level failure like an OptimisticLockException) has
			// independently marked the transaction rollback-only: the chunk must not be
			// counted as written in that case
			if (status.isRollbackOnly()) {
				throw new SilentlyRolledBackException(
						"The chunk write completed without an exception, but the transaction is rollback-only");
			}
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

			if (exception instanceof SilentlyRolledBackException) {
				logger.error("Chunk write completed but the transaction was independently marked rollback-only",
						exception);
			}
			else if (this.faultTolerant && exception instanceof RetryException retryException
					&& this.skipPolicy.shouldSkip(retryException.getCause(), -1)) {
				logger.info("Retry exhausted, entering scan mode for next transaction", retryException);
				this.chunkTracker.get().enterScanMode(scanItems);
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

	/*
	 * Re-attempt a single item in its own transaction after the enclosing chunk has been
	 * rolled back. Unless the item processor has been marked as non-transactional, the
	 * item is re-processed first: the output produced in the rolled back transaction may
	 * have been mutated by the failed write (a JPA entity whose flush failed, for
	 * example) and would otherwise fail again for an unrelated reason.
	 *
	 * Returns the chunk that was actually written, which is empty if the re-processed
	 * item was filtered out or skipped in processing.
	 */
	private Chunk<O> scan(ScanItem<I, O> scanItem, StepContribution contribution, TransactionStatus status)
			throws Exception {
		O item = this.processorTransactional ? processItem(scanItem.input(), contribution) : scanItem.output();
		if (item == null) {
			// the item was filtered out or skipped while being re-processed: there is
			// nothing left to write
			return new Chunk<>();
		}
		Chunk<O> singleItemChunk = new Chunk<>(item);
		try {
			this.compositeItemWriteListener.beforeWrite(singleItemChunk);
			this.itemWriter.write(singleItemChunk);
			// see the equivalent check in writeChunk(): the write may return normally
			// even though the transaction has independently been marked rollback-only
			if (status.isRollbackOnly()) {
				throw new SilentlyRolledBackException(
						"The scanned item write completed without an exception, but the transaction is rollback-only");
			}
			contribution.incrementWriteCount(singleItemChunk.size());
			this.compositeItemWriteListener.afterWrite(singleItemChunk);
		}
		catch (Exception exception) {
			// the chunk listener is notified from here rather than from the caller's
			// error handling, because this is where the item that failed is known:
			// the skip path below does not propagate the exception at all, and by the
			// time the other path does, the caller no longer has a processed chunk to
			// report
			if (this.skipPolicy.shouldSkip(exception, contribution.getStepSkipCount())) {
				this.compositeSkipListener.onSkipInWrite(item, exception);
				contribution.incrementWriteSkipCount();
				contribution.getStepExecution().incrementRollbackCount();
				status.setRollbackOnly();
				notifyChunkError(exception, singleItemChunk);
			}
			else {
				logger.error("Failed to write item: " + item, exception);
				this.compositeItemWriteListener.onWriteError(exception, singleItemChunk);
				notifyChunkError(exception, singleItemChunk);
				throw new NonSkippableWriteException("Skip policy rejected skipping item", exception);
			}
		}
		return singleItemChunk;
	}

	private boolean isConcurrent() {
		return this.taskExecutor != null;
	}

	/*
	 * Signals that a write returned normally while the transaction it ran in had already
	 * been independently marked rollback-only by a participant (eg a JPA transaction
	 * manager, after a resource-level failure like an OptimisticLockException that a
	 * retried write no longer raises). It is used internally to route this case into the
	 * same failure handling as a write that threw, so that the chunk is never counted as
	 * committed.
	 */
	private static class SilentlyRolledBackException extends RuntimeException {

		SilentlyRolledBackException(String message) {
			super(message);
		}

	}

	/**
	 * An input item together with the output item produced for it by the
	 * {@link ItemProcessor}, retained so that the item can be re-attempted during a chunk
	 * scan.
	 *
	 * @param input the item as read by the item reader
	 * @param output the item as produced by the item processor
	 */
	private record ScanItem<I, O>(I input, O output) {
	}

	private static class ChunkTracker<I, O> {

		static <T, U> ChunkTracker<T, U> create() {
			return new ChunkTracker<>();
		}

		private boolean moreItems;

		private boolean scanMode;

		private @Nullable LinkedList<ScanItem<I, O>> pendingScanItems;

		void init() {
			this.moreItems = true;
			this.scanMode = false;
			this.pendingScanItems = null;
		}

		void reset() {
			this.moreItems = false;
		}

		boolean moreItems() {
			return this.moreItems || this.scanMode;
		}

		void enterScanMode(List<ScanItem<I, O>> scanItems) {
			this.scanMode = true;
			this.pendingScanItems = new LinkedList<>(scanItems);
		}

		boolean isScanMode() {
			return this.scanMode;
		}

		@Nullable ScanItem<I, O> pollNextScanItem() {
			return (this.pendingScanItems != null) ? this.pendingScanItems.poll() : null;
		}

		boolean hasPendingScanItems() {
			return this.pendingScanItems != null && !this.pendingScanItems.isEmpty();
		}

		void exitScanMode() {
			this.scanMode = false;
			this.pendingScanItems = null;
		}

	}

}
