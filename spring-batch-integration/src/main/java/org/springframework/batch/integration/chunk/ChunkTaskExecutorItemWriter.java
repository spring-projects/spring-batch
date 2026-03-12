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

import org.springframework.batch.core.BatchStatus;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Similar to {@code ChunkMessageChannelItemWriter}, this item writer submits chunk
 * requests to local workers from a {@link TaskExecutor} instead of sending them over a
 * message channel to remote workers.
 *
 * <p>
 * The aggregation of worker contributions is done in the {@code afterStep} method, which
 * waits for all worker responses and updates the step execution accordingly. If any
 * worker response indicates a failure, the step execution is marked as failed and the
 * exception is added to the step execution's failure exceptions. Otherwise, the step
 * execution is marked as completed and the write counts and skip counts from all worker
 * contributions are aggregated into the step execution's write count and write skip
 * count. The commit count is also incremented for each successful worker contribution,
 * while the rollback count is incremented for each failed worker contribution.
 *
 * @param <T> type of items
 * @see ChunkMessageChannelItemWriter
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public class ChunkTaskExecutorItemWriter<T> implements ItemWriter<T>, StepExecutionListener {

	private static final Log logger = LogFactory.getLog(ChunkTaskExecutorItemWriter.class);

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
		ChunkRequest<T> request = new ChunkRequest<>(++sequence, chunk, this.stepExecution.getJobExecutionId(),
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
			ExitStatus result = ExitStatus.COMPLETED
				.addExitDescription("Waited for " + this.responses.size() + " results.");
			resetCounter(stepExecution);
			for (StepContribution contribution : getStepContributions()) {
				// only write counts and skip counts are aggregated here
				// read counts and process/filter counts are managed by the driving step
				stepExecution.setWriteCount(stepExecution.getWriteCount() + contribution.getWriteCount());
				stepExecution.setWriteSkipCount(stepExecution.getWriteSkipCount() + contribution.getWriteSkipCount());
				ExitStatus exitStatus = contribution.getExitStatus();
				if (ExitStatus.FAILED.getExitCode().equals(exitStatus.getExitCode())) {
					logger.error("Chunk processing failed for contribution: " + contribution
							+ ", marking step execution as failed.");
					result = exitStatus;
					Throwable exitException = exitStatus.getExitException();
					if (exitException != null) {
						stepExecution.addFailureException(exitException);
					}
					stepExecution.setStatus(BatchStatus.FAILED);
					stepExecution.incrementRollbackCount();
				}
				else {
					stepExecution.incrementCommitCount();
				}
			}
			return result;
		}
		catch (ExecutionException | InterruptedException e) {
			stepExecution.setStatus(BatchStatus.FAILED);
			stepExecution.addFailureException(e);
			return ExitStatus.FAILED.addExitDescription(e);
		}
	}

	/**
	 * Reset the write count, write skip count, commit count and rollback count of the
	 * step execution to avoid counting them twice as they are updated by workers and
	 * aggregated in the afterStep method. The read count and process/filter count are not
	 * reset as they are managed by the driving step
	 * @param stepExecution the step execution to reset the counters for
	 */
	private void resetCounter(StepExecution stepExecution) {
		stepExecution.setWriteCount(0);
		stepExecution.setWriteSkipCount(0);
		stepExecution.setCommitCount(0);
		stepExecution.setRollbackCount(0);
	}

	private Collection<StepContribution> getStepContributions() throws ExecutionException, InterruptedException {
		List<StepContribution> contributions = new ArrayList<>();
		for (Future<ChunkResponse> task : this.responses) {
			contributions.add(task.get().getStepContribution());
		}
		return contributions;
	}

}
