/*
 * Copyright 2017-2019 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified CompositeItemWriter.
 *
 * @author Glenn Renfro
 * @author Drummond Dawson
 * @author Mahmoud Ben Hassine
 * @since 4.0
 */
public class CompositeItemWriterBuilder<T> {
	private List<ItemWriter<? super T>> delegates;

	private boolean ignoreItemStream = false;

	/**
	 * Establishes the policy whether to call the open, close, or update methods for the
	 * item writer delegates associated with the CompositeItemWriter.
	 *
	 * @param ignoreItemStream if false the delegates' open, close, or update methods will
	 * be called when the corresponding methods on the CompositeItemWriter are called. If
	 * true the delegates' open, close, nor update methods will not be called (default is false).
	 * @return this instance for method chaining.
	 * 
	 * @see CompositeItemWriter#setIgnoreItemStream(boolean)
	 */
	public CompositeItemWriterBuilder<T> ignoreItemStream(boolean ignoreItemStream) {
		this.ignoreItemStream = ignoreItemStream;

		return this;
	}

	/**
	 * The list of item writers to use as delegates. Items are written to each of the
	 * delegates.
	 *
	 * @param delegates the list of delegates to use. The delegates list must not be null
	 * nor be empty.
	 * @return this instance for method chaining.
	 * 
	 * @see CompositeItemWriter#setDelegates(List)
	 */
	public CompositeItemWriterBuilder<T> delegates(List<ItemWriter<? super T>> delegates) {
		this.delegates = delegates;

		return this;
	}

	/**
	 * The item writers to use as delegates. Items are written to each of the
	 * delegates.
	 *
	 * @param delegates the delegates to use.
	 * @return this instance for method chaining.
	 *
	 * @see CompositeItemWriter#setDelegates(List)
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public final CompositeItemWriterBuilder<T> delegates(ItemWriter<? super T>... delegates) {
		return delegates(Arrays.asList(delegates));
	}

	/**
	 * Returns a fully constructed {@link CompositeItemWriter}.
	 *
	 * @return a new {@link CompositeItemWriter}
	 */
	public CompositeItemWriter<T> build() {
		Assert.notNull(delegates, "A list of delegates is required.");
		Assert.notEmpty(delegates, "The delegates list must have one or more delegates.");

		CompositeItemWriter<T> writer = new CompositeItemWriter<>();
		writer.setDelegates(this.delegates);
		writer.setIgnoreItemStream(this.ignoreItemStream);
		return writer;
	}
}
