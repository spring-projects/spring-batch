/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.batch.item.database.builder;

import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.orm.JpaQueryProvider;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.util.Assert;

/**
 * Builder for {@link JpaCursorItemReader}.
 *
 * @author Mahmoud Ben Hassine
 *
 * @since 4.3
 */
public class JpaCursorItemReaderBuilder<T> {

	private EntityManagerFactory entityManagerFactory;
	private String queryString;
	private JpaQueryProvider queryProvider;
	private Map<String, Object> parameterValues;
	private boolean saveState = true;
	private String name;
	private int maxItemCount = Integer.MAX_VALUE;
	private int currentItemCount;

	/**
	 * Configure if the state of the {@link ItemStreamSupport}
	 * should be persisted within the {@link ExecutionContext}
	 * for restart purposes.
	 *
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public JpaCursorItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * The name used to calculate the key within the {@link ExecutionContext}.
	 * Required if {@link #saveState(boolean)} is set to true.
	 *
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see ItemStreamSupport#setName(String)
	 */
	public JpaCursorItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Configure the max number of items to be read.
	 *
	 * @param maxItemCount the max items to be read
	 * @return The current instance of the builder.
	 * @see AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public JpaCursorItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;

		return this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 *
	 * @param currentItemCount current index
	 * @return this instance for method chaining
	 * @see AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public JpaCursorItemReaderBuilder<T> currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;

		return this;
	}

	/**
	 * A map of parameter values to be set on the query. The key of the map is 
	 * the name of the parameter to be set with the value being the value to be set.
	 *
	 * @param parameterValues map of values
	 * @return this instance for method chaining
	 * @see JpaCursorItemReader#setParameterValues(Map)
	 */
	public JpaCursorItemReaderBuilder<T> parameterValues(Map<String, Object> parameterValues) {
		this.parameterValues = parameterValues;

		return this;
	}

	/**
	 * A query provider. This should be set only if {@link #queryString(String)}
	 * have not been set.
	 *
	 * @param queryProvider the query provider
	 * @return this instance for method chaining
	 * @see JpaCursorItemReader#setQueryProvider(JpaQueryProvider)
	 */
	public JpaCursorItemReaderBuilder<T> queryProvider(JpaQueryProvider queryProvider) {
		this.queryProvider = queryProvider;

		return this;
	}

	/**
	 * The JPQL query string to execute. This should only be set if
	 * {@link #queryProvider(JpaQueryProvider)} has not been set.
	 *
	 * @param queryString the JPQL query
	 * @return this instance for method chaining
	 * @see JpaCursorItemReader#setQueryString(String)
	 */
	public JpaCursorItemReaderBuilder<T> queryString(String queryString) {
		this.queryString = queryString;

		return this;
	}

	/**
	 * The {@link EntityManagerFactory} to be used for executing the configured
	 * {@link #queryString}.
	 *
	 * @param entityManagerFactory {@link EntityManagerFactory} used to create
	 * {@link javax.persistence.EntityManager}
	 * @return this instance for method chaining
	 */
	public JpaCursorItemReaderBuilder<T> entityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;

		return this;
	}

	/**
	 * Returns a fully constructed {@link JpaCursorItemReader}.
	 *
	 * @return a new {@link JpaCursorItemReader}
	 */
	public JpaCursorItemReader<T> build() {
		Assert.notNull(this.entityManagerFactory, "An EntityManagerFactory is required");
		if (this.saveState) {
			Assert.hasText(this.name, "A name is required when saveState is set to true");
		}
		if (this.queryProvider == null) {
			Assert.hasLength(this.queryString, "Query string is required when queryProvider is null");
		}

		JpaCursorItemReader<T> reader = new JpaCursorItemReader<>();
		reader.setEntityManagerFactory(this.entityManagerFactory);
		reader.setQueryProvider(this.queryProvider);
		reader.setQueryString(this.queryString);
		reader.setParameterValues(this.parameterValues);
		reader.setCurrentItemCount(this.currentItemCount);
		reader.setMaxItemCount(this.maxItemCount);
		reader.setSaveState(this.saveState);
		reader.setName(this.name);
		return reader;
	}
}
