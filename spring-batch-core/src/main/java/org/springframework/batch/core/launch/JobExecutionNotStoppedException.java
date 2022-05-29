/*
 * Copyright 2006-2007 the original author or authors.
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

import org.springframework.batch.core.JobExecutionException;

/**
 * Checked exception to indicate that user asked for a job execution to be aborted when
 * hasn't been stopped.
 *
 * @author Dave Syer
 *
 */
@SuppressWarnings("serial")
public class JobExecutionNotStoppedException extends JobExecutionException {

	/**
	 * Create an exception with the given message.
	 * @param msg the message.
	 */
	public JobExecutionNotStoppedException(String msg) {
		super(msg);
	}

}
