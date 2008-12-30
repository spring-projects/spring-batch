package org.springframework.batch.core.step.item;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * A {@link Tasklet} implementing variations on read-process-write item
 * handling.
 * 
 * @author Dave Syer
 * 
 * @param <I> input item type
 */
public class ChunkOrientedTasklet<I> implements Tasklet {

	private static final String INPUTS_KEY = "INPUTS";

	private final ChunkProcessor<I> chunkProcessor;

	private final ChunkProvider<I> chunkProvider;

	private boolean buffering = true;

	public ChunkOrientedTasklet(ChunkProvider<I> chunkProvider, ChunkProcessor<I> chunkProcessor) {
		this.chunkProvider = chunkProvider;
		this.chunkProcessor = chunkProcessor;
	}

	/**
	 * Flag to indicate that items should be buffered once read. Defaults to
	 * true, which is appropriate for forward-only, non-transactional item
	 * readers. Main (or only) use case for setting this flag to true is a
	 * transactional JMS item reader.
	 * 
	 * @param buffering
	 */
	public void setBuffering(boolean buffering) {
		this.buffering = buffering;
	}

	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

		@SuppressWarnings("unchecked")
		Chunk<I> inputs = (Chunk<I>) chunkContext.getAttribute(INPUTS_KEY);
		if (inputs == null) {
			inputs = chunkProvider.provide(contribution);
			if (buffering) {
				chunkContext.setAttribute(INPUTS_KEY, inputs);
			}
		}

		chunkProcessor.process(contribution, inputs);

		chunkContext.removeAttribute(INPUTS_KEY);
		chunkContext.setComplete();
		chunkProvider.postProcess(contribution, inputs);
		if (!inputs.isEnd()) {
			contribution.setExitStatus(ExitStatus.FINISHED);
		}

		return RepeatStatus.continueIf(!inputs.isEnd());

	}

}
