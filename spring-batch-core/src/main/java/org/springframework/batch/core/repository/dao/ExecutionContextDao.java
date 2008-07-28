package org.springframework.batch.core.repository.dao;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

/**
 * DAO interface for persisting and retrieving {@link ExecutionContext}s.
 * 
 * @author Robert Kasanicky
 */
public interface ExecutionContextDao {

	/**
	 * @param jobExecution
	 * @return execution context associated with the given jobExecution
	 */
	ExecutionContext getExecutionContext(JobExecution jobExecution);

	/**
	 * @param stepExecution
	 * @return execution context associated with the given stepExecution
	 */
	ExecutionContext getExecutionContext(StepExecution stepExecution);

	/**
	 * Persist the execution context associated with the given jobExecution
	 * @param jobExecution
	 */
	void persistExecutionContext(final JobExecution jobExecution);

	/**
	 * Persist the execution context associated with the given stepExecution
	 * @param stepExecution
	 */
	void persistExecutionContext(final StepExecution stepExecution);
}
