/*
 * Copyright 2006-2020 the original author or authors.
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepListenerFailedException;
import org.springframework.batch.core.metrics.BatchMetrics;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableProcessException;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.classify.Classifier;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryException;
import org.springframework.retry.support.DefaultRetryState;

/**
 * FaultTolerant implementation of the {@link ChunkProcessor} interface, that
 * allows for skipping or retry of items that cause exceptions during writing.
 *
 */
public class FaultTolerantChunkProcessor<I, O> extends SimpleChunkProcessor<I, O> {

	private SkipPolicy itemProcessSkipPolicy = new LimitCheckingItemSkipPolicy();

	private SkipPolicy itemWriteSkipPolicy = new LimitCheckingItemSkipPolicy();

	private final BatchRetryTemplate batchRetryTemplate;

	private Classifier<Throwable, Boolean> rollbackClassifier = new BinaryExceptionClassifier(true);

	private Log logger = LogFactory.getLog(getClass());

	private boolean buffering = true;

	private KeyGenerator keyGenerator;

	private ChunkMonitor chunkMonitor = new ChunkMonitor();

	private boolean processorTransactional = true;

	/**
	 * The {@link KeyGenerator} to use to identify failed items across rollback.
	 * Not used in the case of the {@link #setBuffering(boolean) buffering flag}
	 * being true (the default).
	 *
	 * @param keyGenerator the {@link KeyGenerator} to set
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	/**
	 * @param SkipPolicy the {@link SkipPolicy} for item processing
	 */
	public void setProcessSkipPolicy(SkipPolicy SkipPolicy) {
		this.itemProcessSkipPolicy = SkipPolicy;
	}

	/**
	 * @param SkipPolicy the {@link SkipPolicy} for item writing
	 */
	public void setWriteSkipPolicy(SkipPolicy SkipPolicy) {
		this.itemWriteSkipPolicy = SkipPolicy;
	}

	/**
	 * A classifier that can distinguish between exceptions that cause rollback
	 * (return true) or not (return false).
	 *
	 * @param rollbackClassifier classifier
	 */
	public void setRollbackClassifier(Classifier<Throwable, Boolean> rollbackClassifier) {
		this.rollbackClassifier = rollbackClassifier;
	}

	/**
	 * @param chunkMonitor monitor
	 */
	public void setChunkMonitor(ChunkMonitor chunkMonitor) {
		this.chunkMonitor = chunkMonitor;
	}

	/**
	 * A flag to indicate that items have been buffered and therefore will
	 * always come back as a chunk after a rollback. Otherwise things are more
	 * complicated because after a rollback the new chunk might or might not
	 * contain items from the previous failed chunk.
	 *
	 * @param buffering true if items will be buffered
	 */
	public void setBuffering(boolean buffering) {
		this.buffering = buffering;
	}

	/**
	 * Flag to say that the {@link ItemProcessor} is transactional (defaults to
	 * true). If false then the processor is only called once per item per
	 * chunk, even if there are rollbacks with retries and skips.
	 *
	 * @param processorTransactional the flag value to set
	 */
	public void setProcessorTransactional(boolean processorTransactional) {
		this.processorTransactional = processorTransactional;
	}

	public FaultTolerantChunkProcessor(ItemProcessor<? super I, ? extends O> itemProcessor,
			ItemWriter<? super O> itemWriter, BatchRetryTemplate batchRetryTemplate) {
		super(itemProcessor, itemWriter);
		this.batchRetryTemplate = batchRetryTemplate;
	}

	@Override
	protected void initializeUserData(Chunk<I> inputs) {
		@SuppressWarnings("unchecked")
		UserData<O> data = (UserData<O>) inputs.getUserData();
		if (data == null) {
			data = new UserData<>();
			inputs.setUserData(data);
			data.setOutputs(new Chunk<>());
		}
		else {
			// BATCH-2663: re-initialize filter count when scanning the chunk
			if (data.scanning()) {
				data.filterCount = 0;
			}
		}
	}

	@Override
	protected int getFilterCount(Chunk<I> inputs, Chunk<O> outputs) {
		@SuppressWarnings("unchecked")
		UserData<O> data = (UserData<O>) inputs.getUserData();
		return data.filterCount;
	}

