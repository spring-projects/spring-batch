/*
 * Copyright 2020-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.json.JsonFileItemWriter;
import org.springframework.batch.infrastructure.item.xml.StaxEventItemWriter;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.util.Assert;

/**
 * An {@link ItemStreamWriter} decorator with a synchronized
 * {@link SynchronizedItemStreamWriter#write write()} method.
 * <p>
 * This decorator is useful when using a non thread-safe item writer in a multi-threaded
 * step. Typical delegate examples are the {@link JsonFileItemWriter JsonFileItemWriter}
 * and {@link StaxEventItemWriter StaxEventItemWriter}.
 *
 * <p>
 * <strong>It should be noted that synchronizing writes might introduce some performance
 * degradation, so this decorator should be used wisely and only when necessary. For
 * example, using a {@link FlatFileItemWriter FlatFileItemWriter} in a multi-threaded step
 * does NOT require synchronizing writes, so using this decorator in such use case might
 * be counter-productive.</strong>
 * </p>
 *
 * @author Dimitrios Liapis
 * @author Mahmoud Ben Hassine
 * @param <T> type of object being written
 */
public class SynchronizedItemStreamWriter<T> implements ItemStreamWriter<T> {

	private ItemStreamWriter<T> delegate;

	private final Lock lock = new ReentrantLock();

	/**
	 * Create a new {@link SynchronizedItemStreamWriter} with the given delegate.
	 * @param delegate the item writer to use as a delegate
	 * @since 6.0
	 */
	public SynchronizedItemStreamWriter(ItemStreamWriter<T> delegate) {
		Assert.notNull(delegate, "The delegate item writer must not be null");
		this.delegate = delegate;
	}

	/**
	 * Set the delegate {@link ItemStreamWriter}.
	 * @param delegate the delegate to set
	 */
	public void setDelegate(ItemStreamWriter<T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * This method delegates to the {@code write} method of the {@code delegate}.
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

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		this.delegate.open(executionContext);
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		this.delegate.update(executionContext);
	}

	@Override
	public void close() throws ItemStreamException {
		this.delegate.close();
	}

}
