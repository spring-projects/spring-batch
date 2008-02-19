package org.springframework.batch.execution.repository.dao;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.StepExecution;
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
	 * Find all {@link ExecutionContext} for the given {@link StepExecution}.
	 * 
	 * @throws IllegalArgumentException if the id is null.
	 */
	ExecutionContext findExecutionContext(StepExecution stepExecution);

	/**
	 * Save the {@link ExecutionContext} of the given {@link StepExecution}.
	 * 
	 * @param executionId to be saved
	 * @param executionContext to be saved.
	 * @throws IllegalArgumentException if the executionId or attributes are
	 * null.
	 */
	void saveExecutionContext(StepExecution stepExecution);

	/**
	 * Update the ExecutionContext of given {@link StepExecution}.
	 */
	void updateExecutionContext(StepExecution stepExecution);
	
	StepExecution getStepExecution(JobExecution jobExecution, String stepName);

}
