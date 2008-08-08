package org.springframework.batch.core.repository.dao;

import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;

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
	 * Preconditions: jobInstance the jobExecution belongs to must have a jobInstanceId.
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
	 * Return list of JobExecutions for given JobInstance.
	 * 
	 * @param jobInstance
	 * @return list of jobExecutions.
	 */
	List<JobExecution> findJobExecutions(JobInstance jobInstance);

	/**
	 * @return last JobExecution for given JobInstance.
	 */
	JobExecution getLastJobExecution(JobInstance jobInstance);

	/**
	 * @return last JobExecution for given job name.
	 */
	JobExecution getLastJobExecution(String jobName);

}
