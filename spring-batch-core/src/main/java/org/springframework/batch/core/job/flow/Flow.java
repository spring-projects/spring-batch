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
package org.springframework.batch.core.job.flow;

import java.util.Collection;

/**
 * @author Dave Syer
 * @since 2.0
 */
public interface Flow {

	/**
	 * @return the name of the flow
	 */
	String getName();

	/**
	 * Retrieve the State with the given name. If there is no State with the
	 * given name, then return null.
	 * 
	 * @param stateName
	 * @return the State
	 */
	State getState(String stateName);

	/**
	 * @throws FlowExecutionException
	 */
	FlowExecution start(FlowExecutor executor) throws FlowExecutionException;

	/**
	 * @param stateName the name of the state to resume on
	 * @param executor the context to be passed into each state executed
	 * @return a {@link FlowExecution} containing the exit status of the flow
	 * @throws FlowExecutionException
	 */
	FlowExecution resume(String stateName, FlowExecutor executor) throws FlowExecutionException;

	/**
	 * Convenient accessor for clients needing to explore the states of this
	 * flow.
	 * @return the states
	 */
	Collection<State> getStates();

}