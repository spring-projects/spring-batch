/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.batch.item.data;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Restartable {@link ItemReader} that reads objects from the graph database Neo4j
 * via a paging technique.
 * </p>
 *
 * <p>
 * It executes cypher queries built from the statement fragments provided to
 * retrieve the requested data.  The query is executed using paged requests of
 * a size specified in {@link #setPageSize(int)}.  Additional pages are requested
 * as needed when the {@link #read()} method is called.  On restart, the reader
 * will begin again at the same number item it left off at.
 * </p>
 *
 * <p>
 * Performance is dependent on your Neo4J configuration (embedded or remote) as
 * well as page size.  Setting a fairly large page size and using a commit
 * interval that matches the page size should provide better performance.
 * </p>
 *
 * <p>
 * This implementation is thread-safe between calls to
 * {@link #open(org.springframework.batch.item.ExecutionContext)}, however you
 * should set <code>saveState=false</code> if used in a multi-threaded
 * environment (no restart available).
 * </p>
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 3.07
 * @deprecated Extend {@link Neo4jItemReader} instead.
 */
@Deprecated
public abstract class AbstractNeo4jItemReader<T> extends
		AbstractPaginatedDataItemReader<T> implements InitializingBean {

	protected Log logger = LogFactory.getLog(getClass());

	private SessionFactory sessionFactory;

	private String startStatement;
	private String returnStatement;
	private String matchStatement;
	private String whereStatement;
	private String orderByStatement;

	private Class<T> targetType;

	private Map<String, Object> parameterValues;

	/**
	 * Optional parameters to be used in the cypher query.
	 *
	 * @param parameterValues the parameter values to be used in the cypher query
	 */
	public void setParameterValues(Map<String, Object> parameterValues) {
		this.parameterValues = parameterValues;
	}

	protected final Map<String, Object> getParameterValues() {
		return this.parameterValues;
	}

	/**
	 * The start segment of the cypher query.  START is prepended
	 * to the statement provided and should <em>not</em> be
	 * included.
	 *
	 * @param startStatement the start fragment of the cypher query.
	 */
	public void setStartStatement(String startStatement) {
		this.startStatement = startStatement;
	}

	/**
	 * The return statement of the cypher query.  RETURN is prepended
	 * to the statement provided and should <em>not</em> be
	 * included
	 *
	 * @param returnStatement the return fragment of the cypher query.
	 */
	public void setReturnStatement(String returnStatement) {
		this.returnStatement = returnStatement;
	}

	/**
	 * An optional match fragment of the cypher query.  MATCH is
	 * prepended to the statement provided and should <em>not</em>
	 * be included.
	 *
	 * @param matchStatement the match fragment of the cypher query
	 */
	public void setMatchStatement(String matchStatement) {
		this.matchStatement = matchStatement;
	}

	/**
	 * An optional where fragment of the cypher query.  WHERE is
	 * prepended to the statement provided and should <em>not</em>
	 * be included.
	 *
	 * @param whereStatement where fragment of the cypher query
	 */
	public void setWhereStatement(String whereStatement) {
		this.whereStatement = whereStatement;
	}

	/**
	 * A list of properties to order the results by.  This is
	 * required so that subsequent page requests pull back the
	 * segment of results correctly.  ORDER BY is prepended to
	 * the statement provided and should <em>not</em> be included.
	 *
	 * @param orderByStatement order by fragment of the cypher query.
	 */
	public void setOrderByStatement(String orderByStatement) {
		this.orderByStatement = orderByStatement;
	}

	protected SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	/**
	 * Establish the session factory for the reader.
	 * @param sessionFactory the factory to use for the reader.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * The object type to be returned from each call to {@link #read()}
	 *
	 * @param targetType the type of object to return.
	 */
	public void setTargetType(Class<T> targetType) {
		this.targetType = targetType;
	}

	protected final Class<T> getTargetType() {
		return this.targetType;
	}

	protected String generateLimitCypherQuery() {
		StringBuilder query = new StringBuilder();

		query.append("START ").append(startStatement);
		query.append(matchStatement != null ? " MATCH " + matchStatement : "");
		query.append(whereStatement != null ? " WHERE " + whereStatement : "");
		query.append(" RETURN ").append(returnStatement);
		query.append(" ORDER BY ").append(orderByStatement);
		query.append(" SKIP " + (pageSize * page));
		query.append(" LIMIT " + pageSize);

		String resultingQuery = query.toString();

		if (logger.isDebugEnabled()) {
			logger.debug(resultingQuery);
		}

		return resultingQuery;
	}

	/**
	 * Checks mandatory properties
	 *
	 * @see InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(sessionFactory != null,"A SessionFactory is required");
		Assert.state(targetType != null, "The type to be returned is required");
		Assert.state(StringUtils.hasText(startStatement), "A START statement is required");
		Assert.state(StringUtils.hasText(returnStatement), "A RETURN statement is required");
		Assert.state(StringUtils.hasText(orderByStatement), "A ORDER BY statement is required");
	}
}
