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
 * MySQL implementation of a  {@link org.springframework.batch.item.database.support.PagingQueryProvider} using database specific features.
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public class HsqlPagingQueryProvider extends AbstractSqlPagingQueryProvider {

	@Override
	public String generateFirstPageQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append("TOP ").append(pageSize).append(" ").append(getSelectClause());
		sql.append(" FROM ").append(getFromClause());
		sql.append(getWhereClause() == null ? "" : " WHERE " + getWhereClause());

		return sql.toString();
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append("TOP ").append(pageSize).append(" ").append(getSelectClause());
		sql.append(" FROM ").append(getFromClause());
		sql.append(" WHERE ").append(getSortKey()).append(" > ?");
		sql.append(getWhereClause() == null ? "" : " AND " + getWhereClause());

		return sql.toString();
	}

	@Override
	public String generateJumpToItemQuery(int itemIndex, int pageSize) {
		int page = itemIndex / pageSize;
		int offset = (page * pageSize) - 1;

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT LIMIT ").append(offset).append(" 1 ").append(getSortKey()).append(" AS SORT_KEY");
		sql.append(" FROM ").append(getFromClause());
		sql.append(getWhereClause() == null ? "" : " WHERE " + getWhereClause());

		return sql.toString();
	}

}
