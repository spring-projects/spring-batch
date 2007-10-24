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
package org.springframework.batch.execution.facade;

import org.springframework.batch.core.domain.JobExecution;

/**
 * Listener interface for the job execution lifecycle.
 * 
 * @author Dave Syer
 * 
 */
public interface JobExecutionListener {

	/**
	 * Callback for the start of a job, before any steps are processed.
	 * 
	 * @param execution
	 *            the current {@link JobExecution}
	 */
	void before(JobExecution execution);

	/**
	 * Callback for the start of a job, after all steps are processed, or on an
	 * error.
	 * 
	 * @param execution
	 */
	void after(JobExecution execution);

	/**
	 * Callback for a job that has been stopped, or asked to stop.
	 * 
	 * @param execution
	 */
	void stop(JobExecution execution);
}
