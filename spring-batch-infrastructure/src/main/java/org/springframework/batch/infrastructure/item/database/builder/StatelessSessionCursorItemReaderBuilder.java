/*
 * Copyright 2026 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database.builder;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;
import org.springframework.batch.infrastructure.item.database.StatelessSessionCursorItemReader;
import org.springframework.batch.infrastructure.item.database.orm.NamedSelectionQueryProvider;
import org.springframework.batch.infrastructure.item.database.orm.NativeSelectionQueryProvider;
import org.springframework.batch.infrastructure.item.database.orm.SelectionQueryProvider;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.util.Assert;

/**
 * Builder for {@link StatelessSessionCursorItemReader}.
 *
 * @author Philippe Marschall
 * @since 6.1
 */
public class StatelessSessionCursorItemReaderBuilder<T> {

	private @Nullable SessionFactory sessionFactory;

	private @Nullable String queryString;

	private @Nullable SelectionQueryProvider<T> selectionQueryProvider;

	private @Nullable Map<String, Object> parameterValues;

	private @Nullable Map<String, Object> hintValues;

	private boolean saveState = true;

	private @Nullable String name;

	private int maxItemCount = Integer.MAX_VALUE;

	private int currentItemCount;

	private final Class<T> itemType;

	/**
	 * Create a new {@link StatelessSessionCursorItemReader}.
	 * @param itemType the item type.
	 */
	public StatelessSessionCursorItemReaderBuilder(Class<T> itemType) {
		Assert.notNull(itemType, "itemType must not be null.");
		this.itemType = itemType;
	}

	/**
	 * Configure if the state of the {@link ItemStreamSupport} should be persisted within
	 * the {@link ExecutionContext} for restart purposes.
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public StatelessSessionCursorItemReaderBuilder<T> saveState(boolean saveState) {
		this.saveState = saveState;

		return this;
	}

	/**
	 * The name used to calculate the key within the {@link ExecutionContext}. Defaults to
	 * the bean name, or to the short class name if this instance is not a bean. Set it
	 * explicitly to disambiguate several non-bean instances of the same type in a step.
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see ItemStreamSupport#setName(String)
	 */
	public StatelessSessionCursorItemReaderBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	/**
	 * Configure the max number of items to be read.
	 * @param maxItemCount the max items to be read
	 * @return The current instance of the builder.
	 * @see AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public StatelessSessionCursorItemReaderBuilder<T> maxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;

		return this;
	}

	/**
	 * Index for the current item. Used on restarts to indicate where to start from.
	 * @param currentItemCount current index
	 * @return this instance for method chaining
	 * @see AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public StatelessSessionCursorItemReaderBuilder<T> currentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;

		return this;
	}

	/**
	 * A map of parameter values to be set on the query. The key of the map is the name of
	 * the parameter to be set with the value being the value to be set.
	 * @param parameterValues map of values
	 * @return this instance for method chaining
	 * @see StatelessSessionCursorItemReader#setParameterValues(Map)
	 */
	public StatelessSessionCursorItemReaderBuilder<T> parameterValues(Map<String, Object> parameterValues) {
		this.parameterValues = parameterValues;

		return this;
	}

	/**
	 * A map of hint values to be set on the query. The key of the map is the name of the
	 * hint to be applied, with the value being the specific setting for that hint.
	 * @param hintValues map of query hints
	 * @return this instance for method chaining
	 * @see StatelessSessionCursorItemReader#setHintValues(Map)
	 */
	public StatelessSessionCursorItemReaderBuilder<T> hintValues(Map<String, Object> hintValues) {
		this.hintValues = hintValues;
		return this;
	}

	/**
	 * A query provider. This should be set only if {@link #queryString(String)} have not
	 * been set.
	 * @param selectionQueryProvider the query provider
	 * @return this instance for method chaining
	 * @see StatelessSessionCursorItemReader#setSelectionQueryProvider(SelectionQueryProvider)
	 */
	public StatelessSessionCursorItemReaderBuilder<T> selectionQueryProvider(
			SelectionQueryProvider<T> selectionQueryProvider) {
		this.selectionQueryProvider = selectionQueryProvider;

		return this;
	}

	/**
	 * Convenience method that set up a NativeSelectionQueryProvider with the given SQL
	 * query.
	 * @param sql the SQL query string
	 * @return this instance for method chaining
	 * @see #selectionQueryProvider(SelectionQueryProvider)
	 * @see NativeSelectionQueryProvider
	 */
	public StatelessSessionCursorItemReaderBuilder<T> nativeQuery(String sql) {
		return this.selectionQueryProvider(new NativeSelectionQueryProvider<>(sql, this.itemType));
	}

	/**
	 * Convenience method that set up a NamedSelectionQueryProvider with the given name.
	 * @param name the name of the query
	 * @return this instance for method chaining
	 * @see #selectionQueryProvider(SelectionQueryProvider)
	 * @see NamedSelectionQueryProvider
	 */
	public StatelessSessionCursorItemReaderBuilder<T> namedQuery(String name) {
		return this.selectionQueryProvider(new NamedSelectionQueryProvider<>(name, this.itemType));
	}

	/**
	 * The HQL query string to execute. This should only be set if
	 * {@link #selectionQueryProvider(SelectionQueryProvider)} has not been set.
	 * @param queryString the HQL query
	 * @return this instance for method chaining
	 * @see StatelessSessionCursorItemReader#setQueryString(String)
	 */
	public StatelessSessionCursorItemReaderBuilder<T> queryString(String queryString) {
		this.queryString = queryString;

		return this;
	}

	/**
	 * The {@link SessionFactory} to be used for executing the configured
	 * {@link #queryString}.
	 * @param sessionFactory {@link SessionFactory} used to create
	 * {@link org.hibernate.StatelessSession}
	 * @return this instance for method chaining
	 */
	public StatelessSessionCursorItemReaderBuilder<T> sessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;

		return this;
	}

	/**
	 * Returns a fully constructed {@link StatelessSessionCursorItemReader}.
	 * @return a new {@link StatelessSessionCursorItemReader}
	 */
	public StatelessSessionCursorItemReader<T> build() {
		Assert.notNull(this.sessionFactory, "An SessionFactory is required");
		if (this.selectionQueryProvider == null) {
			Assert.hasLength(this.queryString, "Query string is required when queryProvider is null");
		}

		StatelessSessionCursorItemReader<T> reader = new StatelessSessionCursorItemReader<>(this.sessionFactory,
				this.itemType);
		if (this.selectionQueryProvider != null) {
			reader.setSelectionQueryProvider(this.selectionQueryProvider);
		}
		if (this.queryString != null) {
			reader.setQueryString(this.queryString);
		}
		if (this.parameterValues != null) {
			reader.setParameterValues(this.parameterValues);
		}
		if (this.hintValues != null) {
			reader.setHintValues(this.hintValues);
		}
		reader.setCurrentItemCount(this.currentItemCount);
		reader.setMaxItemCount(this.maxItemCount);
		reader.setSaveState(this.saveState);
		if (this.name != null) {
			reader.setName(this.name);
		}

		return reader;
	}

}
