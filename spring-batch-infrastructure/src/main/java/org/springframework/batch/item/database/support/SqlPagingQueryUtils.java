/*
 * Copyright 2006-2015 the original author or authors.
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.batch.item.database.Order;
import org.springframework.util.StringUtils;

/**
 * Utility class that generates the actual SQL statements used by query
 * providers.
 * 
 * @author Thomas Risberg
 * @author Dave Syer
 * @author Michael Minella
 * @since 2.0
 */
public class SqlPagingQueryUtils {

	/**
	 * Generate SQL query string using a LIMIT clause
	 * 
	 * @param provider {@link AbstractSqlPagingQueryProvider} providing the
	 * implementation specifics
	 * @param remainingPageQuery is this query for the remaining pages (true) as
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
		buildGroupByClause(provider, sql);
		sql.append(" ORDER BY ").append(buildSortClause(provider));
		sql.append(" " + limitClause);

		return sql.toString();
	}

	/**
	 * Generate SQL query string using a LIMIT clause
	 * 
	 * @param provider {@link AbstractSqlPagingQueryProvider} providing the
	 * implementation specifics
	 * @param remainingPageQuery is this query for the remaining pages (true) as
	 * opposed to the first page (false)
	 * @param limitClause the implementation specific limit clause to be used
	 * @return the generated query
	 */
	public static String generateLimitGroupedSqlQuery(AbstractSqlPagingQueryProvider provider, boolean remainingPageQuery,
			String limitClause) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * ");
		sql.append(" FROM (");
		sql.append("SELECT ").append(provider.getSelectClause());
		sql.append(" FROM ").append(provider.getFromClause());
		sql.append(provider.getWhereClause() == null ? "" : " WHERE " + provider.getWhereClause());
		buildGroupByClause(provider, sql);
		sql.append(") AS MAIN_QRY ");
		sql.append("WHERE ");
		buildSortConditions(provider, sql);
		sql.append(" ORDER BY ").append(buildSortClause(provider));
		sql.append(" " + limitClause);

