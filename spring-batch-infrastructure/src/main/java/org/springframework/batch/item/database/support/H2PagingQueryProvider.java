/*
 * Copyright 2006-2022 the original author or authors.
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

/**
 * H2 implementation of a {@link org.springframework.batch.item.database.PagingQueryProvider} using database specific features.
 *
 * @author Dave Syer
 * @author Henning Pöttker
 * @since 2.1
 */
public class H2PagingQueryProvider extends AbstractSqlPagingQueryProvider {

	@Override
	public String generateFirstPageQuery(int pageSize) {
		return SqlPagingQueryUtils.generateLimitSqlQuery(this, false, buildLimitClause(pageSize));
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		return SqlPagingQueryUtils.generateLimitSqlQuery(this, true, buildLimitClause(pageSize));
	}

	private String buildLimitClause(int pageSize) {
		return new StringBuilder().append("FETCH NEXT ").append(pageSize).append(" ROWS ONLY").toString();
	}

	@Override
	public String generateJumpToItemQuery(int itemIndex, int pageSize) {
		int page = itemIndex / pageSize;
		int offset = (page * pageSize) - 1;
		offset = offset<0 ? 0 : offset;

		String limitClause = new StringBuilder().append("OFFSET ")
				.append(offset).append(" ROWS FETCH NEXT 1 ROWS ONLY")
				.toString();
		return SqlPagingQueryUtils.generateLimitJumpToQuery(this, limitClause);
	}

}
