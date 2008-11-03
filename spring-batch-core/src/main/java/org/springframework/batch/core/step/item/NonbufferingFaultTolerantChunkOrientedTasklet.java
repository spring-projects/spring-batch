package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.skip.ItemSkipPolicy;
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
 * @author Robert Kasanicky
 * 
 * @param <I> input item type
 * @param <O> output item type
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
		inputs.removeAll(skippedInputs.keySet());

		// If there is no input we don't have to do anything more
		if (inputs.isEmpty()) {
			return result;
		}

		final List<O> outputs = new ArrayList<O>();
		process(contribution, inputs, outputs, skippedInputs);

		// filter outputs marked for skipping
		final Map<O, Exception> skippedOutputs = getBufferedSkips(attributes, SKIPPED_OUTPUTS_KEY);
		outputs.removeAll(skippedOutputs.keySet());

		write(outputs, contribution, skippedOutputs);

		callSkipListeners(skippedReads, skippedInputs, skippedOutputs);

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
	private I read(StepContribution contribution, final List<Exception> skipped) throws Exception {

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
