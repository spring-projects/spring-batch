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
package org.springframework.batch.infrastructure.item.database;

import java.util.Map;
import java.util.Objects;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.query.SelectionQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.batch.infrastructure.item.database.orm.SelectionQueryProvider;

/**
 * {@link ItemStreamReader} implementation based on Hibernate
 * {@link SelectionQuery#scroll()}. It executes the HQL query when initialized and
 * iterates over the result set as {@link #read()} method is called, returning an object
 * corresponding to the current row. The query can be set directly using
 * {@link #setQueryString(String)}, or using a query provider via
 * {@link #setSelectionQueryProvider(selectionQueryProvider)}.
 * <p>
 * The implementation is <b>not</b> thread-safe.
 *
 * @author Philippe Marschall
 * @param <T> type of item to read
 * @since 6.1
 */
public class StatelessSessionCursorItemReader<T> extends AbstractItemCountingItemStreamItemReader<T>
		implements InitializingBean {

	private final SessionFactory sessionFactory;

	private @Nullable StatelessSession statelessSession;

	private @Nullable String queryString;

	private @Nullable SelectionQueryProvider<T> selectionQueryProvider;

	private @Nullable Map<String, Object> parameterValues;

	private @Nullable Map<String, Object> hintValues;

	private @Nullable ScrollMode scrollMode;

	private final Class<T> itemType;

	private @Nullable ScrollableResults<T> scrollableResults;

	private @Nullable SelectionQuery<T> query;

	/**
	 * Create a new {@link StatelessSessionCursorItemReader}.
	 * @param sessionFactory the Hibernate session factory.
	 * @param itemType the item type.
	 */
	public StatelessSessionCursorItemReader(SessionFactory sessionFactory, Class<T> itemType) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null.");
		this.sessionFactory = sessionFactory;
		Assert.notNull(itemType, "itemType must not be null.");
		this.itemType = itemType;
	}

	/**
	 * Set the Hibernate selection query provider.
	 * @param selectionQueryProvider Hibernate selection query provider
	 */
	public void setSelectionQueryProvider(SelectionQueryProvider<T> selectionQueryProvider) {
		this.selectionQueryProvider = selectionQueryProvider;
	}

	/**
	 * Set the HQL query string.
	 * @param queryString HQL query string
	 */
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	/**
	 * Set the parameter values to be used for the query execution.
	 * @param parameterValues the values keyed by parameter names used in the query
	 * string.
	 */
	public void setParameterValues(Map<String, Object> parameterValues) {
		this.parameterValues = parameterValues;
	}

	/**
	 * Set the query hint values for the Hibernate selection query. Query hints can be
	 * used to give instructions to Hibernate.
	 * @param hintValues a map where each key is the name of the hint, and the
	 * corresponding value is the hint's value.
	 */
	public void setHintValues(Map<String, Object> hintValues) {
		this.hintValues = hintValues;
	}

	/**
	 * Set the scroll mode. The capabilities of the {@link ScrollableResults} used byte
	 * this reader depend on the specified {@link ScrollMode}.
	 * @param scrollMode the scroll mode used to create the {@link ScrollableResults}
	 */
	public void setScrollMode(ScrollMode scrollMode) {
		this.scrollMode = scrollMode;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.selectionQueryProvider == null) {
			Assert.state(StringUtils.hasLength(this.queryString),
					"Query string is required when queryProvider is null");
		}
	}

	@Override
	protected void doOpen() {
		this.statelessSession = this.sessionFactory.withStatelessOptions().open();
		if (this.statelessSession == null) {
			throw new DataAccessResourceFailureException("Unable to create an StatelessSession");
		}
		this.query = createQuery();
		if (this.parameterValues != null) {
			this.parameterValues.forEach(this.query::setParameter);
		}
		if (this.hintValues != null) {
			this.hintValues.forEach(this.query::setHint);
		}
	}

	protected ScrollableResults<T> getScrollableResults() {
		// delay opening so maxResults can be set
		int maxItemCount = this.getMaxItemCount();
		if (maxItemCount < Integer.MAX_VALUE) {
			Objects.requireNonNull(this.query).setMaxResults(maxItemCount);
		}
		if (this.scrollableResults == null) {
			if (this.scrollMode != null) {
				this.scrollableResults = Objects.requireNonNull(this.query).scroll(this.scrollMode);
			}
			else {
				this.scrollableResults = Objects.requireNonNull(this.query).scroll();
			}
		}
		return this.scrollableResults;
	}

	@Override
	protected void jumpToItem(int itemIndex) {
		Objects.requireNonNull(this.query).setFirstResult(itemIndex);
	}

	private SelectionQuery<T> createQuery() {
		if (this.selectionQueryProvider == null) {
			return Objects.requireNonNull(this.statelessSession).createSelectionQuery(this.queryString, this.itemType);
		}
		else {
			return this.selectionQueryProvider.createSelectionQuery(Objects.requireNonNull(this.statelessSession));
		}
	}

	@Override
	protected @Nullable T doRead() {
		return this.getScrollableResults().next() ? this.getScrollableResults().get() : null;
	}

	@Override
	protected void doClose() {
		if (this.scrollableResults != null) {
			this.scrollableResults.close();
			// so that it will be re-created on re-open
			this.scrollableResults = null;
		}
		if (this.statelessSession != null) {
			this.statelessSession.close();
		}
	}

}
