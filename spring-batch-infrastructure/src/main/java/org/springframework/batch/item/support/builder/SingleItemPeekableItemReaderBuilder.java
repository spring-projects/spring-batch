/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.batch.item.support.builder;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified SingleItemPeekeableItemReader.
 *
 * @author Glenn Renfro
 *
 * @since 4.0
 */
public class SingleItemPeekableItemReaderBuilder<T> {

	private ItemReader<T> delegate;

	/**
	 * The item reader to use as a delegate. Items are read from the delegate and passed
	 * to the caller in
	 * {@link org.springframework.batch.item.support.SingleItemPeekableItemReader#read()}.
	 *
	 * @param delegate the delegate to set
	 * @return this instance for method chaining
	 * @see SingleItemPeekableItemReader#setDelegate(ItemReader)
	 */
	public SingleItemPeekableItemReaderBuilder<T> delegate(ItemReader<T> delegate) {
		this.delegate = delegate;

		return this;
	}

	/**
	 * Returns a fully constructed {@link SingleItemPeekableItemReader}.
	 *
	 * @return a new {@link SingleItemPeekableItemReader}
	 */
	public SingleItemPeekableItemReader<T> build() {
		Assert.notNull(this.delegate, "A delegate is required");

		SingleItemPeekableItemReader<T> reader = new SingleItemPeekableItemReader<>();
		reader.setDelegate(this.delegate);
		return reader;
	}
}
