/*
 * Copyright 2006-2021 the original author or authors.
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

import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.util.StringUtils;

/**
 * DB2 implementation of a {@link PagingQueryProvider} using database specific features.
 *
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class Db2PagingQueryProvider extends SqlWindowingPagingQueryProvider {

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

	@Override
	protected Object getSubQueryAlias() {
		return "AS TMP_SUB ";
	}

	private String buildLimitClause(int pageSize) {
		return new StringBuilder().append("FETCH FIRST ").append(pageSize).append(" ROWS ONLY").toString();
	}

}
