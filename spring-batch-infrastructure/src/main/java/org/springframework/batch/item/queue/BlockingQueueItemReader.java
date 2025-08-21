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

import org.springframework.batch.item.ItemReader;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This is an {@link ItemReader} that reads items from a {@link BlockingQueue}. It stops
 * reading (i.e., returns {@code null}) if no items are available in the queue after a
 * configurable timeout.
 *
 * @param <T> type of items to read.
 * @author Mahmoud Ben Hassine
 * @since 5.2.0
 */
public class BlockingQueueItemReader<T> implements ItemReader<T> {

	private final BlockingQueue<T> queue;

	private long timeout = 1L;

	private TimeUnit timeUnit = TimeUnit.SECONDS;

	/**
	 * Create a new {@link BlockingQueueItemReader}.
	 * @param queue the queue to read items from
	 */
	public BlockingQueueItemReader(BlockingQueue<T> queue) {
		this.queue = queue;
	}

	/**
	 * Set the reading timeout and time unit. Defaults to 1 second.
	 * @param timeout the timeout after which the reader stops reading
	 * @param timeUnit the unit of the timeout
	 */
	public void setTimeout(long timeout, TimeUnit timeUnit) {
		this.timeout = timeout;
		this.timeUnit = timeUnit;
	}

	@Override
	public T read() throws Exception {
		return this.queue.poll(this.timeout, this.timeUnit);
	}

}