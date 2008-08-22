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
 * Interface defining the functionality to be provided for generating paging queries for use with JDBC.
 *
 * Any usage must provide the select clause, from clause and optionally a where clause. In addition a
 * single column sort key must be defined. This sort key will be used to provide the paging functinality.
 * It is recommended that there should be an index for the sort key to provide better performance.
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public interface JdbcPagingQueryProvider {

	/**
	 * Generate the query that will provide the first page, limited by the page size.
	 *
	 * @param pageSize number of rows to read for each page
	 * @param selectClause the columns that are part of the selct clause
	 * @param fromClause the table(s) that are part of the from clause
	 * @param whereClause the conditions that are part of the where clause
	 * @param sortKey the single column used for sorting
	 * @return the generated query
	 */
	String generateFirstPageQuery(int pageSize, String selectClause, String fromClause, String whereClause, String sortKey);

	/**
	 * Generate the query that will provide the first page, limited by the page size.
	 *
	 * @param pageSize number of rows to read for each page
	 * @param selectClause the columns that are part of the selct clause
	 * @param fromClause the table(s) that are part of the from clause
	 * @param whereClause the conditions that are part of the where clause
	 * @param sortKey the single column used for sorting
	 * @return the generated query
	 */
	String generateRemainingPagesQuery(int pageSize, String selectClause, String fromClause, String whereClause, String sortKey);

	/**
	 *
	 * Generate the query that will provide the jump to item query.  The itemIndex provided could be in the middle of
	 * the page and together with the page size it will be used to calculate the last index of the preceding page
	 * to be able to retrieve the sort key for this row.
	 *
	 * @param itemIndex the index for the next item to be read
	 * @param pageSize number of rows to read for each page
	 * @param selectClause the columns that are part of the selct clause
	 * @param fromClause the table(s) that are part of the from clause
	 * @param whereClause the conditions that are part of the where clause
	 * @param sortKey the single column used for sorting
	 * @return the generated query
	 */
	String generateJumpToItemQuery(int itemIndex, int pageSize, String selectClause, String fromClause, String whereClause, String sortKey);

}
