package org.springframework.batch.core.repository.dao;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.util.Assert;

/**
 * In-memory implementation of {@link JobInstanceDao}.
 */
public class MapJobInstanceDao implements JobInstanceDao {

	private static Collection jobInstances = TransactionAwareProxyFactory.createTransactionalList();

	private long currentId = 0;

	public static void clear() {
		jobInstances.clear();
	}

	public JobInstance createJobInstance(Job job, JobParameters jobParameters) {
		
		Assert.state(getJobInstance(job, jobParameters) == null, "JobInstance must not already exist");
		
		JobInstance jobInstance = new JobInstance(new Long(currentId++), jobParameters, job);
		jobInstance.incrementVersion();
		jobInstances.add(jobInstance);
		
		return jobInstance;
	}

	public JobInstance getJobInstance(Job job, JobParameters jobParameters) {
		
		for (Iterator iterator = jobInstances.iterator(); iterator.hasNext();) {
			JobInstance instance = (JobInstance) iterator.next();
			if (instance.getJobName().equals(job.getName()) && instance.getJobParameters().equals(jobParameters)) {
				return instance;
			}
		}
		return null;
		
	}

}
