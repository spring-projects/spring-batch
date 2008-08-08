package org.springframework.batch.core.repository.dao;

import java.util.Collection;

import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.util.Assert;

/**
 * In-memory implementation of {@link JobInstanceDao}.
 */
public class MapJobInstanceDao implements JobInstanceDao {

	private static Collection<JobInstance> jobInstances = TransactionAwareProxyFactory.createTransactionalList();

	private long currentId = 0;

	public static void clear() {
		jobInstances.clear();
	}

	public JobInstance createJobInstance(String jobName, JobParameters jobParameters) {

		Assert.state(getJobInstance(jobName, jobParameters) == null, "JobInstance must not already exist");

		JobInstance jobInstance = new JobInstance(new Long(currentId++), jobParameters, jobName);
		jobInstance.incrementVersion();
		jobInstances.add(jobInstance);

		return jobInstance;
	}

	public JobInstance getJobInstance(String jobName, JobParameters jobParameters) {

		for (JobInstance instance : jobInstances) {
			if (instance.getJobName().equals(jobName) && instance.getJobParameters().equals(jobParameters)) {
				return instance;
			}
		}
		return null;

	}

}
