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

package org.springframework.batch.execution.repository.dao;

import java.util.List;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

/**
 * Data access object for steps.
 * 
 * @author Lucas Ward
 * 
 */
public interface StepDao {

	/**
	 * Find a step with the given JobId and Step Name. Return null if none are
	 * found.
	 * 
	 * @param jobId
	 * @param stepName
	 * @return Step
	 */
	public StepInstance findStep(JobInstance job, String stepName);

	/**
	 * Find all steps with the given Job.
	 * 
	 * @param job the job to use as a search key
	 * @return list of {@link StepInstance}
	 */
	public List findSteps(JobInstance job);

	/**
	 * Create a step for the given Step Name and Job Id.
	 * 
	 * @param job
	 * @param stepName
	 * 
	 * @return
	 */
	public StepInstance createStep(JobInstance job, String stepName);

	/**
	 * Update an existing Step.
	 * 
	 * Preconditions: Step must have an ID.
	 * 
	 * @param job
	 */
	public void update(StepInstance step);

	/**
	 * Save the given StepExecution.
	 * 
	 * Preconditions: Id must be null. Postconditions: Id will be set to a
	 * Unique Long.
	 * 
	 * @param stepExecution
	 */
	public void save(StepExecution stepExecution);

	/**
	 * Update the given StepExecution
	 * 
	 * Preconditions: Id must not be null.
	 * 
	 * @param stepExecution
	 */
	public void update(StepExecution stepExecution);

	/**
	 * Return the count of StepExecutions with the given {@link StepInstance}.
	 * 
	 * @param step the {@link StepInstance} to check for executions
	 * @return the number of step executions for this step
	 */
	public int getStepExecutionCount(StepInstance step);

	/**
	 * Return all StepExecutions for the given step.
	 * 
	 * @param step the step to use as a search key
	 * @return list of stepExecutions
	 */
	public List findStepExecutions(StepInstance step);
	
	/**
	 * Return a StepExecution for the given id.
	 * 
	 * @param stepExecutionId
	 * @return {@link StepExecution} for the provided id.
	 * @throws {@link IncorrectResultSizeDataAccessException} if more
	 * than one execution is found.
	 */
	public StepExecution getStepExecution(Long stepExecutionId, StepInstance stepInstance);
	
	/**
	 * Find all {@link ExecutionAttributes} for the given execution id.
	 * 
	 * @param executionId - Long id of the {@link StepExecution}
	 *   that the attributes belongs to.
	 * @return attributes for the provided id.  If
	 * none are found, an empty {@link ExecutionAttributes} will be returned.
	 * @throws IllegalArgumentException if the id is null.
	 */
	ExecutionAttributes findExecutionAttributes(final Long executionId);
	
	/**
	 * Save the provided {@link ExecutionAttributes} for the given 
	 * execution Id.
	 * 
	 * @param executionId to be saved
	 * @param executionAttributes to be saved.
	 * @throws IllegalArgumentException if the executionId or 
	 * attributes are null.
	 */
	void save(final Long executionId, final ExecutionAttributes executionAttributes);
	
	/**
	 * Update the provided execution attributes.
	 * 
	 * @param executionId
	 * @param executionAttributes
	 */
	void update(final Long executionId, ExecutionAttributes executionAttributes);
}
