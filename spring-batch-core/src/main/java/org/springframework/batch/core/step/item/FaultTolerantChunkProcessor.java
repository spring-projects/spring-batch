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

package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.classify.BinaryExceptionClassifier;
import org.springframework.batch.classify.Classifier;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableProcessException;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.retry.ExhaustedRetryException;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.support.DefaultRetryState;

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
	 * @param rollbackClassifier
	 */
	public void setRollbackClassifier(Classifier<Throwable, Boolean> rollbackClassifier) {
		this.rollbackClassifier = rollbackClassifier;
	}

	/**
	 * @param chunkMonitor
	 */
	public void setChunkMonitor(ChunkMonitor chunkMonitor) {
		this.chunkMonitor = chunkMonitor;
	}

	/**
	 * A flag to indicate that items have been buffered and therefore will
	 * always come back as a chunk after a rollback. Otherwise things are more
	 * complicated because after a rollback the new chunk might or moght not
	 * contain items from the previous failed chunk.
	 * 
	 * @param buffering
	 */
	public void setBuffering(boolean buffering) {
		this.buffering = buffering;
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
			data = new UserData<O>(inputs.size());
			inputs.setUserData(data);
			data.setOutputs(new Chunk<O>());
		}
	}

	@Override
	protected int getFilterCount(Chunk<I> inputs, Chunk<O> outputs) {
		@SuppressWarnings("unchecked")
		UserData<O> data = (UserData<O>) inputs.getUserData();
		return data.size() - outputs.size() - inputs.getSkips().size();
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

		Chunk<O> next = new Chunk<O>(outputs.getItems(), previous.getSkips());
		next.setBusy(previous.isBusy());

		// Remember for next time if there are skips accumulating
		data.setOutputs(next);

		return next;

	}

	@Override
	protected Chunk<O> transform(final StepContribution contribution, Chunk<I> inputs) throws Exception {

		Chunk<O> outputs = new Chunk<O>();
		@SuppressWarnings("unchecked")
		UserData<O> data = (UserData<O>) inputs.getUserData();
		Chunk<O> cache = data.getOutputs();
		final Chunk<O>.ChunkIterator cacheIterator = cache.isEmpty() ? null : cache.iterator();
		final AtomicInteger count = new AtomicInteger(0);

		for (final Chunk<I>.ChunkIterator iterator = inputs.iterator(); iterator.hasNext();) {

			final I item = iterator.next();

			RetryCallback<O> retryCallback = new RetryCallback<O>() {

				public O doWithRetry(RetryContext context) throws Exception {
					O output = null;
					try {
						count.incrementAndGet();
						O cached = (cacheIterator != null) ? cacheIterator.next() : null;
						if (cached != null && count.get() > 1) {
							/*
							 * If there is a cached chunk then we must be
							 * scanning for errors in the writer, in which case
							 * only the first one will be written, and for the
							 * rest we need to fill in the output from the
							 * cache.
							 */
							output = cached;
						}
						else {
							output = doProcess(item);
						}
					}
					catch (Exception e) {
						if (rollbackClassifier.classify(e)) {
							// Default is to rollback unless the classifier
							// allows us to continue
							throw e;
						}
						else if (itemProcessSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
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
					if (output == null) {
						// No need to re-process filtered items
						iterator.remove();
					}
					return output;
				}

			};

			RecoveryCallback<O> recoveryCallback = new RecoveryCallback<O>() {

				public O recover(RetryContext context) throws Exception {
					Exception e = context.getLastThrowable();
					if (itemProcessSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
						contribution.incrementProcessSkipCount();
						iterator.remove(e);
						logger.debug("Skipping after failed process", e);
						return null;
					}
					else {
						throw new RetryException("Non-skippable exception in recoverer while processing", e);
					}
				}

			};

			O output = batchRetryTemplate.execute(retryCallback, recoveryCallback, new DefaultRetryState(
					getInputKey(item), rollbackClassifier));
			if (output != null) {
				outputs.add(output);
			}

		}

		return outputs;

	}

	@Override
	protected void write(final StepContribution contribution, final Chunk<I> inputs, final Chunk<O> outputs)
			throws Exception {

		RetryCallback<Object> retryCallback = new RetryCallback<Object>() {
			public Object doWithRetry(RetryContext context) throws Exception {

				if (!inputs.isBusy()) {
					chunkMonitor.setChunkSize(inputs.size());
					try {
						doWrite(outputs.getItems());
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
					contribution.incrementWriteCount(outputs.size());
				}
				else {
					scan(contribution, inputs, outputs, chunkMonitor);
				}
				return null;

			}
		};

		if (!buffering) {

			RecoveryCallback<Object> batchRecoveryCallback = new RecoveryCallback<Object>() {

				public Object recover(RetryContext context) throws Exception {

					Exception e = context.getLastThrowable();
					if (outputs.size() > 1 && !rollbackClassifier.classify(e)) {
						throw new RetryException("Invalid retry state during write caused by "
								+ "exception that does not classify for rollback: ", e);
					}

					Chunk<I>.ChunkIterator inputIterator = inputs.iterator();
					for (Chunk<O>.ChunkIterator outputIterator = outputs.iterator(); outputIterator.hasNext();) {

						inputIterator.next();
						outputIterator.next();

						checkSkipPolicy(inputIterator, outputIterator, e, contribution);
						if (!rollbackClassifier.classify(e)) {
							throw new RetryException(
									"Invalid retry state during recovery caused by exception that does not classify for rollback: ",
									e);
						}

					}

					return null;

				}

			};

			batchRetryTemplate.execute(retryCallback, batchRecoveryCallback, BatchRetryTemplate.createState(
					getInputKeys(inputs), rollbackClassifier));

		}
		else {

			RecoveryCallback<Object> recoveryCallback = new RecoveryCallback<Object>() {

				public Object recover(RetryContext context) throws Exception {

					/*
					 * If the last exception was not skippable we don't need to
					 * do any scanning. We can just bomb out with a retry
					 * exhausted.
					 */
					if (!itemWriteSkipPolicy.shouldSkip(context.getLastThrowable(), -1)) {
						throw new ExhaustedRetryException(
								"Retry exhausted after last attempt in recovery path, but exception is not skippable.",
								context.getLastThrowable());
					}

					inputs.setBusy(true);
					scan(contribution, inputs, outputs, chunkMonitor);
					return null;
				}

			};

			logger.debug("Attempting to write: " + inputs);
			batchRetryTemplate.execute(retryCallback, recoveryCallback, new DefaultRetryState(inputs,
					rollbackClassifier));

		}

		callSkipListeners(inputs, outputs);

	}

	private void callSkipListeners(final Chunk<I> inputs, final Chunk<O> outputs) {

		for (SkipWrapper<I> wrapper : inputs.getSkips()) {
			I item = wrapper.getItem();
			if (item == null) {
				continue;
			}
			Exception e = wrapper.getException();
			callProcessSkipListener(item, e);
		}

		for (SkipWrapper<O> wrapper : outputs.getSkips()) {
			Exception e = wrapper.getException();
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
	private void callProcessSkipListener(I item, Exception e) {
		try {
			getListener().onSkipInProcess(item, e);
		}
		catch (RuntimeException ex) {
			throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, e);
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
		List<Object> keys = new ArrayList<Object>();
		for (I item : inputs.getItems()) {
			keys.add(keyGenerator.getKey(item));
		}
		return keys;
	}

	private void checkSkipPolicy(Chunk<I>.ChunkIterator inputIterator, Chunk<O>.ChunkIterator outputIterator,
			Exception e, StepContribution contribution) {
		logger.debug("Checking skip policy after failed write");
		if (itemWriteSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
			contribution.incrementWriteSkipCount();
			inputIterator.remove();
			outputIterator.remove(e);
			logger.debug("Skipping after failed write", e);
		}
		else {
			throw new RetryException("Non-skippable exception in recoverer", e);
		}
	}

	private void scan(final StepContribution contribution, final Chunk<I> inputs, final Chunk<O> outputs,
			ChunkMonitor chunkMonitor) throws Exception {

		logger.debug("Scanning for failed item on write: " + inputs);
		if (outputs.isEmpty()) {
			inputs.setBusy(false);
			return;
		}

		Chunk<I>.ChunkIterator inputIterator = inputs.iterator();
		Chunk<O>.ChunkIterator outputIterator = outputs.iterator();

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
			if (!itemWriteSkipPolicy.shouldSkip(e, -1) && !rollbackClassifier.classify(e)) {
				inputIterator.remove();
				outputIterator.remove();
			}
			else {
				checkSkipPolicy(inputIterator, outputIterator, e, contribution);
			}
			if (rollbackClassifier.classify(e)) {
				throw e;
			}
		}
		chunkMonitor.incrementOffset();
		if (outputs.isEmpty()) {
			inputs.setBusy(false);
			chunkMonitor.resetOffset();
		}
	}

	private static class UserData<O> {

		private final int size;

		private Chunk<O> outputs;

		public UserData(int size) {
			this.size = size;
		}

		public int size() {
			return size;
		}

		public Chunk<O> getOutputs() {
			return outputs;
		}

		public void setOutputs(Chunk<O> outputs) {
			this.outputs = outputs;
		}

	}

}
