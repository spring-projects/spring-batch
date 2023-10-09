/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.batch.item.data.builder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A builder implementation for the {@link RepositoryItemReader}.
 *
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @author Drummond Dawson
 * @since 4.0
 * @see RepositoryItemReader
 */
public class RepositoryItemReaderBuilder<T> {

	private PagingAndSortingRepository<?, ?> repository;

	private Map<String, Sort.Direction> sorts;

	private List<?> arguments;

	private int pageSize = 10;

	private String methodName;

	private boolean saveState = true;

	private String name;

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	/**
	 * Configure if the state of the
	 * {@link org.springframework.batch.item.ItemStreamSupport} should be persisted within
	 * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public RepositoryItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}. Required if
	 * {@link #saveState(boolean)} is set to true.
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public RepositoryItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Configure the max number of items to be read.
	 * @param maxItemCount the max items to be read
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public RepositoryItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;

		return this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 * @param currentItemCount current index
	 * @return this instance for method chaining
	 * @see org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public RepositoryItemReaderBuilder<T> currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;

		return this;
	}

	/**
	 * Arguments to be passed to the data providing method.
	 * @param arguments list of method arguments to be passed to the repository.
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setArguments(List)
	 */
	public RepositoryItemReaderBuilder<T> arguments(List<?> arguments) {
		this.arguments = arguments;

		return this;
	}

	/**
	 * Arguments to be passed to the data providing method.
	 * @param arguments the method arguments to be passed to the repository.
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setArguments(List)
	 */
	public RepositoryItemReaderBuilder<T> arguments(Object... arguments) {
		return arguments(Arrays.asList(arguments));
	}

	/**
	 * Provides ordering of the results so that order is maintained between paged queries.
	 * Use a {@link java.util.LinkedHashMap} in case of multiple sort entries to keep the
	 * order.
	 * @param sorts the fields to sort by and the directions.
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setSort(Map)
	 */
	public RepositoryItemReaderBuilder<T> sorts(Map<String, Sort.Direction> sorts) {
		this.sorts = sorts;

		return this;
	}

	/**
	 * Establish the pageSize for the generated RepositoryItemReader.
	 * @param pageSize The number of items to retrieve per page. Must be greater than 0.
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setPageSize(int)
	 */
	public RepositoryItemReaderBuilder<T> pageSize(int pageSize) {
		this.pageSize = pageSize;

		return this;
	}

	/**
	 * The {@link org.springframework.data.repository.PagingAndSortingRepository}
	 * implementation used to read input from.
	 * @param repository underlying repository for input to be read from.
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setRepository(PagingAndSortingRepository)
	 */
	public RepositoryItemReaderBuilder<T> repository(PagingAndSortingRepository<?, ?> repository) {
		this.repository = repository;

		return this;
	}

	/**
	 * Specifies what method on the repository to call. This method must take
	 * {@link org.springframework.data.domain.Pageable} as the <em>last</em> argument.
	 * @param methodName name of the method to invoke.
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setMethodName(String)
	 */
	public RepositoryItemReaderBuilder<T> methodName(String methodName) {
		this.methodName = methodName;

		return this;
	}

	/**
	 * Builds the {@link RepositoryItemReader}.
	 * @return a {@link RepositoryItemReader}
	 */
	public RepositoryItemReader<T> build() {
		Assert.notNull(this.sorts, "sorts map is required.");
		Assert.notNull(this.repository, "repository is required.");
		Assert.isTrue(this.pageSize > 0, "Page size must be greater than 0");
		Assert.hasText(this.methodName, "methodName is required.");
		if (this.saveState) {
			Assert.state(StringUtils.hasText(this.name), "A name is required when saveState is set to true.");
		}

		RepositoryItemReader<T> reader = new RepositoryItemReader<>();
		reader.setArguments(this.arguments);
		reader.setRepository(this.repository);
		reader.setMethodName(this.methodName);
		reader.setPageSize(this.pageSize);
		reader.setCurrentItemCount(this.currentItemCount);
		reader.setMaxItemCount(this.maxItemCount);
		reader.setSaveState(this.saveState);
		reader.setSort(this.sorts);
		reader.setName(this.name);
		return reader;
	}

}
