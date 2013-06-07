package org.springframework.batch.core.repository.dao.gemfire;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.batch.core.DefaultJobKeyGenerator;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobKeyGenerator;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * GemFire implementation of the JobInstanceDao
 *  (note - naming breaks with the convention of exiting Batch implementations due to Spring Data compliance)
 * 
 * @author Will Schipp
 *
 */
public class GemfireJobInstanceDaoImpl implements JobInstanceDao {

	private JobKeyGenerator<JobParameters> jobKeyGenerator = new DefaultJobKeyGenerator();
	
	@Autowired
	private GemfireJobInstanceDao gemfireJobInstanceDao;
	
	@Override
	public JobInstance createJobInstance(String jobName,JobParameters jobParameters) {
		//generate the key
		String key = jobKeyGenerator.generateKey(jobParameters);
		//get the hash
		int hash = key.hashCode();
		//create the instance
		JobInstance jobInstance = new JobInstance(new Long(hash),jobName);
		//persist it
		gemfireJobInstanceDao.save(jobInstance);
		//return
		return jobInstance;
	}

	@Override
	public JobInstance getJobInstance(String jobName,JobParameters jobParameters) {
		//generate the key
		String key = jobKeyGenerator.generateKey(jobParameters);
		//get the hash
		int hash = key.hashCode();
		//return
		return gemfireJobInstanceDao.findOne(new Long(hash));
	}

	@Override
	public JobInstance getJobInstance(Long instanceId) {
		return gemfireJobInstanceDao.findOne(instanceId);
	}

	@Override
	public JobInstance getJobInstance(JobExecution jobExecution) {
		return gemfireJobInstanceDao.findOne(jobExecution.getJobId());
	}

	@Override
	public List<JobInstance> getJobInstances(String jobName, int start,int count) {
		return gemfireJobInstanceDao.findByJobName(jobName);
	}

	@Override
	public List<String> getJobNames() {
		Iterable<JobInstance> jobInstances = gemfireJobInstanceDao.findAll();
		//setup
		List<String> names = new ArrayList<String>();
		//loop
		for (JobInstance jobInstance :jobInstances) {
			names.add(jobInstance.getJobName());
		}//end for
		//return
		return names;
	}

}
