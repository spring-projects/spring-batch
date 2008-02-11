package org.springframework.batch.execution.repository.dao;

import java.util.List;

import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.item.ExecutionAttributes;

public interface StepExecutionDao {

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
