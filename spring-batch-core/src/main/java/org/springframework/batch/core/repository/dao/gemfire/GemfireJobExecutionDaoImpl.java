package org.springframework.batch.core.repository.dao.gemfire;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.query.SelectResults;

/**
 * GemFire implementation of the JobExecutionDao
 *  (note - naming breaks with the convention of exiting Batch implementations due to Spring Data compliance)
 * 
 * @author Will Schipp
 *
 */
public class GemfireJobExecutionDaoImpl implements JobExecutionDao {

	private static final String LAST_EXECUTION = "SELECT DISTINCT * from /JobExecution where JobId = $1 ORDER BY createTime desc";
	
	@Autowired
	private GemfireJobExecutionDao gemfireJobExecutionDao;
	
	@Autowired
	private GemfireTemplate jobExecutionTemplate;
	
	@Autowired
	private GemfireJobInstanceDao gemfireJobInstanceDao;
	
	@Override
	public void saveJobExecution(JobExecution jobExecution) {
		//validate
		validateJobExecution(jobExecution);
		//increment
		jobExecution.incrementVersion();
		//get the job instance
		JobInstance instance = gemfireJobInstanceDao.getJobInstance(jobExecution.getJobId());
		//set
		jobExecution.setId(generateId(instance));
		//save
		gemfireJobExecutionDao.save(jobExecution);
	}

	@Override
	public void updateJobExecution(JobExecution jobExecution) {
		gemfireJobExecutionDao.save(jobExecution);
	}

	@Override
	public List<JobExecution> findJobExecutions(JobInstance jobInstance) {
		return gemfireJobExecutionDao.findByJobId(jobInstance.getId());
	}

	public JobExecution getLastJobExecution(Long jobId) {
		//oql
		SelectResults<JobExecution> executions = jobExecutionTemplate.find(LAST_EXECUTION,jobId);
		if (!executions.isEmpty()) {
			//get the 'first' one (latest)
			return executions.asList().get(0);
		}//end if
		//default return
		return null;		
	}
	
	@Override
	public JobExecution getLastJobExecution(JobInstance jobInstance) {
		return this.getLastJobExecution(jobInstance.getId());
	}

	@Override
	public Set<JobExecution> findRunningJobExecutions(String jobName) {
		List<JobExecution> results = gemfireJobExecutionDao.findByEndTimeIsNull();
		if (results != null) {
			//convert and return
			return new HashSet<JobExecution>(results);
		}//end if
		return Collections.emptySet();
	}

	@Override
	public JobExecution getJobExecution(Long executionId) {
		return gemfireJobExecutionDao.findOne(executionId);
	}

	@Override
	public void synchronizeStatus(JobExecution jobExecution) {
		//load the version
		int currentVersion = gemfireJobExecutionDao.findOne(jobExecution.getId()).getVersion().intValue();
		//test
		if (currentVersion != jobExecution.getVersion().intValue()) {
			jobExecution.upgradeStatus(gemfireJobExecutionDao.findOne(jobExecution.getId()).getStatus());
			jobExecution.setVersion(currentVersion);
		}//end if
		
	}

	/**
	 * very basic algorithm to produce execution ids for new job executions based on the parent job instance
	 * @param jobInstance
	 * @return
	 */
	protected Long generateId(JobInstance jobInstance) {
		int count = 1;//default
		//load the jobs
		List<JobExecution> executions = gemfireJobExecutionDao.findJobExecutions(jobInstance);
		//check
		if (executions != null && !executions.isEmpty()) {
			count += executions.size();//add the value
		}//end if
		//create the id
		return jobInstance.getId().longValue() + count;
	}
	
	/**
	 * Validate JobExecution. At a minimum, JobId, StartTime, EndTime, and
	 * Status cannot be null.
	 *
	 * @param jobExecution
	 * @throws IllegalArgumentException
	 */
	private void validateJobExecution(JobExecution jobExecution) {

		Assert.notNull(jobExecution);
		Assert.notNull(jobExecution.getJobId(), "JobExecution Job-Id cannot be null.");
		Assert.notNull(jobExecution.getStatus(), "JobExecution status cannot be null.");
		Assert.notNull(jobExecution.getCreateTime(), "JobExecution create time cannot be null");
	}	
}
