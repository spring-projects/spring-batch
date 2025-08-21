/*
 * Copyright 2024-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

/**
 * Composite reader that delegates reading to a list of {@link ItemStreamReader}s. This
 * implementation is not thread-safe.
 *
 * @author Mahmoud Ben Hassine
 * @author Elimelec Burghelea
 * @param <T> type of objects to read
 * @since 5.2
 */
public class CompositeItemReader<T> implements ItemStreamReader<T> {

	private final List<ItemStreamReader<? extends T>> delegates;

	private final Iterator<ItemStreamReader<? extends T>> delegatesIterator;

	private @Nullable ItemStreamReader<? extends T> currentDelegate;

	/**
	 * Create a new {@link CompositeItemReader}.
	 * @param delegates the delegate readers to read data
	 */
	public CompositeItemReader(List<ItemStreamReader<? extends T>> delegates) {
		this.delegates = delegates;
		this.delegatesIterator = this.delegates.iterator();
		this.currentDelegate = this.delegatesIterator.hasNext() ? this.delegatesIterator.next() : null;
	}

	// TODO: check if we need to open/close delegates on the fly in read() to avoid
	// opening resources early for a long time
	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		for (ItemStreamReader<? extends T> delegate : delegates) {
			delegate.open(executionContext);
		}
	}

	@Override
	public @Nullable T read() throws Exception {
		if (this.currentDelegate == null) {
			return null;
		}
		T item = currentDelegate.read();
		if (item == null) {
			currentDelegate = this.delegatesIterator.hasNext() ? this.delegatesIterator.next() : null;
			return read();
		}
		return item;
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (this.currentDelegate != null) {
			this.currentDelegate.update(executionContext);
		}
	}

	/**
	 * Close all delegates.
	 * @throws ItemStreamException thrown if one of the delegates fails to close. Original
	 * exceptions thrown by delegates are added as suppressed exceptions into this one, in
	 * the same order as delegates were registered.
	 */
	@Override
	public void close() throws ItemStreamException {
		List<Exception> exceptions = new ArrayList<>();

		for (ItemStreamReader<? extends T> delegate : delegates) {
			try {
				delegate.close();
			}
			catch (Exception e) {
				exceptions.add(e);
			}
		}

		if (!exceptions.isEmpty()) {
			String message = String.format("Failed to close %d delegate(s) due to exceptions", exceptions.size());
			ItemStreamException holder = new ItemStreamException(message);
			exceptions.forEach(holder::addSuppressed);
			throw holder;
		}
	}

}