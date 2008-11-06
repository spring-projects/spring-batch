package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.springframework.batch.retry.RetryOperations;
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
 * @param <I> input item type
 * @param <O> output item type
 * 
 * @author Robert Kasanicky
 */
public class NonbufferingFaultTolerantChunkOrientedTasklet<I, O> extends
		AbstractFaultTolerantChunkOrientedTasklet<I, O> {

	public NonbufferingFaultTolerantChunkOrientedTasklet(ItemReader<? extends I> itemReader,
			ItemProcessor<? super I, ? extends O> itemProcessor, ItemWriter<? super O> itemWriter,
			RepeatOperations chunkOperations, RetryOperations retryTemplate,
			Classifier<Throwable, Boolean> rollbackClassifier, ItemSkipPolicy readSkipPolicy,
			ItemSkipPolicy writeSkipPolicy, ItemSkipPolicy processSkipPolicy) {

		super(itemReader, itemProcessor, itemWriter, retryTemplate, readSkipPolicy, processSkipPolicy, writeSkipPolicy,
				rollbackClassifier, chunkOperations);
	}

	/**
	 * Read-process-write a list of items. Uses fault-tolerant read, process and
	 * write implementations.
	 */
	public ExitStatus execute(final StepContribution contribution, AttributeAccessor attributes) throws Exception {
		ExitStatus result = ExitStatus.CONTINUABLE;
		final List<I> inputs = new ArrayList<I>();

		final List<Exception> skippedReads = getBufferedList(attributes, SKIPPED_READS_KEY);
		result = getRepeatOperations().iterate(new RepeatCallback() {

			public ExitStatus doInIteration(final RepeatContext context) throws Exception {
				I item = read(contribution, skippedReads);

				if (item == null) {
					return ExitStatus.FINISHED;
				}
				inputs.add(item);
				contribution.incrementReadCount();
				return ExitStatus.CONTINUABLE;
			}
		});

		// filter inputs marked for skipping
		final Map<I, Exception> skippedInputs = getBufferedSkips(attributes, SKIPPED_INPUTS_KEY);
		final Map<O, Exception> skippedOutputs = getBufferedSkips(attributes, SKIPPED_OUTPUTS_KEY);
		final Set<I> inputsIncludingSkips = new HashSet<I>(inputs.size());
		final Set<O> outputsIncludingSkips = new HashSet<O>(inputs.size());

		if (!inputs.isEmpty()) {
			inputsIncludingSkips.addAll(inputs);
			inputs.removeAll(skippedInputs.keySet());

			final List<O> outputs = new ArrayList<O>();
			process(contribution, inputs, outputs, skippedInputs);

			// filter outputs marked for skipping
			outputsIncludingSkips.addAll(outputs);
			outputs.removeAll(skippedOutputs.keySet());

			write(outputs, contribution, skippedOutputs);
		}

		callSkipListenersAndCleanSkipsFromBuffer(skippedReads, skippedInputs, skippedOutputs, inputsIncludingSkips,
				outputsIncludingSkips);

		return result;
	}

	/**
	 * Identify items successfully skipped in this tasklet iteration, call skip
	 * listeners and remove skips from buffer. This requires care, because we
	 * might be processing a different chunk after rollback i.e. items marked
	 * for skipping from previous tasklet iteration may not have been
	 * encountered now.
	 */
	private void callSkipListenersAndCleanSkipsFromBuffer(final List<Exception> skippedReads,
			final Map<I, Exception> skippedInputs, final Map<O, Exception> skippedOutputs,
			final Set<I> inputsIncludingSkips, final Set<O> outputsIncludingSkips) {
		for (Exception skippedReadException : skippedReads) {
			try {
				listener.onSkipInRead(skippedReadException);
			}
			catch (RuntimeException e) {
				throw new SkipListenerFailedException("Fatal exception in SkipListener.", e, skippedReadException);
			}
		}
		skippedReads.clear();
		for (I input : inputsIncludingSkips) {
			if (skippedInputs.containsKey(input)) {
				try {
					listener.onSkipInProcess(input, skippedInputs.get(input));
				}
				catch (RuntimeException ex) {
					throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, skippedInputs
							.get(input));
				}
				skippedInputs.remove(input);
			}
		}
		for (O output : outputsIncludingSkips) {
			if (skippedOutputs.containsKey(output)) {
				try {
					listener.onSkipInWrite(output, skippedOutputs.get(output));
				}
				catch (RuntimeException ex) {
					throw new SkipListenerFailedException("Fatal exception in skip listener", ex, skippedOutputs
							.get(output));
				}
				skippedOutputs.remove(output);
			}
		}
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
	protected I read(StepContribution contribution, final List<Exception> skipped) throws Exception {

		try {
			return doRead();
		}
		catch (Exception e) {

			if (getReadSkipPolicy().shouldSkip(e, contribution.getStepSkipCount())) {
				// increment skip count and try again
				contribution.incrementReadSkipCount();
				skipped.add(e);
				logger.debug("Skipping failed input", e);
			}

			throw e;
		}

	}

}
