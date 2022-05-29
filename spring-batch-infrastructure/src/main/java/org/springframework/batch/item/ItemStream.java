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

package org.springframework.batch.item;

/**
 * <p>
 * Marker interface defining a contract for periodically storing state and restoring from
 * that state should an error occur.
 * </p>
 *
 * @author Dave Syer
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
public interface ItemStream {

	/**
	 * Open the stream for the provided {@link ExecutionContext}.
	 * @param executionContext current step's
	 * {@link org.springframework.batch.item.ExecutionContext}. Will be the
	 * executionContext from the last run of the step on a restart.
	 * @throws IllegalArgumentException if context is null
	 */
	void open(ExecutionContext executionContext) throws ItemStreamException;

	/**
	 * Indicates that the execution context provided during open is about to be saved. If
	 * any state is remaining, but has not been put in the context, it should be added
	 * here.
	 * @param executionContext to be updated
	 * @throws IllegalArgumentException if executionContext is null.
	 */
	void update(ExecutionContext executionContext) throws ItemStreamException;

	/**
	 * If any resources are needed for the stream to operate they need to be destroyed
	 * here. Once this method has been called all other methods (except open) may throw an
	 * exception.
	 */
	void close() throws ItemStreamException;

}
