/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.infrastructure.item.support;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.util.Assert;

/**
 * This is an {@link ItemWriter} decorator with a synchronized {@link ItemWriter#write}
 * method. This decorator is useful when using a non thread-safe item writer in a
 * multi-threaded step.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.1.0
 * @param <T> type of objects to write
 */
public class SynchronizedItemWriter<T> implements ItemWriter<T> {

	private final ItemWriter<T> delegate;

	private final Lock lock = new ReentrantLock();

	public SynchronizedItemWriter(ItemWriter<T> delegate) {
		Assert.notNull(delegate, "The delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * This method delegates to the {@code write} method of the delegate and is
	 * synchronized with a lock.
	 */
	@Override
	public void write(Chunk<? extends T> items) throws Exception {
		this.lock.lock();
		try {
			this.delegate.write(items);
		}
		finally {
			this.lock.unlock();
		}
	}

}
