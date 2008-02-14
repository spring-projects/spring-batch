package org.springframework.batch.execution.repository.dao;

import java.util.List;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;

/**
 * Data Access Object for job instances.
 * 
 * @author Lucas Ward
 * @author Robert Kasanicky
 * 
 */
public interface JobInstanceDao {

	/**
	 * Create a JobInstance with given name and parameters.
	 * 
	 * PostConditions: A valid job will be returned which has been persisted and
	 * contains an unique Id.
	 * 
	 * @param jobName
	 * @param jobParameters
	 * @return JobInstance
	 */
	JobInstance createJobInstance(String jobName, JobParameters jobParameters);

	/**
	 * Find all job instances that match the given name and parameters. If no
	 * matching job instances are found, then a list of size 0 will be
	 * returned.
	 * 
	 * @param jobName
	 * @param jobParameters
	 * @return List of {@link JobInstance} objects matching
	 * {@link JobIdentifier}
	 */
	List findJobInstances(String jobName, JobParameters jobParameters);

}
