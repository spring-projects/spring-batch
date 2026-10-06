/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.infrastructure.item.data.builder;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;
import org.springframework.batch.infrastructure.item.data.PageableItemReader;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * A builder for {@link PageableItemReader}.
 *
 * @param <T> type of items to read
 * @author Stefano Cordio
 * @since 6.1
 */
public class PageableItemReaderBuilder<T> {

	private @Nullable Function<Pageable, ? extends Slice<? extends T>> query;

	private Sort sort = Sort.unsorted();

	private int pageSize = 10;

	private boolean saveState = true;

	private @Nullable String name;

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	/**
	 * A function that accepts a {@link Pageable} and returns a {@link Slice} of items.
	 * @param query the query function to apply for each page
	 * @return this instance for method chaining
	 * @see PageableItemReader#PageableItemReader(Function, int, Sort)
	 */
	public PageableItemReaderBuilder<T> query(Function<Pageable, ? extends Slice<? extends T>> query) {
		this.query = query;

		return this;
	}

	/**
	 * The number of items to read with each page.
	 * @param pageSize the number of items per page. Defaults to 10 and must be greater
	 * than 0.
	 * @return this instance for method chaining
	 * @see PageableItemReader#PageableItemReader(Function, int, Sort)
	 */
	public PageableItemReaderBuilder<T> pageSize(int pageSize) {
		this.pageSize = pageSize;

		return this;
	}

	/**
	 * Configures the sort parameters for the query.
	 * @param sort the sort to apply to the query
	 * @return this instance for method chaining
	 * @see PageableItemReader#PageableItemReader(Function, int, Sort)
	 */
	public PageableItemReaderBuilder<T> sort(Sort sort) {
		this.sort = sort;

		return this;
	}

	/**
	 * Configure if the state of the {@link ItemStreamSupport} should be persisted within
	 * the {@link ExecutionContext} for restart purposes.
	 * @param saveState defaults to true
	 * @return this instance for method chaining
	 */
	public PageableItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * The name used to calculate the key within the {@link ExecutionContext}. Defaults to
	 * the bean name, or to the short class name if this instance is not a bean. Set it
	 * explicitly to disambiguate several non-bean instances of the same type in a step.
	 * @param name name of the reader instance
	 * @return this instance for method chaining
	 * @see ItemStreamSupport#setName(String)
	 */
	public PageableItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Configure the max number of items to be read.
	 * @param maxItemCount the max items to be read
	 * @return this instance for method chaining
	 * @see AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public PageableItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;

		return this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 * @param currentItemCount current index
	 * @return this instance for method chaining
	 * @see AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public PageableItemReaderBuilder<T> currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;

		return this;
	}

	/**
	 * Builds the {@link PageableItemReader}.
	 * @return a {@link PageableItemReader}
	 */
	public PageableItemReader<T> build() {
		Assert.notNull(this.query, "query is required.");
		Assert.isTrue(this.pageSize > 0, "Page size must be greater than 0");
		Assert.notNull(this.sort, "sort is required.");

		PageableItemReader<T> reader = new PageableItemReader<>(this.query, this.pageSize, this.sort);
		reader.setCurrentItemCount(this.currentItemCount);
		reader.setMaxItemCount(this.maxItemCount);
		reader.setSaveState(this.saveState);
		if (this.name != null) {
			reader.setName(this.name);
		}
		return reader;
	}

}
