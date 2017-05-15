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

package org.springframework.batch.item.data.builder;

import java.util.List;
import java.util.Map;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A builder implementation for the {@link RepositoryItemReader}.
 *
 * @author Glenn Renfro
 * @since 4.0
 * @see RepositoryItemReader
 */

public class RepositoryItemReaderBuilder<T> {

	private PagingAndSortingRepository<?, ?> repository;

	private Map<String, Sort.Direction> sorts;

	private List<?> arguments;

	private int pageSize = 10;

	private String methodName;

	private int currentItemCount;

	private int maxItemCount;

	private boolean saveState = true;

	private String name;

	/**
	 * Arguments to be passed to the data providing method.
	 *
	 * @param arguments list of method arguments to be passed to the repository.
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setArguments(List)
	 */
	public RepositoryItemReaderBuilder<T> arguments(List<?> arguments) {
		this.arguments = arguments;

		return this;
	}

	/**
	 * Provides ordering of the results so that order is maintained between paged queries.
	 *
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
	 *
	 * @param pageSize The number of items to retrieve per page.
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
	 *
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
	 *
	 * @param methodName name of the method to invoke.
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setMethodName(String)
	 */
	public RepositoryItemReaderBuilder<T> methodName(String methodName) {
		this.methodName = methodName;

		return this;
	}

	/**
	 * The index of the item to start reading from. If the {@link ExecutionContext}
	 * contains a key <code>[name].read.count</code> (where <code>[name]</code> is the
	 * name of this component) the value from the {@link ExecutionContext} will be used in
	 * preference.
	 *
	 * @see RepositoryItemReader#setName(String)
	 *
	 * @param count the value of the current item count
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setCurrentItemCount(int)
	 * 
	 */
	public RepositoryItemReaderBuilder<T> currentItemCount(int count) {
		this.currentItemCount = count;

		return this;
	}

	/**
	 * The maximum index of the items to be read. If the {@link ExecutionContext} contains
	 * a key <code>[name].read.count.max</code> (where <code>[name]</code> is the name of
	 * this component) the value from the {@link ExecutionContext} will be used in
	 * preference.
	 *
	 * @see RepositoryItemReader#setName(String)
	 *
	 * @param count the value of the maximum item count
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setMaxItemCount(int)
	 */
	public RepositoryItemReaderBuilder<T> maxItemCount(int count) {
		this.maxItemCount = count;

		return this;
	}

	/**
	 * Set the flag that determines whether to save internal data for
	 * {@link ExecutionContext}. Only switch this to false if you don't want to save any
	 * state from this stream, and you don't need it to be restartable. Always set it to
	 * false if the reader is being used in a concurrent environment.
	 *
	 * @param saveState flag value (default true).
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setSaveState(boolean)
	 */
	public RepositoryItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * The name of the component which will be used as a stem for keys in the
	 * {@link ExecutionContext}. Subclasses should provide a default value, e.g. the short
	 * form of the class name.
	 *
	 * @param name the name for the component
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setName(String)
	 */
	public RepositoryItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Builds the {@link RepositoryItemReader}.
	 *
	 * @return a {@link RepositoryItemReader}
	 */
	public RepositoryItemReader<T> build() {
		Assert.notNull(this.sorts, "sorts map is required.");
		Assert.notNull(this.repository, "repository is required.");
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
