package org.springframework.batch.core.repository.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.util.Assert;

/**
 * In-memory implementation of {@link JobExecutionDao}.
 */
public class MapJobExecutionDao implements JobExecutionDao {

	private static Map<Long, JobExecution> executionsById = TransactionAwareProxyFactory.createTransactionalMap();

	private static Map<Long, ExecutionContext> contextsByJobExecutionId = TransactionAwareProxyFactory
			.createTransactionalMap();

	private static long currentId = 0;

	public static void clear() {
		executionsById.clear();
		contextsByJobExecutionId.clear();
	}

	public void saveJobExecution(JobExecution jobExecution) {
		Assert.isTrue(jobExecution.getId() == null);
		Long newId = new Long(currentId++);
		jobExecution.setId(newId);
		jobExecution.incrementVersion();
		executionsById.put(newId, jobExecution);
	}

	public List<JobExecution> findJobExecutions(JobInstance jobInstance) {
		List<JobExecution> executions = new ArrayList<JobExecution>();
		for (JobExecution exec : executionsById.values()) {
			if (exec.getJobInstance().equals(jobInstance)) {
				executions.add(exec);
			}
		}
		return executions;
	}

	public void updateJobExecution(JobExecution jobExecution) {
		Long id = jobExecution.getId();
		Assert.notNull(id, "JobExecution is expected to have an id (should be saved already)");
		Assert.notNull(executionsById.get(id), "JobExecution must already be saved");
		jobExecution.incrementVersion();
		executionsById.put(id, jobExecution);
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
		return lastExec;
	}

	public ExecutionContext findExecutionContext(JobExecution jobExecution) {
		return (ExecutionContext) contextsByJobExecutionId.get(jobExecution.getId());
	}

	public void persistExecutionContext(JobExecution jobExecution) {
		contextsByJobExecutionId.put(jobExecution.getId(), jobExecution.getExecutionContext());

	}
}
