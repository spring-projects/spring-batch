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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

/**
 * <p>
 * Repository for storing batch {@link JobExecution} and {@link StepExecution}s. 
 * Before using any methods, a {@link JobExecution}
 * must first be obtained using the createJobExecution method. Once a {@link JobExecution}
 * is obtained, they can be updated.
 * </p>
 * 
 * @author Lucas Ward
 * 
 */
public interface JobRepository {

	/**
	 * Find or create a {@link JobExecution} for a given {@link Job} and
	 * {@link JobParameters}. If the {@link Job} that is uniquely identified by
	 * {@link JobParameters} already exists, its persisted values (including ID)
	 * will be returned in a new {@link JobInstance} object, associated with the
	 * returned {@link JobExecution}. If no previous run
	 * is found, the execution will be associated with a new {@link JobInstance}
	 * 
	 * @param jobParameters the runtime parameters for the job
	 * @param job the job the execution should be associated with.
	 * 
	 * @return a valid job {@link JobExecution} for the arguments provided
	 * @throws JobExecutionAlreadyRunningException if there is a
	 * {@link JobExecution} alrady running for the job instance that would
	 * otherwise be returned
	 * @throws JobRestartException if more than one JobInstance if found or if
	 * JobInstance.getJobExecutionCount() is greater than Job.getStartLimit()
	 * 
	 */
     JobExecution createJobExecution(Job job, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException;

	/**
	 * Save or Update a {@link JobExecution}. If no ID is found a new instance
	 * will be saved. If an ID does exist it will be updated. The ID should only
	 * be assigned to a {@link JobExecution} by calling this method - it should
	 * be left blank on the first call, and assigned by the
	 * {@link JobRepository}.
	 * 
	 * Preconditions: {@link JobExecution} must contain a valid
	 * {@link JobInstance}.
	 * 
	 * @param jobInstance
	 */
	void saveOrUpdate(JobExecution jobExecution);

	/**
	 * Save or update a {@link StepExecution}. If no ID is found a new instance
	 * will be created (and saved). If an ID does exist it will be updated. It
	 * is not advisable that an ID be assigned before calling this method.
	 * Instead, it should be left blank, to be assigned by a
	 * {@link JobRepository}. The {@link ExecutionContext} of the
	 * {@link StepExecution} is <em>not</em> saved: see
	 * {@link #saveExecutionContext(StepExecution)}.
	 * 
	 * Preconditions: {@link StepExecution} must have a valid {@link Step}.
	 * 
	 * @param jobInstance
	 */
	void saveOrUpdate(StepExecution stepExecution);

	/**
	 * Save the {@link ExecutionContext} of the given {@link StepExecution}.
	 * Implementations are allowed to ensure that the {@link StepExecution} is
	 * already saved by calling {@link #saveOrUpdate(StepExecution)} before
	 * saving the {@link ExecutionContext}.
	 * 
	 * @param stepExecution the {@link StepExecution} containing the
	 * {@link ExecutionContext} to be saved.
	 */
	void saveOrUpdateExecutionContext(StepExecution stepExecution);

	/**
	 * @return the last execution of step for the given job instance.
	 */
	StepExecution getLastStepExecution(JobInstance jobInstance, Step step);

	/**
	 * @return the execution count of the step within the given job instance.
	 */
	int getStepExecutionCount(JobInstance jobInstance, Step step);

}
