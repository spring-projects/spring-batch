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

	/**
	 * Flag to indicate if this job can be restarted, at least in principle.
	 * 
	 * @return true if this job can be restarted after a failure
	 */
	boolean isRestartable();

	/**
	 * Run the {@link JobExecution} and update the meta information like status
	 * and statistics as necessary. This method should not throw any exceptions
	 * for failed execution. Clients should be careful to inspect the
	 * {@link JobExecution} status to determine success or failure.
	 * 
	 * @param execution a {@link JobExecution}
	 */
	void execute(JobExecution execution);

	/**
	 * If clients need to generate new parameters for the next execution in a
	 * sequence they can use this incrementer. The return value may be null, in
	 * the case that this job does not have a natural sequence.
	 * 
	 * @return in incrementer to be used for creating new parameters
	 */
	JobParametersIncrementer getJobParametersIncrementer();

	/**
	 * A validator for the job parameters of a {@link JobExecution}. Clients of
	 * a Job may need to validate the parameters for a launch, before or during
	 * the execution.
	 * 
	 * @return a validator that can be used to check parameter values (never
	 * null)
	 */
	JobParametersValidator getJobParametersValidator();

}