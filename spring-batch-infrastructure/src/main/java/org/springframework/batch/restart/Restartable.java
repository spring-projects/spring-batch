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

package org.springframework.batch.restart;

/**
 * <p>Marker interface defining a contract for periodically storing
 * state and restoring from that state should an error occur.
 * <p>
 *
 * <p>The state that is stored is represented as {@link RestartData}
 * which enforces a requirement that any restart data can be represented
 * by a Properties object.  In general, the contract is that RestartData
 * that is returned via the getRestartData method will be given back to
 * the restoreFrom method, exactly as it was provided.  However, since
 * it is primarily stored in a database, there is almost no way to know
 * the whether a blank column in the database refers to null data,
 * null properties, or empty properties.  Therefore, any class implementing
 * this interface should assume that no restart data is equivalent to
 * data with empty Properties.
 * </p>
 *
 * @author Lucas Ward
 *
 */
public interface Restartable {

	/**
	 * Get RestartData representing this object's current state.  Ideally,
	 * if no state should be stored, RestartData.getProperties should return
	 * an empty Properties object.
	 *
	 * @return RestartData representing current state.
	 */
	RestartData getRestartData();

	/**
	 * Restart state given the provided RestartData.
	 *
	 * @param data
	 */
	void restoreFrom(RestartData data);
}
