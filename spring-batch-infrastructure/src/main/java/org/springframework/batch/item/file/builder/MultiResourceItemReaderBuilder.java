/*
 * Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.file.builder;

import java.util.Comparator;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A builder implementation for the {@link MultiResourceItemReader}.
 *
 * @author Glenn Renfro
 * @since 4.0
 * @see MultiResourceItemReader
 */
public class MultiResourceItemReaderBuilder<T> {

	private ResourceAwareItemReaderItemStream<? extends T> delegate;

	private Resource[] resources;

	private boolean saveState = true;

	private boolean strict = false;

	private String name;

	private Comparator<Resource> comparator;

	/**
	 * The array of resources that the {@link MultiResourceItemReader} will use to
	 * retrieve items.
	 *
	 * @param resources the array of resources to use.
	 * @return this instance for method chaining.
	 *
	 * @see MultiResourceItemReader#setResources(Resource[])
	 */
	public MultiResourceItemReaderBuilder<T> resources(Resource[] resources) {
		this.resources = resources;

		return this;
	}

	/**
	 * Establishes the delegate to use for reading the resources provided.
	 *
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
	 * Set the boolean indicating whether or not state should be saved in the provided
	 * {@link ExecutionContext} during the {@link ItemStream} call to update.
	 *
	 * @param saveState true to update ExecutionContext. False do not update
	 * ExecutionContext.
	 * @return this instance for method chaining.
	 * @see MultiResourceItemReader#setSaveState(boolean)
	 * 
	 */
	public MultiResourceItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * In strict mode the reader will throw an exception on
	 * {@link MultiResourceItemReader#open(org.springframework.batch.item.ExecutionContext)}
	 * if there are no resources to read.
	 *
	 * @param strict false by default.
	 * @return this instance for method chaining.
	 * @see MultiResourceItemReader#setStrict(boolean)
	 */
	public MultiResourceItemReaderBuilder<T> setStrict(boolean strict) {
		this.strict = strict;

		return this;
	}

	/**
	 * The name of the component which will be used as a stem for keys in the
	 * {@link ExecutionContext}. Subclasses should provide a default value, e.g. the short
	 * form of the class name.
	 *
	 * @param name the name for the component.
	 * @return this instance for method chaining.
	 * @see MultiResourceItemReader#setName(String)
	 */
	public MultiResourceItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Used to order the injected resources, by default compares
	 * {@link Resource#getFilename()} values.
	 *
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
	 *
	 * @return a {@link MultiResourceItemReader}
	 */
	public MultiResourceItemReader<T> build() {
		Assert.notNull(this.resources, "resources array is required.");
		Assert.notNull(this.delegate, "delegate is required.");
		if (this.saveState) {
			Assert.state(StringUtils.hasText(this.name), "A name is required when saveState is set to true.");
		}

		MultiResourceItemReader<T> reader = new MultiResourceItemReader<>();
		reader.setResources(this.resources);
		reader.setDelegate(this.delegate);
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
