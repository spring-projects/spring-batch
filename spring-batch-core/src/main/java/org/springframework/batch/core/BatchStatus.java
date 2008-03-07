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

package org.springframework.batch.core;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Typesafe enumeration representing the status of an artifact within the batch environment. See Effective Java
 * Programming by Joshua Bloch for more details on the pattern used.
 * 
 * A BatchStatus can be safely serialized, however, it should be noted that the pattern can break down if different
 * class loaders load the enumeration.
 * 
 * @author Lucas Ward
 * @author Greg Kick
 */

public class BatchStatus implements Serializable {

	private static final long serialVersionUID = 1634960297477743037L;

	private final String name;

	private BatchStatus(String name) {
		this.name = name;
	}

	private Object readResolve() throws ObjectStreamException {
		return getStatus(name);
	}

	public String toString() {
		return name;
	}

	public static final BatchStatus COMPLETED = new BatchStatus("COMPLETED");

	public static final BatchStatus STARTED = new BatchStatus("STARTED");

	public static final BatchStatus STARTING = new BatchStatus("STARTING");

	public static final BatchStatus FAILED = new BatchStatus("FAILED");

	public static final BatchStatus STOPPING = new BatchStatus("STOPPING");

	public static final BatchStatus STOPPED = new BatchStatus("STOPPED");

	public static final BatchStatus UNKNOWN = new BatchStatus("UNKNOWN");

	private static final BatchStatus[] VALUES = { STARTING, STARTED, COMPLETED, FAILED, STOPPING, STOPPED, UNKNOWN };

	/**
	 * Given a string representation of a status, return the appropriate BatchStatus.
	 * 
	 * @param statusAsString string representation of a status
	 * @return a valid BatchStatus or null if the input is null
	 * @throws IllegalArgumentException if no status matches provided string.
	 */
	public static BatchStatus getStatus(String statusAsString) {
		if (statusAsString == null) {
			return null;
		}
		final String upperCaseStatusAsString = statusAsString.toUpperCase();
		for (int i = 0; i < VALUES.length; i++) {
			if (VALUES[i].toString().equals(upperCaseStatusAsString)) {
				return VALUES[i];
			}
		}
		throw new IllegalArgumentException("The string did not match a valid status.");
	}
}
