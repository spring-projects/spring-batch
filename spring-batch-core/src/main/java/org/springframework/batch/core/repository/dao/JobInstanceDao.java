package org.springframework.batch.core.repository.dao;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;

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
	 * PostConditions: A valid job instance will be returned which has been persisted and
	 * contains an unique Id.
	 * 
	 * @param job
	 * @param jobParameters
	 * @return JobInstance
	 */
	JobInstance createJobInstance(Job job, JobParameters jobParameters);

	/**
	 * Find all job instances that match the given name and parameters. If no
	 * matching job instances are found, then a list of size 0 will be
	 * returned.
	 * 
	 * @param job
	 * @param jobParameters
	 * @return {@link JobInstance} object matching
	 * {@link Job} and {@link JobParameters}
	 */
	JobInstance getJobInstance(Job job, JobParameters jobParameters);

}
