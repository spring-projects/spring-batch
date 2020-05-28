/*
 * Copyright 2017-2020 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link RepositoryItemWriter}.
 *
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @since 4.0
 * @see RepositoryItemWriter
 */
public class RepositoryItemWriterBuilder<T> {

	private static final Log logger = LogFactory.getLog(RepositoryItemWriterBuilder.class.getName());

	private CrudRepository<T, ?> repository;

	private String methodName;

	private RepositoryMethodReference repositoryMethodReference;

	/**
	 * Specifies what method on the repository to call. This method must have the type of
	 * object passed to this writer as the <em>sole</em> argument.
	 *
	 * @param methodName the name of the method to be used for saving the item.
	 * @return The current instance of the builder.
	 * @see RepositoryItemWriter#setMethodName(String)
	 */
	public RepositoryItemWriterBuilder<T> methodName(String methodName) {
		this.methodName = methodName;

		return this;
	}

	/**
	 * Set the {@link org.springframework.data.repository.CrudRepository} implementation
	 * for persistence
	 *
	 * @param repository the Spring Data repository to be set
	 * @return The current instance of the builder.
	 * @see RepositoryItemWriter#setRepository(CrudRepository)
	 */
	public RepositoryItemWriterBuilder<T> repository(CrudRepository<T, ?> repository) {
		this.repository = repository;

		return this;
	}

	/**
	 * Specifies a repository and the type-safe method to call for the writer. The method
	 * configured via this mechanism must take
	 * {@link org.springframework.data.domain.Pageable} as the <em>last</em>
	 * argument. This method can be used in place of {@link #repository(CrudRepository)},
	 * {@link #methodName(String)}}.
	 *
	 * Note: The repository that is used by the repositoryMethodReference must be
	 * non-final.
	 *
	 * @param repositoryMethodReference of the used to get a repository and type-safe
	 * method for use by the writer.
	 * @return The current instance of the builder.
	 * @see RepositoryItemWriter#setMethodName(String)
	 * @see RepositoryItemWriter#setRepository(CrudRepository)
	 *
	 */
	public RepositoryItemWriterBuilder<T> repository(RepositoryItemWriterBuilder.RepositoryMethodReference repositoryMethodReference) {
		this.repositoryMethodReference = repositoryMethodReference;

		return this;
	}

	/**
	 * Builds the {@link RepositoryItemWriter}.
	 *
	 * @return a {@link RepositoryItemWriter}
	 */
	public RepositoryItemWriter<T> build() {
		if (this.repositoryMethodReference != null) {
			this.methodName = this.repositoryMethodReference.getMethodName();
			this.repository = this.repositoryMethodReference.getRepository();
		}

		Assert.notNull(this.repository, "repository is required.");

		RepositoryItemWriter<T> writer = new RepositoryItemWriter<>();
		writer.setRepository(this.repository);
		if (this.methodName != null) {
			Assert.hasText(this.methodName, "methodName must not be empty.");
			writer.setMethodName(this.methodName);
		} else {
			logger.debug("No method name provided, CrudRepository.saveAll will be used.");
		}
		return writer;
	}

	/**
	 * Establishes a proxy that will capture a the Repository and the associated
	 * methodName that will be used by the writer.
	 * @param <T> The type of repository that will be used by the writer.  The class must
	 * not be final.
	 */
	public static class RepositoryMethodReference<T> {
		private RepositoryMethodInterceptor repositoryInvocationHandler;

		private CrudRepository<?, ?> repository;

		public RepositoryMethodReference(CrudRepository<?, ?> repository) {
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

		CrudRepository<?, ?> getRepository() {
			return this.repository;
		}

		String getMethodName() {
			return this.repositoryInvocationHandler.getMethodName();
		}
	}

	private static class RepositoryMethodInterceptor implements MethodInterceptor {
		private String methodName;

		@Override
		public Object intercept(Object o, Method method, Object[] objects,
				MethodProxy methodProxy) throws Throwable {
			this.methodName = method.getName();
			return null;
		}

		String getMethodName() {
			return this.methodName;
		}

	}
}
