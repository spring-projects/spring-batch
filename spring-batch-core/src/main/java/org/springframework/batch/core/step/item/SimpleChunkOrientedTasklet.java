package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.core.AttributeAccessor;

/**
 * Simplest possible implementation of chunk-oriented {@link Tasklet} with no
 * skipping or recovering. Just delegates all calls to the provided
 * {@link ItemReader}, {@link ItemProcessor} and {@link ItemWriter}.
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class SimpleChunkOrientedTasklet<I, O> extends AbstractItemOrientedTasklet<I, O> {

	private RepeatOperations repeatOperations;

	public SimpleChunkOrientedTasklet(ItemReader<? extends I> itemReader,
			ItemProcessor<? super I, ? extends O> itemProcessor, ItemWriter<? super O> itemWriter,
			RepeatOperations repeatOperations) {
		super(itemReader, itemProcessor, itemWriter);
		this.repeatOperations = repeatOperations;
	}

	/**
	 * Read-process-write a list of items.
	 */
	public ExitStatus execute(final StepContribution contribution, AttributeAccessor attributes) throws Exception {
		ExitStatus result = ExitStatus.CONTINUABLE;
		final List<I> inputs = new ArrayList<I>();

		result = repeatOperations.iterate(new RepeatCallback() {

			public ExitStatus doInIteration(final RepeatContext context) throws Exception {
				I item = doRead();

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

		List<O> outputs = new ArrayList<O>();
		for (I item : inputs) {
			O output = doProcess(item);
			if (output != null) {
				outputs.add(output);
			}
		}
		contribution.incrementFilterCount(inputs.size() - outputs.size());

		doWrite(outputs);
		contribution.incrementWriteCount(outputs.size());

		return result;
	}

}
