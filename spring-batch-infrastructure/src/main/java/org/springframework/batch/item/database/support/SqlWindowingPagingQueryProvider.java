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

/**
 * Generic Paging Query Provider using standard SQL:2003 windowing functions.  These features are supported by
 * DB2, Oracle, SQL Server 2005, Sybase and Apache Derby version 10.4.1.3
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public class SqlWindowingPagingQueryProvider extends AbstractSqlPagingQueryProvider {

	@Override
	public String generateFirstPageQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM ( ");
		sql.append("SELECT ").append(getSelectClause()).append(", ");
		sql.append("ROW_NUMBER() OVER (ORDER BY ").append(getSortKey()).append(" ASC) AS ROW_NUMBER");
		sql.append(" FROM ").append(getFromClause()).append(getWhereClause() == null ? "" : " WHERE " + getWhereClause());
		sql.append(") WHERE ROW_NUMBER <= ").append(pageSize);

		return sql.toString();
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM ( ");
		sql.append("SELECT ").append(getSelectClause()).append(", ");
		sql.append("ROW_NUMBER() OVER (ORDER BY ").append(getSortKey()).append(" ASC) AS ROW_NUMBER");
		sql.append(" FROM ").append(getFromClause()).append(" WHERE ").append(getSortKey()).append(" > ?");
		sql.append(getWhereClause() == null ? "" : " AND " + getWhereClause());
		sql.append(") WHERE ROW_NUMBER <= ").append(pageSize);

		return sql.toString();
	}

	@Override
	public String generateJumpToItemQuery(int itemIndex, int pageSize) {
		int page = itemIndex / pageSize;
		int lastRowNum = (page * pageSize);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT SORT_KEY FROM ( ");
		sql.append("SELECT ").append(getSortKey()).append(" AS SORT_KEY, ");
		sql.append("ROW_NUMBER() OVER (ORDER BY ").append(getSortKey()).append(" ASC) AS ROW_NUMBER");
		sql.append(" FROM ").append(getFromClause()).append(getWhereClause() == null ? "" : " WHERE " + getWhereClause());
		sql.append(") WHERE ROW_NUMBER = ").append(lastRowNum);

		return sql.toString();
	}

}
