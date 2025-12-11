/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.chunk;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.core.task.TaskExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Similar to {@code ChunkMessageChannelItemWriter}, this item writer submits chunk
 * requests to local workers from a {@link TaskExecutor} instead of sending them over a
 * message channel to remote workers.
 *
 * @param <T> type of items
 * @see ChunkMessageChannelItemWriter
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public class ChunkTaskExecutorItemWriter<T> implements ItemWriter<T>, StepExecutionListener {

	@SuppressWarnings("NullAway.Init")
	private StepExecution stepExecution;

	private int sequence;

	private final TaskExecutor taskExecutor;

	private final ChunkProcessorChunkRequestHandler<T> chunkProcessorChunkHandler = new ChunkProcessorChunkRequestHandler<>();

	private final Set<Future<ChunkResponse>> responses = new HashSet<>();

	/**
	 * Create a new {@link ChunkTaskExecutorItemWriter}.
	 * @param chunkRequestProcessor the chunk processor to process chunks
	 * @param taskExecutor the task executor to submit chunk processing tasks to
	 */
	public ChunkTaskExecutorItemWriter(ChunkProcessor<T> chunkRequestProcessor, TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		this.chunkProcessorChunkHandler.setChunkProcessor(chunkRequestProcessor);
	}

	@Override
	public void write(Chunk<? extends T> chunk) {
		ChunkRequest<T> request = new ChunkRequest<>(++sequence, chunk, this.stepExecution.getJobExecution().getId(),
				this.stepExecution.createStepContribution());
		FutureTask<ChunkResponse> chunkResponseFutureTask = new FutureTask<>(
				() -> this.chunkProcessorChunkHandler.handle(request));
		this.responses.add(chunkResponseFutureTask);
		this.taskExecutor.execute(chunkResponseFutureTask);
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		try {
			for (StepContribution contribution : getStepContributions()) {
				stepExecution.apply(contribution);
			}
		}
		catch (ExecutionException | InterruptedException e) {
			return ExitStatus.FAILED.addExitDescription(e);
		}
		return ExitStatus.COMPLETED.addExitDescription("Waited for " + this.responses.size() + " results.");
	}

	private Collection<StepContribution> getStepContributions() throws ExecutionException, InterruptedException {
		List<StepContribution> contributions = new ArrayList<>();
		for (Future<ChunkResponse> task : this.responses) {
			contributions.add(task.get().getStepContribution());
		}
		return contributions;
	}

}
