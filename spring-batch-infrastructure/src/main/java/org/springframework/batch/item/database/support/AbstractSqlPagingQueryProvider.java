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

/**
 * Abstract SQL Paging Query Provider to serve as a base class for all provided SQL paging query providers.
 *
 * Any implementation must provide a way to specify the select clause, from clause and optionally a where clause. 
 * In addition a way to specify a single column sort key must also be provided. This sort key will be used to 
 * provide the paging functionality. It is recommended that there should be an index for the sort key to provide 
 * better performance.
 *
 * Provides properties and preparation for the mandatory "selectClause" and "fromClause" as well as for the
 * optional "whereClause".  Also provides property for the mandatory "sortKey".
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public abstract class AbstractSqlPagingQueryProvider implements PagingQueryProvider {

	private String selectClause;

	private String fromClause;

	private String whereClause;

	private String sortKey;

	/**
	 * @param selectClause SELECT clause part of SQL query string
	 */
	public void setSelectClause(String selectClause) {
		String keyWord = "select ";
		String temp = selectClause.trim();
		if (temp.toLowerCase().startsWith(keyWord) && temp.length() > keyWord.length()) {
			this.selectClause = temp.substring(keyWord.length());
		}
		else {
			this.selectClause = temp;
		}
	}

	/**
	 *
	 * @return the
	 */
	protected String getSelectClause() {
		return selectClause;
	}

	/**
	 * @param fromClause FROM clause part of SQL query string
	 */
	public void setFromClause(String fromClause) {
		String keyWord = "from ";
		String temp = fromClause.trim();
		if (temp.toLowerCase().startsWith(keyWord) && temp.length() > keyWord.length()) {
			this.fromClause = temp.substring(keyWord.length());
		}
		else {
			this.fromClause = temp;
		}
	}

	/**
	 *
	 * @return SQL "from" clause
	 */
	protected String getFromClause() {
		return fromClause;
	}

	/**
	 * @param whereClause WHERE clause part of SQL query string
	 */
	public void setWhereClause(String whereClause) {
		String keyWord = "where ";
		String temp = whereClause.trim();
		if (temp.toLowerCase().startsWith(keyWord) && temp.length() > keyWord.length()) {
			this.whereClause = temp.substring(keyWord.length());
		}
		else {
			this.whereClause = temp;
		}
	}

	/**
	 *
	 * @return WHERE clause part of SQL query string
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
	 *
	 * @return sortKey key to use to sort and limit page content
	 */
	protected String getSortKey() {
		return sortKey;
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
	}

	public abstract String generateFirstPageQuery(int pageSize);

	public abstract String generateRemainingPagesQuery(int pageSize);

	public abstract String generateJumpToItemQuery(int itemIndex, int pageSize);

}
