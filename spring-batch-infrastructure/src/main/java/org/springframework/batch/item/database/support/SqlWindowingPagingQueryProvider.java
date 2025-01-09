/*
 * Copyright 2006-2024 the original author or authors.
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

import org.springframework.util.StringUtils;

/**
 * Generic Paging Query Provider using standard SQL:2003 windowing functions. These
 * features are supported by DB2, Oracle, SQL Server 2005, Sybase and Apache Derby version
 * 10.4.1.3
 *
 * @author Thomas Risberg
 * @author Michael Minella
 * @since 2.0
 * @deprecated since 5.2.1 with no replacement. Scheduled for removal in 6.0.
 */
@Deprecated(forRemoval = true)
public class SqlWindowingPagingQueryProvider extends AbstractSqlPagingQueryProvider {

	@Override
	public String generateFirstPageQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM ( ");
		sql.append("SELECT ")
			.append(StringUtils.hasText(getOrderedQueryAlias()) ? getOrderedQueryAlias() + ".*, " : "*, ");
		sql.append("ROW_NUMBER() OVER (").append(getOverClause());
		sql.append(") AS ROW_NUMBER");
		sql.append(getOverSubstituteClauseStart());
		sql.append(" FROM ")
			.append(getFromClause())
			.append(getWhereClause() == null ? "" : " WHERE " + getWhereClause());
		sql.append(getGroupClause() == null ? "" : " GROUP BY " + getGroupClause());
		sql.append(getOverSubstituteClauseEnd());
		sql.append(") ")
			.append(getSubQueryAlias())
			.append("WHERE ")
			.append(extractTableAlias())
			.append("ROW_NUMBER <= ")
			.append(pageSize);
		sql.append(" ORDER BY ").append(SqlPagingQueryUtils.buildSortClause(this));

		return sql.toString();
	}

	protected String getOrderedQueryAlias() {
		return "";
	}

	protected Object getSubQueryAlias() {
		return "AS TMP_SUB ";
	}

	protected Object extractTableAlias() {
		String alias = String.valueOf(getSubQueryAlias());
		if (StringUtils.hasText(alias) && alias.toUpperCase().startsWith("AS")) {
			alias = alias.substring(3).trim() + ".";
		}
		return alias;
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM ( ");
		sql.append("SELECT ")
			.append(StringUtils.hasText(getOrderedQueryAlias()) ? getOrderedQueryAlias() + ".*, " : "*, ");
		sql.append("ROW_NUMBER() OVER (").append(getOverClause());
		sql.append(") AS ROW_NUMBER");
		sql.append(getOverSubstituteClauseStart());
		sql.append(" FROM ").append(getFromClause());
		if (getWhereClause() != null) {
			sql.append(" WHERE ");
			sql.append(getWhereClause());
		}

		sql.append(getGroupClause() == null ? "" : " GROUP BY " + getGroupClause());
		sql.append(getOverSubstituteClauseEnd());
		sql.append(") ")
			.append(getSubQueryAlias())
			.append("WHERE ")
			.append(extractTableAlias())
			.append("ROW_NUMBER <= ")
			.append(pageSize);
		sql.append(" AND ");
		SqlPagingQueryUtils.buildSortConditions(this, sql);
		sql.append(" ORDER BY ").append(SqlPagingQueryUtils.buildSortClause(this));

		return sql.toString();
	}

	protected String getOverClause() {
		StringBuilder sql = new StringBuilder();

		sql.append(" ORDER BY ").append(buildSortClause(this));

		return sql.toString();
	}

	protected String getOverSubstituteClauseStart() {
		return "";
	}

	protected String getOverSubstituteClauseEnd() {
		return "";
	}

	/**
	 * Generates ORDER BY attributes based on the sort keys.
	 * @param provider the paging query provider
	 * @return a String that can be appended to an ORDER BY clause.
	 */
	private String buildSortClause(AbstractSqlPagingQueryProvider provider) {
		return SqlPagingQueryUtils.buildSortClause(provider.getSortKeysWithoutAliases());
	}

}
