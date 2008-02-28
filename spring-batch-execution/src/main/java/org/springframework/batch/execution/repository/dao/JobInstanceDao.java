package org.springframework.batch.execution.repository.dao;

import org.springframework.batch.core.domain.Job;
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
	 * PreConditions: JobInstance for given name and parameters must not already exist
	 * 
	 * PostConditions: A valid job instancewill be returned which has been persisted and
	 * contains an unique Id.
	 * 
	 * @param jobName
	 * @param jobParameters
	 * @return JobInstance
	 */
	JobInstance createJobInstance(Job job, JobParameters jobParameters);

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
	JobInstance getJobInstance(Job job, JobParameters jobParameters);

}
