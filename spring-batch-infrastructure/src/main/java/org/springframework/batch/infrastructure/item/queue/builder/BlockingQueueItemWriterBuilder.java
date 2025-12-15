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
package org.springframework.batch.infrastructure.item.queue.builder;

import java.util.concurrent.BlockingQueue;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.queue.BlockingQueueItemWriter;
import org.springframework.util.Assert;

/**
 * Builder for a {@link BlockingQueueItemWriter}.
 *
 * @param <T> type of items to write
 * @since 5.2.0
 * @author Mahmoud Ben Hassine
 */
public class BlockingQueueItemWriterBuilder<T> {

	private @Nullable BlockingQueue<T> queue;

	/**
	 * Create a new {@link BlockingQueueItemWriterBuilder}
	 * @param queue the queue to write items to
	 * @return this instance of the builder
	 */
	public BlockingQueueItemWriterBuilder<T> queue(BlockingQueue<T> queue) {
		this.queue = queue;
		return this;
	}

	/**
	 * Create a configured {@link BlockingQueueItemWriter}.
	 * @return a configured {@link BlockingQueueItemWriter}.
	 */
	public BlockingQueueItemWriter<T> build() {
		Assert.state(this.queue != null, "The blocking queue is required.");
		return new BlockingQueueItemWriter<>(this.queue);
	}

}
