/*
 * Copyright 2006-2009 the original author or authors.
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
		if (inputs.isEnd()) {
			contribution.setExitStatus(ExitStatus.FINISHED);
		}

		return RepeatStatus.continueIf(!inputs.isEnd());

	}

}
