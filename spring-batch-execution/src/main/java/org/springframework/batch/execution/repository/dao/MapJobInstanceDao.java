package org.springframework.batch.execution.repository.dao;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;

public class MapJobInstanceDao implements JobInstanceDao {

	private static Collection jobInstances = TransactionAwareProxyFactory.createTransactionalList();

	private long currentId = 0;

	public static void clear() {
		jobInstances.clear();
	}

	public JobInstance createJobInstance(Job job, JobParameters jobParameters) {
		
		if (getJobInstance(job, jobParameters) != null) {
			throw new IllegalArgumentException("JobInstance already exists for given job and parameters");
		}
		
		JobInstance jobInstance = new JobInstance(new Long(currentId++), jobParameters, job);
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
