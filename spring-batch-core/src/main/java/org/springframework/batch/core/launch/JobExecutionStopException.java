/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.core.launch;

/**
 * Exception thrown when a request to stop a job execution does not complete within the
 * configured timeout, i.e. its running step(s) did not reach a terminal state in time.
 *
 * @author Kyungrae Kim
 * @since 6.0
 */
public class JobExecutionStopException extends RuntimeException {

	/**
	 * Create a {@link JobExecutionStopException} with a message and a cause.
	 * @param msg the message to signal the cause of failure
	 * @param cause the underlying cause
	 */
	public JobExecutionStopException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
