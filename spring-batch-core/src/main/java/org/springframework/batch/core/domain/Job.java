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
package org.springframework.batch.core.domain;

import java.util.List;

import org.springframework.batch.io.exception.InfrastructureException;

/**
 * Batch domain object representing a job. Job is an explicit abstraction
 * representing the configuration of a job specified by a developer. It should
 * be noted that restart policy is applied to the job as a whole and not to a
 * step.
 * 
 * @author Dave Syer
 * 
 */
public interface Job {

	String getName();

	List getSteps();

	boolean isRestartable();

	/**
	 * Run the {@link JobExecution} and update the meta information like status
	 * and statistics as necessary.
	 * 
	 * @param execution a {@link JobExecution}
	 * @throws InfrastructureException
	 */
	void execute(JobExecution execution) throws InfrastructureException;

}