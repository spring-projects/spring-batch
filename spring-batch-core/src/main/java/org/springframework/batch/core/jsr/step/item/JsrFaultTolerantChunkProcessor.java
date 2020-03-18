/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.step.item;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.MulticasterBatchListener;
import org.springframework.batch.core.step.item.BatchRetryTemplate;
import org.springframework.batch.core.step.item.Chunk;
import org.springframework.batch.core.step.item.ChunkMonitor;
import org.springframework.batch.core.step.item.ForceRollbackForWriteSkipException;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.skip.SkipPolicyFailedException;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.classify.Classifier;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryException;
import org.springframework.util.Assert;

import javax.batch.operations.BatchRuntimeException;
import java.util.List;

/**
 * Extension of the {@link JsrChunkProcessor} that adds skip and retry functionality.
 *
 * @author Michael Minella
 * @author Chris Schaefer
 *
 * @param <I> input type for the step
 * @param <O> output type for the step
 */
public class JsrFaultTolerantChunkProcessor<I,O> extends JsrChunkProcessor<I, O> {
	protected final Log logger = LogFactory.getLog(getClass());
	private SkipPolicy skipPolicy = new LimitCheckingItemSkipPolicy();
	private Classifier<Throwable, Boolean> rollbackClassifier = new BinaryExceptionClassifier(true);
	private final BatchRetryTemplate batchRetryTemplate;
	private ChunkMonitor chunkMonitor = new ChunkMonitor();
	private boolean hasProcessor = false;

	public JsrFaultTolerantChunkProcessor(ItemReader<? extends I> reader, ItemProcessor<? super I, ? extends O> processor, ItemWriter<? super O> writer, RepeatOperations repeatTemplate, BatchRetryTemplate batchRetryTemplate) {
		super(reader, processor, writer, repeatTemplate);
		hasProcessor = processor != null;
		this.batchRetryTemplate = batchRetryTemplate;
	}

	/**
	 * @param skipPolicy a {@link SkipPolicy}
	 */
	public void setSkipPolicy(SkipPolicy skipPolicy) {
		Assert.notNull(skipPolicy, "A skip policy is required");

		this.skipPolicy = skipPolicy;
	}

	/**
	 * @param rollbackClassifier a {@link Classifier}
	 */
	public void setRollbackClassifier(Classifier<Throwable, Boolean> rollbackClassifier) {
		Assert.notNull(rollbackClassifier, "A rollbackClassifier is required");

		this.rollbackClassifier = rollbackClassifier;
	}

	/**
	 * @param chunkMonitor a {@link ChunkMonitor}
	 */
	public void setChunkMonitor(ChunkMonitor chunkMonitor) {
		Assert.notNull(chunkMonitor, "A chunkMonitor is required");

		this.chunkMonitor = chunkMonitor;
	}

	/**
	 * Register some {@link StepListener}s with the handler. Each will get the
	 * callbacks in the order specified at the correct stage.
	 *
	 * @param listeners listeners to be registered
	 */
	@Override
	public void setListeners(List<? extends StepListener> listeners) {
		for (StepListener listener : listeners) {
			registerListener(listener);
		}
	}

	/**
	 * Register a listener for callbacks at the appropriate stages in a process.
	 *
	 * @param listener a {@link StepListener}
	 */
	@Override
	public void registerListener(StepListener listener) {
		getListener().register(listener);
	}

	/**
	 * Adds retry and skip logic to the reading phase of the chunk loop.
	 *
	 * @param contribution a {@link StepContribution}
	 * @param chunk a {@link Chunk}
	 * @return I an item
	 * @throws Exception thrown if error occurs.
	 */
	@Override
	protected I provide(final StepContribution contribution, final Chunk<I> chunk) throws Exception {
		RetryCallback<I, Exception> retryCallback = new RetryCallback<I, Exception>() {

			@Override
			public I doWithRetry(RetryContext arg0) throws Exception {
				while (true) {
					try {
						return doProvide(contribution, chunk);
					}
					catch (Exception e) {
						if (shouldSkip(skipPolicy, e, contribution.getStepSkipCount())) {

							// increment skip count and try again
							contribution.incrementReadSkipCount();
							chunk.skip(e);

							getListener().onSkipInRead(e);

							logger.debug("Skipping failed input", e);
						}
						else {
							getListener().onRetryReadException(e);

							if(rollbackClassifier.classify(e)) {
								throw e;
							}
							else {
								throw e;
							}
						}
					}
				}
			}
		};

		RecoveryCallback<I> recoveryCallback = new RecoveryCallback<I>() {

			@Override
			public I recover(RetryContext context) throws Exception {
				Throwable e = context.getLastThrowable();
				if (shouldSkip(skipPolicy, e, contribution.getStepSkipCount())) {
					contribution.incrementReadSkipCount();
					logger.debug("Skipping after failed process", e);
					return null;
				}
				else {
					if (rollbackClassifier.classify(e)) {
						// Default is to rollback unless the classifier
						// allows us to continue
						throw new RetryException("Non-skippable exception in recoverer while reading", e);
					}

					throw new BatchRuntimeException(e);
				}
			}

		};

		return batchRetryTemplate.execute(retryCallback, recoveryCallback);
	}

