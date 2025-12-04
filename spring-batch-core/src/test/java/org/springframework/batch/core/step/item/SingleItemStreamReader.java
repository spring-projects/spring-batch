/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.core.step.item;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

/**
 * An {@link ItemStreamReader} that reads a single item. Useful for testing purposes.
 *
 * @param <T> the type of item to be read
 * @author Mahmoud Ben Hassine
 */
public class SingleItemStreamReader<T> implements ItemStreamReader<T> {

	private T item;

	private final T initialValue;

	/**
	 * Create a new {@link SingleItemStreamReader} with the given initial value.
	 * @param initialValue the initial value to be read
	 */
	public SingleItemStreamReader(T initialValue) {
		this.initialValue = initialValue;
	}

	@Override
	public @Nullable T read() throws Exception {
		T returnedItem = this.item;
		this.item = null;
		return returnedItem;
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		this.item = this.initialValue;
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		this.item = null;
	}

	@Override
	public void close() throws ItemStreamException {
		this.item = null;
	}

}
