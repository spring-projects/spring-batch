/*
 * Copyright 2015-2025 the original author or authors.
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

import org.springframework.batch.item.ExecutionContext;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 *
 * This is a simple ItemStreamReader decorator with a synchronized ItemReader.read()
 * method - which makes a non-thread-safe ItemReader thread-safe.
 * <p>
 * However, if reprocessing an item is problematic, then using this will make a job not
 * restartable.
 * <p>
 * Here is the motivation behind this class: https://stackoverflow.com/a/20002493/2910265
 *
 * @author Matthew Ouyang
 * @author Mahmoud Ben Hassine
 * @since 3.0.4
 * @param <T> type of object being read
 */
public class SynchronizedItemStreamReader<T> implements ItemStreamReader<T> {

	private ItemStreamReader<T> delegate;

	private final Lock lock = new ReentrantLock();

	/**
	 * Create a new {@link SynchronizedItemStreamReader} with the given delegate.
	 * @param delegate the item reader to use as a delegate
	 * @since 6.0
	 */
	public SynchronizedItemStreamReader(ItemStreamReader<T> delegate) {
		Assert.notNull(delegate, "The delegate item reader must not be null");
		this.delegate = delegate;
	}

	public void setDelegate(ItemStreamReader<T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * This delegates to the read method of the <code>delegate</code>
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

	@Override
	public void close() {
		this.delegate.close();
	}

	@Override
	public void open(ExecutionContext executionContext) {
		this.delegate.open(executionContext);
	}

	@Override
	public void update(ExecutionContext executionContext) {
		this.delegate.update(executionContext);
	}

}
