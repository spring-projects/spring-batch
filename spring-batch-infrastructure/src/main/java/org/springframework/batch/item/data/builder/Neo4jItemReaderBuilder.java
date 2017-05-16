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

import java.util.Map;

import org.neo4j.ogm.session.SessionFactory;

import org.springframework.batch.item.builder.AbstractItemCountingItemStreamItemReaderBuilder;
import org.springframework.batch.item.data.Neo4jItemReader;
import org.springframework.util.Assert;

/**
 * A builder for the {@link Neo4jItemReader}.
 *
 * @author Glenn Renfro
 * @since 4.0
 * @see Neo4jItemReader
 */
public class Neo4jItemReaderBuilder<T>
		extends AbstractItemCountingItemStreamItemReaderBuilder<Neo4jItemReaderBuilder<T>> {

	private SessionFactory sessionFactory;

	private String startStatement;

	private String returnStatement;

	private String matchStatement;

	private String whereStatement;

	private String orderByStatement;

	private Class<T> targetType;

	private Map<String, Object> parameterValues;

	private int pageSize = 10;

	/**
	 * Establish the session factory for the reader.
	 * @param sessionFactory the factory to use for the reader.
	 * @return this instance for method chaining
	 * @see Neo4jItemReader#setSessionFactory(SessionFactory)
	 */
	public Neo4jItemReaderBuilder<T> sessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;

		return this;
	}

	/**
	 * The number of items to be read with each page.
	 *
	 * @param pageSize the number of items
	 * @return this instance for method chaining
	 * @see Neo4jItemReader#setPageSize(int)
	 */
	public Neo4jItemReaderBuilder<T> pageSize(int pageSize) {
		this.pageSize = pageSize;

		return this;
	}

	/**
	 * Optional parameters to be used in the cypher query.
	 *
	 * @param parameterValues the parameter values to be used in the cypher query
	 * @return this instance for method chaining
	 * @see Neo4jItemReader#setParameterValues(Map)
	 */
	public Neo4jItemReaderBuilder<T> parameterValues(Map<String, Object> parameterValues) {
		this.parameterValues = parameterValues;

		return this;
	}

	/**
	 * The start segment of the cypher query. START is prepended to the statement provided
	 * and should <em>not</em> be included.
	 *
	 * @param startStatement the start fragment of the cypher query.
	 * @return this instance for method chaining
	 * @see Neo4jItemReader#setStartStatement(String)
	 */
	public Neo4jItemReaderBuilder<T> startStatement(String startStatement) {
		this.startStatement = startStatement;

		return this;
	}

	/**
	 * The return statement of the cypher query. RETURN is prepended to the statement
	 * provided and should <em>not</em> be included
	 *
	 * @param returnStatement the return fragment of the cypher query.
	 * @return this instance for method chaining
	 * @see Neo4jItemReader#setReturnStatement(String)
	 */
	public Neo4jItemReaderBuilder<T> returnStatement(String returnStatement) {
		this.returnStatement = returnStatement;

		return this;
	}

	/**
	 * An optional match fragment of the cypher query. MATCH is prepended to the statement
	 * provided and should <em>not</em> be included.
	 *
	 * @param matchStatement the match fragment of the cypher query
	 * @return this instance for method chaining
	 * @see Neo4jItemReader#setMatchStatement(String)
	 */
	public Neo4jItemReaderBuilder<T> matchStatement(String matchStatement) {
		this.matchStatement = matchStatement;

		return this;
	}

	/**
	 * An optional where fragment of the cypher query. WHERE is prepended to the statement
	 * provided and should <em>not</em> be included.
	 *
	 * @param whereStatement where fragment of the cypher query
	 * @return this instance for method chaining
	 * @see Neo4jItemReader#setWhereStatement(String)
	 */
	public Neo4jItemReaderBuilder<T> whereStatement(String whereStatement) {
		this.whereStatement = whereStatement;

		return this;
	}

	/**
	 * A list of properties to order the results by. This is required so that subsequent
	 * page requests pull back the segment of results correctly. ORDER BY is prepended to
	 * the statement provided and should <em>not</em> be included.
	 *
	 * @param orderByStatement order by fragment of the cypher query.
	 * @return this instance for method chaining
	 * @see Neo4jItemReader#setOrderByStatement(String)
	 */
	public Neo4jItemReaderBuilder<T> orderByStatement(String orderByStatement) {
		this.orderByStatement = orderByStatement;

		return this;
	}

	/**
	 * The object type to be returned from each call to {@link Neo4jItemReader#read()}
	 *
	 * @param targetType the type of object to return.
	 * @return this instance for method chaining
	 * @see Neo4jItemReader#setTargetType(Class)
	 */
	public Neo4jItemReaderBuilder<T> targetType(Class<T> targetType) {
		this.targetType = targetType;

		return this;
	}

	/**
	 * Returns a fully constructed {@link Neo4jItemReader}.
	 *
	 * @return a new {@link Neo4jItemReader}
	 */
	public Neo4jItemReader<T> build() {
		if (this.saveState) {
			Assert.hasText(this.name, "A name is required when saveState is set to true");
		}
		Assert.notNull(this.sessionFactory, "sessionFactory is required.");
		Assert.notNull(this.targetType, "targetType is required.");
		Assert.hasText(this.startStatement, "startStatement is required.");
		Assert.hasText(this.returnStatement, "returnStatement is required.");
		Assert.hasText(this.orderByStatement, "orderByStatement is required.");

		Neo4jItemReader<T> reader = new Neo4jItemReader<>();
		reader.setMatchStatement(this.matchStatement);
		reader.setOrderByStatement(this.orderByStatement);
		reader.setPageSize(this.pageSize);
		reader.setParameterValues(this.parameterValues);
		reader.setSessionFactory(this.sessionFactory);
		reader.setTargetType(this.targetType);
		reader.setStartStatement(this.startStatement);
		reader.setReturnStatement(this.returnStatement);
		reader.setWhereStatement(this.whereStatement);
		reader.setName(this.name);
		reader.setSaveState(this.saveState);
		reader.setCurrentItemCount(this.currentItemCount);
		reader.setMaxItemCount(this.maxItemCount);

		return reader;
	}

}
