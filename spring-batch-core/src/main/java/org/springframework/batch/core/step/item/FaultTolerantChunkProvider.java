package org.springframework.batch.core.step.item;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableReadException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.repeat.RepeatOperations;

public class FaultTolerantChunkProvider<I> extends SimpleChunkProvider<I> {

	private SkipPolicy skipPolicy = new LimitCheckingItemSkipPolicy(0);

	public FaultTolerantChunkProvider(ItemReader<? extends I> itemReader, RepeatOperations repeatOperations) {
		super(itemReader, repeatOperations);
	}

	public void setSkipPolicy(SkipPolicy SkipPolicy) {
		this.skipPolicy = SkipPolicy;
	}

	@Override
	protected I read(StepContribution contribution, Chunk<I> chunk) throws Exception {
		while (true) {
			try {
				return doRead();
			}
			catch (Exception e) {

				if (skipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
					// increment skip count and try again
					contribution.incrementReadSkipCount();
					chunk.skip(e);

					logger.debug("Skipping failed input", e);
				}
				else {
					throw new NonSkippableReadException("Non-skippable exception during read", e);
				}

			}
		}
	}

}
