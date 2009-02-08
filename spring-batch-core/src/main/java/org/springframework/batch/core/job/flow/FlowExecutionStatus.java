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

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;

/**
 * This class is used as a holder for a BatchStatus/ExitStatus pair.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public class FlowExecutionStatus implements Comparable<FlowExecutionStatus> {

	private final String status;
	private final ExitStatus exitStatus;

	/**
	 * Special well-known status value.
	 */
	public static final FlowExecutionStatus COMPLETED = new FlowExecutionStatus(BatchStatus.COMPLETED,
			ExitStatus.COMPLETED);

	/**
	 * Special well-known status value.
	 */
	public static final FlowExecutionStatus STOPPED = new FlowExecutionStatus(BatchStatus.STOPPED, ExitStatus.FAILED);

	/**
	 * Special well-known status value.
	 */
	public static final FlowExecutionStatus FAILED = new FlowExecutionStatus(BatchStatus.FAILED, ExitStatus.FAILED);

	/**
	 * Special well-known status value.
	 */
	public static final FlowExecutionStatus UNKNOWN = new FlowExecutionStatus(BatchStatus.UNKNOWN, ExitStatus.UNKNOWN);

	/**
	 * @param status
	 */
	public FlowExecutionStatus(String status) {
		this(status, null);
	}

	/**
	 * @param status
	 * @param exitStatus
	 */
	public FlowExecutionStatus(String status, ExitStatus exitStatus) {
		this.status = status;
		this.exitStatus = exitStatus;
	}

	/**
	 * Convenience constructor that accepts a {@link BatchStatus} and
	 * {@link ExitStatus}.
	 * 
	 * @param batchStatus
	 * @param exitStatus
	 */
	public FlowExecutionStatus(BatchStatus batchStatus, ExitStatus exitStatus) {
		this(batchStatus.toString(), exitStatus);
	}

	/**
	 * Convenience getter for the {@link BatchStatus}.
	 * 
	 * @return a {@link BatchStatus} representing the status
	 */
	public BatchStatus getBatchStatus() {
		return BatchStatus.match(status);
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
		BatchStatus one = this.getBatchStatus();
		BatchStatus two = other.getBatchStatus();
		int comparison = one.compareTo(two);
		if (comparison == 0) {
			return status.compareTo(other.getStatus());
		}
		return comparison;
	}

	/**
	 * Check the equality of the statuses.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof FlowExecutionStatus)) {
			return false;
		}
		FlowExecutionStatus flowExecutionStatus = (FlowExecutionStatus) other;
		return status.equals(flowExecutionStatus.getStatus());
	}

	public String toString() {
		return "FlowExecutionStatus: status=[" + status + "] exitstatus=[" + exitStatus + "]";
	}

	public String getStatus() {
		return status;
	}

	public ExitStatus getExitStatus() {
		return exitStatus;
	}
}
