/*
 * Copyright 2017-2025 the original author or authors.
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

package org.springframework.batch.infrastructure.item.support.builder;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified SynchronizedItemStreamReader.
 *
 * @author Glenn Renfro
 * @since 4.0
 */
public class SynchronizedItemStreamReaderBuilder<T> {

	private @Nullable ItemStreamReader<T> delegate;

	private boolean synchronizeUpdateMethod = false;

	/**
	 * The item stream reader to use as a delegate. Items are read from the delegate and
	 * passed to the caller in {@link SynchronizedItemStreamReader#read()}.
	 * @param delegate the delegate to set
	 * @return this instance for method chaining
	 * @see SynchronizedItemStreamReader#setDelegate(ItemStreamReader)
	 */
	public SynchronizedItemStreamReaderBuilder<T> delegate(ItemStreamReader<T> delegate) {
		this.delegate = delegate;

		return this;
	}

	/**
	 * Synchronize the update method of the ItemStreamReader, when using parallel
	 * execution and if the delegate reader is not thread-safe with respect to the update
	 * method. I.e. JpaCursorItemReader.
	 * @param synchronizeUpdateMethod whether the update method should be synchronized or
	 * not
	 * @return this instance for method chaining
	 */
	public SynchronizedItemStreamReaderBuilder<T> synchronizeUpdateMethod(final boolean synchronizeUpdateMethod) {
		this.synchronizeUpdateMethod = synchronizeUpdateMethod;

		return this;
	}

	/**
	 * Returns a fully constructed {@link SynchronizedItemStreamReader}.
	 * @return a new {@link SynchronizedItemStreamReader}
	 */
	public SynchronizedItemStreamReader<T> build() {
		Assert.notNull(this.delegate, "A delegate is required");

		final SynchronizedItemStreamReader<T> reader = new SynchronizedItemStreamReader<>(this.delegate);
		reader.setSynchronizeUpdate(this.synchronizeUpdateMethod);
		return reader;
	}

}
