/*
 * Copyright 2024 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.batch.item.queue;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.concurrent.BlockingQueue;

import org.jspecify.annotations.NonNull;

/**
 * This is an {@link ItemWriter} that writes items to a {@link BlockingQueue}.
 *
 * @param <T> type of items to write
 * @since 5.2.0
 * @author Mahmoud Ben Hassine
 */
public class BlockingQueueItemWriter<T> implements ItemWriter<@NonNull T> {

	private final BlockingQueue<T> queue;

	/**
	 * Create a new {@link BlockingQueueItemWriter}.
	 * @param queue the queue to write items to
	 */
	public BlockingQueueItemWriter(BlockingQueue<T> queue) {
		this.queue = queue;
	}

	@Override
	public void write(Chunk<? extends @NonNull T> items) throws Exception {
		for (T item : items) {
			this.queue.put(item);
		}
	}

}