package org.springframework.batch.core.repository.dao.gemfire;

import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.data.gemfire.repository.GemfireRepository;

/**
 * Spring Data Gemfire compliant JobExecution Repository
 * 
 * 
 * @author Will Schipp
 *
 */
public interface GemfireJobExecutionDao extends GemfireRepository<JobExecution,Long>, JobExecutionDao {

	/**
	 * helper method to retrieve a JobExecution by it's id
	 * @param jobId
	 * @return
	 */
	public List<JobExecution> findByJobId(Long jobId);
	
	/**
	 * helper method to return 'unfinished'/running jobs
	 * @return
	 */
	public List<JobExecution> findByEndTimeIsNull();
	
}
