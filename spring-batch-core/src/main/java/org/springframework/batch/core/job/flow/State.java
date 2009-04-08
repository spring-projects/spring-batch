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

/**
 * @author Dave Syer
 * @since 2.0
 */
public interface State {

	/**
	 * The name of the state. Should be unique within a flow.
	 * 
	 * @return the name of this state
	 */
	String getName();

	/**
	 * Handle some business or processing logic and return a status that can be
	 * used to drive a flow to the next {@link State}. The status can be any
	 * string, but special meaning is assigned to the static constants in
	 * {@link FlowExecution}. The context can be used by implementations to do
	 * whatever they need to do. The same context will be passed to all
	 * {@link State} instances, so implementations should be careful that the
	 * context is thread safe, or used in a thread safe manner.
	 * 
	 * @param executor the context passed in by the caller
	 * @return a status for the execution
	 * @throws Exception if anything goes wrong
	 */
	FlowExecutionStatus handle(FlowExecutor executor) throws Exception;

	/**
	 * Inquire as to whether a {@link State} is an end state. Implementations
	 * should return false if processing can continue, even if that would
	 * require a restart.
	 * 
	 * @return true if this {@link State} is the end of processing
	 */
	boolean isEndState();

}