	@Override
	protected boolean isComplete(Chunk<I> inputs) {

		/*
		 * Need to remember the write skips across transactions, otherwise they
		 * keep coming back. Since we register skips with the inputs they will
		 * not be processed again but the output skips need to be saved for
		 * registration later with the listeners. The inputs are going to be the
		 * same for all transactions processing the same chunk, but the outputs
		 * are not, so we stash them in user data on the inputs.
		 */

		@SuppressWarnings("unchecked")
		UserData<O> data = (UserData<O>) inputs.getUserData();
		Chunk<O> previous = data.getOutputs();

		return inputs.isEmpty() && previous.getSkips().isEmpty();

	}

	@Override
	protected Chunk<O> getAdjustedOutputs(Chunk<I> inputs, Chunk<O> outputs) {

		@SuppressWarnings("unchecked")
		UserData<O> data = (UserData<O>) inputs.getUserData();
		Chunk<O> previous = data.getOutputs();

		Chunk<O> next = new Chunk<>(outputs.getItems(), previous.getSkips());
		next.setBusy(previous.isBusy());

		// Remember for next time if there are skips accumulating
		data.setOutputs(next);

		return next;

	}

	@Override
	protected Chunk<O> transform(final StepContribution contribution, Chunk<I> inputs) throws Exception {

		Chunk<O> outputs = new Chunk<>();
		@SuppressWarnings("unchecked")
		final UserData<O> data = (UserData<O>) inputs.getUserData();
		final Chunk<O> cache = data.getOutputs();
		final Iterator<O> cacheIterator = cache.isEmpty() ? null : new ArrayList<>(cache.getItems()).iterator();
		final AtomicInteger count = new AtomicInteger(0);

		// final int scanLimit = processorTransactional && data.scanning() ? 1 :
		// 0;

		for (final Chunk<I>.ChunkIterator iterator = inputs.iterator(); iterator.hasNext();) {

			final I item = iterator.next();

			RetryCallback<O, Exception> retryCallback = new RetryCallback<O, Exception>() {

				@Override
				public O doWithRetry(RetryContext context) throws Exception {
					Timer.Sample sample = BatchMetrics.createTimerSample();
					String status = BatchMetrics.STATUS_SUCCESS;
					O output = null;
					try {
						count.incrementAndGet();
						O cached = (cacheIterator != null && cacheIterator.hasNext()) ? cacheIterator.next() : null;
						if (cached != null && !processorTransactional) {
							output = cached;
						}
						else {
							output = doProcess(item);
							if (output == null) {
								data.incrementFilterCount();
							} else if (!processorTransactional && !data.scanning()) {
								cache.add(output);
							}
						}
					}
					catch (Exception e) {
						status = BatchMetrics.STATUS_FAILURE;
						if (rollbackClassifier.classify(e)) {
							// Default is to rollback unless the classifier
							// allows us to continue
							throw e;
						}
						else if (shouldSkip(itemProcessSkipPolicy, e, contribution.getStepSkipCount())) {
							// If we are not re-throwing then we should check if
							// this is skippable
							contribution.incrementProcessSkipCount();
							logger.debug("Skipping after failed process with no rollback", e);
							// If not re-throwing then the listener will not be
							// called in next chunk.
							callProcessSkipListener(item, e);
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
					finally {
						stopTimer(sample, contribution.getStepExecution(), "item.process", status, "Item processing");
					}
					if (output == null) {
						// No need to re-process filtered items
						iterator.remove();
					}
					return output;
				}

			};

			RecoveryCallback<O> recoveryCallback = new RecoveryCallback<O>() {

				@Override
				public O recover(RetryContext context) throws Exception {
					Throwable e = context.getLastThrowable();
					if (shouldSkip(itemProcessSkipPolicy, e, contribution.getStepSkipCount())) {
						iterator.remove(e);
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
						iterator.remove(e);
						return null;
					}
				}

			};

			O output = batchRetryTemplate.execute(retryCallback, recoveryCallback, new DefaultRetryState(
					getInputKey(item), rollbackClassifier));
			if (output != null) {
				outputs.add(output);
			}

			/*
			 * We only want to process the first item if there is a scan for a
			 * failed item.
			 */
			if (data.scanning()) {
				while (cacheIterator != null && cacheIterator.hasNext()) {
					outputs.add(cacheIterator.next());
				}
				// Only process the first item if scanning
				break;
			}
		}

		return outputs;

	}

	@Override
	protected void write(final StepContribution contribution, final Chunk<I> inputs, final Chunk<O> outputs)
			throws Exception {
		@SuppressWarnings("unchecked")
		final UserData<O> data = (UserData<O>) inputs.getUserData();
		final AtomicReference<RetryContext> contextHolder = new AtomicReference<>();

		RetryCallback<Object, Exception> retryCallback = new RetryCallback<Object, Exception>() {
			@Override
			public Object doWithRetry(RetryContext context) throws Exception {
				contextHolder.set(context);

				if (!data.scanning()) {
					chunkMonitor.setChunkSize(inputs.size());
					Timer.Sample sample = BatchMetrics.createTimerSample();
					String status = BatchMetrics.STATUS_SUCCESS;
					try {
						doWrite(outputs.getItems());
					}
					catch (Exception e) {
						status = BatchMetrics.STATUS_FAILURE;
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
					finally {
						stopTimer(sample, contribution.getStepExecution(), "chunk.write", status, "Chunk writing");
					}
					contribution.incrementWriteCount(outputs.size());
				}
				else {
					scan(contribution, inputs, outputs, chunkMonitor, false);
				}
				return null;

			}
		};

		if (!buffering) {

			RecoveryCallback<Object> batchRecoveryCallback = new RecoveryCallback<Object>() {

				@Override
				public Object recover(RetryContext context) throws Exception {

					Throwable e = context.getLastThrowable();
					if (outputs.size() > 1 && !rollbackClassifier.classify(e)) {
						throw new RetryException("Invalid retry state during write caused by "
								+ "exception that does not classify for rollback: ", e);
					}

					Chunk<I>.ChunkIterator inputIterator = inputs.iterator();
					for (Chunk<O>.ChunkIterator outputIterator = outputs.iterator(); outputIterator.hasNext();) {

						inputIterator.next();
						outputIterator.next();

						checkSkipPolicy(inputIterator, outputIterator, e, contribution, true);
						if (!rollbackClassifier.classify(e)) {
							throw new RetryException(
									"Invalid retry state during recovery caused by exception that does not classify for rollback: ",
									e);
						}

					}

					return null;

				}

			};

			batchRetryTemplate.execute(retryCallback, batchRecoveryCallback,
					BatchRetryTemplate.createState(getInputKeys(inputs), rollbackClassifier));

		}
		else {

			RecoveryCallback<Object> recoveryCallback = new RecoveryCallback<Object>() {

				@Override
				public Object recover(RetryContext context) throws Exception {
					/*
					 * If the last exception was not skippable we don't need to
					 * do any scanning. We can just bomb out with a retry
					 * exhausted.
					 */
					if (!shouldSkip(itemWriteSkipPolicy, context.getLastThrowable(), -1)) {
						throw new ExhaustedRetryException(
								"Retry exhausted after last attempt in recovery path, but exception is not skippable.",
								context.getLastThrowable());
					}

					inputs.setBusy(true);
					data.scanning(true);
					scan(contribution, inputs, outputs, chunkMonitor, true);
					return null;
				}

			};

			if (logger.isDebugEnabled()) {
				logger.debug("Attempting to write: " + inputs);
			}
			try {
				batchRetryTemplate.execute(retryCallback, recoveryCallback, new DefaultRetryState(inputs,
						rollbackClassifier));
			}
			catch (Exception e) {
				RetryContext context = contextHolder.get();
				if (!batchRetryTemplate.canRetry(context)) {
					/*
					 * BATCH-1761: we need advance warning of the scan about to
					 * start in the next transaction, so we can change the
					 * processing behaviour.
					 */
					data.scanning(true);
				}
				throw e;
			}

		}

		callSkipListeners(inputs, outputs);

	}

	private void callSkipListeners(final Chunk<I> inputs, final Chunk<O> outputs) {

		for (SkipWrapper<I> wrapper : inputs.getSkips()) {
			I item = wrapper.getItem();
			if (item == null) {
				continue;
			}
			Throwable e = wrapper.getException();
			callProcessSkipListener(item, e);
		}

		for (SkipWrapper<O> wrapper : outputs.getSkips()) {
			Throwable e = wrapper.getException();
			try {
				getListener().onSkipInWrite(wrapper.getItem(), e);
			}
			catch (RuntimeException ex) {
				throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, e);
			}
		}

		// Clear skips if we are possibly going to process this chunk again
		outputs.clearSkips();
		inputs.clearSkips();

	}

	/**
	 * Convenience method for calling process skip listener, so that it can be
	 * called from multiple places.
	 *
	 * @param item the item that is skipped
	 * @param e the cause of the skip
	 */
	private void callProcessSkipListener(I item, Throwable e) {
		try {
			getListener().onSkipInProcess(item, e);
		}
		catch (RuntimeException ex) {
			throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, e);
		}
	}

	/**
	 * Convenience method for calling process skip policy, so that it can be
	 * called from multiple places.
	 *
	 * @param policy the skip policy
	 * @param e the cause of the skip
	 * @param skipCount the current skip count
	 */
	private boolean shouldSkip(SkipPolicy policy, Throwable e, int skipCount) {
		try {
			return policy.shouldSkip(e, skipCount);
		}
		catch (SkipLimitExceededException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new SkipListenerFailedException("Fatal exception in SkipPolicy.", ex, e);
		}
	}

	private Object getInputKey(I item) {
		if (keyGenerator == null) {
			return item;
		}
		return keyGenerator.getKey(item);
	}

	private List<?> getInputKeys(final Chunk<I> inputs) {
		if (keyGenerator == null) {
			return inputs.getItems();
		}
		List<Object> keys = new ArrayList<>();
		for (I item : inputs.getItems()) {
			keys.add(keyGenerator.getKey(item));
		}
		return keys;
	}

	private void checkSkipPolicy(Chunk<I>.ChunkIterator inputIterator, Chunk<O>.ChunkIterator outputIterator,
			Throwable e, StepContribution contribution, boolean recovery) throws Exception {
		logger.debug("Checking skip policy after failed write");
		if (shouldSkip(itemWriteSkipPolicy, e, contribution.getStepSkipCount())) {
			contribution.incrementWriteSkipCount();
			inputIterator.remove();
			outputIterator.remove(e);
			logger.debug("Skipping after failed write", e);
		}
		else {
			if (recovery) {
				// Only if already recovering should we check skip policy
				throw new RetryException("Non-skippable exception in recoverer", e);
			}
			else {
				if (e instanceof Exception) {
					throw (Exception) e;
				}
				else if (e instanceof Error) {
					throw (Error) e;
				}
				else {
					throw new RetryException("Non-skippable throwable in recoverer", e);
				}
			}
		}
	}

	private void scan(final StepContribution contribution, final Chunk<I> inputs, final Chunk<O> outputs,
			ChunkMonitor chunkMonitor, boolean recovery) throws Exception {

		@SuppressWarnings("unchecked")
		final UserData<O> data = (UserData<O>) inputs.getUserData();

		if (logger.isDebugEnabled()) {
			if (recovery) {
				logger.debug("Scanning for failed item on recovery from write: " + inputs);
			}
			else {
				logger.debug("Scanning for failed item on write: " + inputs);
			}
		}
		if (outputs.isEmpty() || inputs.isEmpty()) {
			data.scanning(false);
			inputs.setBusy(false);
			chunkMonitor.resetOffset();
			return;
		}

		Chunk<I>.ChunkIterator inputIterator = inputs.iterator();
		Chunk<O>.ChunkIterator outputIterator = outputs.iterator();

		if (!inputs.getSkips().isEmpty() && inputs.getItems().size() != outputs.getItems().size()) {
			if (outputIterator.hasNext()) {
				outputIterator.remove();
				return;
			}
		}

		List<O> items = Collections.singletonList(outputIterator.next());
		inputIterator.next();
		try {
			writeItems(items);
			// If successful we are going to return and allow
			// the driver to commit...
			doAfterWrite(items);
			contribution.incrementWriteCount(1);
			inputIterator.remove();
			outputIterator.remove();
		}
		catch (Exception e) {
			try {
				doOnWriteError(e, items);
			}
			finally {
				Throwable cause = e;
				if(e instanceof StepListenerFailedException) {
					cause = e.getCause();
				}

				if (!shouldSkip(itemWriteSkipPolicy, cause, -1) && !rollbackClassifier.classify(cause)) {
					inputIterator.remove();
					outputIterator.remove();
				}
				else {
					checkSkipPolicy(inputIterator, outputIterator, cause, contribution, recovery);
				}
				if (rollbackClassifier.classify(cause)) {
					throw (Exception) cause;
				}
			}
		}
		chunkMonitor.incrementOffset();
		if (outputs.isEmpty()) {
			data.scanning(false);
			inputs.setBusy(false);
			chunkMonitor.resetOffset();
		}
	}

	private static class UserData<O> {

		private Chunk<O> outputs;

		private int filterCount = 0;

		private boolean scanning;

		public boolean scanning() {
			return scanning;
		}

		public void scanning(boolean scanning) {
			this.scanning = scanning;
		}

		public void incrementFilterCount() {
			filterCount++;
		}

		public Chunk<O> getOutputs() {
			return outputs;
		}

		public void setOutputs(Chunk<O> outputs) {
			this.outputs = outputs;
		}

	}

}
