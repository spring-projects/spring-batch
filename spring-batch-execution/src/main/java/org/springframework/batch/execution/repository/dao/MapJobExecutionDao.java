package org.springframework.batch.execution.repository.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;

/**
 * In-memory implementation of {@link JobExecutionDao}.
 * 
 */
public class MapJobExecutionDao implements JobExecutionDao {

	private static Map executionsByJobInstanceId = TransactionAwareProxyFactory.createTransactionalMap();

	private static long currentId;

	public static void clear() {
		executionsByJobInstanceId.clear();
	}

	public int getJobExecutionCount(JobInstance jobInstance) {
		Set executions = (Set) executionsByJobInstanceId.get(jobInstance.getId());
		if (executions == null) {
			return 0;
		}
		return executions.size();
	}

	public void saveJobExecution(JobExecution jobExecution) {
		Set executions = (Set) executionsByJobInstanceId.get(jobExecution.getJobId());
		if (executions == null) {
			executions = TransactionAwareProxyFactory.createTransactionalSet();
			executionsByJobInstanceId.put(jobExecution.getJobId(), executions);
		}
		executions.add(jobExecution);
		jobExecution.setId(new Long(currentId++));
	}

	public List findJobExecutions(JobInstance jobInstance) {
		Set executions = (Set) executionsByJobInstanceId.get(jobInstance.getId());
		if (executions == null) {
			return new ArrayList();
		}
		else {
			return new ArrayList(executions);
		}
	}

	public void updateJobExecution(JobExecution jobExecution) {
		// no-op
	}

	public JobExecution getLastJobExecution(JobInstance jobInstance) {
		Set executions = (Set) executionsByJobInstanceId.get(jobInstance.getId());
		if (executions == null) {
			return null;
		}
		JobExecution lastExec = null;
		for (Iterator iterator = executions.iterator(); iterator.hasNext();) {
			JobExecution exec = (JobExecution) iterator.next();
			if (lastExec == null) {
				lastExec = exec;
			}
			if (lastExec.getStartTime().getTime() < exec.getStartTime().getTime()) {
				lastExec = exec;
			}
		}
		return lastExec;
	}
}
