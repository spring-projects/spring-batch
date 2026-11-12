/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.core.step.item;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link ChunkProcessor} interface that processes each item
 * of a chunk with an optional {@link ItemProcessor} and writes the resulting items with
 * an {@link ItemWriter}. It is meant to be used where a {@link ChunkProcessor} is needed
 * outside of a {@link ChunkOrientedStep}, for example in a remote chunking worker or with
 * a {@link ChunkTaskExecutorItemWriter}.
 *
 * <p>
 * Items for which the item processor returns {@code null} are filtered out, and are
 * reflected in the filter count of the {@link StepContribution}. The write count of the
 * contribution is incremented by the number of items written. Any exception thrown by the
 * item processor or the item writer is rethrown as is: there is no transaction
 * management, listener notification, metrics or fault tolerance (retry and skip) here.
 * </p>
 *
 * <p>
 * This class is the replacement of the deprecated {@link SimpleChunkProcessor}.
 * </p>
 *
 * @param <I> type of input items
 * @param <O> type of output items
 * @author Mahmoud Ben Hassine
 * @since 6.1
 * @see ChunkProcessor
 */
public class DefaultChunkProcessor<I, O> implements ChunkProcessor<I> {

	private final @Nullable ItemProcessor<? super I, ? extends O> itemProcessor;

	private final ItemWriter<? super O> itemWriter;

	/**
	 * Create a new {@link DefaultChunkProcessor}.
	 * @param itemProcessor the item processor to apply to each item, or {@code null} if
	 * items should be written as is
	 * @param itemWriter the item writer to write the processed items
	 */
	public DefaultChunkProcessor(@Nullable ItemProcessor<? super I, ? extends O> itemProcessor,
			ItemWriter<? super O> itemWriter) {
		Assert.notNull(itemWriter, "itemWriter must not be null");
		this.itemProcessor = itemProcessor;
		this.itemWriter = itemWriter;
	}

	/**
	 * Create a new {@link DefaultChunkProcessor} that writes items without processing
	 * them. Items are written as is, so the type of input items must be assignable to the
	 * type of output items.
	 * @param itemWriter the item writer to write the items
	 */
	public DefaultChunkProcessor(ItemWriter<? super O> itemWriter) {
		this(null, itemWriter);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void process(Chunk<I> chunk, StepContribution contribution) throws Exception {
		if (chunk.isEmpty()) {
			return;
		}
		Chunk<O> outputs = new Chunk<>();
		for (I item : chunk) {
			O output = (this.itemProcessor != null) ? this.itemProcessor.process(item) : (O) item;
			if (output != null) {
				outputs.add(output);
			}
		}
		contribution.incrementFilterCount(chunk.size() - outputs.size());
		this.itemWriter.write(outputs);
		contribution.incrementWriteCount(outputs.size());
	}

}
