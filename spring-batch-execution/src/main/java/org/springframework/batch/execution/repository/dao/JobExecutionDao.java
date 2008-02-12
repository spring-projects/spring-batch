package org.springframework.batch.execution.repository.dao;

import java.util.List;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;

/**
 * Data Access Object for job executions.
 * 
 * @author Lucas Ward
 * @author Robert Kasanicky
 */
public interface JobExecutionDao {

	/**
	 * Save a new JobExecution.
	 * 
	 * Preconditions: jobExecution must have a jobInstanceId.
	 * 
	 * @param jobExecution
	 */
	void saveJobExecution(JobExecution jobExecution);

	/**
	 * Update and existing JobExecution.
	 * 
	 * Preconditions: jobExecution must have an Id (which can be obtained by the
	 * save method) and a jobInstanceId.
	 * 
	 * @param jobExecution
	 */
	void updateJobExecution(JobExecution jobExecution);

	/**
	 * Return the number of JobExecutions with the given jobInstanceId
	 * 
	 * Preconditions: jobInstance must have an id.
	 * 
	 * @param jobInstanceId
	 */
	int getJobExecutionCount(Long jobInstanceId);

	/**
	 * Return list of JobExecutions for given JobInstance.
	 * 
	 * @param jobInstance
	 * @return list of jobExecutions.
	 */
	List findJobExecutions(JobInstance jobInstance);

	/**
	 * Given an id, return the matching JobExecution.
	 * 
	 * @param jobExecutionId - id of the execution to be returned.
	 * @return {@link JobExecution} matching the id.
	 * @throws {@link IncorrectResultSizeDataAccessException} if more than one
	 * execution is found for the given id.
	 */
	JobExecution getJobExecution(Long jobExecutionId);
}
