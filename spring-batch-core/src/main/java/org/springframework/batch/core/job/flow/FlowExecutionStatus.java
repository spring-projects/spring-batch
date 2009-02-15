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
 * This class is used as a holder for a BatchStatus/ExitStatus pair.
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

	private final String status;

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
		this.status = status;
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
		Status one = Status.match(this.status);
		Status two = Status.match(other.status);
		int comparison = one.compareTo(two);
		if (comparison == 0) {
			return this.status.compareTo(other.status);
		}
		return comparison;
	}

	/**
	 * Check the equality of the statuses.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (!(object instanceof FlowExecutionStatus)) {
			return false;
		}
		FlowExecutionStatus other = (FlowExecutionStatus) object;
		return status.equals(other.status);
	}

	public String toString() {
		return "FlowExecutionStatus: " + status;
	}

	public String getStatus() {
		return status;
	}

}
