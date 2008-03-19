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
 * Root of exception hierarchy for checked exceptions in job and step execution.
 * Clients of the {@link Job} should expect to have to catch and deal with these
 * exceptions because they signal a user error, or an inconsistent state between
 * the user's instructions and the data.
 * 
 * @author Dave Syer
 * 
 */
public class JobExecutionException extends Exception {

	/**
	 * Construct a {@link JobExecutionException} with a generic message.
	 * @param msg the message
	 */
	public JobExecutionException(String msg) {
		super(msg);
	}

	/**
	 * Construct a {@link JobExecutionException} with a generic message and a
	 * cause.
	 * 
	 * @param msg the message
	 * @param cause the cause of the exception
	 */
	public JobExecutionException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
