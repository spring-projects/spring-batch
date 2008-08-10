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
package org.springframework.batch.core.configuration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;

/**
 * Checked exception that indicates a name clash when registering
 * {@link Job} instances.
 * 
 * @author Dave Syer
 * 
 */
public class DuplicateJobException extends JobExecutionException {

	/**
	 * Create an exception with the given message.
	 */
	public DuplicateJobException(String msg) {
		super(msg);
	}

	/**
	 * @param msg The message to send to caller
	 * @param e the cause of the exception
	 */
	public DuplicateJobException(String msg, Throwable e) {
		super(msg, e);
	}

}
