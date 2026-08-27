/*
 * Copyright 2025-2026 the original author or authors.
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

import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.core.task.TaskExecutor;

/**
 * @param <T> type of items
 * @author Mahmoud Ben Hassine
 * @since 6.0
 * @deprecated since 6.0.4 in favor of
 * {@link org.springframework.batch.core.step.item.ChunkTaskExecutorItemWriter}
 */
@Deprecated(since = "6.0.4")
public class ChunkTaskExecutorItemWriter<T>
		extends org.springframework.batch.core.step.item.ChunkTaskExecutorItemWriter<T> {

	/**
	 * Create a new {@link ChunkTaskExecutorItemWriter}.
	 * @param chunkRequestProcessor the chunk processor to process chunks
	 * @param taskExecutor the task executor to submit chunk processing tasks to
	 */
	public ChunkTaskExecutorItemWriter(ChunkProcessor<T> chunkRequestProcessor, TaskExecutor taskExecutor) {
		super(chunkRequestProcessor, taskExecutor);
	}

}