	/**
	 * Convenience method for calling process skip policy.
	 *
	 * @param policy the skip policy
	 * @param e the cause of the skip
	 * @param skipCount the current skip count
	 */
	private boolean shouldSkip(SkipPolicy policy, Throwable e, int skipCount) {
		try {
			return policy.shouldSkip(e, skipCount);
		}
		catch (SkipException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new SkipPolicyFailedException("Fatal exception in SkipPolicy.", ex, e);
		}
	}

	/**
	 * Adds retry and skip logic to the process phase of the chunk loop.
	 *
	 * @param contribution a {@link StepContribution}
	 * @param item an item to be processed
	 * @return O an item that has been processed if a processor is available
	 * @throws Exception thrown if error occurs.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected O transform(final StepContribution contribution, final I item) throws Exception {
		if (!hasProcessor) {
			return (O) item;
		}

		RetryCallback<O, Exception> retryCallback = new RetryCallback<O, Exception>() {

			@Override
			public O doWithRetry(RetryContext context) throws Exception {
				try {
					return doTransform(item);
				}
				catch (Exception e) {
					if (shouldSkip(skipPolicy, e, contribution.getStepSkipCount())) {
						// If we are not re-throwing then we should check if
						// this is skippable
						contribution.incrementProcessSkipCount();
						logger.debug("Skipping after failed process with no rollback", e);
						// If not re-throwing then the listener will not be
						// called in next chunk.
						getListener().onSkipInProcess(item, e);
					} else {
						getListener().onRetryProcessException(item, e);

						if (rollbackClassifier.classify(e)) {
							// Default is to rollback unless the classifier
							// allows us to continue
							throw e;
						}
						else {
							throw e;
						}
					}
				}
				return null;
			}

		};

		RecoveryCallback<O> recoveryCallback = new RecoveryCallback<O>() {
			@Override
			public O recover(RetryContext context) throws Exception {
				Throwable e = context.getLastThrowable();
				if (shouldSkip(skipPolicy, e, contribution.getStepSkipCount())) {
					contribution.incrementProcessSkipCount();
					logger.debug("Skipping after failed process", e);
					return null;
				}
				else {
					if (rollbackClassifier.classify(e)) {
						// Default is to rollback unless the classifier
						// allows us to continue
						throw new RetryException("Non-skippable exception in recoverer while processing", e);
					}

					throw new BatchRuntimeException(e);
				}
			}
		};

		return batchRetryTemplate.execute(retryCallback, recoveryCallback);
	}

	/**
	 * Adds retry and skip logic to the write phase of the chunk loop.
	 *
	 * @param contribution a {@link StepContribution}
	 * @param chunk a {@link Chunk}
	 * @throws Exception thrown if error occurs.
	 */
	@Override
	protected void persist(final StepContribution contribution, final Chunk<O> chunk) throws Exception {

		RetryCallback<Object, Exception> retryCallback = new RetryCallback<Object, Exception>() {
			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public Object doWithRetry(RetryContext context) throws Exception {

				chunkMonitor.setChunkSize(chunk.size());
				try {
					doPersist(contribution, chunk);
				}
				catch (Exception e) {
					if (shouldSkip(skipPolicy, e, contribution.getStepSkipCount())) {
						// Per section 9.2.7 of JSR-352, the SkipListener receives all the items within the chunk 						 
						((MulticasterBatchListener) getListener()).onSkipInWrite(chunk.getItems(), e);
					} else {
						getListener().onRetryWriteException((List<Object>) chunk.getItems(), e);

						if (rollbackClassifier.classify(e)) {
							throw e;
						}
					}
					/*
					 * If the exception is marked as no-rollback, we need to
					 * override that, otherwise there's no way to write the
					 * rest of the chunk or to honour the skip listener
					 * contract.
					 */
					throw new ForceRollbackForWriteSkipException(
							"Force rollback on skippable exception so that skipped item can be located.", e);
				}
				contribution.incrementWriteCount(chunk.size());
				return null;

			}
		};

		RecoveryCallback<Object> recoveryCallback = new RecoveryCallback<Object>() {

			@Override
			public O recover(RetryContext context) throws Exception {
				Throwable e = context.getLastThrowable();
				if (shouldSkip(skipPolicy, e, contribution.getStepSkipCount())) {
					contribution.incrementWriteSkipCount();
					logger.debug("Skipping after failed write", e);
					return null;
				}
				else {
					if (rollbackClassifier.classify(e)) {
						// Default is to rollback unless the classifier
						// allows us to continue
						throw new RetryException("Non-skippable exception in recoverer while write", e);
					}
					return null;
				}
			}

		};

		batchRetryTemplate.execute(retryCallback, recoveryCallback);
	}
}
