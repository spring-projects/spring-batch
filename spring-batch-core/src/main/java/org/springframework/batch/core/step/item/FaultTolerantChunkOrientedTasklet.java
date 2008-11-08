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
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.skip.ItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableReadException;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.retry.RetryOperations;
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
 * <code>ItemProcessor</code> is assumed to be transactional. In case of
 * rollback caused by error on write the processing phase will be repeated.
 * 
 * @param <I> input item type
 * @param <O> output item type
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class FaultTolerantChunkOrientedTasklet<I, O> extends AbstractFaultTolerantChunkOrientedTasklet<I, O> {

	final static private String INPUT_BUFFER_KEY = "INPUT_BUFFER_KEY";

	public FaultTolerantChunkOrientedTasklet(ItemReader<? extends I> itemReader,
			ItemProcessor<? super I, ? extends O> itemProcessor, ItemWriter<? super O> itemWriter,
			RepeatOperations chunkOperations, RetryOperations retryTemplate,
			Classifier<Throwable, Boolean> rollbackClassifier, ItemSkipPolicy readSkipPolicy,
			ItemSkipPolicy writeSkipPolicy, ItemSkipPolicy processSkipPolicy) {

		super(itemReader, itemProcessor, itemWriter, retryTemplate, readSkipPolicy, processSkipPolicy, writeSkipPolicy,
				rollbackClassifier, chunkOperations);
	}

	/**
	 * Read the next chunk of items and if not empty pass the items one-by-one
	 * to {@link #process(StepContribution, List, List, Map)} and finally write
	 * all items by {@link #write(List, StepContribution, Map)}.
	 * 
	 * @see org.springframework.batch.core.step.tasklet.Tasklet#execute(org.springframework.batch.core.StepContribution,
	 * AttributeAccessor)
	 */
	public RepeatStatus execute(final StepContribution contribution, AttributeAccessor attributes) throws Exception {

		final List<I> inputs = getBufferedList(attributes, INPUT_BUFFER_KEY);
		final List<O> outputs = new ArrayList<O>();

		final List<Exception> skippedReads = getBufferedList(attributes, SKIPPED_READS_KEY);

		// TODO: invert logic below so that default can be FINISHED? 
		RepeatStatus continuable = RepeatStatus.CONTINUABLE;

		if (inputs.isEmpty() && outputs.isEmpty()) {

			continuable = getRepeatOperations().iterate(new RepeatCallback() {
				public RepeatStatus doInIteration(final RepeatContext context) throws Exception {
					I item = read(contribution, skippedReads);

					if (item == null) {
						return RepeatStatus.FINISHED;
					}
					inputs.add(item);
					contribution.incrementReadCount();
					return RepeatStatus.CONTINUABLE;
				}
			});
			
			ExitStatus status = continuable.isContinuable() ? ExitStatus.EXECUTING : ExitStatus.FINISHED; 
			contribution.setExitStatus(status);

		}

		final Map<I, Exception> skippedInputs = getBufferedSkips(attributes, SKIPPED_INPUTS_KEY);
		final Map<O, Exception> skippedOutputs = getBufferedSkips(attributes, SKIPPED_OUTPUTS_KEY);

		if (!inputs.isEmpty()) {
			inputs.removeAll(skippedInputs.keySet());
			process(contribution, inputs, outputs, skippedInputs);

			outputs.removeAll(skippedOutputs.keySet());
			write(outputs, contribution, skippedOutputs);
		}

		callSkipListeners(skippedReads, skippedInputs, skippedOutputs);

		// On successful completion clear the attributes to signal that there is
		// no more processing
		for (String key : attributes.attributeNames()) {
			attributes.removeAttribute(key);
		}

		return continuable;

	}

	/**
	 * Tries to read the item from the reader, in case of exception skip the
	 * item if the skip policy allows, otherwise re-throw.
	 * 
	 * @param contribution current StepContribution holding skipped items count
	 * @param skippedReads
	 * @return next item for processing
	 */
	protected I read(StepContribution contribution, List<Exception> skippedReads) throws Exception {

		while (true) {
			try {
				return doRead();
			}
			catch (Exception e) {

				if (getReadSkipPolicy().shouldSkip(e, contribution.getStepSkipCount())) {
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

}
