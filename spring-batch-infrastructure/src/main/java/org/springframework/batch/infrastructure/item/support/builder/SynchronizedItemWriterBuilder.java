/*
 * Copyright 2023 the original author or authors.
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
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemWriter;
import org.springframework.util.Assert;

/**
 * Builder for {@link SynchronizedItemWriter}.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.1.0
 */
public class SynchronizedItemWriterBuilder<T> {

	private @Nullable ItemWriter<T> delegate;

	/**
	 * The item writer to use as a delegate.
	 * @param delegate the delegate writer to set
	 * @return this instance for method chaining
	 */
	public SynchronizedItemWriterBuilder<T> delegate(ItemWriter<T> delegate) {
		this.delegate = delegate;

		return this;
	}

	/**
	 * Returns a new {@link SynchronizedItemWriter}.
	 * @return a new {@link SynchronizedItemWriter}
	 */
	public SynchronizedItemWriter<T> build() {
		Assert.notNull(this.delegate, "A delegate is required");

		return new SynchronizedItemWriter<>(this.delegate);
	}

}
