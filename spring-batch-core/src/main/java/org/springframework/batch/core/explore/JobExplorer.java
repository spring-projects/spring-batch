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
package org.springframework.batch.core.explore;

import java.util.List;
import java.util.Set;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;

/**
 * Entry point for browsing executions of running or historical jobs and steps.
 * Since the data may be re-hydrated from persistent storage, it may not contain
 * volatile fields that would have been present when the execution was active.
 * 
 * @author Dave Syer
 * 
 * @since 2.0
 */
public interface JobExplorer {

	/**
	 * Fetch {@link JobInstance} values in descending order of creation (and
	 * therefore usually of first execution).
	 * 
	 * @param jobName the name of the job to query
	 * @param start the start index of the instances to return
	 * @param count the maximum number of instances to return
	 * @return the {@link JobInstance} values up to a maximum of count values
	 */
	List<JobInstance> getJobInstances(String jobName, int start, int count);

	/**
	 * Retrieve a {@link JobExecution} by its id.
	 * 
	 * @param executionId the job execution id
	 * @return the {@link JobExecution} with this id, or null if not found
	 */
	JobExecution getJobExecution(Long executionId);

	/**
	 * Retrieve a {@link StepExecution} by its id and parent
	 * {@link JobExecution} id.
	 * 
	 * @param jobExecutionId the parent job execution id
	 * @param stepExecutionId the step execution id
	 * @return the {@link StepExecution} with this id, or null if not found
	 */
	StepExecution getStepExecution(Long jobExecutionId, Long stepExecutionId);

	/**
	 * @param instanceId
	 * @return the {@link JobInstance} with this id, or null
	 */
	JobInstance getJobInstance(Long instanceId);

	/**
	 * @param jobInstance the {@link JobInstance} to query
	 * @return the set of all executions for the specified {@link JobInstance}
	 */
	List<JobExecution> getJobExecutions(JobInstance jobInstance);

	/**
	 * @param jobName the name of the job
	 * @return the set of running executions for jobs with the specified name
	 */
	Set<JobExecution> findRunningJobExecutions(String jobName);

}
