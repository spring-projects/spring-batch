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
package org.springframework.batch.io.sql;

import org.springframework.batch.restart.RestartData;

/**
 * Converts an object representing a composite key to RestartData and
 * back again.
 *
 * @author Lucas Ward
 *
 */
public interface CompositeKeyRestartDataConverter {

	/**
	 * Given the provided composite key, return a RestartData representation.
	 *
	 * @param compositeKey
	 * @return ResartData representing the composite key.
	 */
	public RestartData createRestartData(Object compositeKey);

	/**
	 * Given the provided restart data, return an array of objects that can
	 * be used as parameters to a driving query.
	 *
	 * @param restartData
	 * @return an array of objects that can be used as arguments to a JdbcTemplate.
	 */
	public Object[] createArguments(RestartData restartData);
}
