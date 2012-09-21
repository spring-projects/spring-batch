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

import org.springframework.util.StringUtils;

/**
 * Generic Paging Query Provider using standard SQL:2003 windowing functions.
 * These features are supported by DB2, Oracle, SQL Server 2005, Sybase and
 * Apache Derby version 10.4.1.3
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
		sql.append("ROW_NUMBER() OVER (").append(getOverClause());
		sql.append(") AS ROW_NUMBER");
		sql.append(getOverSubstituteClauseStart());
		sql.append(" FROM ").append(getFromClause()).append(
				getWhereClause() == null ? "" : " WHERE " + getWhereClause());
		sql.append(getOverSubstituteClauseEnd());
		sql.append(") ").append(getSubQueryAlias()).append("WHERE ").append(extractTableAlias()).append(
				"ROW_NUMBER <= ").append(pageSize);

		return sql.toString();
	}

	protected Object getSubQueryAlias() {
		return "AS TMP_SUB ";
	}

	protected Object extractTableAlias() {
		String alias = "" + getSubQueryAlias();
		if (StringUtils.hasText(alias) && alias.toUpperCase().startsWith("AS")) {
			alias = alias.substring(3).trim() + ".";
		}
		return alias;
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM ( ");
		sql.append("SELECT ").append(getSelectClause()).append(", ");
		sql.append("ROW_NUMBER() OVER (").append(getOverClause());
		sql.append(") AS ROW_NUMBER");
		sql.append(getOverSubstituteClauseStart());
		sql.append(" FROM ").append(getFromClause());
		sql.append(" WHERE ");
		if (getWhereClause() != null) {
			sql.append(getWhereClause());
			sql.append(" AND ");
		}
		sql.append(getSortKey());
		if (isAscending()) {
			sql.append(" > ");
		}
		else {
			sql.append(" < ");
		}
		sql.append(getSortKeyPlaceHolder());
		sql.append(getOverSubstituteClauseEnd());
		sql.append(") ").append(getSubQueryAlias()).append("WHERE ").append(extractTableAlias()).append(
				"ROW_NUMBER <= ").append(pageSize);

		return sql.toString();
	}

	@Override
	public String generateJumpToItemQuery(int itemIndex, int pageSize) {
		int page = itemIndex / pageSize;
		int lastRowNum = (page * pageSize);
		if (lastRowNum <= 0) {
			lastRowNum = 1;
		}

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT SORT_KEY FROM ( ");
		sql.append("SELECT ").append(getSortKey()).append(" AS SORT_KEY, ");
		sql.append("ROW_NUMBER() OVER (").append(getOverClause());
		sql.append(") AS ROW_NUMBER");
		sql.append(getOverSubstituteClauseStart());
		sql.append(" FROM ").append(getFromClause());
		sql.append(getWhereClause() == null ? "" : " WHERE " + getWhereClause());
		sql.append(getOverSubstituteClauseEnd());
		sql.append(") ").append(getSubQueryAlias()).append("WHERE ").append(extractTableAlias()).append(
				"ROW_NUMBER = ").append(lastRowNum);

		return sql.toString();
	}

	protected String getOverClause() {
		return "ORDER BY " + getSortKeyWithoutAlias() + " " + getAscendingClause();
	}

	protected String getOverSubstituteClauseStart() {
		return "";
	}

	protected String getOverSubstituteClauseEnd() {
		return "";
	}

	private String getAscendingClause() {
		if (isAscending()) {
			return "ASC";
		}
		else {
			return "DESC";
		}
	}

}
