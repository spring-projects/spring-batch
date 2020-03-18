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

package org.springframework.batch.item.database;

import java.util.Map;
import javax.sql.DataSource;


/**
 * Interface defining the functionality to be provided for generating paging queries for use with Paging
 * Item Readers.
 *
 * @author Thomas Risberg
 * @author Michael Minella
 * @since 2.0
 */
public interface PagingQueryProvider {

	/**
	 * Initialize the query provider using the provided {@link DataSource} if necessary.
	 * 
	 * @param dataSource DataSource to use for any initialization
	 * @throws Exception for errors when initializing
	 */
	void init(DataSource dataSource) throws Exception;

	/**
	 * Generate the query that will provide the first page, limited by the page size.
	 *
	 * @param pageSize number of rows to read for each page
	 * @return the generated query
	 */
	String generateFirstPageQuery(int pageSize);

	/**
	 * Generate the query that will provide the first page, limited by the page size.
	 *
	 * @param pageSize number of rows to read for each page
	 * @return the generated query
	 */
	String generateRemainingPagesQuery(int pageSize);

	/**
	 *
	 * Generate the query that will provide the jump to item query.  The itemIndex provided could be in the middle of
	 * the page and together with the page size it will be used to calculate the last index of the preceding page
	 * to be able to retrieve the sort key for this row.
	 *
	 * @param itemIndex the index for the next item to be read
	 * @param pageSize number of rows to read for each page
	 * @return the generated query
	 */
	String generateJumpToItemQuery(int itemIndex, int pageSize);

	/**
	 * The number of parameters that are declared in the query
	 * @return number of parameters
	 */
	int getParameterCount();

	/**
	 * Indicate whether the generated queries use named parameter syntax.
	 *
	 * @return true if named parameter syntax is used
	 */
	boolean isUsingNamedParameters();

	/**
	 * The sort keys.  A Map of the columns that make up the key and a Boolean indicating ascending or descending 
	 * (ascending = true). 
	 *  
	 * @return the sort keys used to order the query
	 */
	Map<String, Order> getSortKeys();
	
	/**
	 * Returns either a String to be used as the named placeholder for a sort key value (based on the column name)
	 * or a ? for unnamed parameters.
	 * 
	 * @param keyName The sort key name
	 * @return The string to be used for a parameterized query.
	 */
	String getSortKeyPlaceHolder(String keyName);

	/**
	 * The sort key (unique single column name) without alias.
	 *
	 * @return the sort key used to order the query (without alias)
	 */
	Map<String, Order> getSortKeysWithoutAliases();
}
