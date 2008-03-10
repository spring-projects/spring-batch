/*
 * Copyright 2006-2007 the original author or authors.
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

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.DrivingQueryItemReader;
import org.springframework.batch.item.database.KeyCollector;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

/**
 * <p>
 * Extension of the standard {@link RowMapper} interface to provide for
 * converting an object returned from a RowMapper to {@link ExecutionContext}
 * and back again. This interface serves three purposes:
 * </p>
 * 
 * <ul>
 * <li>Map the results of a driving query to an object, thus fulfilling the
 * normal {@link RowMapper} contract. Generally, this will be used to take
 * multiple columns and condense them into one 'key' that can then be used as
 * part of the {@link DrivingQueryItemReader}
 * <li>Map a key to an ExecutionContext. Because implementations of this
 * interface defined what a key is, they are the only ones that know how to
 * store it in an {@link ExecutionContext}.
 * <li>Create a {@link PreparedStatementSetter} so that, in the case of restart
 * a query can be created to return only the keys remaining to be processed.
 * </ul>
 * 
 * The most common use case for this interface is the need to map multiple
 * columns to a key, as is necessary by the
 * {@link MultipleColumnJdbcKeyCollector}. In general, it's much simpler to use
 * one key as a column, and if the data itself allows this, then it should be
 * used.  However, in certain cases there is no choice and multiple columns must
 * be used. Using a {@link ExecutionContextRowMapper}, developers can create
 * each unique key to suite their specific needs, and also describe how such a
 * key would be converted to {@link ExecutionContext}, so that it can be
 * serialized and stored.
 * 
 * @author Lucas Ward
 * @see RowMapper
 * @see MultipleColumnJdbcKeyCollector
 * @see KeyCollector
 * @since 1.0
 */
public interface ExecutionContextRowMapper extends RowMapper {

	/**
	 * Given the provided composite key, return a {@link ExecutionContext}
	 * representation.
	 * 
	 * @param key
	 * @return ExecutionContext representing the composite key.
	 * @throws IllegalArgumentException
	 *             if key is null or of an unsupported type.
	 */
	public void mapKeys(Object key, ExecutionContext executionContext);

	/**
	 * Given the provided restart data, return a PreparedStatementSeter that can
	 * be used as parameters to a JdbcTemplate.
	 * 
	 * @param executionContext
	 * @return an array of objects that can be used as arguments to a
	 *         JdbcTemplate.
	 */
	public PreparedStatementSetter createSetter(
			ExecutionContext executionContext);
}
