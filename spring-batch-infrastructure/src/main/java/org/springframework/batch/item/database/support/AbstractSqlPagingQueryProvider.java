/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.batch.item.database.support;

import javax.sql.DataSource;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.batch.item.database.JdbcParameterUtils;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.util.List;
import java.util.ArrayList;

/**
 * Abstract SQL Paging Query Provider to serve as a base class for all provided
 * SQL paging query providers.
 * 
 * Any implementation must provide a way to specify the select clause, from
 * clause and optionally a where clause. In addition a way to specify a single
 * column sort key must also be provided. This sort key will be used to provide
 * the paging functionality. It is recommended that there should be an index for
 * the sort key to provide better performance.
 * 
 * Provides properties and preparation for the mandatory "selectClause" and
 * "fromClause" as well as for the optional "whereClause". Also provides
 * property for the mandatory "sortKey".
 * 
 * @author Thomas Risberg
 * @author Dave Syer
 * @since 2.0
 */
public abstract class AbstractSqlPagingQueryProvider implements PagingQueryProvider {

	private String selectClause;

	private String fromClause;

	private String whereClause;

	private String sortKey;

	private boolean ascending = true;

	private int parameterCount;

	private boolean usingNamedParameters;

	/**
	 * @param selectClause SELECT clause part of SQL query string
	 */
	public void setSelectClause(String selectClause) {
		this.selectClause = removeKeyWord("select", selectClause);
	}

	/**
	 * 
	 * @return SQL SELECT clause part of SQL query string
	 */
	protected String getSelectClause() {
		return selectClause;
	}

	/**
	 * @param fromClause FROM clause part of SQL query string
	 */
	public void setFromClause(String fromClause) {
		this.fromClause = removeKeyWord("from", fromClause);
	}

	/**
	 * 
	 * @return SQL FROM clause part of SQL query string
	 */
	protected String getFromClause() {
		return fromClause;
	}

	/**
	 * @param whereClause WHERE clause part of SQL query string
	 */
	public void setWhereClause(String whereClause) {
		if (StringUtils.hasText(whereClause)) {
			this.whereClause = removeKeyWord("where", whereClause);
		}
		else {
			this.whereClause = null;
		}
	}

	/**
	 * 
	 * @return SQL WHERE clause part of SQL query string
	 */
	protected String getWhereClause() {
		return whereClause;
	}

	/**
	 * @param sortKey key to use to sort and limit page content
	 */
	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	/**
	 * Set the flag that signals that the sort key is applied ascending (default
	 * true).
	 * 
	 * @param ascending the ascending value to set
	 */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	/**
	 * Get the flag that signals that the sort key is applied ascending.
	 * 
	 * @return the ascending flag
	 */
	public boolean isAscending() {
		return ascending;
	}

	/**
	 *
	 * @return sortKey key to use to sort and limit page content
	 */
	public String getSortKey() {
		return sortKey;
	}

	/**
	 *
	 * @return sortKey key to use to sort and limit page content (without alias)
	 */
	public String getSortKeyWithoutAlias() {
		String sortKey = getSortKey();
		int separator = sortKey.indexOf('.');
		if (separator > 0) {
			int columnIndex = separator + 1;
			if (columnIndex < sortKey.length()) {
				sortKey = sortKey.substring(columnIndex);
			}
		}
		return sortKey;
	}

	public int getParameterCount() {
		return parameterCount;
	}

	public boolean isUsingNamedParameters() {
		return usingNamedParameters;
	}

	/**
	 * The sort key placeholder will vary depending on whether named parameters
	 * or traditional placeholders are used in query strings.
	 * 
	 * @return place holder for sortKey.
	 */
	protected String getSortKeyPlaceHolder() {
		return usingNamedParameters ? ":_sortKey" : "?";
	}

	/**
	 * Check mandatory properties.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void init(DataSource dataSource) throws Exception {
		Assert.notNull(dataSource);
		Assert.hasLength(selectClause, "selectClause must be specified");
		Assert.hasLength(fromClause, "fromClause must be specified");
		Assert.hasLength(sortKey, "sortKey must be specified");
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(selectClause);
		sql.append(" FROM ").append(fromClause);
		if (whereClause != null) {
			sql.append(" WHERE ").append(whereClause);
		}
		List<String> namedParameters = new ArrayList<String>();
		parameterCount = JdbcParameterUtils.countParameterPlaceholders(sql.toString(), namedParameters);
		if (namedParameters.size() > 0) {
			if (parameterCount != namedParameters.size()) {
				throw new InvalidDataAccessApiUsageException(
						"You can't use both named parameters and classic \"?\" placeholders: " + sql);
			}
			usingNamedParameters = true;
		}
	}

	/**
	 * Method generating the query string to be used for retrieving the first
	 * page. This method must be implemented in sub classes.
	 * 
	 * @param pageSize number of rows to read per page
	 * @return query string
	 */
	public abstract String generateFirstPageQuery(int pageSize);

	/**
	 * Method generating the query string to be used for retrieving the pages
	 * following the first page. This method must be implemented in sub classes.
	 * 
	 * @param pageSize number of rows to read per page
	 * @return query string
	 */
	public abstract String generateRemainingPagesQuery(int pageSize);

	/**
	 * Method generating the query string to be used for jumping to a specific
	 * item position. This method must be implemented in sub classes.
	 * 
	 * @param itemIndex the index of the item to jump to
	 * @param pageSize number of rows to read per page
	 * @return query string
	 */
	public abstract String generateJumpToItemQuery(int itemIndex, int pageSize);

	private String removeKeyWord(String keyWord, String clause) {
		String temp = clause.trim();
		String keyWordString = keyWord + " ";
		if (temp.toLowerCase().startsWith(keyWordString) && temp.length() > keyWordString.length()) {
			return temp.substring(keyWordString.length());
		}
		else {
			return temp;
		}
	}

}
