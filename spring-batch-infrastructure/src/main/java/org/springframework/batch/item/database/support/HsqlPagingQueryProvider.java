/*
 * Copyright 2006-2012 the original author or authors.
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
 * HSQLDB implementation of a
 * {@link org.springframework.batch.item.database.PagingQueryProvider} using database
 * specific features.
 *
 * @author Thomas Risberg
 * @author Michael Minella
 * @since 2.0
 */
public class HsqlPagingQueryProvider extends AbstractSqlPagingQueryProvider {

	@Override
	public String generateFirstPageQuery(int pageSize) {
		return SqlPagingQueryUtils.generateTopSqlQuery(this, false, buildTopClause(pageSize));
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		if (StringUtils.hasText(getGroupClause())) {
			return SqlPagingQueryUtils.generateGroupedTopSqlQuery(this, true, buildTopClause(pageSize));
		}
		else {
			return SqlPagingQueryUtils.generateTopSqlQuery(this, true, buildTopClause(pageSize));
		}
	}

	private String buildTopClause(int pageSize) {
		return new StringBuilder().append("TOP ").append(pageSize).toString();
	}

	@Override
	public String generateJumpToItemQuery(int itemIndex, int pageSize) {
		int page = itemIndex / pageSize;
		int offset = (page * pageSize) - 1;
		offset = offset < 0 ? 0 : offset;

		String topClause = new StringBuilder().append("LIMIT ").append(offset).append(" 1").toString();
		return SqlPagingQueryUtils.generateTopJumpToQuery(this, topClause);
	}

}
