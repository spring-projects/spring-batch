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


/**
 * Exception to indicate the the job has been interrupted. The exception state
 * indicated is not normally recoverable by batch application clients, but
 * internally it is useful to force a check. The exception will often be wrapped
 * in a runtime exception (usually {@link UnexpectedJobExecutionException} before
 * reaching the client.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class JobInterruptedException extends JobExecutionException {
	
	private BatchStatus status = BatchStatus.STOPPED;

	public JobInterruptedException(String msg) {
		super(msg);
	}

	public JobInterruptedException(String msg, BatchStatus status) {
		super(msg);
		this.status = status;
	}
	
	/**
	 * The desired status of the surrounding execution after the interruption.
	 * 
	 * @return the status of the interruption (default STOPPED)
	 */
	public BatchStatus getStatus() {
		return status;
	}
}
