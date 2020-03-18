/*
 * Copyright 2017-2018 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
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

	private RepositoryMethodReference<?> repositoryMethodReference;

	private boolean saveState = true;

	private String name;

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	/**
	 * Configure if the state of the {@link org.springframework.batch.item.ItemStreamSupport}
	 * should be persisted within the {@link org.springframework.batch.item.ExecutionContext}
	 * for restart purposes.
	 *
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
	 *
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
	 *
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
	 *
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
	 * Arguments to be passed to the data providing method.
	 *
	 * @param arguments the method arguments to be passed to the repository.
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setArguments(List)
	 */
	public RepositoryItemReaderBuilder<T> arguments(Object... arguments) {
		return arguments(Arrays.asList(arguments));
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
	 * Specifies a repository and the type-safe method to call for the reader. The method
	 * configured via this mechanism must take
	 * {@link org.springframework.data.domain.Pageable} as the <em>last</em>
	 * argument. This method can be used in place of {@link #repository(PagingAndSortingRepository)},
	 * {@link #methodName(String)}, and {@link #arguments(List)}.
	 *
	 * Note: The repository that is used by the repositoryMethodReference must be
	 * non-final.
	 *
	 * @param repositoryMethodReference of the used to get a repository and type-safe
	 * method for use by the reader.
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setMethodName(String)
	 * @see RepositoryItemReader#setRepository(PagingAndSortingRepository)
	 *
	 */
	public RepositoryItemReaderBuilder<T> repository(RepositoryMethodReference<?> repositoryMethodReference) {
		this.repositoryMethodReference = repositoryMethodReference;

		return this;
	}

	/**
	 * Builds the {@link RepositoryItemReader}.
	 *
	 * @return a {@link RepositoryItemReader}
	 */
	public RepositoryItemReader<T> build() {
		if (this.repositoryMethodReference != null) {
			this.methodName = this.repositoryMethodReference.getMethodName();
			this.repository = this.repositoryMethodReference.getRepository();

			if(CollectionUtils.isEmpty(this.arguments)) {
				this.arguments = this.repositoryMethodReference.getArguments();
			}
		}

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

	/**
	 * Establishes a proxy that will capture a the Repository and the associated
	 * methodName that will be used by the reader.
	 * @param <T> The type of repository that will be used by the reader.  The class must
	 * not be final.
	 */
	public static class RepositoryMethodReference<T> {
		private RepositoryMethodInterceptor repositoryInvocationHandler;

		private PagingAndSortingRepository<?, ?> repository;

		public RepositoryMethodReference(PagingAndSortingRepository<?, ?> repository) {
			this.repository = repository;
			this.repositoryInvocationHandler = new RepositoryMethodInterceptor();
		}

		/**
		 * The proxy returned prevents actual method execution and is only used to gather,
		 * information about the method.
		 * @return T is a proxy of the object passed in in the constructor
		 */
		@SuppressWarnings("unchecked")
		public T methodIs() {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(this.repository.getClass());
			enhancer.setCallback(this.repositoryInvocationHandler);
			return (T) enhancer.create();
		}

		PagingAndSortingRepository<?, ?> getRepository() {
			return this.repository;
		}

		String getMethodName() {
			return this.repositoryInvocationHandler.getMethodName();
		}

		List<Object> getArguments() {
			return this.repositoryInvocationHandler.getArguments();
		}
	}

	private static class RepositoryMethodInterceptor implements MethodInterceptor {
		private String methodName;

		private List<Object> arguments;

		@Override
		public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
			this.methodName = method.getName();
			if (objects != null && objects.length > 1) {
				arguments = new ArrayList<>(Arrays.asList(objects));
				// remove last entry because that will be provided by the
				// RepositoryItemReader
				arguments.remove(objects.length - 1);
			}
			return null;
		}

		String getMethodName() {
			return this.methodName;
		}

		List<Object> getArguments() {
			return arguments;
		}
	}
}
