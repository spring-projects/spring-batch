package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.skip.ItemSkipPolicy;
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
 * Fault-tolerant chunk-oriented tasklet implementation which does not buffer
 * items that have been read - the assumption is that item reader is
 * transactional and will re-present the items after transaction rollback, while
 * item ordering might not be preserved (JMS).
 * 
 * Note that the implementation relies on {@link Object#equals(Object)}
 * comparisons for recognizing items on retry/skip.
 * 
 * @author Robert Kasanicky
 * 
 * @param <I> input item type
 * @param <O> output item type
 */
public class NonbufferingFaultTolerantChunkOrientedTasklet<I, O> extends AbstractItemOrientedTasklet<I, O> {

	private static final String SKIPPED_INPUTS_KEY = "SKIPPED_INPUTS_KEY";

	private static final String SKIPPED_OUTPUTS_KEY = "SKIPPED_OUTPUTS_KEY";

	private final RepeatOperations repeatOperations;

	private final RetryOperations retryOperations;

	private final ItemSkipPolicy readSkipPolicy;

	private final ItemSkipPolicy writeSkipPolicy;

	private final ItemSkipPolicy processSkipPolicy;

	private final Classifier<Throwable, Boolean> rollbackClassifier;

	public NonbufferingFaultTolerantChunkOrientedTasklet(ItemReader<? extends I> itemReader,
			ItemProcessor<? super I, ? extends O> itemProcessor, ItemWriter<? super O> itemWriter,
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
	 * 
	 * @param <T> buffer type
	 * @param attributes used to store the state of the tasklet
	 * @param key the key buffer is stored under in the attributes
	 * @return newly created or existing buffer stored under the given key
	 */
	private static <T> Set<T> getBuffer(AttributeAccessor attributes, String key) {
		Set<T> buffer;
		if (!attributes.hasAttribute(key)) {
			buffer = new HashSet<T>();
			attributes.setAttribute(key, buffer);
		}
		else {
			@SuppressWarnings("unchecked")
			Set<T> casted = (Set<T>) attributes.getAttribute(key);
			buffer = casted;
		}
		return buffer;
	}

	/**
	 * Read-process-write a list of items. Uses fault-tolerant read, process and
	 * write implementations.
	 */
	public ExitStatus execute(final StepContribution contribution, AttributeAccessor attributes) throws Exception {
		ExitStatus result = ExitStatus.CONTINUABLE;
		final List<I> inputs = new ArrayList<I>();

		result = repeatOperations.iterate(new RepeatCallback() {

			public ExitStatus doInIteration(final RepeatContext context) throws Exception {
				I item = read(contribution);

				if (item == null) {
					return ExitStatus.FINISHED;
				}
				inputs.add(item);
				contribution.incrementReadCount();
				return ExitStatus.CONTINUABLE;
			}
		});

		// filter inputs marked for skipping
		Set<I> skippedInputs = getBuffer(attributes, SKIPPED_INPUTS_KEY);
		inputs.removeAll(skippedInputs);

		// If there is no input we don't have to do anything more
		if (inputs.isEmpty()) {
			return result;
		}

		List<O> outputs = new ArrayList<O>();
		process(contribution, inputs, outputs, skippedInputs);

		// filter outputs marked for skipping
		Set<O> skippedOutputs = getBuffer(attributes, SKIPPED_OUTPUTS_KEY);
		outputs.removeAll(skippedOutputs);

		write(contribution, outputs, skippedOutputs);

		return result;
	}

	/**
	 * Tries to read the item from the reader, in case of exception skip the
	 * skip listener is called and exception is re-thrown (failed read causes
	 * rollback automatically because the reader is assumed to be
	 * transactional).
	 * 
	 * @param contribution current StepContribution holding skipped items count
	 * @return next item for processing
	 */
	private I read(StepContribution contribution) throws Exception {

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

			throw e;
		}

	}

	/**
	 * Incorporate retry and skip into the item processor stage. Any
	 * {@link SkipListener} provided is called when retry attempts are
	 * exhausted. Adds failed items into skipped inputs list so that they can be
	 * filtered if they are encountered again (after rollback).
	 * 
	 * @param skippedInputs container for items marked for skipping
	 */
	private void process(final StepContribution contribution, final List<I> inputs, final List<O> outputs,
			final Set<I> skippedInputs) throws Exception {

		int filtered = 0;

		for (final Iterator<I> iterator = inputs.iterator(); iterator.hasNext();) {

			final I item = iterator.next();

			RetryCallback<O> retryCallback = new RetryCallback<O>() {

				public O doWithRetry(RetryContext context) throws Exception {
					O output = doProcess(item);
					return output;
				}

			};

			RecoveryCallback<O> recoveryCallback = new RecoveryCallback<O>() {

				public O recover(RetryContext context) throws Exception {
					Exception e = (Exception) context.getLastThrowable();
					if (processSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
						contribution.incrementProcessSkipCount();
						skippedInputs.add(item);
						try {
							listener.onSkipInProcess(item, e);
						}
						catch (RuntimeException ex) {
							throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, e);
						}
						return null;
					}
					else {
						throw new RetryException("Non-skippable exception in recoverer while processing", e);
					}
				}

			};

			O output = retryOperations.execute(retryCallback, recoveryCallback, new DefaultRetryState(item,
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
	 * Write the items in a stateful retry. Any {@link SkipListener} provided is
	 * called when retry attempts are exhausted. The listener callback (on write
	 * failure) will happen in the next transaction automatically.
	 * 
	 * Adds failed items into skipped outputs list so that they can be filtered
	 * if they are encountered again (after rollback).
	 * 
	 * @param skippedOutputs container for items marked for skipping
	 */
	private void write(final StepContribution contribution, final List<O> outputs,  final Set<O> skippedOutputs)
			throws Exception {

		RetryCallback<Object> retryCallback = new RetryCallback<Object>() {
			public Object doWithRetry(RetryContext context) throws Exception {
				doWrite(outputs);
				contribution.incrementWriteCount(outputs.size());
				return null;
			}
		};

		RecoveryCallback<Object> recoveryCallback = new RecoveryCallback<Object>() {

			public Object recover(RetryContext context) throws Exception {

				for (final O item : outputs) {
					try {
						doWrite(Collections.singletonList(item));
						contribution.incrementWriteCount(1);
					}
					catch (Exception e) {
						if (writeSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
							contribution.incrementWriteSkipCount();
							skippedOutputs.add(item);
							try {
								listener.onSkipInWrite(item, e);
							}
							catch (RuntimeException ex) {
								throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, e);
							}
						}
						else {
							throw new RetryException("Non-skippable exception in recoverer", e);
						}
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

		};

		retryOperations.execute(retryCallback, recoveryCallback, new DefaultRetryState(outputs, rollbackClassifier));

	}

}
