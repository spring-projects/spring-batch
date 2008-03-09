package org.springframework.batch.core.repository.support.dao;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
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
	 * @param stepExecution the {@link StepExecution} containing the
	 * {@link ExecutionContext} to be saved.
	 * @throws IllegalArgumentException if the attributes are null.
	 */
	void saveOrUpdateExecutionContext(StepExecution stepExecution);

	StepExecution getStepExecution(JobExecution jobExecution, Step step);

}
