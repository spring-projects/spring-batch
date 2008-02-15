package org.springframework.batch.execution.repository.dao;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.item.ExecutionContext;

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
	 * Find all {@link ExecutionContext} for the given execution id.
	 * 
	 * @param executionId - Long id of the {@link StepExecution} that the
	 * attributes belongs to.
	 * @return attributes for the provided id. If none are found, an empty
	 * {@link ExecutionContext} will be returned.
	 * @throws IllegalArgumentException if the id is null.
	 */
	ExecutionContext findExecutionContext(final Long executionId);

	/**
	 * Save the provided {@link ExecutionContext} for the given executionId.
	 * 
	 * @param executionId to be saved
	 * @param executionContext to be saved.
	 * @throws IllegalArgumentException if the executionId or attributes are
	 * null.
	 */
	void saveExecutionContext(final Long executionId, final ExecutionContext executionContext);

	/**
	 * Update the provided ExecutionContext.
	 * 
	 * @param executionId
	 * @param executionContext
	 */
	void updateExecutionContext(final Long executionId, ExecutionContext executionContext);

	/**
	 * @param lastJobExecution last job execution
	 * @return the last execution of the given instance
	 */
	StepExecution getLastStepExecution(StepInstance stepInstance, JobExecution lastJobExecution);
}
