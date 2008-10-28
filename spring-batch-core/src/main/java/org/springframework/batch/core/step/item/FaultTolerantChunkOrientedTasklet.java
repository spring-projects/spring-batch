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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
 * ItemProcessor is assumed to be transactional. In case of rollback caused by
 * error on write the processing phase will be repeated.
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

	private static final String SKIPPED_INPUTS_KEY = "SKIPPED_INPUTS_BUFFER_KEY";

	private static final String SKIPPED_READS_KEY = "SKIPPED_READS_BUFFER_KEY";

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
	 * Get the next item from {@link #read(StepContribution, List)} and if not null
	 * pass the item to {@link #write(List, StepContribution, Map)}. If the
	 * {@link ItemProcessor} returns null, the write is omitted and another item
	 * taken from the reader.
	 * 
	 * @see org.springframework.batch.core.step.tasklet.Tasklet#execute(org.springframework.batch.core.StepContribution,
	 * AttributeAccessor)
	 */
	public ExitStatus execute(final StepContribution contribution, AttributeAccessor attributes) throws Exception {

		final List<T> inputs = getBuffer(attributes, INPUT_BUFFER_KEY);
		final List<S> outputs = new ArrayList<S>();

		ExitStatus result = ExitStatus.CONTINUABLE;

		final List<Exception> skippedReads = getBuffer(attributes, SKIPPED_READS_KEY);

		if (inputs.isEmpty() && outputs.isEmpty()) {

			result = repeatOperations.iterate(new RepeatCallback() {
				public ExitStatus doInIteration(final RepeatContext context) throws Exception {
					T item = read(contribution, skippedReads);

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

		}

		Map<T, Exception> skippedInputs = getSkippedBuffer(attributes, SKIPPED_INPUTS_KEY);
		if (!inputs.isEmpty()) {
			inputs.removeAll(skippedInputs.keySet());
			process(contribution, inputs, outputs, skippedInputs);
		}

		Map<S, Exception> skippedOutputs = getSkippedBuffer(attributes, SKIPPED_OUTPUTS_KEY);
		outputs.removeAll(skippedOutputs.keySet());
		write(outputs, contribution, skippedOutputs);

		for (Exception e : skippedReads) {
			try {
				listener.onSkipInRead(e);
			}
			catch (RuntimeException ex) {
				throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, e);
			}
		}
		for (Entry<T, Exception> skip : skippedInputs.entrySet()) {
			try {
				listener.onSkipInProcess(skip.getKey(), skip.getValue());
			}
			catch (RuntimeException ex) {
				throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, skip.getValue());
			}
		}

		for (Entry<S, Exception> skip : skippedOutputs.entrySet()) {
			try {
				listener.onSkipInWrite(skip.getKey(), skip.getValue());
			}
			catch (RuntimeException ex) {
				throw new SkipListenerFailedException("Fatal exception in skip listener", ex, skip.getValue());
			}
		}

		// On successful completion clear the attributes to signal that there is
		// no more processing
		if (outputs.isEmpty()) {
			for (String key : attributes.attributeNames()) {
				attributes.removeAttribute(key);
			}
			inputs.clear();
			outputs.clear();
			skippedInputs.clear();
			skippedOutputs.clear();
		}

		return result;

	}

	/**
	 * Tries to read the item from the reader, in case of exception skip the
	 * item if the skip policy allows, otherwise re-throw.
	 * 
	 * @param contribution current StepContribution holding skipped items count
	 * @param skippedReads
	 * @return next item for processing
	 */
	protected T read(StepContribution contribution, List<Exception> skippedReads) throws Exception {

		while (true) {
			try {
				return doRead();
			}
			catch (Exception e) {

				if (readSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
					// increment skip count and try again
					contribution.incrementReadSkipCount();
					skippedReads.add(e);

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
	protected void process(final StepContribution contribution, final List<T> inputs, final List<S> outputs,
			final Map<T, Exception> skippedInputs) throws Exception {

		int filtered = 0;

		for (final T item : inputs) {

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
						skippedInputs.put(item, e);
						logger.debug("Skipping after failed process", e);
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
	protected void write(final List<S> chunk, final StepContribution contribution, final Map<S, Exception> skipped)
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
				Exception le = (Exception) context.getLastThrowable();
				if (!rollbackClassifier.classify(le)) {
					throw new RetryException(
							"Invalid retry state caused during write by exception that does not classify for rollback: ", le);
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
							throw new RetryException(
									"Invalid retry state caused during recovery by exception that does not classify for rollback: ", e);
						}
					}
				}

				return null;

			}

			private void checkSkipPolicy(S item, Exception e, StepContribution contribution) {
				if (writeSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
					contribution.incrementWriteSkipCount();
					skipped.put(item, e);
					logger.debug("Skipping after failed write", e);
				}
				else {
					throw new RetryException("Non-skippable exception in recoverer", e);
				}
			}

		};

		retryOperations.execute(retryCallback, recoveryCallback, new DefaultRetryState(skipped, rollbackClassifier));

		chunk.clear();

	}

	private static <W> List<W> getBuffer(AttributeAccessor attributes, String key) {
		if (!attributes.hasAttribute(key)) {
			List<W> emptyList = new ArrayList<W>();
			attributes.setAttribute(key, emptyList);
			return emptyList;
		}
		@SuppressWarnings("unchecked")
		List<W> resource = (List<W>) attributes.getAttribute(key);
		return resource;
	}

	private static <E> Map<E, Exception> getSkippedBuffer(AttributeAccessor attributes, String key) {
		if (!attributes.hasAttribute(key)) {
			Map<E, Exception> emptyMap = new LinkedHashMap<E, Exception>();
			attributes.setAttribute(key, emptyMap);
			return emptyMap;
		}
		@SuppressWarnings("unchecked")
		Map<E, Exception> resource = (Map<E, Exception>) attributes.getAttribute(key);
		return resource;
	}

}
