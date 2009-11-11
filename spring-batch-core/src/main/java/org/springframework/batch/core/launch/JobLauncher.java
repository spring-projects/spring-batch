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
package org.springframework.batch.core.launch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

/**
 * Simple interface for controlling jobs, including possible ad-hoc executions,
 * based on different runtime identifiers. It is extremely important to note
 * that this interface makes absolutely no guarantees about whether or not calls
 * to it are executed synchronously or asynchronously. The javadocs for specific
 * implementations should be checked to ensure callers fully understand how the
 * job will be run.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */

public interface JobLauncher {

	/**
	 * Start a job execution for the given {@link Job} and {@link JobParameters}
	 * . If a {@link JobExecution} was able to be created successfully, it will
	 * always be returned by this method, regardless of whether or not the
	 * execution was successful. If there is a past {@link JobExecution} which
	 * has paused, the same {@link JobExecution} is returned instead of a new
	 * one created. A exception will only be thrown if there is a failure to
	 * start the job. If the job encounters some error while processing, the
	 * JobExecution will be returned, and the status will need to be inspected.
	 * 
	 * @return the {@link JobExecution} if it returns synchronously. If the
	 * implementation is asynchronous, the status might well be unknown.
	 * 
	 * @throws JobExecutionAlreadyRunningException if the JobInstance identified
	 * by the properties already has an execution running.
	 * @throws IllegalArgumentException if the job or jobInstanceProperties are
	 * null.
	 * @throws JobRestartException if the job has been run before and
	 * circumstances that preclude a re-start.
	 * @throws JobInstanceAlreadyCompleteException if the job has been run
	 * before with the same parameters and completed successfully
	 * @throws JobParametersInvalidException if the parameters are not valid for
	 * this job
	 */
	public JobExecution run(Job job, JobParameters jobParameters) throws JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException;

}
