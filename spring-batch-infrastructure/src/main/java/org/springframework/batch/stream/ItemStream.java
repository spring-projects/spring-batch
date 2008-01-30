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

package org.springframework.batch.stream;

/**
 * <p>
 * Marker interface defining a contract for periodically storing state and
 * restoring from that state should an error occur.
 * <p>
 * 
 * <p>
 * The state that is stored is represented as {@link StreamContext} which
 * enforces a requirement that any restart data can be represented by a
 * Properties object. In general, the contract is that {@link StreamContext}
 * that is returned via the {@link #getRestartData()} method will be given back
 * to the {@link #restoreFrom(StreamContext)} method, exactly as it was
 * provided.
 * </p>
 * 
 * @author Lucas Ward
 * 
 */
public interface ItemStream {

	/**
	 * Get {@link StreamContext} representing this object's current state.
	 * Should not return null even if there is no state.
	 * 
	 * @return {@link StreamContext} representing current state.
	 */
	StreamContext getRestartData();

	/**
	 * Restart state given the provided {@link StreamContext}.
	 * 
	 * @param data
	 */
	void restoreFrom(StreamContext data);
}
