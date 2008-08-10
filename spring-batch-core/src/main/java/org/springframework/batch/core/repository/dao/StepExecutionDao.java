package org.springframework.batch.core.repository.dao;

import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

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
	 * Retrieve a {@link StepExecution} from its parent {@link JobExecution} and
	 * step name.
	 * 
	 * @param jobExecution the parent job execution
	 * @param stepName the name of the step that was used to create the step execution
	 * @return a {@link StepExecution}
	 */
	StepExecution getStepExecution(JobExecution jobExecution, String stepName);
	
	/**
	 * Retrieve all the {@link StepExecution} for the parent {@link JobExecution}.
	 * 
	 * @param jobExecution the parent job execution
	 * @return a list of {@link StepExecution}
	 */
	List<StepExecution> getStepExecutions(JobExecution jobExecution);

}
