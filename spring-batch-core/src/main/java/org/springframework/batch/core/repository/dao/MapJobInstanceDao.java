package org.springframework.batch.core.repository.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.util.Assert;

/**
 * In-memory implementation of {@link JobInstanceDao}.
 */
public class MapJobInstanceDao implements JobInstanceDao {

	private static Collection<JobInstance> jobInstances = TransactionAwareProxyFactory.createTransactionalSet();

	private static long currentId = 0;

	public static void clear() {
		jobInstances.clear();
	}

	public JobInstance createJobInstance(String jobName, JobParameters jobParameters) {

		Assert.state(getJobInstance(jobName, jobParameters) == null, "JobInstance must not already exist");

		JobInstance jobInstance = new JobInstance(currentId++, jobParameters, jobName);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.core.repository.dao.JobInstanceDao#getJobInstance
	 * (java.lang.Long)
	 */
	public JobInstance getJobInstance(Long instanceId) {
		for (JobInstance instance : jobInstances) {
			if (instance.getId().equals(instanceId)) {
				return instance;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.core.repository.dao.JobInstanceDao#getJobNames
	 * ()
	 */
	public List<String> getJobNames() {
		List<String> result = new ArrayList<String>();
		for (JobInstance instance : jobInstances) {
			result.add(instance.getJobName());
		}
		Collections.sort(result);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.springframework.batch.core.repository.dao.JobInstanceDao#
	 * getLastJobInstances(java.lang.String, int)
	 */
	public List<JobInstance> getLastJobInstances(String jobName, int count) {
		List<JobInstance> result = new ArrayList<JobInstance>();
		for (JobInstance instance : jobInstances) {
			if (instance.getJobName().equals(jobName)) {
				result.add(instance);
			}
		}
		Collections.sort(result, new Comparator<JobInstance>() {
			// sort by ID descending
			public int compare(JobInstance o1, JobInstance o2) {
				return Long.signum(o1.getId() - o2.getId());
			}
		});
		int length = count > result.size() ? result.size() : count;
		return result.subList(0, length);
	}

	public JobInstance getJobInstance(JobExecution jobExecution) {
		return jobExecution.getJobInstance();
	}

}
