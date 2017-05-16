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


import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link RepositoryItemWriter}.
 *
 * @author Glenn Renfro
 * @since 4.0
 * @see RepositoryItemWriter
 */
public class RepositoryItemWriterBuilder<T> {

	private CrudRepository<T, ?> repository;

	private String methodName;

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
	 * Builds the {@link RepositoryItemWriter}.
	 *
	 * @return a {@link RepositoryItemWriter}
	 */
	public RepositoryItemWriter<T> build() {
		Assert.hasText(this.methodName, "methodName is required.");
		Assert.notNull(this.repository, "repository is required.");

		RepositoryItemWriter<T> writer = new RepositoryItemWriter<>();
		writer.setMethodName(this.methodName);
		writer.setRepository(this.repository);
		return writer;
	}
}
