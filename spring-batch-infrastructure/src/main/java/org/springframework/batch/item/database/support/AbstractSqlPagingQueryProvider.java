/*
 * Copyright 2006-2020 the original author or authors.
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

package org.springframework.batch.item.database.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.batch.item.database.JdbcParameterUtils;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
 * property for the mandatory "sortKeys".  <b>Note:</b> The columns that make up 
 * the sort key must be a true key and not just a column to order by. It is important
 * to have a unique key constraint on the sort key to guarantee that no data is lost
 * between executions.
 * 
 * @author Thomas Risberg
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Benjamin Hetz
 * @since 2.0
 */
public abstract class AbstractSqlPagingQueryProvider implements PagingQueryProvider {

	private String selectClause;

	private String fromClause;

	private String whereClause;
	
	private Map<String, Order> sortKeys = new LinkedHashMap<>();

	private String groupClause;

	private int parameterCount;

	private boolean usingNamedParameters;
	
	/**
	 * The setter for the group by clause
	 * 
	 * @param groupClause SQL GROUP BY clause part of the SQL query string
	 */
	public void setGroupClause(String groupClause) {
		if (StringUtils.hasText(groupClause)) {
			this.groupClause = removeKeyWord("group by", groupClause);
		}
		else {
			this.groupClause = null;
		}
	}
	
	/**
	 * The getter for the group by clause
	 * 
	 * @return SQL GROUP BY clause part of the SQL query string
	 */
	public String getGroupClause() {
		return this.groupClause;
	}

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
	 * @param sortKeys key to use to sort and limit page content
	 */
	public void setSortKeys(Map<String, Order> sortKeys) {
		this.sortKeys = sortKeys;
	}

	/**
	 * A Map&lt;String, Boolean&gt; of sort columns as the key and boolean for ascending/descending (ascending = true).
	 * 
	 * @return sortKey key to use to sort and limit page content
	 */
    @Override
	public Map<String, Order> getSortKeys() {
		return sortKeys;
	}

    @Override
	public int getParameterCount() {
		return parameterCount;
	}

    @Override
	public boolean isUsingNamedParameters() {
		return usingNamedParameters;
	}

	/**
	 * The sort key placeholder will vary depending on whether named parameters
	 * or traditional placeholders are used in query strings.
	 * 
	 * @return place holder for sortKey.
	 */
    @Override
	public String getSortKeyPlaceHolder(String keyName) {
		return usingNamedParameters ? ":_" + keyName : "?";
	}

	/**
	 * Check mandatory properties.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
    @Override
	public void init(DataSource dataSource) throws Exception {
		Assert.notNull(dataSource, "A DataSource is required");
		Assert.hasLength(selectClause, "selectClause must be specified");
		Assert.hasLength(fromClause, "fromClause must be specified");
		Assert.notEmpty(sortKeys, "sortKey must be specified");
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(selectClause);
		sql.append(" FROM ").append(fromClause);
		if (whereClause != null) {
			sql.append(" WHERE ").append(whereClause);
		}
		if(groupClause != null) {
			sql.append(" GROUP BY ").append(groupClause);
		}
		List<String> namedParameters = new ArrayList<>();
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
    @Override
	public abstract String generateFirstPageQuery(int pageSize);

	/**
	 * Method generating the query string to be used for retrieving the pages
	 * following the first page. This method must be implemented in sub classes.
	 * 
	 * @param pageSize number of rows to read per page
	 * @return query string
	 */
    @Override
	public abstract String generateRemainingPagesQuery(int pageSize);

	/**
	 * Method generating the query string to be used for jumping to a specific
	 * item position. This method must be implemented in sub classes.
	 * 
	 * @param itemIndex the index of the item to jump to
	 * @param pageSize number of rows to read per page
	 * @return query string
	 */
    @Override
	public abstract String generateJumpToItemQuery(int itemIndex, int pageSize);

	private String removeKeyWord(String keyWord, String clause) {
		String temp = clause.trim();
		int length = keyWord.length();
		if (temp.toLowerCase().startsWith(keyWord) && Character.isWhitespace(temp.charAt(length)) && temp.length() > length + 1) {
			return temp.substring(length + 1);
		}
		else {
			return temp;
		}
	}

	/**
	 *
	 * @return sortKey key to use to sort and limit page content (without alias)
	 */
	@Override
	public Map<String, Order> getSortKeysWithoutAliases() {
		Map<String, Order> sortKeysWithoutAliases = new LinkedHashMap<>();

		for (Map.Entry<String, Order> sortKeyEntry : sortKeys.entrySet()) {
			String key = sortKeyEntry.getKey();
			int separator = key.indexOf('.');
			if (separator > 0) {
				int columnIndex = separator + 1;
				if (columnIndex < key.length()) {
					sortKeysWithoutAliases.put(key.substring(columnIndex), sortKeyEntry.getValue());
				}
			} else {
				sortKeysWithoutAliases.put(sortKeyEntry.getKey(), sortKeyEntry.getValue());
			}
		}

		return sortKeysWithoutAliases;
	}
}
