package org.springframework.batch.item.database.support;

/**
 * MySQL implementation of a  {@link org.springframework.batch.item.database.support.PagingQueryProvider} using database specific features.
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public class PostgresPagingQueryProvider extends AbstractSqlPagingQueryProvider {

	@Override
	public String generateFirstPageQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(getSelectClause());
		sql.append(" FROM ").append(getFromClause());
		sql.append(getWhereClause() == null ? "" : " WHERE " + getWhereClause());
		sql.append(" LIMIT ").append(pageSize);

		return sql.toString();
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(getSelectClause());
		sql.append(" FROM ").append(getFromClause());
		sql.append(" WHERE ").append(getSortKey()).append(" > ?");
		sql.append(getWhereClause() == null ? "" : " AND " + getWhereClause());
		sql.append(" LIMIT ").append(pageSize);

		return sql.toString();
	}

	@Override
	public String generateJumpToItemQuery(int itemIndex, int pageSize) {
		int page = itemIndex / pageSize;
		int offset = (page * pageSize) - 1;

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ").append(getSortKey()).append(" AS SORT_KEY");
		sql.append(" FROM ").append(getFromClause()).append(getWhereClause() == null ? "" : " WHERE " + getWhereClause());
		sql.append(" LIMIT ").append(offset).append(" 1");

		return sql.toString();
	}

}
