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
package org.springframework.batch.core.executor;

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Support class with empty implementations of interface methods.
 * 
 * @author Dave Syer
 * 
 */
public abstract class AbstractJobExecutor implements JobExecutor {

	/**
	 * Wraps a call to {@link #run(JobConfiguration, JobExecution)} with a call
	 * to the listener's before and after methods. The after listener is also
	 * executed in a finally block.
	 * 
	 * @see org.springframework.batch.core.executor.JobExecutor#run(org.springframework.batch.core.configuration.JobConfiguration,
	 *      org.springframework.batch.core.domain.JobExecution,
	 *      org.springframework.batch.core.executor.JobExecutionListener)
	 */
	public final ExitStatus run(JobConfiguration configuration,
			JobExecution execution, JobExecutionListener listener)
			throws BatchCriticalException {
		listener.before(execution);
		try {
			return this.run(configuration, execution);
		} finally {
			listener.after(execution);
		}
	}

}
