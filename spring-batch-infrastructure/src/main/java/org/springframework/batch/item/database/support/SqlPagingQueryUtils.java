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
 * Utility class that generates the actual SQL statements used by query
 * providers.
 * 
 * @author Thomas Risberg
 * @author Dave Syer
 * @since 2.0
 */
public class SqlPagingQueryUtils {

	/**
	 * Generate SQL query string using a LIMIT clause
	 * 
	 * @param provider {@link AbstractSqlPagingQueryProvider} providing the
	 * implementation specifics
	 * @param remainingPageQuery is this query for the ramining pages (true) as
	 * opposed to the first page (false)
	 * @param limitClause the implementation specific limit clause to be used
	 * @return the generated query
	 */
	public static String generateLimitSqlQuery(AbstractSqlPagingQueryProvider provider, boolean remainingPageQuery,
			String limitClause) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(provider.getSelectClause());
		sql.append(" FROM ").append(provider.getFromClause());
		buildWhereClause(provider, remainingPageQuery, sql);
		sql.append(" ORDER BY ").append(provider.getSortKeyWithoutAlias());
		buildAscendingClause(provider, sql);
		sql.append(" " + limitClause);

		return sql.toString();
	}

	/**
	 * Generate SQL query string using a TOP clause
	 * 
	 * @param provider {@link AbstractSqlPagingQueryProvider} providing the
	 * implementation specifics
	 * @param remainingPageQuery is this query for the ramining pages (true) as
	 * opposed to the first page (false)
	 * @param topClause the implementation specific top clause to be used
	 * @return the generated query
	 */
	public static String generateTopSqlQuery(AbstractSqlPagingQueryProvider provider, boolean remainingPageQuery,
			String topClause) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(topClause).append(" ").append(provider.getSelectClause());
		sql.append(" FROM ").append(provider.getFromClause());
		buildWhereClause(provider, remainingPageQuery, sql);
		sql.append(" ORDER BY ").append(provider.getSortKeyWithoutAlias());
		buildAscendingClause(provider, sql);

		return sql.toString();
	}

	/**
	 * Generate SQL query string using a ROW_NUM condition
	 * 
	 * @param provider {@link AbstractSqlPagingQueryProvider} providing the
	 * implementation specifics
	 * @param remainingPageQuery is this query for the remaining pages (true) as
	 * opposed to the first page (false)
	 * @param rowNumClause the implementation specific row num clause to be used
	 * @return the generated query
	 */
	public static String generateRowNumSqlQuery(AbstractSqlPagingQueryProvider provider, boolean remainingPageQuery,
			String rowNumClause) {

		return generateRowNumSqlQuery(provider, provider.getSelectClause(), remainingPageQuery, rowNumClause);

	}

	/**
	 * Generate SQL query string using a ROW_NUM condition
	 * 
	 * @param provider {@link AbstractSqlPagingQueryProvider} providing the
	 * implementation specifics
	 * @param remainingPageQuery is this query for the remaining pages (true) as
	 * opposed to the first page (false)
	 * @param rowNumClause the implementation specific row num clause to be used
	 * @return the generated query
	 */
	public static String generateRowNumSqlQuery(AbstractSqlPagingQueryProvider provider, String selectClause,
			boolean remainingPageQuery, String rowNumClause) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM (SELECT ").append(selectClause).append(", ROWNUM as TMP_ROW_NUM");
		sql.append(" FROM ").append(provider.getFromClause());
		buildWhereClause(provider, remainingPageQuery, sql);
		sql.append(" ORDER BY ").append(provider.getSortKeyWithoutAlias());
		buildAscendingClause(provider, sql);
		sql.append(") WHERE ").append(rowNumClause);

		return sql.toString();

	}

	public static String generateRowNumSqlQueryWithNesting(AbstractSqlPagingQueryProvider provider,
			String selectClause, boolean remainingPageQuery, String rowNumClause) {
		return generateRowNumSqlQueryWithNesting(provider, selectClause, selectClause, remainingPageQuery, rowNumClause);
	}

	public static String generateRowNumSqlQueryWithNesting(AbstractSqlPagingQueryProvider provider,
			String innerSelectClause, String outerSelectClause, boolean remainingPageQuery, String rowNumClause) {

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(outerSelectClause).append(" FROM (SELECT ").append(outerSelectClause)
				.append(", ROWNUM as TMP_ROW_NUM");
		sql.append(" FROM (SELECT ").append(innerSelectClause).append(" FROM ").append(provider.getFromClause());
		buildWhereClause(provider, remainingPageQuery, sql);
		sql.append(" ORDER BY ").append(provider.getSortKeyWithoutAlias());
		buildAscendingClause(provider, sql);
		sql.append(")) WHERE ").append(rowNumClause);

		return sql.toString();

	}

	/**
	 * Generate SQL query string using a LIMIT clause
	 * 
	 * @param provider {@link AbstractSqlPagingQueryProvider} providing the
	 * implementation specifics
	 * @param limitClause the implementation specific top clause to be used
	 * @return the generated query
	 */
	public static String generateLimitJumpToQuery(AbstractSqlPagingQueryProvider provider, String limitClause) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(provider.getSortKey()).append(" AS SORT_KEY");
		sql.append(" FROM ").append(provider.getFromClause());
		sql.append(provider.getWhereClause() == null ? "" : " WHERE " + provider.getWhereClause());
		sql.append(" ORDER BY ").append(provider.getSortKeyWithoutAlias());
		buildAscendingClause(provider, sql);
		sql.append(" " + limitClause);

		return sql.toString();
	}

	/**
	 * Generate SQL query string using a TOP clause
	 * 
	 * @param provider {@link AbstractSqlPagingQueryProvider} providing the
	 * implementation specifics
	 * @param topClause the implementation specific top clause to be used
	 * @return the generated query
	 */
	public static String generateTopJumpToQuery(AbstractSqlPagingQueryProvider provider, String topClause) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(topClause).append(" ").append(provider.getSortKey()).append(" AS SORT_KEY");
		sql.append(" FROM ").append(provider.getFromClause());
		sql.append(provider.getWhereClause() == null ? "" : " WHERE " + provider.getWhereClause());
		sql.append(" ORDER BY ").append(provider.getSortKeyWithoutAlias());
		buildAscendingClause(provider, sql);

		return sql.toString();
	}

	private static void buildAscendingClause(AbstractSqlPagingQueryProvider provider, StringBuilder sql) {
		if (provider.isAscending()) {
			sql.append(" ASC");
		}
		else {
			sql.append(" DESC");
		}
	}

	private static void buildWhereClause(AbstractSqlPagingQueryProvider provider, boolean remainingPageQuery,
			StringBuilder sql) {
		if (remainingPageQuery) {
			sql.append(" WHERE ");
			if (provider.getWhereClause() != null) {
				sql.append(provider.getWhereClause());
				sql.append(" AND ");
			}
			sql.append(provider.getSortKey());
			if (provider.isAscending()) {
				sql.append(" > ");
			}
			else {
				sql.append(" < ");
			}
			sql.append(provider.getSortKeyPlaceHolder());
		}
		else {
			sql.append(provider.getWhereClause() == null ? "" : " WHERE " + provider.getWhereClause());
		}
	}

}
