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

import org.springframework.batch.core.job.flow.support.State;

/**
 * @author Dave Syer
 *
 */
public interface Flow {

	/**
	 * @return the name of the flow
	 */
	String getName();

	/**
	 * Retrieve the State with the given name. An IllegalStateException is
	 * thrown if there is no State with the given name.
	 * 
	 * @param stateName
	 * @return the State
	 */
	public State getState(String stateName);
	
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

}