		return sql.toString();
	}

	/**
	 * Generate SQL query string using a TOP clause
	 * 
	 * @param provider {@link AbstractSqlPagingQueryProvider} providing the
	 * implementation specifics
	 * @param remainingPageQuery is this query for the remaining pages (true) as
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
		buildGroupByClause(provider, sql);
		sql.append(" ORDER BY ").append(buildSortClause(provider));

		return sql.toString();
	}

	/**
	 * Generate SQL query string using a TOP clause
	 * 
	 * @param provider {@link AbstractSqlPagingQueryProvider} providing the
	 * implementation specifics
	 * @param remainingPageQuery is this query for the remaining pages (true) as
	 * opposed to the first page (false)
	 * @param topClause the implementation specific top clause to be used
	 * @return the generated query
	 */
	public static String generateGroupedTopSqlQuery(AbstractSqlPagingQueryProvider provider, boolean remainingPageQuery,
			String topClause) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(topClause).append(" * FROM (");
		sql.append("SELECT ").append(provider.getSelectClause());
		sql.append(" FROM ").append(provider.getFromClause());
		sql.append(provider.getWhereClause() == null ? "" : " WHERE " + provider.getWhereClause());
		buildGroupByClause(provider, sql);
		sql.append(") AS MAIN_QRY ");
		sql.append("WHERE ");
		buildSortConditions(provider, sql);
		sql.append(" ORDER BY ").append(buildSortClause(provider));

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
	 * @param selectClause {@link String} containing the select portion of the query.
	 * @param remainingPageQuery is this query for the remaining pages (true) as
	 * opposed to the first page (false)
	 * @param rowNumClause the implementation specific row num clause to be used
	 * @return the generated query
	 */
	public static String generateRowNumSqlQuery(AbstractSqlPagingQueryProvider provider, String selectClause,
			boolean remainingPageQuery, String rowNumClause) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM (SELECT ").append(selectClause);
		sql.append(" FROM ").append(provider.getFromClause());
		sql.append(provider.getWhereClause() == null ? "" : " WHERE " + provider.getWhereClause());
		buildGroupByClause(provider, sql);
		sql.append(" ORDER BY ").append(buildSortClause(provider));
		sql.append(") WHERE ").append(rowNumClause);
		if(remainingPageQuery) {
			sql.append(" AND ");
			buildSortConditions(provider, sql);
		}

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
				.append(", ").append(StringUtils.hasText(provider.getGroupClause()) ? "MIN(ROWNUM) as TMP_ROW_NUM" : "ROWNUM as TMP_ROW_NUM");
		sql.append(" FROM (SELECT ").append(innerSelectClause).append(" FROM ").append(provider.getFromClause());
		buildWhereClause(provider, remainingPageQuery, sql);
		buildGroupByClause(provider, sql);
		sql.append(" ORDER BY ").append(buildSortClause(provider));
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
		sql.append("SELECT ").append(buildSortKeySelect(provider));
		sql.append(" FROM ").append(provider.getFromClause());
		sql.append(provider.getWhereClause() == null ? "" : " WHERE " + provider.getWhereClause());
		buildGroupByClause(provider, sql);
		sql.append(" ORDER BY ").append(buildSortClause(provider));
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
		sql.append("SELECT ").append(topClause).append(" ").append(buildSortKeySelect(provider));
		sql.append(" FROM ").append(provider.getFromClause());
		sql.append(provider.getWhereClause() == null ? "" : " WHERE " + provider.getWhereClause());
		buildGroupByClause(provider, sql);
		sql.append(" ORDER BY ").append(buildSortClause(provider));

		return sql.toString();
	}

	/**
	 * Generates ORDER BY attributes based on the sort keys.
	 * 
	 * @param provider the {@link AbstractSqlPagingQueryProvider} to be used for
	 * used for pagination.
	 * @return a String that can be appended to an ORDER BY clause.
	 */
	public static String buildSortClause(AbstractSqlPagingQueryProvider provider) {
		return buildSortClause(provider.getSortKeys());
	}
	
	/**
	 * Generates ORDER BY attributes based on the sort keys.
	 * 
	 * @param sortKeys {@link Map} where the key is the name of the column to be
	 * sorted and the value contains the {@link Order}.
	 * @return a String that can be appended to an ORDER BY clause.
	 */
	public static String buildSortClause(Map<String, Order> sortKeys) {
		StringBuilder builder = new StringBuilder();
		String prefix = "";
		
		for (Map.Entry<String, Order> sortKey : sortKeys.entrySet()) {
			builder.append(prefix);
			
			prefix = ", ";
			
			builder.append(sortKey.getKey());
			
			if(sortKey.getValue() != null && sortKey.getValue() == Order.DESCENDING) {
				builder.append(" DESC");
			}
			else {
				builder.append(" ASC");
			}
		}
		
		return builder.toString();
	}

	/**
	 * Appends the where conditions required to query for the subsequent pages.
	 * 
	 * @param provider the {@link AbstractSqlPagingQueryProvider} to be used for
	 * pagination.
	 * @param sql {@link StringBuilder} containing the sql to be used for the
	 * query.
	 */
	public static void buildSortConditions(
			AbstractSqlPagingQueryProvider provider, StringBuilder sql) {
		List<Map.Entry<String, Order>> keys = new ArrayList<>(provider.getSortKeys().entrySet());
		List<String> clauses = new ArrayList<>();
		
		for(int i = 0; i < keys.size(); i++) {
			StringBuilder clause = new StringBuilder();
			
			String prefix = "";
			for(int j = 0; j < i; j++) {
				clause.append(prefix);
				prefix = " AND ";
				Entry<String, Order> entry = keys.get(j);
				clause.append(entry.getKey());
				clause.append(" = ");
				clause.append(provider.getSortKeyPlaceHolder(entry.getKey()));
			}
			
			if(clause.length() > 0) {
				clause.append(" AND ");
			}
			clause.append(keys.get(i).getKey());
			
			if(keys.get(i).getValue() != null && keys.get(i).getValue() == Order.DESCENDING) {
				clause.append(" < ");
			}
			else {
				clause.append(" > ");
			}

			clause.append(provider.getSortKeyPlaceHolder(keys.get(i).getKey()));
			
			clauses.add(clause.toString());
		}
		
		sql.append("(");
		String prefix = "";
		
		for (String curClause : clauses) {
			sql.append(prefix);
			prefix = " OR ";
			sql.append("(");
			sql.append(curClause);
			sql.append(")");
		}
		sql.append(")");
	}

	private static String buildSortKeySelect(AbstractSqlPagingQueryProvider provider) {
		StringBuilder select = new StringBuilder();
		
		String prefix = "";
		
		for (Map.Entry<String, Order> sortKey : provider.getSortKeys().entrySet()) {
			select.append(prefix);
			
			prefix = ", ";
			
			select.append(sortKey.getKey());
		}
		
		return select.toString();
	}

	private static void buildWhereClause(AbstractSqlPagingQueryProvider provider, boolean remainingPageQuery,
			StringBuilder sql) {
		if (remainingPageQuery) {
			sql.append(" WHERE ");
			if (provider.getWhereClause() != null) {
				sql.append("(");
				sql.append(provider.getWhereClause());
				sql.append(") AND ");
			}

			buildSortConditions(provider, sql);
		}
		else {
			sql.append(provider.getWhereClause() == null ? "" : " WHERE " + provider.getWhereClause());
		}
	}
	
	private static void buildGroupByClause(AbstractSqlPagingQueryProvider provider, StringBuilder sql) {
		if(StringUtils.hasText(provider.getGroupClause())) {
			sql.append(" GROUP BY ");
			sql.append(provider.getGroupClause());
		}
	}

}
