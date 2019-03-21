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

import java.util.Map;

import org.springframework.batch.item.database.Order;

/**
 * Oracle implementation of a
 * {@link org.springframework.batch.item.database.PagingQueryProvider} using
 * database specific features.
 * 
 * @author Thomas Risberg
 * @author Michael Minella
 * @since 2.0
 */
public class OraclePagingQueryProvider extends AbstractSqlPagingQueryProvider {

	@Override
	public String generateFirstPageQuery(int pageSize) {
		return SqlPagingQueryUtils.generateRowNumSqlQuery(this, false, buildRowNumClause(pageSize));
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		return SqlPagingQueryUtils.generateRowNumSqlQuery(this, true, buildRowNumClause(pageSize));
	}

	@Override
	public String generateJumpToItemQuery(int itemIndex, int pageSize) {
		int page = itemIndex / pageSize;
		int offset = (page * pageSize);
		offset = offset == 0 ? 1 : offset;
		String sortKeySelect = this.getSortKeySelect();
		return SqlPagingQueryUtils.generateRowNumSqlQueryWithNesting(this, sortKeySelect, sortKeySelect, false, "TMP_ROW_NUM = "
				+ offset);
	}
	
	private String getSortKeySelect() {
		StringBuilder sql = new StringBuilder();
		String prefix = "";
		
		for (Map.Entry<String, Order> sortKey : this.getSortKeys().entrySet()) {
			sql.append(prefix);
			prefix = ", ";
			sql.append(sortKey.getKey());
		}
		
		return sql.toString();
	}

	private String buildRowNumClause(int pageSize) {
		return new StringBuilder().append("ROWNUM <= ").append(pageSize).toString();
	}

}
