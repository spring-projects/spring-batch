package org.springframework.batch.core.repository.dao.gemfire;

import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.data.gemfire.repository.GemfireRepository;

/**
 * Spring Data Gemfire compliant JobInstance Repository
 * 
 * 
 * @author Will Schipp
 *
 */
public interface GemfireJobInstanceDao extends GemfireRepository<JobInstance, Long>, JobInstanceDao {

	/**
	 * helper method to find by name
	 * @param jobName
	 * @return
	 */
	public List<JobInstance> findByJobName(String jobName);
	
}
