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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.builder.AbstractItemCountingItemStreamItemReaderBuilder;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
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

public class RepositoryItemReaderBuilder<T> extends AbstractItemCountingItemStreamItemReaderBuilder<RepositoryItemReaderBuilder<T>> {

	private PagingAndSortingRepository<?, ?> repository;

	private Map<String, Sort.Direction> sorts;

	private List<?> arguments;

	private int pageSize = 10;

	private String methodName;

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
	 * Specifies a repository and the type-safe method to call for the reader. This method
	 * must take {@link org.springframework.data.domain.Pageable} as the <em>last</em>
	 * argument. This method can be used in place of {@link #methodName(String)} and
	 * {@link #repository(PagingAndSortingRepository)}.
	 *
	 * @param repositoryReference of the used to get a repository and type-safe method for
	 * use by the reader.
	 * @return The current instance of the builder.
	 * @see RepositoryItemReader#setMethodName(String)
	 * @see RepositoryItemReader#setRepository(PagingAndSortingRepository)
	 *
	 */
	public RepositoryItemReaderBuilder<T> repository(RepositoryMethodReference repositoryReference) {
		Assert.notNull(repositoryReference, "repositoryReference must not be null.");
		this.methodName = repositoryReference.getMethodName();
		this.repository = repositoryReference.getRepository();

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

	/**
	 * Establishes a proxy that will capture a the Repository and the associated
	 * methodName that will be used by the reader.
	 * @param <T> The type of repository that will be used by the reader.
	 */
	public static class RepositoryMethodReference<T> {
		private RepositoryMethodIterceptor repositoryInvocationHandler;

		private PagingAndSortingRepository<?, ?> repository;

		public RepositoryMethodReference(PagingAndSortingRepository<?, ?> repository) {
			this.repository = repository;
			this.repositoryInvocationHandler = new RepositoryMethodIterceptor();
		}

		// T is a proxy of the object passed in in the constructor
		public T methodIs() {

			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(this.repository.getClass());
			enhancer.setCallback(this.repositoryInvocationHandler);
			return (T) enhancer.create();
		}

		public PagingAndSortingRepository<?, ?> getRepository() {
			return this.repository;
		}

		public String getMethodName() {
			return this.repositoryInvocationHandler.getMethodName();
		}

	}

	private static class RepositoryMethodIterceptor implements MethodInterceptor {
		private String methodName;

		@Override
		public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
			this.methodName = method.getName();
			return null;
		}

		public String getMethodName() {
			return this.methodName;
		}
	}
}
