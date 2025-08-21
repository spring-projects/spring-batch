package org.springframework.batch.item.database.support;

import org.springframework.util.StringUtils;

public class KingbasePagingQueryProvider extends AbstractSqlPagingQueryProvider {

	@Override
	public String generateFirstPageQuery(int pageSize) {
		return SqlPagingQueryUtils.generateLimitSqlQuery(this, false, buildLimitClause(pageSize));
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		if (StringUtils.hasText(getGroupClause())) {
			return SqlPagingQueryUtils.generateLimitGroupedSqlQuery(this, buildLimitClause(pageSize));
		}
		else {
			return SqlPagingQueryUtils.generateLimitSqlQuery(this, true, buildLimitClause(pageSize));
		}
	}

	private String buildLimitClause(int pageSize) {
		return new StringBuilder().append("LIMIT ").append(pageSize).toString();
	}

}
