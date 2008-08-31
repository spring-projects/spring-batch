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
 * MySQL implementation of a  {@link PagingQueryProvider} using database specific features.
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public class MySqlPagingQueryProvider extends AbstractSqlPagingQueryProvider {

	@Override
	public String generateFirstPageQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(getSelectClause());
		sql.append(" FROM ").append(getFromClause());
		sql.append(getWhereClause() == null ? "" : " WHERE " + getWhereClause());
		sql.append(" ORDER BY ").append(getSortKey()).append(" ASC");
		sql.append(" LIMIT ").append(pageSize);

		return sql.toString();
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(getSelectClause());
		sql.append(" FROM ").append(getFromClause());
		sql.append(" WHERE ");
		if (getWhereClause() != null) {
			sql.append(getWhereClause());
			sql.append(" AND ");
		}
		sql.append(getSortKey()).append(" > ").append(getSortKeyPlaceHolder());
		sql.append(" ORDER BY ").append(getSortKey()).append(" ASC");
		sql.append(" LIMIT ").append(pageSize);

		return sql.toString();
	}

	@Override
	public String generateJumpToItemQuery(int itemIndex, int pageSize) {
		int page = itemIndex / pageSize;
		int offset = (page * pageSize) - 1;

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(getSortKey()).append(" AS SORT_KEY");
		sql.append(" FROM ").append(getFromClause());
		sql.append(getWhereClause() == null ? "" : " WHERE " + getWhereClause());
		sql.append(" ORDER BY ").append(getSortKey()).append(" ASC");
		sql.append(" LIMIT ").append(offset).append(" 1");

		return sql.toString();
	}

}
