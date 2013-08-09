/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.step.item;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.step.item.BatchRetryTemplate;
import org.springframework.batch.core.step.item.Chunk;
import org.springframework.batch.core.step.item.ChunkMonitor;
import org.springframework.batch.core.step.item.ForceRollbackForWriteSkipException;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableProcessException;
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

/**
 * Extension of the {@link JsrChunkProcessor} that adds skip and retry functionality.
 *
 * @author Michael Minella
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

	public JsrFaultTolerantChunkProcessor() {
		this(null, null, null, null, null);
	}

	public JsrFaultTolerantChunkProcessor(ItemReader<I> reader, ItemProcessor<I,O> processor, ItemWriter<O> writer, RepeatOperations repeatTemplate, BatchRetryTemplate batchRetryTemplate) {
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
	 * @param listeners
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
	 * @throws Exception
	 */
	@Override
	protected I provide(final StepContribution contribution, final Chunk<I> chunk) throws Exception {
		RetryCallback<I> retryCallback = new RetryCallback<I>() {

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

							logger.debug("Skipping failed input", e);
						}
						else {
							if (rollbackClassifier.classify(e)) {
								throw e;
							}
							logger.debug("No-rollback for non-skippable exception (ignored)", e);
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
					return null;
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
	 * @throws Exception
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected O transform(final StepContribution contribution, final I item) throws Exception {
		if (!hasProcessor) {
			O result = (O) item;
			return result;
		}

		RetryCallback<O> retryCallback = new RetryCallback<O>() {

			@Override
			public O doWithRetry(RetryContext context) throws Exception {
				try {
					return doTransform(item);
				}
				catch (Exception e) {
					if (rollbackClassifier.classify(e)) {
						// Default is to rollback unless the classifier
						// allows us to continue
						throw e;
					}
					else if (shouldSkip(skipPolicy, e, contribution.getStepSkipCount())) {
						// If we are not re-throwing then we should check if
						// this is skippable
						contribution.incrementProcessSkipCount();
						logger.debug("Skipping after failed process with no rollback", e);
						// If not re-throwing then the listener will not be
						// called in next chunk.
						getListener().onSkipInProcess(item, e);
					}
					else {
						// If it's not skippable that's an error in
						// configuration - it doesn't make sense to not roll
						// back if we are also not allowed to skip
						throw new NonSkippableProcessException(
								"Non-skippable exception in processor.  Make sure any exceptions that do not cause a rollback are skippable.",
								e);
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
					return null;
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
	 * @throws Exception
	 */
	@Override
	protected void persist(final StepContribution contribution, final Chunk<O> chunk) throws Exception {

		RetryCallback<Object> retryCallback = new RetryCallback<Object>() {
			@Override
			public Object doWithRetry(RetryContext context) throws Exception {

				chunkMonitor.setChunkSize(chunk.size());
				try {
					doPersist(contribution, chunk);
				}
				catch (Exception e) {
					if (rollbackClassifier.classify(e)) {
						throw e;
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
