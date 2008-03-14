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
package org.springframework.batch.core.repository;

import org.springframework.batch.core.JobExecutionException;

/**
 * An exception indicating an illegal attempt to restart a job that was already
 * completed successfully.
 * 
 * @author Dave Syer
 * 
 */
public class JobInstanceAlreadyCompleteException extends JobExecutionException {

	/**
	 * @param string the message
	 */
	public JobInstanceAlreadyCompleteException(String string) {
		super(string);
	}

	/**
	 * @param msg the cause
	 * @param t the message
	 */
	public JobInstanceAlreadyCompleteException(String msg, Throwable t) {
		super(msg, t);
	}

}
