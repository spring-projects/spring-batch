/*
 * Copyright 2009-2023 the original author or authors.
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
package org.springframework.batch.core.job.parameters;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecutionException;

/**
 * Exception for {@link Job} to signal that some {@link JobParameters} are invalid.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class JobParametersInvalidException extends JobExecutionException {

	/**
	 * Constructor that sets the message for the exception.
	 * @param msg The {@link String} message for the {@link Exception}.
	 */
	public JobParametersInvalidException(String msg) {
		super(msg);
	}

}
