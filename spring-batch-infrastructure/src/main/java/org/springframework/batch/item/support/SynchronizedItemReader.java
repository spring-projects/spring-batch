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
package org.springframework.batch.item.support;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.batch.item.ItemReader;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * This is an {@link ItemReader} decorator with a synchronized {@link ItemReader#read}
 * method. This decorator is useful when using a non thread-safe item reader in a
 * multi-threaded step.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.1.0
 * @param <T> type of objects to read
 */
public class SynchronizedItemReader<T> implements ItemReader<T> {

	private final ItemReader<T> delegate;

	private final Lock lock = new ReentrantLock();

	public SynchronizedItemReader(ItemReader<T> delegate) {
		Assert.notNull(delegate, "The delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * This method delegates to the {@code read} method of the delegate and is
	 * synchronized with a lock.
	 */
	@Override
	public @Nullable T read() throws Exception {
		this.lock.lock();
		try {
			return this.delegate.read();
		}
		finally {
			this.lock.unlock();
		}
	}

}
