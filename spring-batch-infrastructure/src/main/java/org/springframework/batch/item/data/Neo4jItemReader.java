/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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
 *
 */
public class Neo4jItemReader<T> extends AbstractPaginatedDataItemReader<T> implements
InitializingBean {

	protected Log logger = LogFactory.getLog(getClass());

	private Neo4jOperations template;

	private String startStatement;
	private String returnStatement;
	private String matchStatement;
	private String whereStatement;
	private String orderByStatement;

	private Class targetType;

	private Map<String, Object> parameterValues;

	private ResultConverter resultConverter;

	public Neo4jItemReader() {
		setName(ClassUtils.getShortName(Neo4jItemReader.class));
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
	 * An optional where fragement of the cypher query.  WHERE is
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

	/**
	 * Used to perform operations against the Neo4J database.
	 *
	 * @param template the Neo4jOperations instance to use
	 * @see Neo4jOperations
	 */
	public void setTemplate(Neo4jOperations template) {
		this.template = template;
	}

	/**
	 * The object type to be returned from each call to {@link #read()}
	 *
	 * @param targetType the type of object to return.
	 */
	public void setTargetType(Class targetType) {
		this.targetType = targetType;
	}

	/**
	 * Set the converter used to convert node to the targetType.  By
	 * default, {@link DefaultConverter} is used.
	 *
	 * @param resultConverter the converter to use.
	 */
	public void setResultConverter(ResultConverter resultConverter) {
		this.resultConverter = resultConverter;
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected Iterator<T> doPageRead() {
		Result<Map<String, Object>> queryResults = template.query(
				generateLimitCypherQuery(), parameterValues);

		if(queryResults != null) {
			if (resultConverter != null) {
				return queryResults.to(targetType, resultConverter).iterator();
			}
			else {
				return queryResults.to(targetType).iterator();
			}
		}
		else {
			return new ArrayList().iterator();
		}
	}

	private String generateLimitCypherQuery() {
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
		Assert.state(template != null, "A Neo4JOperations implementation is required");
		Assert.state(targetType != null, "The type to be returned is required");
		Assert.state(StringUtils.hasText(startStatement), "A START statement is required");
		Assert.state(StringUtils.hasText(returnStatement), "A RETURN statement is required");
		Assert.state(StringUtils.hasText(orderByStatement), "A ORDER BY statement is required");
	}
}
