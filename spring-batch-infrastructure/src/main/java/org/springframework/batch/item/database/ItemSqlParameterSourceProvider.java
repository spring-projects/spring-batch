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

import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * A convenient strategy for providing SqlParameterSource for named parameter SQL updates.
 * 
 * @author Thomas Risberg
 * @since 2.0
 */
public interface ItemSqlParameterSourceProvider<T> {

	/**
	 * Provide parameter values in an {@link SqlParameterSource} based on values from  
	 * the provided item.
	 * @param item the item to use for parameter values
	 */
	SqlParameterSource createSqlParameterSource(T item);

}
