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
package org.springframework.batch.infrastructure.item.support.builder;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemStreamWriter;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamWriter;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified {@link SynchronizedItemStreamWriter}.
 *
 * @author Dimitrios Liapis
 * @author Mahmoud Ben Hassine
 */
public class SynchronizedItemStreamWriterBuilder<T> {

	private @Nullable ItemStreamWriter<T> delegate;

	/**
	 * Set the delegate {@link ItemStreamWriter}.
	 * @param delegate the delegate to set
	 * @return this instance for method chaining
	 */
	public SynchronizedItemStreamWriterBuilder<T> delegate(ItemStreamWriter<T> delegate) {
		this.delegate = delegate;

		return this;
	}

	/**
	 * Returns a fully constructed {@link SynchronizedItemStreamWriter}.
	 * @return a new {@link SynchronizedItemStreamWriter}
	 */
	public SynchronizedItemStreamWriter<T> build() {
		Assert.notNull(this.delegate, "A delegate item writer is required");

		return new SynchronizedItemStreamWriter<>(this.delegate);
	}

}
