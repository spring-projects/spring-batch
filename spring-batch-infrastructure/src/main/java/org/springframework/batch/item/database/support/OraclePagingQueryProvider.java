package org.springframework.batch.item.database.support;

/**
 * Oracle implementation of a  {@link org.springframework.batch.item.database.support.PagingQueryProvider} using
 * database specific features.
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public class OraclePagingQueryProvider extends SqlWindowingPagingQueryProvider {

	@Override
	public String generateFirstPageQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(getSelectClause());
		sql.append(" FROM ").append(getFromClause());
		sql.append(" WHERE ROWNUM <= ").append(pageSize);
		sql.append(getWhereClause() == null ? "" : " AND " + getWhereClause());

		return sql.toString();
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(getSelectClause());
		sql.append(" FROM ").append(getFromClause());
		sql.append(" WHERE ").append(getSortKey()).append(" > ?");
		sql.append(" AND ROWNUM <= ").append(pageSize);
		sql.append(getWhereClause() == null ? "" : " AND " + getWhereClause());

		return sql.toString();
	}
}
