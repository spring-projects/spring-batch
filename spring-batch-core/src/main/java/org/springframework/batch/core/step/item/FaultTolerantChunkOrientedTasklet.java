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

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.skip.ItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableReadException;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.support.DefaultRetryState;
import org.springframework.batch.support.Classifier;
import org.springframework.core.AttributeAccessor;

/**
 * If there is an exception on input it is skipped if allowed. If there is an
 * exception on output, it will be re-thrown in any case, and the behaviour when
 * the item is next encountered depends on the retryable and skippable exception
 * configuration. If the exception is retryable the write will be attempted
 * again up to the retry limit. When retry attempts are exhausted the skip
 * listener is invoked and the skip count incremented. A retryable exception is
 * thus also effectively also implicitly skippable.
 * 
 * Known limitation: ItemProcessor is assumed to be non-transactional. In case
 * of rollback caused by error on write the processing phase will not be
 * repeated, only the failed write will.
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class FaultTolerantChunkOrientedTasklet<T, S> extends AbstractItemOrientedTasklet<T, S> {

	private static final String INPUT_BUFFER_KEY = "INPUT_BUFFER_KEY";

	private final RepeatOperations repeatOperations;

	final private RetryOperations retryOperations;

	final private ItemSkipPolicy readSkipPolicy;

	final private ItemSkipPolicy writeSkipPolicy;

	final private ItemSkipPolicy processSkipPolicy;

	final private Classifier<Throwable, Boolean> rollbackClassifier;

	private static final String SKIPPED_OUTPUTS_KEY = "SKIPPED_OUTPUTS_BUFFER_KEY";

	public FaultTolerantChunkOrientedTasklet(ItemReader<? extends T> itemReader,
			ItemProcessor<? super T, ? extends S> itemProcessor, ItemWriter<? super S> itemWriter,
			RepeatOperations chunkOperations, RetryOperations retryTemplate,
			Classifier<Throwable, Boolean> rollbackClassifier, ItemSkipPolicy readSkipPolicy,
			ItemSkipPolicy writeSkipPolicy, ItemSkipPolicy processSkipPolicy) {
		super(itemReader, itemProcessor, itemWriter);
		this.repeatOperations = chunkOperations;
		this.retryOperations = retryTemplate;
		this.rollbackClassifier = rollbackClassifier;
		this.readSkipPolicy = readSkipPolicy;
		this.writeSkipPolicy = writeSkipPolicy;
		this.processSkipPolicy = processSkipPolicy;
	}

	/**
	 * Get the next item from {@link #read(StepContribution)} and if not null
	 * pass the item to {@link #write(List, StepContribution, List)}. If the
	 * {@link ItemProcessor} returns null, the write is omitted and another item
	 * taken from the reader.
	 * 
	 * @see org.springframework.batch.core.step.tasklet.Tasklet#execute(org.springframework.batch.core.StepContribution,
	 * AttributeAccessor)
	 */
	public ExitStatus execute(final StepContribution contribution, AttributeAccessor attributes) throws Exception {

		// TODO: check flags to see if these need to be saved or not (e.g. JMS
		// not)
		final Chunk<T> inputs = getBuffer(attributes, INPUT_BUFFER_KEY);
		final List<S> outputs = new ArrayList<S>();

		ExitStatus result = ExitStatus.CONTINUABLE;

		if (inputs.isEmpty() && outputs.isEmpty()) {

			result = repeatOperations.iterate(new RepeatCallback() {
				public ExitStatus doInIteration(final RepeatContext context) throws Exception {
					T item = read(contribution);

					if (item == null) {
						return ExitStatus.FINISHED;
					}
					inputs.add(item);
					contribution.incrementReadCount();
					return ExitStatus.CONTINUABLE;
				}
			});

			// If there is no input we don't have to do anything more
			if (inputs.isEmpty()) {
				return result;
			}

			// store inputs
			attributes.setAttribute(INPUT_BUFFER_KEY, inputs);

		}

		if (!inputs.isEmpty()) {
			process(contribution, inputs, outputs);
		}

		List<S> skippedOutputs = getSkippedOutputsBuffer(attributes);
		// TODO: use ItemWriter interface properly
		// TODO: make sure exceptions get handled by the appropriate handler
		outputs.removeAll(skippedOutputs);
		write(outputs, contribution, skippedOutputs);

		// On successful completion clear the attributes to signal that there is
		// no more processing
		if (outputs.isEmpty()) {
			for (String key : attributes.attributeNames()) {
				attributes.removeAttribute(key);
			}
			inputs.clear();
			outputs.clear();
			skippedOutputs.clear();
		}

		return result;

	}

	/**
	 * Tries to read the item from the reader, in case of exception skip the
	 * item if the skip policy allows, otherwise re-throw.
	 * 
	 * @param contribution current StepContribution holding skipped items count
	 * @return next item for processing
	 */
	protected T read(StepContribution contribution) throws Exception {

		while (true) {
			try {
				return doRead();
			}
			catch (Exception e) {

				if (readSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
					// increment skip count and try again
					contribution.incrementReadSkipCount();
					try {
						listener.onSkipInRead(e);
					}
					catch (RuntimeException ex) {
						throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, e);
					}
					logger.debug("Skipping failed input", e);
				}
				else {
					throw new NonSkippableReadException("Non-skippable exception during read", e);
				}

			}
		}

	}

	/**
	 * 
	 * @param inputs the items to process
	 * @param outputs the items to write
	 * @param contribution current context
	 */
	/**
	 * Incorporate retry into the item processor stage.
	 */
	protected void process(final StepContribution contribution, final Chunk<T> inputs, final List<S> outputs)
			throws Exception {

		int filtered = 0;

		for (final Chunk<T>.ChunkIterator iterator = inputs.iterator(); iterator.hasNext();) {

			final T item = iterator.next();

			RetryCallback<S> retryCallback = new RetryCallback<S>() {

				public S doWithRetry(RetryContext context) throws Exception {
					S output = doProcess(item);
					return output;
				}

			};

			RecoveryCallback<S> recoveryCallback = new RecoveryCallback<S>() {

				public S recover(RetryContext context) throws Exception {
					Exception e = (Exception) context.getLastThrowable();
					if (processSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
						contribution.incrementProcessSkipCount();
						iterator.remove(e);
						return null;
					}
					else {
						throw new RetryException("Non-skippable exception in recoverer while processing", e);
					}
				}

			};

			S output = retryOperations.execute(retryCallback, recoveryCallback, new DefaultRetryState(item,
					rollbackClassifier));
			if (output != null) {
				outputs.add(output);
			}
			else {
				filtered++;
			}

		}

		for (ItemWrapper<T> skip : inputs.getSkips()) {
			Exception exception = skip.getException();
			try {
				listener.onSkipInProcess(skip.getItem(), exception);
			}
			catch (RuntimeException e) {
				throw new SkipListenerFailedException("Fatal exception in SkipListener.", e, exception);
			}
		}

		contribution.incrementFilterCount(filtered);

	}

	/**
	 * Execute the business logic, delegating to the writer.<br/>
	 * 
	 * Process the items with the {@link ItemWriter} in a stateful retry. Any
	 * {@link SkipListener} provided is called when retry attempts are
	 * exhausted. The listener callback (on write failure) will happen in the
	 * next transaction automatically.<br/>
	 */
	protected void write(final List<S> chunk, final StepContribution contribution, final List<S> skipped)
			throws Exception {

		RetryCallback<Object> retryCallback = new RetryCallback<Object>() {
			public Object doWithRetry(RetryContext context) throws Exception {
				doWrite(chunk);
				contribution.incrementWriteCount(chunk.size());
				return null;
			}
		};

		RecoveryCallback<Object> recoveryCallback = new RecoveryCallback<Object>() {

			public Object recover(RetryContext context) throws Exception {
				if (chunk.size() == 1) {
					Exception e = (Exception) context.getLastThrowable();
					S item = chunk.get(0);
					checkSkipPolicy(item, e, contribution);
					return null;
				}
				for (S item : chunk) {
						try {
							doWrite(Collections.singletonList(item));
							contribution.incrementWriteCount(1);
						}
						catch (Exception e) {
							checkSkipPolicy(item, e, contribution);
							if (rollbackClassifier.classify(e)) {
								throw e;
							}
							else {
								logger.error("Exception encountered that does not classify for rollback: ", e);
							}
						}
					}

				return null;

			}
			
			private void checkSkipPolicy(S item, Exception e, StepContribution contribution) {
				if (writeSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
					contribution.incrementWriteSkipCount();
					skipped.add(item);
					try {
						listener.onSkipInWrite(item, e);
					}
					catch (RuntimeException ex) {
						throw new SkipListenerFailedException("Fatal exception in skip listener", ex, e);
					}
				}
				else {
					throw new RetryException("Non-skippable exception in recoverer", e);
				}
			}

		};

		retryOperations.execute(retryCallback, recoveryCallback, new DefaultRetryState(skipped, rollbackClassifier));

		chunk.clear();

	}

	/**
	 * @param attributes
	 * @param inputBufferKey
	 * @return
	 */
	private static <W> Chunk<W> getBuffer(AttributeAccessor attributes, String key) {
		if (!attributes.hasAttribute(key)) {
			return new Chunk<W>();
		}
		@SuppressWarnings("unchecked")
		Chunk<W> resource = (Chunk<W>) attributes.getAttribute(key);
		return resource;
	}

	private List<S> getSkippedOutputsBuffer(AttributeAccessor attributes) {
		if (!attributes.hasAttribute(SKIPPED_OUTPUTS_KEY)) {
			List<S> result = new ArrayList<S>();
			attributes.setAttribute(SKIPPED_OUTPUTS_KEY, result);
			return result;
		}
		@SuppressWarnings("unchecked")
		List<S> resource = (List<S>) attributes.getAttribute(SKIPPED_OUTPUTS_KEY);
		return resource;
	}

}
