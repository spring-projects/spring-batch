package org.springframework.batch.core.repository.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.util.Assert;

/**
 * In-memory implementation of {@link JobExecutionDao}.
 * 
 */
public class MapJobExecutionDao implements JobExecutionDao {

	private static Map executionsById = TransactionAwareProxyFactory.createTransactionalMap();

	private static Map contextsByJobExecutionId = TransactionAwareProxyFactory.createTransactionalMap();

	private static long currentId = 0;

	public static void clear() {
		executionsById.clear();
		contextsByJobExecutionId.clear();
	}

	public int getJobExecutionCount(JobInstance jobInstance) {
		int count = 0;
		for (Iterator iterator = executionsById.values().iterator(); iterator.hasNext();) {
			JobExecution exec = (JobExecution) iterator.next();
			if (exec.getJobInstance().equals(jobInstance)) {
				count++;
			}
		}
		return count;
	}

	public void saveJobExecution(JobExecution jobExecution) {
		Assert.isTrue(jobExecution.getId() == null);
		Long newId = new Long(currentId++);
		jobExecution.setId(newId);
		jobExecution.incrementVersion();
		executionsById.put(newId, jobExecution);
	}

	public List findJobExecutions(JobInstance jobInstance) {
		List executions = new ArrayList();
		for (Iterator iterator = executionsById.values().iterator(); iterator.hasNext();) {
			JobExecution exec = (JobExecution) iterator.next();
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
		for (Iterator iterator = executionsById.values().iterator(); iterator.hasNext();) {
			JobExecution exec = (JobExecution) iterator.next();
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

	public void saveOrUpdateExecutionContext(JobExecution jobExecution) {
		contextsByJobExecutionId.put(jobExecution.getId(), jobExecution.getExecutionContext());

	}
}
