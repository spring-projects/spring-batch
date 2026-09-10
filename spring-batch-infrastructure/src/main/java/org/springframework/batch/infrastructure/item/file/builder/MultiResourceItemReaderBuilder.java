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

package org.springframework.batch.infrastructure.item.file.builder;

import java.util.Comparator;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;
import org.springframework.batch.infrastructure.item.file.MultiResourceItemReader;
import org.springframework.batch.infrastructure.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A builder implementation for the {@link MultiResourceItemReader}.
 *
 * @author Glenn Renfro
 * @author Drummond Dawson
 * @author Stefano Cordio
 * @author Andrey Litvitski
 * @since 4.0
 * @see MultiResourceItemReader
 */
public class MultiResourceItemReaderBuilder<T> {

	private @Nullable ResourceAwareItemReaderItemStream<? extends T> delegate;

	private Resource @Nullable [] resources;

	private boolean strict = false;

	private @Nullable Comparator<Resource> comparator;

	private boolean saveState = true;

	private @Nullable String name;

	/**
	 * Configure if the state of the {@link ItemStreamSupport} should be persisted within
	 * the {@link ExecutionContext} for restart purposes.
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public MultiResourceItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * The name used to calculate the key within the {@link ExecutionContext}. Required if
	 * {@link #saveState(boolean)} is set to true.
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see ItemStreamSupport#setName(String)
	 */
	public MultiResourceItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * The array of resources that the {@link MultiResourceItemReader} will use to
	 * retrieve items.
	 * @param resources the array of resources to use.
	 * @return this instance for method chaining.
	 *
	 * @see MultiResourceItemReader#setResources(Resource[])
	 */
	public MultiResourceItemReaderBuilder<T> resources(Resource... resources) {
		this.resources = resources;

		return this;
	}

	/**
	 * Establishes the delegate to use for reading the resources provided.
	 * @param delegate reads items from single {@link Resource}.
	 * @return this instance for method chaining.
	 *
	 * @see MultiResourceItemReader#setDelegate(ResourceAwareItemReaderItemStream)
	 */
	public MultiResourceItemReaderBuilder<T> delegate(ResourceAwareItemReaderItemStream<? extends T> delegate) {
		this.delegate = delegate;

		return this;
	}

	/**
	 * In strict mode the reader will throw an exception on
	 * {@link MultiResourceItemReader#open(ExecutionContext)} if there are no resources to
	 * read.
	 * @param strict false by default.
	 * @return this instance for method chaining.
	 * @see MultiResourceItemReader#setStrict(boolean)
	 */
	public MultiResourceItemReaderBuilder<T> setStrict(boolean strict) {
		this.strict = strict;

		return this;
	}

	/**
	 * Used to order the injected resources, by default compares
	 * {@link Resource#getFilename()} values.
	 * @param comparator the comparator to use for ordering resources.
	 * @return this instance for method chaining.
	 * @see MultiResourceItemReader#setComparator(Comparator)
	 */
	public MultiResourceItemReaderBuilder<T> comparator(Comparator<Resource> comparator) {
		this.comparator = comparator;

		return this;
	}

	/**
	 * Builds the {@link MultiResourceItemReader}.
	 * @return a {@link MultiResourceItemReader}
	 */
	public MultiResourceItemReader<T> build() {
		Assert.notNull(this.resources, "resources array is required.");
		Assert.notNull(this.delegate, "delegate is required.");

		MultiResourceItemReader<T> reader = new MultiResourceItemReader<>(this.delegate);
		reader.setResources(this.resources);
		reader.setSaveState(this.saveState);
		reader.setStrict(this.strict);

		if (comparator != null) {
			reader.setComparator(this.comparator);
		}
		if (StringUtils.hasText(this.name)) {
			reader.setName(this.name);
		}

		return reader;
	}

}
