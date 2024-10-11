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
package org.springframework.batch.item.queue.builder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.batch.item.queue.BlockingQueueItemReader;
import org.springframework.util.Assert;

/**
 * Builder for {@link BlockingQueueItemReader}.
 *
 * @param <T> type of items to read
 * @since 5.2.0
 * @author Mahmoud Ben Hassine
 */
public class BlockingQueueItemReaderBuilder<T> {

	private BlockingQueue<T> queue;

	private long timeout = 1L;

	private TimeUnit timeUnit = TimeUnit.SECONDS;

	/**
	 * Set the queue to read items from.
	 * @param queue the queue to read items from.
	 * @return this instance of the builder
	 */
	public BlockingQueueItemReaderBuilder<T> queue(BlockingQueue<T> queue) {
		this.queue = queue;
		return this;
	}

	/**
	 * Set the reading timeout. Defaults to 1 second.
	 * @param timeout the reading timeout.
	 * @return this instance of the builder
	 */
	public BlockingQueueItemReaderBuilder<T> timeout(long timeout, TimeUnit timeUnit) {
		this.timeout = timeout;
		this.timeUnit = timeUnit;
		return this;
	}

	/**
	 * Create a configured {@link BlockingQueueItemReader}.
	 * @return a configured {@link BlockingQueueItemReader}.
	 */
	public BlockingQueueItemReader<T> build() {
		Assert.state(this.queue != null, "The blocking queue is required.");
		BlockingQueueItemReader<T> blockingQueueItemReader = new BlockingQueueItemReader<>(this.queue);
		blockingQueueItemReader.setTimeout(this.timeout, this.timeUnit);
		return blockingQueueItemReader;
	}

}
