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
 * 
 */
public class FlowExecution implements Comparable<FlowExecution> {

	/**
	 * Special well-known status value.
	 */
	public static final String COMPLETED = Status.COMPLETED.toString();

	/**
	 * Special well-known status value.
	 */
	public static final String PAUSED = Status.PAUSED.toString();
	
	/**
	 * Special well-known status value.
	 */
	public static final String FAILED = Status.FAILED.toString();

	/**
	 * Special well-known status value.
	 */
	public static final String UNKNOWN = Status.UNKNOWN.toString();

	private final String name;

	private final String status;

	private enum Status {

		COMPLETED, PAUSED, FAILED, UNKNOWN;

		static Status match(String value) {
			for (int i = 0; i < values().length; i++) {
				Status status = values()[i];
				if (value.startsWith(status.toString())) {
					return status;
				}
			}
			// Default match should be the lowest priority
			return COMPLETED;
		}

	};

	/**
	 * 
	 */
	public FlowExecution(String name, String status) {
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
	 * @return the exit status
	 */
	public String getStatus() {
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
		Status one = Status.match(this.getStatus());
		Status two = Status.match(other.getStatus());
		int comparison = one.compareTo(two);
		if (comparison==0) {
			return this.getStatus().compareTo(other.getStatus());
		}
		return comparison;
	}

	@Override
	public String toString() {
		return String.format("FlowExecution: name=%s, status=%s", name, status);
	}

}
