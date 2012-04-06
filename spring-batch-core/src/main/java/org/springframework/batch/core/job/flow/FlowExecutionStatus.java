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
 * Represents the status of {@link FlowExecution}.
 * 
 * @author Dan Garrette
 * @author Dave Syer
 * @since 2.0
 */
public class FlowExecutionStatus implements Comparable<FlowExecutionStatus> {

	/**
	 * Special well-known status value.
	 */
	public static final FlowExecutionStatus COMPLETED = new FlowExecutionStatus(Status.COMPLETED.toString());

	/**
	 * Special well-known status value.
	 */
	public static final FlowExecutionStatus STOPPED = new FlowExecutionStatus(Status.STOPPED.toString());

	/**
	 * Special well-known status value.
	 */
	public static final FlowExecutionStatus FAILED = new FlowExecutionStatus(Status.FAILED.toString());

	/**
	 * Special well-known status value.
	 */
	public static final FlowExecutionStatus UNKNOWN = new FlowExecutionStatus(Status.UNKNOWN.toString());

	private final String name;

	private enum Status {

		COMPLETED, STOPPED, FAILED, UNKNOWN;

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
	 * @param status
	 */
	public FlowExecutionStatus(String status) {
		this.name = status;
	}

	/**
	 * @return true if the status starts with "STOPPED"
	 */
	public boolean isStop() {
		return name.startsWith(STOPPED.getName());
	}

	/**
	 * @return true if the status starts with "FAILED"
	 */
	public boolean isFail() {
		return name.startsWith(FAILED.getName());
	}


	/**
	 * @return true if this status represents the end of a flow
	 */
	public boolean isEnd() {
		return isStop() || isFail() || isComplete();
	}

	/**
	 * @return true if the status starts with "COMPLETED"
	 */
	private boolean isComplete() {
		return name.startsWith(COMPLETED.getName());
	}
	/**
	 * Create an ordering on {@link FlowExecutionStatus} instances by comparing
	 * their statuses.
	 * 
	 * @see Comparable#compareTo(Object)
	 * 
	 * @param other
	 * @return negative, zero or positive as per the contract
	 */
	public int compareTo(FlowExecutionStatus other) {
		Status one = Status.match(this.name);
		Status two = Status.match(other.name);
		int comparison = one.compareTo(two);
		if (comparison == 0) {
			return this.name.compareTo(other.name);
		}
		return comparison;
	}

	/**
	 * Check the equality of the statuses.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (!(object instanceof FlowExecutionStatus)) {
			return false;
		}
		FlowExecutionStatus other = (FlowExecutionStatus) object;
		return name.equals(other.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * @return the name of this status
	 */
	public String getName() {
		return name;
	}

}
