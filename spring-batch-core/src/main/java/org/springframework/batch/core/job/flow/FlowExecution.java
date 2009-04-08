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
public class FlowExecution implements Comparable<FlowExecution> {

	private final String name;
	private final FlowExecutionStatus status;

	/**
	 * @param name
	 * @param status
	 */
	public FlowExecution(String name, FlowExecutionStatus status) {
		this.name = name;
		this.status = status;
	}

	/**
	 * @return the name of the end state reached
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the FlowExecutionStatus
	 */
	public FlowExecutionStatus getStatus() {
		return status;
	}

	/**
	 * Create an ordering on {@link FlowExecution} instances by comparing their
	 * statuses.
	 * 
	 * @see Comparable#compareTo(Object)
	 * 
	 * @param other
	 * @return negative, zero or positive as per the contract
	 */
	public int compareTo(FlowExecution other) {
		return this.status.compareTo(other.getStatus());
	}

	@Override
	public String toString() {
		return String.format("FlowExecution: name=%s, status=%s", name, status);
	}

}
