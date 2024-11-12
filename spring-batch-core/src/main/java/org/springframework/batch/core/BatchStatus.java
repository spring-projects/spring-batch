/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.core;

import java.util.Set;

/**
 * Enumeration representing the status of an execution.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
public enum BatchStatus {

	/*
	 * The order of the status values is significant because it can be used to aggregate a
	 * set of status values. The result should be the maximum value. Since {@code
	 * COMPLETED} is first in the order, only if all elements of an execution are {@code
	 * COMPLETED} can the aggregate status be COMPLETED. A running execution is expected
	 * to move from {@code STARTING} to {@code STARTED} to {@code COMPLETED} (through the
	 * order defined by {@link #upgradeTo(BatchStatus)}). Higher values than {@code
	 * STARTED} signify more serious failures. {@code ABANDONED} is used for steps that
	 * have finished processing but were not successful and where they should be skipped
	 * on a restart (so {@code FAILED} is the wrong status).
	 */

	/**
	 * The batch job has successfully completed its execution.
	 */
	COMPLETED,
	/**
	 * Status of a batch job prior to its execution.
	 */
	STARTING,
	/**
	 * Status of a batch job that is running.
	 */
	STARTED,
	/**
	 * Status of batch job waiting for a step to complete before stopping the batch job.
	 */
	STOPPING,
	/**
	 * Status of a batch job that has been stopped by request.
	 */
	STOPPED,
	/**
	 * Status of a batch job that has failed during its execution.
	 */
	FAILED,
	/**
	 * Status of a batch job that did not stop properly and can not be restarted.
	 */
	ABANDONED,
	/**
	 * Status of a batch job that is in an uncertain state.
	 */
	UNKNOWN;

	public static final Set<BatchStatus> RUNNING_STATUSES = Set.of(STARTING, STARTED, STOPPING);

	/**
	 * Convenience method to return the higher value status of the statuses passed to the
	 * method.
	 * @param status1 The first status to check.
	 * @param status2 The second status to check.
	 * @return The higher value status of the two statuses.
	 */
	public static BatchStatus max(BatchStatus status1, BatchStatus status2) {
		return status1.isGreaterThan(status2) ? status1 : status2;
	}

	/**
	 * Convenience method to decide if a status indicates that work is in progress.
	 * @return true if the status is STARTING, STARTED, STOPPING
	 */
	public boolean isRunning() {
		return RUNNING_STATUSES.contains(this);
	}

	/**
	 * Convenience method to decide if a status indicates execution was unsuccessful.
	 * @return {@code true} if the status is {@code FAILED} or greater.
	 */
	public boolean isUnsuccessful() {
		return this == FAILED || this.isGreaterThan(FAILED);
	}

	/**
	 * Method used to move status values through their logical progression, and override
	 * less severe failures with more severe ones. This value is compared with the
	 * parameter, and the one that has higher priority is returned. If both are
	 * {@code STARTED} or less than the value returned is the largest in the sequence
	 * {@code STARTING}, {@code STARTED}, {@code COMPLETED}. Otherwise, the value returned
	 * is the maximum of the two.
	 * @param other Another status to which to compare.
	 * @return either this or the other status, depending on their priority.
	 */
	public BatchStatus upgradeTo(BatchStatus other) {
		if (isGreaterThan(STARTED) || other.isGreaterThan(STARTED)) {
			return max(this, other);
		}
		// Both less than or equal to STARTED
		if (this == COMPLETED || other == COMPLETED) {
			return COMPLETED;
		}
		return max(this, other);
	}

	/**
	 * @param other A status value to which to compare.
	 * @return {@code true} if this is greater than {@code other}.
	 */
	public boolean isGreaterThan(BatchStatus other) {
		return this.compareTo(other) > 0;
	}

	/**
	 * @param other A status value to which to compare.
	 * @return {@code true} if this is less than {@code other}.
	 */
	public boolean isLessThan(BatchStatus other) {
		return this.compareTo(other) < 0;
	}

	/**
	 * @param other A status value to which to compare.
	 * @return {@code true} if this is less than {@code other}.
	 */
	public boolean isLessThanOrEqualTo(BatchStatus other) {
		return this.compareTo(other) <= 0;
	}

	/**
	 * Find a {@code BatchStatus} that matches the beginning of the given value. If no
	 * match is found, return {@code COMPLETED} as the default because it has low
	 * precedence.
	 * @param value A string representing a status.
	 * @return a {BatchStatus} object.
	 */
	public static BatchStatus match(String value) {
		for (BatchStatus status : values()) {
			if (value.startsWith(status.toString())) {
				return status;
			}
		}
		// Default match should be the lowest priority
		return COMPLETED;
	}

}
