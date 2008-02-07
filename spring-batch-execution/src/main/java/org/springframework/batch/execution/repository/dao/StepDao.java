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
	 * @param jobInstance
	 * @param stepName
	 * @return StepInstance
	 */
	StepInstance findStepInstance(JobInstance jobInstance, String stepName);

	/**
	 * Find all StepInstances of the given JobInstance.
	 * 
	 * @param jobInstance the job to use as a search key
	 * @return list of {@link StepInstance}
	 */
	List findStepInstances(JobInstance jobInstance);

	/**
	 * Create a StepInstance for the given name and JobInstance.
	 * 
	 * @param jobInstance
	 * @param stepName
	 * 
	 * @return
	 */
	StepInstance createStepInstance(JobInstance jobInstance, String stepName);

	/**
	 * Update an existing StepInstance.
	 * 
	 * Preconditions: StepInstance must have an ID.
	 * 
	 * @param job
	 */
	void updateStepInstance(StepInstance stepInstance);

	/**
	 * Save the given StepExecution.
	 * 
	 * Preconditions: Id must be null.
	 * 
	 * Postconditions: Id will be set to a unique Long.
	 * 
	 * @param stepExecution
	 */
	void saveStepExecution(StepExecution stepExecution);

	/**
	 * Update the given StepExecution
	 * 
	 * Preconditions: Id must not be null.
	 * 
	 * @param stepExecution
	 */
	void updateStepExecution(StepExecution stepExecution);

	/**
	 * Return the count of StepExecutions for the given {@link StepInstance}.
	 * 
	 * @param stepInstance the {@link StepInstance} to check for executions
	 * @return the number of step executions for this step
	 */
	int getStepExecutionCount(StepInstance stepInstance);

	/**
	 * Return all StepExecutions for the given step.
	 * 
	 * @param stepInstance the step to use as a search key
	 * @return list of stepExecutions
	 */
	List findStepExecutions(StepInstance stepInstance);

	/**
	 * Return a StepExecution for the given id.
	 * 
	 * @param stepExecutionId
	 * @return {@link StepExecution} for the provided id.
	 * @throws {@link IncorrectResultSizeDataAccessException} if more than one
	 * execution is found.
	 */
	StepExecution getStepExecution(Long stepExecutionId, StepInstance stepInstance);

	/**
	 * Find all {@link ExecutionAttributes} for the given execution id.
	 * 
	 * @param executionId - Long id of the {@link StepExecution} that the
	 * attributes belongs to.
	 * @return attributes for the provided id. If none are found, an empty
	 * {@link ExecutionAttributes} will be returned.
	 * @throws IllegalArgumentException if the id is null.
	 */
	ExecutionAttributes findExecutionAttributes(final Long executionId);

	/**
	 * Save the provided {@link ExecutionAttributes} for the given executionId.
	 * 
	 * @param executionId to be saved
	 * @param executionAttributes to be saved.
	 * @throws IllegalArgumentException if the executionId or attributes are
	 * null.
	 */
	void saveExecutionAttributes(final Long executionId, final ExecutionAttributes executionAttributes);

	/**
	 * Update the provided ExecutionAttributes.
	 * 
	 * @param executionId
	 * @param executionAttributes
	 */
	void updateExecutionAttributes(final Long executionId, ExecutionAttributes executionAttributes);
}
