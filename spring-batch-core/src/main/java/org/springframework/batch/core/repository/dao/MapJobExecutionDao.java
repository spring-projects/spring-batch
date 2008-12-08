package org.springframework.batch.core.repository.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.SerializationUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.util.Assert;

/**
 * In-memory implementation of {@link JobExecutionDao}.
 */
public class MapJobExecutionDao implements JobExecutionDao {

	private static Map<Long, JobExecution> executionsById = TransactionAwareProxyFactory.createTransactionalMap();

	private static long currentId = 0;

	public static void clear() {
		executionsById.clear();
	}

	private static JobExecution copy(JobExecution original) {
		JobExecution copy = (JobExecution) SerializationUtils.deserialize(SerializationUtils.serialize(original));
		return copy;
	}

	public void saveJobExecution(JobExecution jobExecution) {
		Assert.isTrue(jobExecution.getId() == null);
		Long newId = currentId++;
		jobExecution.setId(newId);
		jobExecution.incrementVersion();
		executionsById.put(newId, copy(jobExecution));
	}

	public List<JobExecution> findJobExecutions(JobInstance jobInstance) {
		List<JobExecution> executions = new ArrayList<JobExecution>();
		for (JobExecution exec : executionsById.values()) {
			if (exec.getJobInstance().equals(jobInstance)) {
				executions.add(copy(exec));
			}
		}
		Collections.sort(executions, new Comparator<JobExecution>() {

			public int compare(JobExecution e1, JobExecution e2) {
				long result = (e1.getId() - e2.getId());
				if (result > 0) {
					return -1;
				}
				else if (result < 0) {
					return 1;
				}
				else {
					return 0;
				}
			}
		});
		return executions;
	}

	public void updateJobExecution(JobExecution jobExecution) {
		Long id = jobExecution.getId();
		Assert.notNull(id, "JobExecution is expected to have an id (should be saved already)");
		JobExecution persistedExecution = executionsById.get(id);
		Assert.notNull(persistedExecution, "JobExecution must already be saved");

		synchronized (jobExecution) {
			if (!persistedExecution.getVersion().equals(jobExecution.getVersion())) {
				throw new OptimisticLockingFailureException("Attempt to update step execution id=" + id
						+ " with wrong version (" + jobExecution.getVersion() + "), where current version is "
						+ persistedExecution.getVersion());
			}
			jobExecution.incrementVersion();
			executionsById.put(id, copy(jobExecution));
		}
	}

	public JobExecution getLastJobExecution(JobInstance jobInstance) {
		JobExecution lastExec = null;
		for (JobExecution exec : executionsById.values()) {
			if (!exec.getJobInstance().equals(jobInstance)) {
				continue;
			}
			if (lastExec == null) {
				lastExec = exec;
			}
			if (lastExec.getCreateTime().before(exec.getCreateTime())) {
				lastExec = exec;
			}
		}
		return copy(lastExec);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.springframework.batch.core.repository.dao.JobExecutionDao#
	 * findRunningJobExecutions(java.lang.String)
	 */
	public Set<JobExecution> findRunningJobExecutions(String jobName) {
		Set<JobExecution> result = new HashSet<JobExecution>();
		for (JobExecution exec : executionsById.values()) {
			if (!exec.getJobInstance().getJobName().equals(jobName) || !exec.isRunning()) {
				continue;
			}
			result.add(copy(exec));
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.core.repository.dao.JobExecutionDao#getJobExecution
	 * (java.lang.Long)
	 */
	public JobExecution getJobExecution(Long executionId) {
		return copy(executionsById.get(executionId));
	}

	public void synchronizeStatus(JobExecution jobExecution) {
		JobExecution saved = getJobExecution(jobExecution.getId());
		if (saved.getVersion().intValue() != jobExecution.getVersion().intValue()) {
			jobExecution.setStatus(saved.getStatus());
			jobExecution.setVersion(saved.getVersion());
		}
	}
}
