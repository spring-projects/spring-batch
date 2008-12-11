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
package org.springframework.batch.item.database;

import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * A convenient implementation for providing BeanPropertySqlParameterSource when the item has JavaBean properties
 * that correspond to names used for parameters in the SQL statement.
 * 
 * @author Thomas Risberg
 * @since 2.0
 */
public class BeanPropertyItemSqlParameterSourceProvider<T> implements ItemSqlParameterSourceProvider<T> {

	/**
	 * Provide parameter values in an {@link BeanPropertySqlParameterSource} based on values from  
	 * the provided item.
	 * @param item the item to use for parameter values
	 */
	public SqlParameterSource createSqlParameterSource(T item) {
		return new BeanPropertySqlParameterSource(item);
	}

}
