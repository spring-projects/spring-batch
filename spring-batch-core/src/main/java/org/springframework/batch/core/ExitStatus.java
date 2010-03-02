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

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import org.springframework.util.StringUtils;

/**
 * Value object used to carry information about the status of a
 * job or step execution.
 * 
 * ExitStatus is immutable and therefore thread-safe.
 * 
 * @author Dave Syer
 * 
 */
public class ExitStatus implements Serializable, Comparable<ExitStatus> {

	/**
	 * Convenient constant value representing unknown state - assumed not
	 * continuable.
	 */
	public static final ExitStatus UNKNOWN = new ExitStatus("UNKNOWN");

	/**
	 * Convenient constant value representing continuable state where processing
	 * is still taking place, so no further action is required. Used for
	 * asynchronous execution scenarios where the processing is happening in
	 * another thread or process and the caller is not required to wait for the
	 * result.
	 */
	public static final ExitStatus EXECUTING = new ExitStatus("EXECUTING");

	/**
	 * Convenient constant value representing finished processing.
	 */
	public static final ExitStatus COMPLETED = new ExitStatus("COMPLETED");

	/**
	 * Convenient constant value representing job that did no processing (e.g.
	 * because it was already complete).
	 */
	public static final ExitStatus NOOP = new ExitStatus("NOOP");

	/**
	 * Convenient constant value representing finished processing with an error.
	 */
	public static final ExitStatus FAILED = new ExitStatus("FAILED");

	/**
	 * Convenient constant value representing finished processing with
	 * interrupted status.
	 */
	public static final ExitStatus STOPPED = new ExitStatus("STOPPED");

	private final String exitCode;

	private final String exitDescription;

	public ExitStatus(String exitCode) {
		this(exitCode, "");
	}

	public ExitStatus(String exitCode, String exitDescription) {
		super();
		this.exitCode = exitCode;
		this.exitDescription = exitDescription == null ? "" : exitDescription;
	}

	/**
	 * Getter for the exit code (defaults to blank).
	 * 
	 * @return the exit code.
	 */
	public String getExitCode() {
		return exitCode;
	}

	/**
	 * Getter for the exit description (defaults to blank)
	 */
	public String getExitDescription() {
		return exitDescription;
	}

	/**
	 * Create a new {@link ExitStatus} with a logical combination of the exit
	 * code, and a concatenation of the descriptions. If either value has a
	 * higher severity then its exit code will be used in the result. In the
	 * case of equal severity, the exit code is replaced if the new value is
	 * alphabetically greater.<br/>
	 * <br/>
	 * 
	 * Severity is defined by the exit code:
	 * <ul>
	 * <li>Codes beginning with EXECUTING have severity 1</li>
	 * <li>Codes beginning with COMPLETED have severity 2</li>
	 * <li>Codes beginning with NOOP have severity 3</li>
	 * <li>Codes beginning with STOPPED have severity 4</li>
	 * <li>Codes beginning with FAILED have severity 5</li>
	 * <li>Codes beginning with UNKNOWN have severity 6</li>
	 * </ul>
	 * Others have severity 7, so custom exit codes always win.<br/>
	 * 
	 * If the input is null just return this.
	 * 
	 * @param status an {@link ExitStatus} to combine with this one.
	 * @return a new {@link ExitStatus} combining the current value and the
	 * argument provided.
	 */
	public ExitStatus and(ExitStatus status) {
		if (status == null) {
			return this;
		}
		ExitStatus result = addExitDescription(status.exitDescription);
		if (compareTo(status) < 0) {
			result = result.replaceExitCode(status.exitCode);
		}
		return result;
	}
	
	/**
	 * @param status an {@link ExitStatus} to compare
	 * @return 1,0,-1 according to the severity and exit code
	 */
	public int compareTo(ExitStatus status) {
		if (severity(status) > severity(this)) {
			return -1;
		}
		if (severity(status) < severity(this)) {
			return 1;
		}
		return this.getExitCode().compareTo(status.getExitCode());
	}

	/**
	 * @param status
	 * @return
	 */
	private int severity(ExitStatus status) {
		if (status.exitCode.startsWith(EXECUTING.exitCode)) {
			return 1;
		}
		if (status.exitCode.startsWith(COMPLETED.exitCode)) {
			return 2;
		}
		if (status.exitCode.startsWith(NOOP.exitCode)) {
			return 3;
		}
		if (status.exitCode.startsWith(STOPPED.exitCode)) {
			return 4;
		}
		if (status.exitCode.startsWith(FAILED.exitCode)) {
			return 5;
		}
		if (status.exitCode.startsWith(UNKNOWN.exitCode)) {
			return 6;
		}
		return 7;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("exitCode=%s;exitDescription=%s", exitCode, exitDescription);
	}

	/**
	 * Compare the fields one by one.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		return toString().equals(obj.toString());
	}

	/**
	 * Compatible with the equals implementation.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Add an exit code to an existing {@link ExitStatus}. If there is already a
	 * code present tit will be replaced.
	 * 
	 * @param code the code to add
	 * @return a new {@link ExitStatus} with the same properties but a new exit
	 * code.
	 */
	public ExitStatus replaceExitCode(String code) {
		return new ExitStatus(code, exitDescription);
	}

	/**
	 * Check if this status represents a running process.
	 * 
	 * @return true if the exit code is "RUNNING" or "UNKNOWN"
	 */
	public boolean isRunning() {
		return "RUNNING".equals(this.exitCode) || "UNKNOWN".equals(this.exitCode);
	}

	/**
	 * Add an exit description to an existing {@link ExitStatus}. If there is
	 * already a description present the two will be concatenated with a
	 * semicolon.
	 * 
	 * @param description the description to add
	 * @return a new {@link ExitStatus} with the same properties but a new exit
	 * description
	 */
	public ExitStatus addExitDescription(String description) {
		StringBuffer buffer = new StringBuffer();
		boolean changed = StringUtils.hasText(description) && !exitDescription.equals(description);
		if (StringUtils.hasText(exitDescription)) {
			buffer.append(exitDescription);
			if (changed) {
				buffer.append("; ");
			}
		}
		if (changed) {
			buffer.append(description);
		}
		return new ExitStatus(exitCode, buffer.toString());
	}

	/**
	 * Extract the stack trace from the throwable provided and append it to
	 * the exist description.
	 * 
	 * @param throwable
	 * @return a new ExitStatus with the stack trace appended
	 */
	public ExitStatus addExitDescription(Throwable throwable) {
		StringWriter writer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(writer));
		String message = writer.toString();
		return addExitDescription(message);
	}

}
